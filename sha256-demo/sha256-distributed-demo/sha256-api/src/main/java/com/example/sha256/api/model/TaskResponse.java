package com.example.sha256.api.model;

import com.example.sha256.common.model.Sha256TaskRecord;
import com.example.sha256.common.model.TaskStatus;

import java.time.Instant;

public record TaskResponse(
        String taskId,
        String originalFilename,
        long totalBytes,
        long processedBytes,
        int progress,
        TaskStatus status,
        String sha256,
        String error,
        String broker,
        Instant createdAt,
        Instant updatedAt
) {
    public static TaskResponse from(Sha256TaskRecord record) {
        return new TaskResponse(
                record.getTaskId(), record.getOriginalFilename(), record.getTotalBytes(),
                record.getProcessedBytes(), record.getProgress(), record.getStatus(),
                record.getSha256(), record.getError(), record.getBroker(),
                record.getCreatedAt(), record.getUpdatedAt()
        );
    }
}
