package pl.kalin.dreamlog.dream.tasks;

import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask;
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pl.kalin.dreamlog.dream.ai.port.AiServiceException;
import pl.kalin.dreamlog.dream.ai.port.DreamAnalysisAiService;
import pl.kalin.dreamlog.dream.ai.port.dto.AnalysisResult;
import pl.kalin.dreamlog.dream.events.AnalysisFailedEvent;
import pl.kalin.dreamlog.dream.events.TextAnalysisCompletedEvent;
import pl.kalin.dreamlog.dream.model.DreamAnalysis;
import pl.kalin.dreamlog.dream.model.DreamEntry;
import pl.kalin.dreamlog.dream.model.DreamProcessingState;
import pl.kalin.dreamlog.dream.repository.DreamAnalysisRepository;
import pl.kalin.dreamlog.dream.repository.DreamEntryRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;

/**
 * db-scheduler task for analyzing dream text with AI.
 *
 * Flow:
 * 1. Load dream entry by ID
 * 2. Check if analysis already exists (idempotency)
 * 3. Update state to ANALYZING_TEXT
 * 4. Call AI service to analyze text
 * 5. Save DreamAnalysis entity
 * 6. Update state to TEXT_ANALYZED
 * 7. Publish TextAnalysisCompletedEvent (triggers image generation)
 *
 * Retry: Handled by db-scheduler with exponential backoff
 * After 8 failures: Set state to FAILED, publish AnalysisFailedEvent
 */
@Component
@Slf4j
public class AnalyzeTextTask extends OneTimeTask<DreamTaskData> {

    private static final String TASK_NAME = "analyze-text";
    private static final int MAX_RETRIES = 8;

    private final DreamEntryRepository dreamRepository;
    private final DreamAnalysisRepository analysisRepository;
    private final DreamAnalysisAiService aiService;
    private final ApplicationEventPublisher eventPublisher;

    public AnalyzeTextTask(
            DreamEntryRepository dreamRepository,
            DreamAnalysisRepository analysisRepository,
            DreamAnalysisAiService aiService,
            ApplicationEventPublisher eventPublisher) {
        super(TASK_NAME, DreamTaskData.class);
        this.dreamRepository = dreamRepository;
        this.analysisRepository = analysisRepository;
        this.aiService = aiService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public void executeOnce(TaskInstance<DreamTaskData> taskInstance, ExecutionContext executionContext) {
        DreamTaskData data = taskInstance.getData();
        log.info("Executing text analysis task for dreamId={}", data.dreamId());

        try {
            // Load dream entry
            DreamEntry dream = dreamRepository.findById(data.dreamId())
                .orElseThrow(() -> new IllegalStateException("Dream not found: " + data.dreamId()));

            // Idempotency check: Skip if analysis already exists
            if (analysisRepository.findByDreamId(data.dreamId()).isPresent()) {
                log.info("Analysis already exists for dreamId={}, skipping", data.dreamId());
                return;  // Mark as complete, don't retry
            }

            // Increment retry count
            int currentRetry = dream.getRetryCount() + 1;
            dream.setRetryCount(currentRetry);

            // Check if max retries exceeded
            if (currentRetry > MAX_RETRIES) {
                handleMaxRetriesExceeded(dream, "Text analysis failed after " + MAX_RETRIES + " attempts");
                return;
            }

            // Update state to ANALYZING_TEXT
            dream.setProcessingState(DreamProcessingState.ANALYZING_TEXT);
            dreamRepository.save(dream);

            // Call AI service
            log.debug("Calling AI service for text analysis: dreamId={}", data.dreamId());
            AnalysisResult result = aiService.analyzeText(dream.getContent());

            // Save analysis
            DreamAnalysis analysis = DreamAnalysis.builder()
                .dream(dream)
                .createdAt(LocalDateTime.now())
                .summary(result.summary())
                .tags(new ArrayList<>(result.tags()))
                .entities(new ArrayList<>(result.entities()))
                .emotions(result.emotions())
                .interpretation(result.interpretation())
                .modelVersion(result.modelVersion())
                .build();

            analysisRepository.save(analysis);
            log.info("Analysis saved successfully: dreamId={}, analysisId={}", data.dreamId(), analysis.getId());

            // Update state to TEXT_ANALYZED
            dream.setProcessingState(DreamProcessingState.TEXT_ANALYZED);
            dream.setRetryCount(0);  // Reset retry count on success
            dreamRepository.save(dream);

            // Publish event to trigger image generation
            eventPublisher.publishEvent(TextAnalysisCompletedEvent.of(
                dream.getId(),
                analysis.getId(),
                result.summary()
            ));

            log.info("Text analysis completed successfully for dreamId={}", data.dreamId());

        } catch (AiServiceException e) {
            log.error("AI service error during text analysis for dreamId={}: {}", data.dreamId(), e.getMessage());
            handleFailure(data.dreamId(), e);
            throw e;  // Re-throw to trigger db-scheduler retry
        } catch (Exception e) {
            log.error("Unexpected error during text analysis for dreamId={}", data.dreamId(), e);
            handleFailure(data.dreamId(), e);
            throw new RuntimeException("Text analysis failed", e);
        }
    }

    /**
     * Handles task failure. Logs the error and updates retry count.
     */
    @Transactional
    private void handleFailure(java.util.UUID dreamId, Exception error) {
        DreamEntry dream = dreamRepository.findById(dreamId).orElse(null);
        if (dream == null) {
            log.error("Dream not found during failure handling: {}", dreamId);
            return;
        }

        int currentRetry = dream.getRetryCount();
        log.warn("Text analysis attempt {} failed for dreamId={}: {}",
            currentRetry, dreamId, error.getMessage());

        dreamRepository.save(dream);
    }

    /**
     * Handles max retries exceeded. Marks dream as FAILED.
     */
    @Transactional
    private void handleMaxRetriesExceeded(DreamEntry dream, String reason) {
        log.error("Max retries ({}) exceeded for dreamId={}, marking as FAILED",
            MAX_RETRIES, dream.getId());

        dream.setProcessingState(DreamProcessingState.FAILED);
        dream.setFailureReason(reason);
        dreamRepository.save(dream);

        // Publish failure event
        eventPublisher.publishEvent(AnalysisFailedEvent.of(
            dream.getId(),
            dream.getUser().getId(),
            reason,
            MAX_RETRIES
        ));
    }
}
