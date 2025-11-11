package pl.kalin.dreamlog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Initializes MinIO bucket on application startup.
 * Separated from MinioConfig to avoid circular dependency issues.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class MinioBucketInitializer {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${minio.init-bucket:true}")
    private boolean initBucket;

    /**
     * Ensures the configured bucket exists on application startup.
     * Creates bucket if it doesn't exist.
     * <p>
     * Uses @EventListener(ApplicationReadyEvent) to ensure MinioClient bean is fully initialized.
     * Skipped if minio.init-bucket=false (e.g., in test environments).
     */
    @EventListener(ApplicationReadyEvent.class)
    public void ensureBucketExists() {
        if (!initBucket) {
            log.info("MinIO bucket initialization disabled (minio.init-bucket=false)");
            return;
        }

        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build()
            );

            if (!exists) {
                log.info("Creating MinIO bucket: {}", bucketName);
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucketName).build()
                );
                log.info("MinIO bucket created successfully: {}", bucketName);
            } else {
                log.info("MinIO bucket already exists: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("Failed to ensure MinIO bucket exists: {}, error: {}", bucketName, e.getMessage());
            // Don't throw exception - allow app to start even if MinIO is unavailable
            // This is important for test environments where MinIO is mocked
            log.warn("MinIO bucket initialization failed, but application will continue. " +
                    "Ensure MinIO is running and accessible, or set minio.init-bucket=false to disable this check.");
        }
    }
}
