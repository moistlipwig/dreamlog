package pl.kalin.dreamlog.dream.storage.port;

/**
 * Exception thrown when storage operations fail.
 * Wraps underlying errors (network failures, permission errors, quota limits).
 */
public class StorageException extends RuntimeException {

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Factory method for upload failures.
     */
    public static StorageException uploadFailed(String filename, Throwable cause) {
        return new StorageException("Failed to upload image: " + filename, cause);
    }

    /**
     * Factory method for delete failures.
     */
    public static StorageException deleteFailed(String storageKey, Throwable cause) {
        return new StorageException("Failed to delete image: " + storageKey, cause);
    }

    /**
     * Factory method for missing object errors.
     */
    public static StorageException objectNotFound(String storageKey) {
        return new StorageException("Image not found in storage: " + storageKey);
    }
}
