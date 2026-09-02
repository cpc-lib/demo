package com.example.sha256.model;

import java.time.Instant;

public class Sha256Task {

    private final String taskId;
    private final String fileName;
    private final Instant createdAt;

    private volatile TaskStatus status;
    private volatile long totalBytes;
    private volatile long processedBytes;
    private volatile String sha256;
    private volatile String error;
    private volatile Instant startedAt;
    private volatile Instant finishedAt;

    public Sha256Task(String taskId, String fileName) {
        this.taskId = taskId;
        this.fileName = fileName;
        this.createdAt = Instant.now();
        this.status = TaskStatus.UPLOADING;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getFileName() {
        return fileName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public long getProcessedBytes() {
        return processedBytes;
    }

    public String getSha256() {
        return sha256;
    }

    public String getError() {
        return error;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public int getProgress() {
        if (status == TaskStatus.SUCCESS) {
            return 100;
        }
        if (totalBytes <= 0) {
            return status == TaskStatus.RUNNING ? 1 : 0;
        }
        return (int) Math.min(99, processedBytes * 100L / totalBytes);
    }

    public void markQueued() {
        this.status = TaskStatus.QUEUED;
    }

    public void markRunning(long totalBytes) {
        this.totalBytes = totalBytes;
        this.processedBytes = 0;
        this.startedAt = Instant.now();
        this.status = TaskStatus.RUNNING;
    }

    public void updateProcessedBytes(long processedBytes) {
        this.processedBytes = processedBytes;
    }

    public void markSuccess(String sha256) {
        this.sha256 = sha256;
        this.processedBytes = this.totalBytes;
        this.finishedAt = Instant.now();
        this.status = TaskStatus.SUCCESS;
    }

    public void markFailed(String error) {
        this.error = error;
        this.finishedAt = Instant.now();
        this.status = TaskStatus.FAILED;
    }
}
