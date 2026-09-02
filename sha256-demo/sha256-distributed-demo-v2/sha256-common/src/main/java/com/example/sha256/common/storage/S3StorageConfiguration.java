package com.example.sha256.common.storage;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class S3StorageConfiguration {

    @Bean
    S3Client sha256S3Client(StorageProperties properties) {
        var builder = S3Client.builder()
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(credentials(properties))
                .serviceConfiguration(s3Configuration(properties));

        if (hasText(properties.getEndpoint())) {
            builder.endpointOverride(URI.create(properties.getEndpoint()));
        }
        return builder.build();
    }

    @Bean
    S3Presigner sha256S3Presigner(StorageProperties properties) {
        var builder = S3Presigner.builder()
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(credentials(properties))
                .serviceConfiguration(s3Configuration(properties));

        String signingEndpoint = hasText(properties.getPublicEndpoint())
                ? properties.getPublicEndpoint() : properties.getEndpoint();
        if (hasText(signingEndpoint)) {
            builder.endpointOverride(URI.create(signingEndpoint));
        }
        return builder.build();
    }

    @Bean
    ObjectStorageService objectStorageService(S3Client sha256S3Client,
                                              S3Presigner sha256S3Presigner,
                                              StorageProperties properties) {
        return new S3CompatibleObjectStorageService(sha256S3Client, sha256S3Presigner, properties);
    }

    private AwsCredentialsProvider credentials(StorageProperties properties) {
        if (hasText(properties.getAccessKey())) {
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey()));
        }
        return DefaultCredentialsProvider.create();
    }

    private S3Configuration s3Configuration(StorageProperties properties) {
        return S3Configuration.builder()
                .pathStyleAccessEnabled(properties.isPathStyleAccess())
                .chunkedEncodingEnabled(properties.isChunkedEncoding())
                .build();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
