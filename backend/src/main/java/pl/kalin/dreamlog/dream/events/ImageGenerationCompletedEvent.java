package pl.kalin.dreamlog.dream.events;

import java.util.UUID;

/**
 * Domain event published when dream image generation successfully completes.
 * Signals that the full AI pipeline is done - frontend can be notified via SSE.
 *
 * Event flow:
 * 1. GenerateImageTask completes successfully
 * 2. Image uploaded to MinIO
 * 3. Dream state updated to COMPLETED
 * 4. This event published
 * 5. SSE broadcaster sends notification to frontend
 */
public record ImageGenerationCompletedEvent(
    UUID dreamId,
    UUID userId,
    String imageUri
) {
    public static ImageGenerationCompletedEvent of(UUID dreamId, UUID userId, String imageUri) {
        return new ImageGenerationCompletedEvent(dreamId, userId, imageUri);
    }
}
