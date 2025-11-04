package pl.kalin.dreamlog.dream.tasks;

import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay;
import lombok.RequiredArgsConstructor;
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

import java.time.Duration;
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
 * Retry: 15min intervals, max 8 attempts
 * After 8 failures: Set state to FAILED, publish AnalysisFailedEvent
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AnalyzeTextTask extends RecurringTask<DreamTaskData> {

    private static final String TASK_NAME = "analyze-text";
    private static final int MAX_RETRIES = 8;

    private final DreamEntryRepository dreamRepository;
    private final DreamAnalysisRepository analysisRepository;
    private final DreamAnalysisAiService aiService;
    private final ApplicationEventPublisher eventPublisher;

    public AnalyzeTextTask() {
        super(
            TASK_NAME,
            FixedDelay.of(Duration.ofMinutes(15)),  // Retry every 15 minutes on failure
            DreamTaskData.class
        );
    }

    @Override
    @Transactional
    public void executeRecurringly(TaskInstance<DreamTaskData> taskInstance, ExecutionContext executionContext) {
        DreamTaskData data = taskInstance.getData();
        log.info("Executing text analysis task for dreamId={}, execution={}",
            data.dreamId(), executionContext.getExecutionAttempts());

        try {
            // Load dream entry
            DreamEntry dream = dreamRepository.findById(data.dreamId())
                .orElseThrow(() -> new IllegalStateException("Dream not found: " + data.dreamId()));

            // Idempotency check: Skip if analysis already exists
            if (analysisRepository.findByDreamId(data.dreamId()).isPresent()) {
                log.info("Analysis already exists for dreamId={}, skipping", data.dreamId());
                return;  // Mark as complete, don't retry
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
            handleFailure(data.dreamId(), executionContext.getExecutionAttempts(), e);
            throw e;  // Re-throw to trigger db-scheduler retry
        } catch (Exception e) {
            log.error("Unexpected error during text analysis for dreamId={}", data.dreamId(), e);
            handleFailure(data.dreamId(), executionContext.getExecutionAttempts(), e);
            throw new RuntimeException("Text analysis failed", e);
        }
    }

    /**
     * Handles task failure. After MAX_RETRIES, marks dream as FAILED.
     */
    @Transactional
    private void handleFailure(java.util.UUID dreamId, int attemptNumber, Exception error) {
        log.warn("Text analysis attempt {} failed for dreamId={}: {}",
            attemptNumber, dreamId, error.getMessage());

        DreamEntry dream = dreamRepository.findById(dreamId).orElse(null);
        if (dream == null) {
            log.error("Dream not found during failure handling: {}", dreamId);
            return;
        }

        dream.setRetryCount(attemptNumber);

        if (attemptNumber >= MAX_RETRIES) {
            log.error("Max retries ({}) exceeded for dreamId={}, marking as FAILED", MAX_RETRIES, dreamId);
            dream.setProcessingState(DreamProcessingState.FAILED);
            dream.setFailureReason("Text analysis failed after " + MAX_RETRIES + " attempts: " + error.getMessage());
            dreamRepository.save(dream);

            // Publish failure event
            eventPublisher.publishEvent(AnalysisFailedEvent.of(
                dreamId,
                dream.getUser().getId(),
                error.getMessage(),
                attemptNumber
            ));
        } else {
            log.info("Will retry text analysis for dreamId={}, attempt {}/{}",
                dreamId, attemptNumber + 1, MAX_RETRIES);
            dreamRepository.save(dream);
        }
    }
}
