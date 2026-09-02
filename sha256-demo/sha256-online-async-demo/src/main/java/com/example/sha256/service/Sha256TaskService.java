package com.example.sha256.service;

import com.example.sha256.model.Sha256Task;
import com.example.sha256.model.TaskResponse;
import com.example.sha256.util.Sha256Utils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

@Service
public class Sha256TaskService {

    private static final Duration TASK_RETENTION = Duration.ofMinutes(30);

    private final Map<String, Sha256Task> tasks = new ConcurrentHashMap<>();
    private final ExecutorService sha256Executor;
    private final Path uploadDirectory;

    public Sha256TaskService(ExecutorService sha256Executor) throws IOException {
        this.sha256Executor = sha256Executor;
        this.uploadDirectory = Paths.get(System.getProperty("java.io.tmpdir"), "sha256-online-uploads");
        Files.createDirectories(uploadDirectory);
    }

    public Sha256Task createTask(String originalFileName) {
        String taskId = UUID.randomUUID().toString().replace("-", "");
        Sha256Task task = new Sha256Task(taskId, sanitizeFileName(originalFileName));
        tasks.put(taskId, task);
        return task;
    }

    public Path allocateUploadPath(String taskId) {
        return uploadDirectory.resolve(taskId + ".upload");
    }

    public void submit(String taskId, Path path) {
        Sha256Task task = requireTask(taskId);
        task.markQueued();

        try {
            sha256Executor.execute(() -> calculate(task, path));
        } catch (RejectedExecutionException e) {
            task.markFailed("服务器当前计算任务过多，请稍后重试");
            deleteQuietly(path);
        }
    }

    public void failUpload(String taskId, Path path, Throwable throwable) {
        Sha256Task task = tasks.get(taskId);
        if (task != null) {
            task.markFailed("文件上传失败: " + safeMessage(throwable));
        }
        deleteQuietly(path);
    }

    public Optional<TaskResponse> find(String taskId) {
        Sha256Task task = tasks.get(taskId);
        return task == null ? Optional.empty() : Optional.of(TaskResponse.from(task));
    }

    private void calculate(Sha256Task task, Path path) {
        try {
            long totalBytes = Files.size(path);
            task.markRunning(totalBytes);

            String sha256 = Sha256Utils.calculateFileSha256(path, task::updateProcessedBytes);
            task.markSuccess(sha256);
        } catch (Exception e) {
            task.markFailed("SHA-256 计算失败: " + safeMessage(e));
        } finally {
            deleteQuietly(path);
        }
    }

    @Scheduled(fixedDelay = 300_000L)
    public void cleanupExpiredTaskMetadata() {
        Instant expireBefore = Instant.now().minus(TASK_RETENTION);
        tasks.entrySet().removeIf(entry -> {
            Sha256Task task = entry.getValue();
            Instant finishedAt = task.getFinishedAt();
            return finishedAt != null && finishedAt.isBefore(expireBefore);
        });
    }

    private Sha256Task requireTask(String taskId) {
        Sha256Task task = tasks.get(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }
        return task;
    }

    private String sanitizeFileName(String original) {
        if (original == null || original.isBlank()) {
            return "unnamed-file";
        }
        String normalized = original.replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        String fileName = slash >= 0 ? normalized.substring(slash + 1) : normalized;
        return fileName.isBlank() ? "unnamed-file" : fileName;
    }

    private String safeMessage(Throwable throwable) {
        if (throwable == null) {
            return "未知错误";
        }
        return throwable.getMessage() == null ? throwable.getClass().getSimpleName() : throwable.getMessage();
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // 临时文件删除失败不影响任务结果，生产环境建议接入日志/监控告警。
        }
    }
}
