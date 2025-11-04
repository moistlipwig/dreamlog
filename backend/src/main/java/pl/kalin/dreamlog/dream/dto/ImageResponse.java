package pl.kalin.dreamlog.dream.dto;

import java.time.LocalDateTime;

/**
 * Image data in dream response.
 * Nested within DreamResponse when image is available.
 */
public record ImageResponse(
    String uri,              // Presigned URL (valid for 2 hours)
    LocalDateTime generatedAt
) {
    public static ImageResponse from(String imageUri, LocalDateTime generatedAt) {
        if (imageUri == null || imageUri.isBlank()) {
            return null;
        }
        return new ImageResponse(imageUri, generatedAt);
    }
}
