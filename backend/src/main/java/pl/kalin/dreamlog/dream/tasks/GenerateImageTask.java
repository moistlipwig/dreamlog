package pl.kalin.dreamlog.dream.tasks;

import java.time.LocalDateTime;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask;

import lombok.extern.slf4j.Slf4j;
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

/**
 * db-scheduler task for generating dream images with AI.
 * <p>
 * Flow:
 * 1. Load dream entry + analysis
 * 2. Check if image already exists (idempotency)
 * 3. Update state to GENERATING_IMAGE
 * 4. Call AI service to generate image
 * 5. Upload image to storage (MinIO)
 * 6. Save image URI to dream_entry
 * 7. Update state to COMPLETED
 * 8. Publish ImageGenerationCompletedEvent (triggers SSE notification)
 * <p>
 * Retry: Handled by db-scheduler with exponential backoff
 * After 8 failures: Set state to FAILED, publish AnalysisFailedEvent
 */
@Component
@Slf4j
public class GenerateImageTask extends OneTimeTask<DreamTaskData> {

    private static final String TASK_NAME = "generate-image";
    private static final int MAX_RETRIES = 8;

    private final DreamEntryRepository dreamRepository;
    private final DreamAnalysisRepository analysisRepository;
    private final DreamAnalysisAiService aiService;
    private final ImageStorageService storageService;
    private final ApplicationEventPublisher eventPublisher;

    public GenerateImageTask(
        DreamEntryRepository dreamRepository,
        DreamAnalysisRepository analysisRepository,
        DreamAnalysisAiService aiService,
        ImageStorageService storageService,
        ApplicationEventPublisher eventPublisher) {
        super(TASK_NAME, DreamTaskData.class);
        this.dreamRepository = dreamRepository;
        this.analysisRepository = analysisRepository;
        this.aiService = aiService;
        this.storageService = storageService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public void executeOnce(TaskInstance<DreamTaskData> taskInstance, ExecutionContext executionContext) {
        DreamTaskData data = taskInstance.getData();
        log.info("Executing image generation task for dreamId={}", data.dreamId());

        try {
            // Load dream entry
            DreamEntry dream = dreamRepository.findById(data.dreamId())
                .orElseThrow(() -> new IllegalStateException("Dream not found: " + data.dreamId()));

            // Idempotency check: Skip if image already exists
            if (dream.getImageUri() != null && !dream.getImageUri().isBlank()) {
                log.info("Image already exists for dreamId={}, skipping", data.dreamId());
                return;  // Mark as complete, don't retry
            }

            // Increment retry count
            int currentRetry = dream.getRetryCount() + 1;
            dream.setRetryCount(currentRetry);

            // Check if max retries exceeded
            if (currentRetry > MAX_RETRIES) {
                handleMaxRetriesExceeded(dream, "Image generation failed after " + MAX_RETRIES + " attempts");
                return;
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
            log.error("Service error during image generation for dreamId={}: {}", data.dreamId(), e.getMessage());
            handleFailure(data.dreamId(), e);
            throw e;  // Re-throw to trigger db-scheduler retry
        } catch (Exception e) {
            log.error("Unexpected error during image generation for dreamId={}", data.dreamId(), e);
            handleFailure(data.dreamId(), e);
            throw new RuntimeException("Image generation failed", e);
        }
    }

    /**
     * Handles task failure. Logs the error and updates retry count.
     */
    @Transactional
    void handleFailure(java.util.UUID dreamId, Exception error) {
        DreamEntry dream = dreamRepository.findById(dreamId).orElse(null);
        if (dream == null) {
            log.error("Dream not found during failure handling: {}", dreamId);
            return;
        }

        int currentRetry = dream.getRetryCount();
        log.warn("Image generation attempt {} failed for dreamId={}: {}",
            currentRetry, dreamId, error.getMessage());

        dreamRepository.save(dream);
    }

    /**
     * Handles max retries exceeded. Marks dream as FAILED.
     */
    @Transactional
    void handleMaxRetriesExceeded(DreamEntry dream, String reason) {
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
