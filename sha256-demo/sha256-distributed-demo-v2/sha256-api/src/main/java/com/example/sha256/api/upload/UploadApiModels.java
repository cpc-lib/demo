package com.example.sha256.api.upload;

import java.time.Instant;
import java.util.List;

public final class UploadApiModels {
    private UploadApiModels() { }

    public record InitRequest(String fileName, long fileSize, long lastModified, String contentType, String fingerprint) { }

    public record UploadedPartView(int partNumber, long size) { }

    public record InitResponse(
            String sessionId,
            String storageKey,
            long partSize,
            int totalParts,
            int recommendedConcurrency,
            List<UploadedPartView> uploadedParts,
            long uploadedBytes,
            boolean resumed,
            String taskId
    ) { }

    public record PresignRequest(List<Integer> partNumbers) { }
    public record PresignedPartView(int partNumber, String url, Instant expiresAt) { }
    public record PresignResponse(List<PresignedPartView> parts) { }
    public record CompleteResponse(String taskId, String status, String broker) { }
}
