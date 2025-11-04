package pl.kalin.dreamlog.dream.storage.port.dto;

/**
 * Information about a successfully stored image.
 * Returned by ImageStorageService.store() method.
 */
public record StoredImageInfo(
    String storageKey,          // Object key in storage (e.g., "dreams/2025/01/abc-123.jpg")
    String presignedUrl,        // Presigned URL for accessing image (valid for configured duration)
    long sizeBytes              // Image size in bytes (for monitoring)
) {
}
