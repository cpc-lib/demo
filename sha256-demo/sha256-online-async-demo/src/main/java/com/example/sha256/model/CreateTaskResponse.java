package com.example.sha256.model;

public record CreateTaskResponse(
        String taskId,
        String fileName,
        TaskStatus status,
        String statusUrl
) {
}
