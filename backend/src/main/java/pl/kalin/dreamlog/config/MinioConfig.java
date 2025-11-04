package pl.kalin.dreamlog.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO configuration for S3-compatible object storage.
 * Creates MinioClient bean and ensures bucket exists on startup.
 */
@Configuration
@Slf4j
public class MinioConfig {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Bean
    public MinioClient minioClient() {
        log.info("Initializing MinIO client with endpoint: {}", endpoint);
        return MinioClient.builder()
            .endpoint(endpoint)
            .credentials(accessKey, secretKey)
            .build();
    }

    /**
     * Ensures the configured bucket exists on application startup.
     * Creates bucket if it doesn't exist.
     */
    @PostConstruct
    public void ensureBucketExists() {
        try {
            MinioClient client = minioClient();
            boolean exists = client.bucketExists(
                BucketExistsArgs.builder().bucket(bucketName).build()
            );

            if (!exists) {
                log.info("Creating MinIO bucket: {}", bucketName);
                client.makeBucket(
                    MakeBucketArgs.builder().bucket(bucketName).build()
                );
                log.info("MinIO bucket created successfully: {}", bucketName);
            } else {
                log.info("MinIO bucket already exists: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("Failed to ensure MinIO bucket exists: {}", bucketName, e);
            throw new IllegalStateException("MinIO bucket initialization failed", e);
        }
    }
}
