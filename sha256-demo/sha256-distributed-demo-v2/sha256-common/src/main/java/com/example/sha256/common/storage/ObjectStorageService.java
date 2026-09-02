package com.example.sha256.common.storage;

import java.io.InputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public interface ObjectStorageService {
    void putObject(Path localFile, String storageKey, String contentType);
    InputStream getObject(String storageKey);
    void deleteObject(String storageKey);
    long objectSize(String storageKey);

    String createMultipartUpload(String storageKey, String contentType);
    PresignedPart presignUploadPart(String storageKey, String uploadId, int partNumber, Duration validFor);
    List<UploadedPart> listUploadedParts(String storageKey, String uploadId);
    void completeMultipartUpload(String storageKey, String uploadId, List<UploadedPart> parts);
    void abortMultipartUpload(String storageKey, String uploadId);

    String bucket();
    String provider();

    record PresignedPart(int partNumber, String url, Instant expiresAt) { }
    record UploadedPart(int partNumber, String eTag, long size) { }
}
