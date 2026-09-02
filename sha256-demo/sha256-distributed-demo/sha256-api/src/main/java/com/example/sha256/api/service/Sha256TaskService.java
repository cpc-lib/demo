package com.example.sha256.api.service;

import com.example.sha256.api.broker.TaskPublisher;
import com.example.sha256.api.model.CreateTaskResponse;
import com.example.sha256.api.model.TaskResponse;
import com.example.sha256.common.model.Sha256TaskMessage;
import com.example.sha256.common.model.Sha256TaskRecord;
import com.example.sha256.common.repository.RedisTaskRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class Sha256TaskService {
    private final RedisTaskRepository taskRepository;
    private final TaskPublisher taskPublisher;
    private final Path storageDirectory;

    public Sha256TaskService(RedisTaskRepository taskRepository,
                             TaskPublisher taskPublisher,
                             @Value("${sha256.storage-dir:${user.home}/.sha256-demo/uploads}") String storageDir) {
        this.taskRepository = taskRepository;
        this.taskPublisher = taskPublisher;
        this.storageDirectory = Paths.get(storageDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    void initializeStorage() throws IOException {
        Files.createDirectories(storageDirectory);
    }

    public Mono<CreateTaskResponse> createTask(FilePart filePart) {
        if (filePart == null || filePart.filename() == null || filePart.filename().isBlank()) {
            return Mono.error(new IllegalArgumentException("请选择需要计算 SHA-256 的文件"));
        }

        String taskId = UUID.randomUUID().toString().replace("-", "");
        String storageKey = taskId + ".upload";
        Path target = safeResolve(storageKey);

        return filePart.transferTo(target)
                .then(Mono.fromCallable(() -> Files.size(target)).subscribeOn(Schedulers.boundedElastic()))
                .flatMap(size -> {
                    Sha256TaskRecord record = Sha256TaskRecord.queued(
                            taskId, filePart.filename(), storageKey, size, taskPublisher.brokerName());
                    Sha256TaskMessage message = new Sha256TaskMessage(taskId, storageKey, filePart.filename(), size);

                    return taskRepository.save(record)
                            .then(taskPublisher.publish(message)
                                    .onErrorResume(error -> {
                                        record.markFailed("消息投递失败: " + rootMessage(error));
                                        return taskRepository.save(record)
                                                .then(deleteQuietly(target))
                                                .then(Mono.error(error));
                                    }))
                            .thenReturn(new CreateTaskResponse(taskId, record.getStatus().name(), taskPublisher.brokerName()));
                })
                .onErrorResume(error -> deleteQuietly(target).then(Mono.error(error)));
    }

    public Mono<TaskResponse> getTask(String taskId) {
        return taskRepository.find(taskId)
                .flatMap(optional -> optional
                        .map(record -> Mono.just(TaskResponse.from(record)))
                        .orElseGet(() -> Mono.error(new NoSuchElementException("任务不存在或已过期: " + taskId))));
    }

    public String brokerName() {
        return taskPublisher.brokerName();
    }

    private Path safeResolve(String storageKey) {
        Path path = storageDirectory.resolve(storageKey).normalize();
        if (!path.startsWith(storageDirectory)) {
            throw new IllegalArgumentException("非法文件存储路径");
        }
        return path;
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }

    private Mono<Void> deleteQuietly(Path path) {
        return Mono.fromRunnable(() -> {
                    try { Files.deleteIfExists(path); } catch (IOException ignored) { }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }
}
