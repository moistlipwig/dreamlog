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
import pl.kalin.dreamlog.dream.ai.port.dto.ImageGenerationResult;
import pl.kalin.dreamlog.dream.events.AnalysisFailedEvent;
import pl.kalin.dreamlog.dream.events.ImageGenerationCompletedEvent;
import pl.kalin.dreamlog.dream.model.DreamAnalysis;
import pl.kalin.dreamlog.dream.model.DreamEntry;
import pl.kalin.dreamlog.dream.model.DreamProcessingState;
import pl.kalin.dreamlog.dream.repository.DreamAnalysisRepository;
import pl.kalin.dreamlog.dream.repository.DreamEntryRepository;
import pl.kalin.dreamlog.dream.storage.port.ImageStorageService;
import pl.kalin.dreamlog.dream.storage.port.StorageException;
import pl.kalin.dreamlog.dream.storage.port.dto.StoredImageInfo;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * db-scheduler task for generating dream images with AI.
 *
 * Flow:
 * 1. Load dream entry + analysis
 * 2. Check if image already exists (idempotency)
 * 3. Update state to GENERATING_IMAGE
 * 4. Call AI service to generate image
 * 5. Upload image to storage (MinIO)
 * 6. Save image URI to dream_entry
 * 7. Update state to COMPLETED
 * 8. Publish ImageGenerationCompletedEvent (triggers SSE notification)
 *
 * Retry: 15min intervals, max 8 attempts
 * After 8 failures: Set state to FAILED, publish AnalysisFailedEvent
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GenerateImageTask extends RecurringTask<DreamTaskData> {

    private static final String TASK_NAME = "generate-image";
    private static final int MAX_RETRIES = 8;

    private final DreamEntryRepository dreamRepository;
    private final DreamAnalysisRepository analysisRepository;
    private final DreamAnalysisAiService aiService;
    private final ImageStorageService storageService;
    private final ApplicationEventPublisher eventPublisher;

    public GenerateImageTask() {
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
        log.info("Executing image generation task for dreamId={}, execution={}",
            data.dreamId(), executionContext.getExecutionAttempts());

        try {
            // Load dream entry
            DreamEntry dream = dreamRepository.findById(data.dreamId())
                .orElseThrow(() -> new IllegalStateException("Dream not found: " + data.dreamId()));

            // Idempotency check: Skip if image already exists
            if (dream.getImageUri() != null && !dream.getImageUri().isBlank()) {
                log.info("Image already exists for dreamId={}, skipping", data.dreamId());
                return;  // Mark as complete, don't retry
            }

            // Load analysis (required for image prompt)
            DreamAnalysis analysis = analysisRepository.findByDreamId(data.dreamId())
                .orElseThrow(() -> new IllegalStateException("Analysis not found for dream: " + data.dreamId()));

            // Update state to GENERATING_IMAGE
            dream.setProcessingState(DreamProcessingState.GENERATING_IMAGE);
            dreamRepository.save(dream);

            // Call AI service to generate image
            log.debug("Calling AI service for image generation: dreamId={}", data.dreamId());
            ImageGenerationResult result = aiService.generateImage(analysis.getSummary());

            // Upload image to storage
            String filename = result.suggestFilename("dream-" + data.dreamId().toString().substring(0, 8));
            log.debug("Uploading generated image to storage: dreamId={}, filename={}, size={} bytes",
                data.dreamId(), filename, result.imageData().length);

            StoredImageInfo storedImage = storageService.store(
                result.imageData(),
                filename,
                result.mimeType()
            );

            log.info("Image uploaded successfully: dreamId={}, storageKey={}, size={} bytes",
                data.dreamId(), storedImage.storageKey(), storedImage.sizeBytes());

            // Update dream with image info
            dream.setImageUri(storedImage.presignedUrl());
            dream.setImageStorageKey(storedImage.storageKey());
            dream.setImageGeneratedAt(LocalDateTime.now());
            dream.setProcessingState(DreamProcessingState.COMPLETED);
            dream.setRetryCount(0);  // Reset retry count on success
            dreamRepository.save(dream);

            // Publish completion event (for SSE notification)
            eventPublisher.publishEvent(ImageGenerationCompletedEvent.of(
                dream.getId(),
                dream.getUser().getId(),
                storedImage.presignedUrl()
            ));

            log.info("Image generation completed successfully for dreamId={}", data.dreamId());

        } catch (AiServiceException | StorageException e) {
            handleFailure(data.dreamId(), executionContext.getExecutionAttempts(), e);
            throw e;  // Re-throw to trigger db-scheduler retry
        } catch (Exception e) {
            log.error("Unexpected error during image generation for dreamId={}", data.dreamId(), e);
            handleFailure(data.dreamId(), executionContext.getExecutionAttempts(), e);
            throw new RuntimeException("Image generation failed", e);
        }
    }

    /**
     * Handles task failure. After MAX_RETRIES, marks dream as FAILED.
     */
    @Transactional
    private void handleFailure(java.util.UUID dreamId, int attemptNumber, Exception error) {
        log.warn("Image generation attempt {} failed for dreamId={}: {}",
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
            dream.setFailureReason("Image generation failed after " + MAX_RETRIES + " attempts: " + error.getMessage());
            dreamRepository.save(dream);

            // Publish failure event
            eventPublisher.publishEvent(AnalysisFailedEvent.of(
                dreamId,
                dream.getUser().getId(),
                error.getMessage(),
                attemptNumber
            ));
        } else {
            log.info("Will retry image generation for dreamId={}, attempt {}/{}",
                dreamId, attemptNumber + 1, MAX_RETRIES);
            dreamRepository.save(dream);
        }
    }
}
