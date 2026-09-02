package com.example.sha256.api.service;

import com.example.sha256.api.broker.TaskPublisher;
import com.example.sha256.api.model.CreateTaskResponse;
import com.example.sha256.api.model.TaskResponse;
import com.example.sha256.api.persistence.TaskPersistenceRepository;
import com.example.sha256.common.model.Sha256TaskMessage;
import com.example.sha256.common.model.Sha256TaskRecord;
import com.example.sha256.common.repository.RedisTaskRepository;
import com.example.sha256.common.storage.ObjectStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class Sha256TaskService {
    private static final Logger log = LoggerFactory.getLogger(Sha256TaskService.class);

    private final RedisTaskRepository taskRepository;
    private final TaskPersistenceRepository persistenceRepository;
    private final TaskPublisher taskPublisher;
    private final ObjectStorageService objectStorage;

    public Sha256TaskService(RedisTaskRepository taskRepository,
                             TaskPersistenceRepository persistenceRepository,
                             TaskPublisher taskPublisher,
                             ObjectStorageService objectStorage) {
        this.taskRepository = taskRepository;
        this.persistenceRepository = persistenceRepository;
        this.taskPublisher = taskPublisher;
        this.objectStorage = objectStorage;
    }

    public Mono<CreateTaskResponse> createTask(FilePart filePart) {
        if (filePart == null || filePart.filename() == null || filePart.filename().isBlank()) {
            return Mono.error(new IllegalArgumentException("请选择需要计算 SHA-256 的文件"));
        }

        String taskId = UUID.randomUUID().toString().replace("-", "");
        String storageKey = buildStorageKey(taskId);
        String contentType = filePart.headers().getContentType() == null
                ? "application/octet-stream" : filePart.headers().getContentType().toString();

        return Mono.usingWhen(
                Mono.fromCallable(() -> Files.createTempFile("sha256-upload-", ".part"))
                        .subscribeOn(Schedulers.boundedElastic()),
                tempFile -> filePart.transferTo(tempFile)
                        .then(Mono.fromCallable(() -> uploadObject(tempFile, storageKey, contentType))
                                .subscribeOn(Schedulers.boundedElastic()))
                        .flatMap(size -> persistTask(taskId, filePart.filename(), storageKey, size))
                        .onErrorResume(error -> deleteObjectQuietly(storageKey).then(Mono.error(error))),
                this::deleteTempQuietly,
                (temp, error) -> deleteTempQuietly(temp),
                this::deleteTempQuietly
        );
    }

    public Mono<TaskResponse> getTask(String taskId) {
        return taskRepository.find(taskId)
                .flatMap(optional -> {
                    if (optional.isPresent()) {
                        return Mono.just(TaskResponse.from(optional.get()));
                    }
                    return Mono.fromCallable(() -> persistenceRepository.findTask(taskId))
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMap(dbTask -> dbTask
                                    .filter(record -> Duration.between(record.getCreatedAt(), Instant.now()).toMinutes() < 2)
                                    .map(record -> Mono.just(TaskResponse.from(record)))
                                    .orElseGet(() -> Mono.error(
                                            new NoSuchElementException("任务不存在或已过期: " + taskId))));
                });
    }

    public String brokerName() {
        return taskPublisher.brokerName();
    }

    public String storageProvider() {
        return objectStorage.provider();
    }

    public String storageBucket() {
        return objectStorage.bucket();
    }

    private Mono<CreateTaskResponse> persistTask(String taskId, String originalFilename,
                                                  String storageKey, long size) {
        Sha256TaskRecord record = Sha256TaskRecord.queued(
                taskId, originalFilename, storageKey, objectStorage.bucket(), size, taskPublisher.brokerName());
        Sha256TaskMessage message = new Sha256TaskMessage(
                taskId, storageKey, objectStorage.bucket(), originalFilename, size, taskPublisher.brokerName());

        return Mono.fromRunnable(() -> persistenceRepository.createTaskAndOutbox(record, message))
                .subscribeOn(Schedulers.boundedElastic())
                .then(taskRepository.createQueued(record)
                        .onErrorResume(error -> {
                            // MySQL + Outbox is the durable source. Worker can rebuild Redis state from the message.
                            log.warn("Redis initial task projection failed, taskId={}", taskId, error);
                            return Mono.just(false);
                        }))
                .thenReturn(new CreateTaskResponse(taskId, record.getStatus().name(), taskPublisher.brokerName()));
    }

    private long uploadObject(Path tempFile, String storageKey, String contentType) throws IOException {
        long size = Files.size(tempFile);
        objectStorage.putObject(tempFile, storageKey, contentType);
        return size;
    }

    private String buildStorageKey(String taskId) {
        LocalDate date = LocalDate.now(ZoneOffset.UTC);
        return "sha256/%04d/%02d/%02d/%s".formatted(
                date.getYear(), date.getMonthValue(), date.getDayOfMonth(), taskId);
    }

    private Mono<Void> deleteTempQuietly(Path path) {
        return Mono.fromRunnable(() -> {
                    try { Files.deleteIfExists(path); } catch (IOException ignored) { }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    private Mono<Void> deleteObjectQuietly(String storageKey) {
        return Mono.fromRunnable(() -> {
                    try { objectStorage.deleteObject(storageKey); } catch (Exception ignored) { }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }
}
