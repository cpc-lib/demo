package com.example.sha256.common.storage;

import jakarta.annotation.PostConstruct;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.UploadPartPresignRequest;

import java.io.InputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class S3CompatibleObjectStorageService implements ObjectStorageService {
    private final S3Client s3Client;
    private final S3Presigner presigner;
    private final StorageProperties properties;

    public S3CompatibleObjectStorageService(S3Client s3Client, S3Presigner presigner, StorageProperties properties) {
        this.s3Client = s3Client;
        this.presigner = presigner;
        this.properties = properties;
    }

    @PostConstruct
    void initializeBucket() {
        if (!properties.isAutoCreateBucket()) return;
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(properties.getBucket()).build());
        } catch (S3Exception e) {
            if (e.statusCode() != 404 && e.statusCode() != 403) throw e;
            if (e.statusCode() == 403) return;
            try {
                s3Client.createBucket(CreateBucketRequest.builder().bucket(properties.getBucket()).build());
            } catch (S3Exception createError) {
                if (createError.statusCode() != 409) throw createError;
            }
        }
    }

    @Override
    public void putObject(Path localFile, String storageKey, String contentType) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(storageKey)
                .contentType(normalizeContentType(contentType))
                .metadata(Map.of("storage-key", storageKey))
                .build();
        s3Client.putObject(request, RequestBody.fromFile(localFile));
    }

    @Override
    public InputStream getObject(String storageKey) {
        return s3Client.getObject(GetObjectRequest.builder()
                .bucket(properties.getBucket()).key(storageKey).build());
    }

    @Override
    public void deleteObject(String storageKey) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(properties.getBucket()).key(storageKey).build());
    }

    @Override
    public long objectSize(String storageKey) {
        return s3Client.headObject(HeadObjectRequest.builder()
                .bucket(properties.getBucket()).key(storageKey).build()).contentLength();
    }

    @Override
    public String createMultipartUpload(String storageKey, String contentType) {
        CreateMultipartUploadResponse response = s3Client.createMultipartUpload(
                CreateMultipartUploadRequest.builder()
                        .bucket(properties.getBucket())
                        .key(storageKey)
                        .contentType(normalizeContentType(contentType))
                        .metadata(Map.of("storage-key", storageKey))
                        .build());
        return response.uploadId();
    }

    @Override
    public PresignedPart presignUploadPart(String storageKey, String uploadId, int partNumber, Duration validFor) {
        UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                .bucket(properties.getBucket())
                .key(storageKey)
                .uploadId(uploadId)
                .partNumber(partNumber)
                .build();
        Duration duration = validFor == null ? Duration.ofMinutes(30) : validFor;
        var result = presigner.presignUploadPart(UploadPartPresignRequest.builder()
                .signatureDuration(duration)
                .uploadPartRequest(uploadPartRequest)
                .build());
        return new PresignedPart(partNumber, result.url().toString(), Instant.now().plus(duration));
    }

    @Override
    public List<UploadedPart> listUploadedParts(String storageKey, String uploadId) {
        List<UploadedPart> result = new ArrayList<>();
        Integer marker = null;
        boolean truncated;
        do {
            ListPartsRequest.Builder builder = ListPartsRequest.builder()
                    .bucket(properties.getBucket())
                    .key(storageKey)
                    .uploadId(uploadId)
                    .maxParts(1000);
            if (marker != null) builder.partNumberMarker(marker);
            ListPartsResponse response = s3Client.listParts(builder.build());
            for (Part part : response.parts()) {
                result.add(new UploadedPart(part.partNumber(), part.eTag(), part.size()));
            }
            truncated = Boolean.TRUE.equals(response.isTruncated());
            marker = response.nextPartNumberMarker();
        } while (truncated);
        result.sort(Comparator.comparingInt(UploadedPart::partNumber));
        return result;
    }

    @Override
    public void completeMultipartUpload(String storageKey, String uploadId, List<UploadedPart> parts) {
        List<CompletedPart> completedParts = parts.stream()
                .sorted(Comparator.comparingInt(UploadedPart::partNumber))
                .map(part -> CompletedPart.builder()
                        .partNumber(part.partNumber())
                        .eTag(part.eTag())
                        .build())
                .toList();
        s3Client.completeMultipartUpload(CompleteMultipartUploadRequest.builder()
                .bucket(properties.getBucket())
                .key(storageKey)
                .uploadId(uploadId)
                .multipartUpload(CompletedMultipartUpload.builder().parts(completedParts).build())
                .build());
    }

    @Override
    public void abortMultipartUpload(String storageKey, String uploadId) {
        s3Client.abortMultipartUpload(AbortMultipartUploadRequest.builder()
                .bucket(properties.getBucket())
                .key(storageKey)
                .uploadId(uploadId)
                .build());
    }

    @Override
    public String bucket() { return properties.getBucket(); }

    @Override
    public String provider() { return properties.getProvider(); }

    private String normalizeContentType(String contentType) {
        return contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType;
    }
}
