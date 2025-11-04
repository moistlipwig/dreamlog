package pl.kalin.dreamlog.dream.events;

import com.github.kagkarlsson.scheduler.Scheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import pl.kalin.dreamlog.dream.tasks.AnalyzeTextTask;
import pl.kalin.dreamlog.dream.tasks.DreamTaskData;
import pl.kalin.dreamlog.dream.tasks.GenerateImageTask;

import java.time.Instant;

/**
 * Event listener for dream-related domain events.
 * Uses @TransactionalEventListener(AFTER_COMMIT) to ensure events are processed
 * only after the database transaction successfully commits.
 *
 * This prevents scheduling async tasks for uncommitted data.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DreamEventListener {

    private final Scheduler scheduler;
    private final AnalyzeTextTask analyzeTextTask;
    private final GenerateImageTask generateImageTask;

    /**
     * Handles DreamCreatedEvent by scheduling text analysis task.
     * Executed AFTER_COMMIT to ensure dream is persisted before scheduling.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDreamCreated(DreamCreatedEvent event) {
        log.info("Dream created event received for dreamId={}, userId={}",
            event.dreamId(), event.userId());

        // Schedule text analysis task immediately
        scheduler.schedule(
            analyzeTextTask.instance(
                event.dreamId().toString(),
                new DreamTaskData(event.dreamId())
            ),
            Instant.now()
        );

        log.info("Text analysis task scheduled for dreamId={}", event.dreamId());
    }

    /**
     * Handles TextAnalysisCompletedEvent by scheduling image generation task.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTextAnalysisCompleted(TextAnalysisCompletedEvent event) {
        log.info("Text analysis completed for dreamId={}, analysisId={}",
            event.dreamId(), event.analysisId());

        // Schedule image generation task immediately
        scheduler.schedule(
            generateImageTask.instance(
                event.dreamId().toString(),
                new DreamTaskData(event.dreamId())
            ),
            Instant.now()
        );

        log.info("Image generation task scheduled for dreamId={}", event.dreamId());
    }

    /**
     * Handles ImageGenerationCompletedEvent for SSE notifications.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onImageGenerationCompleted(ImageGenerationCompletedEvent event) {
        log.info("Image generation completed for dreamId={}, userId={}, imageUri={}",
            event.dreamId(), event.userId(), event.imageUri());

        // TODO Stage 8: Send SSE notification to frontend
        // sseService.sendDreamCompletedNotification(event.userId(), event.dreamId());

        log.debug("SSE notification will be sent for dreamId={}", event.dreamId());
    }

    /**
     * Handles AnalysisFailedEvent for logging and monitoring.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAnalysisFailed(AnalysisFailedEvent event) {
        log.error("AI processing failed for dreamId={}, userId={}, retries={}, reason={}",
            event.dreamId(), event.userId(), event.retryCount(), event.failureReason());

        // TODO: Add monitoring/alerting (e.g., send to metrics, Sentry, etc.)
        // TODO Stage 8: Optionally notify user via SSE about failure

        log.warn("Dream processing marked as FAILED for dreamId={}", event.dreamId());
    }
}
