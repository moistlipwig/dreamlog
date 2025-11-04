package pl.kalin.dreamlog.dream.storage.port;

import pl.kalin.dreamlog.dream.storage.port.dto.StoredImageInfo;

/**
 * Port (interface) for image storage operations.
 * Follows Hexagonal Architecture - domain defines what it needs, adapter provides implementation.
 *
 * Implementations:
 * - MinioImageStorageAdapter (primary adapter, S3-compatible)
 * - Future: AwsS3Adapter, AzureBlobAdapter, LocalFilesystemAdapter
 *
 * Benefits:
 * - Swappable storage backends without changing domain code
 * - Easy mocking in unit tests
 * - No infrastructure lock-in
 */
public interface ImageStorageService {

    /**
     * Stores image in object storage and returns storage metadata.
     *
     * @param imageData raw image bytes (JPEG format)
     * @param filename suggested filename (e.g., "dream-abc-123.jpg")
     * @param contentType MIME type (e.g., "image/jpeg")
     * @return storage info with object key and presigned URL
     * @throws StorageException if upload fails
     */
    StoredImageInfo store(byte[] imageData, String filename, String contentType);

    /**
     * Generates a presigned URL for accessing stored image.
     * URL is valid for configured duration (default: 2 hours).
     *
     * @param storageKey the object key returned from store()
     * @return presigned URL (public, temporary access)
     * @throws StorageException if key doesn't exist or operation fails
     */
    String getPresignedUrl(String storageKey);

    /**
     * Deletes image from storage.
     * Used for cleanup or when regenerating images.
     *
     * @param storageKey the object key to delete
     * @throws StorageException if deletion fails
     */
    void delete(String storageKey);
}
