package cc.ivera.ragdemo.config;

import io.minio.MinioClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO client bean configuration.
 * <p>
 * Activated only when {@code rag.object-storage.type=minio}.
 */
@Configuration
@ConditionalOnProperty(name = "rag.object-storage.type", havingValue = "minio")
public class MinioConfig {

    @Bean
    public MinioClient minioClient(RagProperties properties) {
        RagProperties.ObjectStorage config = properties.getObjectStorage();
        return MinioClient.builder()
                .endpoint(config.getEndpoint())
                .credentials(config.getAccessKey(), config.getSecretKey())
                .build();
    }
}
