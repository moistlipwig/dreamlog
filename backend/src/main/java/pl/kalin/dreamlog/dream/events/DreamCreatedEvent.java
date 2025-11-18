package pl.kalin.dreamlog.dream.events;

import java.util.UUID;

/**
 * Domain event published AFTER_COMMIT when a new dream is successfully created.
 * Triggers the async AI analysis pipeline: text analysis → image generation.
 *
 * Event flow:
 * 1. POST /api/dreams → DreamService.createDream()
 * 2. Dream saved with state=CREATED
 * 3. @TransactionalEventListener(AFTER_COMMIT) receives this event
 * 4. Schedules AnalyzeTextTask via db-scheduler
 */
public record DreamCreatedEvent(
    UUID dreamId,
    UUID userId,
    String dreamContent
) {
    public static DreamCreatedEvent of(UUID dreamId, UUID userId, String content) {
        return new DreamCreatedEvent(dreamId, userId, content);
    }
}
