package pl.kalin.dreamlog.dream.storage.adapter;

import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pl.kalin.dreamlog.dream.storage.port.ImageStorageService;
import pl.kalin.dreamlog.dream.storage.port.StorageException;
import pl.kalin.dreamlog.dream.storage.port.dto.StoredImageInfo;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * MinIO adapter for ImageStorageService port.
 * Stores dream images in S3-compatible object storage.
 *
 * Object key format: dreams/{year}/{month}/{uuid}.jpg
 * Example: dreams/2025/01/abc-123-def.jpg
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MinioImageStorageAdapter implements ImageStorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${minio.url-expiry-seconds:7200}")  // Default: 2 hours
    private int urlExpirySeconds;

    @Override
    public StoredImageInfo store(byte[] imageData, String filename, String contentType) {
        try {
            // Generate unique storage key with date-based prefix
            String storageKey = generateStorageKey(filename);

            log.debug("Uploading image to MinIO: bucket={}, key={}, size={} bytes",
                bucketName, storageKey, imageData.length);

            // Upload to MinIO
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(storageKey)
                    .stream(new ByteArrayInputStream(imageData), imageData.length, -1)
                    .contentType(contentType)
                    .build()
            );

            // Generate presigned URL
            String presignedUrl = getPresignedUrl(storageKey);

            log.info("Image uploaded successfully: key={}, size={} bytes", storageKey, imageData.length);

            return new StoredImageInfo(storageKey, presignedUrl, imageData.length);

        } catch (Exception e) {
            log.error("Failed to upload image to MinIO: filename={}", filename, e);
            throw StorageException.uploadFailed(filename, e);
        }
    }

    @Override
    public String getPresignedUrl(String storageKey) {
        try {
            String url = minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucketName)
                    .object(storageKey)
                    .expiry(urlExpirySeconds, TimeUnit.SECONDS)
                    .build()
            );

            log.debug("Generated presigned URL for key={}, expiry={}s", storageKey, urlExpirySeconds);
            return url;

        } catch (Exception e) {
            log.error("Failed to generate presigned URL for key={}", storageKey, e);
            throw new StorageException("Failed to generate presigned URL for: " + storageKey, e);
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(storageKey)
                    .build()
            );

            log.info("Image deleted successfully: key={}", storageKey);

        } catch (Exception e) {
            log.error("Failed to delete image from MinIO: key={}", storageKey, e);
            throw StorageException.deleteFailed(storageKey, e);
        }
    }

    /**
     * Generates storage key with date-based prefix for organization.
     * Format: dreams/{year}/{month}/{uuid}_{filename}
     */
    private String generateStorageKey(String filename) {
        LocalDate now = LocalDate.now();
        String uuid = UUID.randomUUID().toString().substring(0, 8);  // Short UUID
        return String.format("dreams/%d/%02d/%s_%s",
            now.getYear(),
            now.getMonthValue(),
            uuid,
            sanitizeFilename(filename)
        );
    }

    /**
     * Sanitizes filename to remove special characters.
     */
    private String sanitizeFilename(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
