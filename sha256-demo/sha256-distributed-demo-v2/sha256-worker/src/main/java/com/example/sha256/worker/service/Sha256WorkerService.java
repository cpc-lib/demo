package com.example.sha256.worker.service;

import com.example.sha256.common.model.ClaimResult;
import com.example.sha256.common.model.Sha256TaskMessage;
import com.example.sha256.common.repository.RedisTaskRepository;
import com.example.sha256.common.storage.ObjectStorageService;
import com.example.sha256.common.storage.StorageProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class Sha256WorkerService {
    private static final Logger log = LoggerFactory.getLogger(Sha256WorkerService.class);
    private static final int BUFFER_SIZE = 1024 * 1024;

    private final RedisTaskRepository taskRepository;
    private final ObjectStorageService objectStorage;
    private final long progressStepBytes;
    private final boolean deleteAfterSuccess;

    public Sha256WorkerService(RedisTaskRepository taskRepository,
                               ObjectStorageService objectStorage,
                               StorageProperties storageProperties,
                               @Value("${sha256.worker.progress-step-mb:8}") long progressStepMb) {
        this.taskRepository = taskRepository;
        this.objectStorage = objectStorage;
        this.progressStepBytes = Math.max(1, progressStepMb) * 1024L * 1024L;
        this.deleteAfterSuccess = storageProperties.isDeleteAfterSuccess();
    }

    public void process(Sha256TaskMessage message) {
        taskRepository.ensureQueued(message).block();

        String workerToken = UUID.randomUUID().toString();
        ClaimResult claim = taskRepository.claim(message.taskId(), workerToken).block();
        if (claim != ClaimResult.CLAIMED) {
            return;
        }

        try {
            String hash = calculate(message, workerToken);
            Boolean committed = taskRepository.markSuccess(message.taskId(), workerToken, hash).block();
            if (!Boolean.TRUE.equals(committed)) {
                throw new IllegalStateException("Worker lost task lease before SUCCESS commit");
            }

            if (deleteAfterSuccess) {
                try {
                    objectStorage.deleteObject(message.storageKey());
                } catch (Exception deleteError) {
                    log.warn("SHA-256 succeeded but object cleanup failed: taskId={}, storageKey={}",
                            message.taskId(), message.storageKey(), deleteError);
                }
            }
        } catch (Exception e) {
            try {
                taskRepository.markRetrying(message.taskId(), workerToken, rootMessage(e)).block();
            } catch (Exception stateError) {
                log.error("Failed to move task to RETRYING: taskId={}", message.taskId(), stateError);
                releaseQuietly(message.taskId(), workerToken);
            }
            throw new Sha256ProcessingException(message.taskId(), rootMessage(e), e);
        } finally {
            releaseQuietly(message.taskId(), workerToken);
        }
    }

    private String calculate(Sha256TaskMessage message, String workerToken) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        long processed = 0;
        long lastReported = 0;
        byte[] buffer = new byte[BUFFER_SIZE];

        try (InputStream inputStream = objectStorage.getObject(message.storageKey())) {
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, length);
                processed += length;

                if (processed - lastReported >= progressStepBytes) {
                    int progress = progress(message.totalBytes(), processed);
                    Boolean updated = taskRepository.updateProgress(
                            message.taskId(), workerToken, processed, progress).block();
                    if (!Boolean.TRUE.equals(updated)) {
                        throw new IllegalStateException("Worker lost task lease while updating progress");
                    }
                    lastReported = processed;
                }
            }
        }

        taskRepository.updateProgress(message.taskId(), workerToken, processed, 99).block();
        return HexFormat.of().formatHex(digest.digest());
    }

    private int progress(long totalBytes, long processedBytes) {
        if (totalBytes <= 0) return 99;
        long value = processedBytes * 100L / totalBytes;
        return (int) Math.max(0, Math.min(99, value));
    }

    private void releaseQuietly(String taskId, String workerToken) {
        try { taskRepository.releaseLock(taskId, workerToken).block(); } catch (Exception ignored) { }
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) current = current.getCause();
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }

    public static class Sha256ProcessingException extends RuntimeException {
        private final String taskId;

        public Sha256ProcessingException(String taskId, String message, Throwable cause) {
            super(message, cause);
            this.taskId = taskId;
        }

        public String getTaskId() { return taskId; }
    }
}
