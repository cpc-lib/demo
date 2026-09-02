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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.Instant;
import java.util.NoSuchElementException;

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

    /**
     * taskId is the upload sessionId. This makes Complete -> Task creation idempotent across retries/crashes.
     */
    public Mono<CreateTaskResponse> createTaskForStoredObject(String taskId, String originalFilename,
                                                               String storageKey, long size) {
        if (taskId == null || taskId.isBlank()) return Mono.error(new IllegalArgumentException("taskId 不能为空"));
        if (originalFilename == null || originalFilename.isBlank()) return Mono.error(new IllegalArgumentException("文件名不能为空"));
        if (storageKey == null || storageKey.isBlank() || size < 0) return Mono.error(new IllegalArgumentException("对象存储信息无效"));

        Sha256TaskRecord record = Sha256TaskRecord.queued(
                taskId, originalFilename, storageKey, objectStorage.bucket(), size, taskPublisher.brokerName());
        Sha256TaskMessage message = new Sha256TaskMessage(
                taskId, storageKey, objectStorage.bucket(), originalFilename, size, taskPublisher.brokerName());

        return Mono.fromRunnable(() -> persistenceRepository.createTaskAndOutbox(record, message))
                .subscribeOn(Schedulers.boundedElastic())
                .then(projectRedis(record))
                .thenReturn(new CreateTaskResponse(taskId, record.getStatus().name(), taskPublisher.brokerName()))
                .onErrorResume(DataIntegrityViolationException.class, duplicate ->
                        Mono.fromCallable(() -> persistenceRepository.findTask(taskId))
                                .subscribeOn(Schedulers.boundedElastic())
                                .flatMap(existing -> existing
                                        .map(task -> Mono.just(new CreateTaskResponse(
                                                task.getTaskId(), task.getStatus().name(), task.getBroker())))
                                        .orElseGet(() -> Mono.error(duplicate))));
    }

    public Mono<java.util.Optional<CreateTaskResponse>> findExistingTask(String taskId) {
        return Mono.fromCallable(() -> persistenceRepository.findTask(taskId)
                        .map(task -> new CreateTaskResponse(task.getTaskId(), task.getStatus().name(), task.getBroker())))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<TaskResponse> getTask(String taskId) {
        return taskRepository.find(taskId)
                .flatMap(optional -> {
                    if (optional.isPresent()) return Mono.just(TaskResponse.from(optional.get()));
                    return Mono.fromCallable(() -> persistenceRepository.findTask(taskId))
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMap(dbTask -> dbTask
                                    .filter(record -> Duration.between(record.getCreatedAt(), Instant.now()).toMinutes() < 2)
                                    .map(record -> Mono.just(TaskResponse.from(record)))
                                    .orElseGet(() -> Mono.error(new NoSuchElementException("任务不存在或已过期: " + taskId))));
                });
    }

    public String brokerName() { return taskPublisher.brokerName(); }
    public String storageProvider() { return objectStorage.provider(); }
    public String storageBucket() { return objectStorage.bucket(); }

    private Mono<Boolean> projectRedis(Sha256TaskRecord record) {
        return taskRepository.createQueued(record)
                .onErrorResume(error -> {
                    log.warn("Redis initial task projection failed, taskId={}", record.getTaskId(), error);
                    return Mono.just(false);
                });
    }
}
