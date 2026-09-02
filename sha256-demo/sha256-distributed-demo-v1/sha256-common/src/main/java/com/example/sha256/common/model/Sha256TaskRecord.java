package com.example.sha256.common.model;

import java.time.Instant;

public class Sha256TaskRecord {
    private String taskId;
    private String originalFilename;
    private String storageKey;
    private String storageBucket;
    private long totalBytes;
    private long processedBytes;
    private int progress;
    private TaskStatus status;
    private String sha256;
    private String error;
    private String broker;
    private int retryCount;
    private Instant createdAt;
    private Instant updatedAt;

    public static Sha256TaskRecord queued(String taskId, String originalFilename, String storageKey,
                                           String storageBucket, long totalBytes, String broker) {
        Sha256TaskRecord record = new Sha256TaskRecord();
        Instant now = Instant.now();
        record.taskId = taskId;
        record.originalFilename = originalFilename;
        record.storageKey = storageKey;
        record.storageBucket = storageBucket;
        record.totalBytes = totalBytes;
        record.processedBytes = 0;
        record.progress = 0;
        record.status = TaskStatus.QUEUED;
        record.broker = broker;
        record.retryCount = 0;
        record.createdAt = now;
        record.updatedAt = now;
        return record;
    }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String storageKey) { this.storageKey = storageKey; }
    public String getStorageBucket() { return storageBucket; }
    public void setStorageBucket(String storageBucket) { this.storageBucket = storageBucket; }
    public long getTotalBytes() { return totalBytes; }
    public void setTotalBytes(long totalBytes) { this.totalBytes = totalBytes; }
    public long getProcessedBytes() { return processedBytes; }
    public void setProcessedBytes(long processedBytes) { this.processedBytes = processedBytes; }
    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public String getSha256() { return sha256; }
    public void setSha256(String sha256) { this.sha256 = sha256; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public String getBroker() { return broker; }
    public void setBroker(String broker) { this.broker = broker; }
    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
