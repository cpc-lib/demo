package com.example.vocab.infrastructure.storage;

import com.example.vocab.config.MinioProperties;
import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ObjectStorageService {
    private final MinioClient minioClient;
    private final MinioProperties properties;

    public String putText(String objectName, String content, String contentType) {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(properties.getBucket()).build());
            if (!exists) minioClient.makeBucket(MakeBucketArgs.builder().bucket(properties.getBucket()).build());
            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(objectName)
                    .contentType(contentType)
                    .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
                    .build());
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(properties.getBucket())
                    .object(objectName)
                    .expiry(7, TimeUnit.DAYS)
                    .build());
        } catch (Exception e) {
            throw new IllegalStateException("upload export file failed: " + e.getMessage(), e);
        }
    }
}
