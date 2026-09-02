package com.example.sha256.api.upload;

import java.time.Instant;

public record UploadSession(
        String sessionId,
        String fingerprint,
        String uploadId,
        String storageKey,
        String originalFilename,
        String contentType,
        long fileSize,
        long lastModified,
        long partSize,
        int totalParts,
        String completedTaskId,
        Instant createdAt,
        Instant updatedAt
) {
    public UploadSession complete(String taskId) {
        return new UploadSession(sessionId, fingerprint, uploadId, storageKey, originalFilename, contentType,
                fileSize, lastModified, partSize, totalParts, taskId, createdAt, Instant.now());
    }
}
