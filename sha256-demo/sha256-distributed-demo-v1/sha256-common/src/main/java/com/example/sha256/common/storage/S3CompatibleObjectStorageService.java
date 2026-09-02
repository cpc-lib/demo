package com.example.sha256.common.storage;

import jakarta.annotation.PostConstruct;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;

public class S3CompatibleObjectStorageService implements ObjectStorageService {
    private final S3Client s3Client;
    private final StorageProperties properties;

    public S3CompatibleObjectStorageService(S3Client s3Client, StorageProperties properties) {
        this.s3Client = s3Client;
        this.properties = properties;
    }

    @PostConstruct
    void initializeBucket() {
        if (!properties.isAutoCreateBucket()) return;
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(properties.getBucket()).build());
        } catch (S3Exception e) {
            if (e.statusCode() != 404 && e.statusCode() != 403) throw e;
            if (e.statusCode() == 403) {
                // S3/OSS production accounts may allow object access but not bucket creation.
                return;
            }
            try {
                s3Client.createBucket(CreateBucketRequest.builder().bucket(properties.getBucket()).build());
            } catch (S3Exception createError) {
                // Multiple API/Worker instances may race to create the same bucket.
                if (createError.statusCode() != 409) throw createError;
            }
        }
    }

    @Override
    public void putObject(Path localFile, String storageKey, String contentType) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(storageKey)
                .contentType(contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType)
                .metadata(Map.of("storage-key", storageKey))
                .build();
        s3Client.putObject(request, RequestBody.fromFile(localFile));
    }

    @Override
    public InputStream getObject(String storageKey) {
        return s3Client.getObject(GetObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(storageKey)
                .build());
    }

    @Override
    public void deleteObject(String storageKey) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(storageKey)
                .build());
    }

    @Override
    public String bucket() {
        return properties.getBucket();
    }

    @Override
    public String provider() {
        return properties.getProvider();
    }
}
