package com.example.sha256.model;

import java.time.Instant;

public record TaskResponse(
        String taskId,
        String fileName,
        TaskStatus status,
        long totalBytes,
        long processedBytes,
        int progress,
        String sha256,
        String error,
        Instant createdAt,
        Instant startedAt,
        Instant finishedAt
) {
    public static TaskResponse from(Sha256Task task) {
        return new TaskResponse(
                task.getTaskId(),
                task.getFileName(),
                task.getStatus(),
                task.getTotalBytes(),
                task.getProcessedBytes(),
                task.getProgress(),
                task.getSha256(),
                task.getError(),
                task.getCreatedAt(),
                task.getStartedAt(),
                task.getFinishedAt()
        );
    }
}
