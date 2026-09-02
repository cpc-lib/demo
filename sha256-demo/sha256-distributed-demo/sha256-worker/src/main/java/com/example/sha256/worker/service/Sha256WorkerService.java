package com.example.sha256.worker.service;

import com.example.sha256.common.model.Sha256TaskMessage;
import com.example.sha256.common.model.Sha256TaskRecord;
import com.example.sha256.common.model.TaskStatus;
import com.example.sha256.common.repository.RedisTaskRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class Sha256WorkerService {
    private static final int BUFFER_SIZE = 1024 * 1024;

    private final RedisTaskRepository taskRepository;
    private final Path storageDirectory;
    private final long progressStepBytes;

    public Sha256WorkerService(RedisTaskRepository taskRepository,
                               @Value("${sha256.storage-dir:${user.home}/.sha256-demo/uploads}") String storageDir,
                               @Value("${sha256.worker.progress-step-mb:8}") long progressStepMb) {
        this.taskRepository = taskRepository;
        this.storageDirectory = Paths.get(storageDir).toAbsolutePath().normalize();
        this.progressStepBytes = Math.max(1, progressStepMb) * 1024L * 1024L;
    }

    public void process(Sha256TaskMessage message) {
        Optional<Sha256TaskRecord> optional = taskRepository.find(message.taskId()).block();
        if (optional == null || optional.isEmpty()) {
            return;
        }

        Sha256TaskRecord record = optional.get();
        if (record.getStatus() == TaskStatus.SUCCESS) {
            return;
        }

        Boolean locked = taskRepository.tryLock(message.taskId()).block();
        if (!Boolean.TRUE.equals(locked)) {
            return;
        }

        Path file = safeResolve(message.storageKey());
        try {
            record.markRunning();
            save(record);

            String hash = calculate(file, record);
            record.markSuccess(hash);
            save(record);
        } catch (Exception e) {
            record.markFailed(rootMessage(e));
            save(record);
        } finally {
            deleteQuietly(file);
            unlockQuietly(message.taskId());
        }
    }

    private String calculate(Path file, Sha256TaskRecord record)
            throws IOException, NoSuchAlgorithmException {
        if (!Files.isRegularFile(file)) {
            throw new IOException("Worker 找不到上传文件: " + file);
        }

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        long processed = 0;
        long lastReported = 0;
        byte[] buffer = new byte[BUFFER_SIZE];

        try (InputStream inputStream = Files.newInputStream(file)) {
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, length);
                processed += length;

                if (processed - lastReported >= progressStepBytes) {
                    int progress = progress(record.getTotalBytes(), processed);
                    record.updateProgress(processed, progress);
                    save(record);
                    lastReported = processed;
                }
            }
        }

        record.updateProgress(processed, 99);
        save(record);
        return HexFormat.of().formatHex(digest.digest());
    }

    private int progress(long totalBytes, long processedBytes) {
        if (totalBytes <= 0) {
            return 99;
        }
        long value = processedBytes * 100L / totalBytes;
        return (int) Math.max(0, Math.min(99, value));
    }

    private Path safeResolve(String storageKey) {
        Path path = storageDirectory.resolve(storageKey).normalize();
        if (!path.startsWith(storageDirectory)) {
            throw new IllegalArgumentException("非法 Worker 文件路径");
        }
        return path;
    }

    private void save(Sha256TaskRecord record) {
        taskRepository.save(record).block();
    }

    private void deleteQuietly(Path path) {
        try { Files.deleteIfExists(path); } catch (IOException ignored) { }
    }

    private void unlockQuietly(String taskId) {
        try { taskRepository.unlock(taskId).block(); } catch (Exception ignored) { }
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }
}
