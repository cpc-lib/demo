package com.example.sha256.common.model;

public record Sha256TaskMessage(
        String taskId,
        String storageKey,
        String originalFilename,
        long totalBytes
) {
}
