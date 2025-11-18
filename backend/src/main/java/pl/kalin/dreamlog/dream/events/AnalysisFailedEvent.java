package pl.kalin.dreamlog.dream.events;

import java.util.UUID;

/**
 * Domain event published when AI processing fails after max retries (8 attempts).
 * Triggers state update to FAILED and optionally notifies user.
 *
 * Event flow:
 * 1. Task (AnalyzeTextTask or GenerateImageTask) fails for 8th time
 * 2. Dream state updated to FAILED
 * 3. This event published
 * 4. Can trigger notifications, alerts, or monitoring events
 */
public record AnalysisFailedEvent(
    UUID dreamId,
    UUID userId,
    String failureReason,
    int retryCount
) {
    public static AnalysisFailedEvent of(UUID dreamId, UUID userId, String reason, int retries) {
        return new AnalysisFailedEvent(dreamId, userId, reason, retries);
    }
}
