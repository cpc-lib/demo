package com.example.sha256.api.upload;

import com.example.sha256.api.model.CreateTaskResponse;
import com.example.sha256.api.service.Sha256TaskService;
import com.example.sha256.common.storage.ObjectStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.services.s3.model.NoSuchUploadException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

import static com.example.sha256.api.upload.UploadApiModels.*;

@Service
public class MultipartUploadService {
    private static final Logger log = LoggerFactory.getLogger(MultipartUploadService.class);
    private static final long S3_MIN_PART_SIZE = 5L * 1024 * 1024;
    private static final int S3_MAX_PARTS = 10_000;

    private final UploadSessionRepository sessionRepository;
    private final ObjectStorageService storage;
    private final MultipartUploadProperties properties;
    private final Sha256TaskService taskService;

    public MultipartUploadService(UploadSessionRepository sessionRepository,
                                  ObjectStorageService storage,
                                  MultipartUploadProperties properties,
                                  Sha256TaskService taskService) {
        this.sessionRepository = sessionRepository;
        this.storage = storage;
        this.properties = properties;
        this.taskService = taskService;
    }

    public Mono<InitResponse> initialize(InitRequest request) {
        validateInit(request);
        return sessionRepository.findByFingerprint(request.fingerprint())
                .flatMap(existing -> existing.map(session -> {
                            if (session.completedTaskId() != null && completedGraceExpired(session)) {
                                return sessionRepository.delete(session).then(createNew(request));
                            }
                            return restore(session)
                                    .onErrorResume(this::isMissingMultipartUpload, error ->
                                            taskService.findExistingTask(session.sessionId())
                                                    .flatMap(existingTask -> existingTask
                                                            .map(task -> saveCompletedAndRespond(session, task))
                                                            .orElseGet(() -> objectAlreadyComplete(session)
                                                                    .flatMap(done -> done ? finalizeTask(session)
                                                                            .flatMap(task -> saveCompletedAndRespond(session, task))
                                                                            : sessionRepository.delete(session).then(createNew(request))))));
                        })
                        .orElseGet(() -> createNew(request)));
    }

    public Mono<InitResponse> status(String sessionId) {
        return requireSession(sessionId).flatMap(session -> {
            if (session.completedTaskId() != null) {
                return Mono.just(toResponse(session, List.of(), true));
            }
            return listParts(session).map(parts -> toResponse(session, parts, true));
        });
    }

    public Mono<PresignResponse> presign(String sessionId, PresignRequest request) {
        if (request == null || request.partNumbers() == null || request.partNumbers().isEmpty()) {
            return Mono.error(new IllegalArgumentException("partNumbers 不能为空"));
        }
        if (request.partNumbers().size() > Math.max(1, properties.getMaxPresignBatch())) {
            return Mono.error(new IllegalArgumentException("单次预签名分片数量过多"));
        }
        return requireSession(sessionId).flatMap(session -> {
            if (session.completedTaskId() != null) {
                return Mono.error(new IllegalStateException("上传已完成"));
            }
            Set<Integer> unique = new HashSet<>(request.partNumbers());
            for (Integer partNumber : unique) {
                if (partNumber == null || partNumber < 1 || partNumber > session.totalParts()) {
                    return Mono.error(new IllegalArgumentException("非法 partNumber: " + partNumber));
                }
            }
            return Mono.fromCallable(() -> {
                        List<PresignedPartView> result = new ArrayList<>(unique.size());
                        Duration validFor = Duration.ofMinutes(Math.max(1, properties.getPresignExpireMinutes()));
                        unique.stream().sorted().forEach(partNumber -> {
                            var signed = storage.presignUploadPart(session.storageKey(), session.uploadId(), partNumber, validFor);
                            result.add(new PresignedPartView(signed.partNumber(), signed.url(), signed.expiresAt()));
                        });
                        return new PresignResponse(result);
                    })
                    .subscribeOn(Schedulers.boundedElastic());
        });
    }

    public Mono<CompleteResponse> complete(String sessionId) {
        return requireSession(sessionId).flatMap(session -> {
            if (session.completedTaskId() != null) {
                return Mono.just(new CompleteResponse(session.completedTaskId(), "QUEUED", taskService.brokerName()));
            }
            return taskService.findExistingTask(session.sessionId()).flatMap(existingTask -> {
                if (existingTask.isPresent()) {
                    CreateTaskResponse task = existingTask.get();
                    return sessionRepository.save(session.complete(task.taskId()))
                            .thenReturn(new CompleteResponse(task.taskId(), task.status(), task.broker()));
                }
                return objectAlreadyComplete(session).flatMap(alreadyComplete -> {
                    Mono<Void> ensureCompleted;
                    if (alreadyComplete) {
                        ensureCompleted = Mono.empty();
                    } else {
                        ensureCompleted = listParts(session)
                                .doOnNext(parts -> verifyAllParts(session, parts))
                                .flatMap(parts -> Mono.fromRunnable(() -> storage.completeMultipartUpload(
                                                session.storageKey(), session.uploadId(), parts))
                                        .subscribeOn(Schedulers.boundedElastic()))
                                .then();
                    }
                    return ensureCompleted.then(finalizeTask(session))
                            .flatMap(task -> sessionRepository.save(session.complete(task.taskId()))
                                    .thenReturn(new CompleteResponse(task.taskId(), task.status(), task.broker())));
                });
            });
        });
    }

    public Mono<Void> abort(String sessionId) {
        return requireSession(sessionId).flatMap(session -> {
            if (session.completedTaskId() != null) {
                return Mono.error(new IllegalStateException("任务已创建，不能取消已完成的 Multipart Upload"));
            }
            return Mono.fromRunnable(() -> storage.abortMultipartUpload(session.storageKey(), session.uploadId()))
                    .subscribeOn(Schedulers.boundedElastic())
                    .onErrorResume(error -> isMissingMultipartUpload(error) ? Mono.empty() : Mono.error(error))
                    .then(sessionRepository.delete(session));
        });
    }

    private Mono<InitResponse> restore(UploadSession session) {
        if (session.completedTaskId() != null) {
            return Mono.just(toResponse(session, List.of(), true));
        }
        return listParts(session).flatMap(parts -> sessionRepository.save(
                        new UploadSession(session.sessionId(), session.fingerprint(), session.uploadId(), session.storageKey(),
                                session.originalFilename(), session.contentType(), session.fileSize(), session.lastModified(),
                                session.partSize(), session.totalParts(), session.completedTaskId(), session.createdAt(), Instant.now()))
                .thenReturn(toResponse(session, parts, true)));
    }

    private Mono<InitResponse> createNew(InitRequest request) {
        long partSize = choosePartSize(request.fileSize());
        int totalParts = Math.toIntExact((request.fileSize() + partSize - 1) / partSize);
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        String storageKey = buildStorageKey(sessionId, request.fileName());
        return Mono.fromCallable(() -> storage.createMultipartUpload(storageKey, request.contentType()))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(uploadId -> {
                    Instant now = Instant.now();
                    UploadSession session = new UploadSession(sessionId, request.fingerprint(), uploadId, storageKey,
                            request.fileName(), normalizeContentType(request.contentType()), request.fileSize(),
                            request.lastModified(), partSize, totalParts, null, now, now);
                    return sessionRepository.save(session).thenReturn(toResponse(session, List.of(), false));
                });
    }



    private Mono<InitResponse> saveCompletedAndRespond(UploadSession session, CreateTaskResponse task) {
        UploadSession completed = session.complete(task.taskId());
        return sessionRepository.save(completed)
                .thenReturn(toResponse(completed, List.of(), true));
    }

    private Mono<CreateTaskResponse> finalizeTask(UploadSession session) {
        return taskService.createTaskForStoredObject(session.sessionId(), session.originalFilename(),
                session.storageKey(), session.fileSize());
    }

    private Mono<Boolean> objectAlreadyComplete(UploadSession session) {
        return Mono.fromCallable(() -> storage.objectSize(session.storageKey()) == session.fileSize())
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorReturn(false);
    }

    private boolean completedGraceExpired(UploadSession session) {
        long minutes = Math.max(1, properties.getCompletedResumeMinutes());
        return Duration.between(session.updatedAt(), Instant.now()).toMinutes() >= minutes;
    }

    private Mono<List<ObjectStorageService.UploadedPart>> listParts(UploadSession session) {
        return Mono.fromCallable(() -> storage.listUploadedParts(session.storageKey(), session.uploadId()))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<UploadSession> requireSession(String sessionId) {
        return sessionRepository.find(sessionId)
                .flatMap(optional -> optional.map(Mono::just)
                        .orElseGet(() -> Mono.error(new NoSuchElementException("上传会话不存在或已过期: " + sessionId))));
    }

    private InitResponse toResponse(UploadSession session, List<ObjectStorageService.UploadedPart> parts, boolean resumed) {
        long uploadedBytes = parts.stream().mapToLong(ObjectStorageService.UploadedPart::size).sum();
        List<UploadedPartView> uploaded = parts.stream()
                .map(part -> new UploadedPartView(part.partNumber(), part.size()))
                .toList();
        return new InitResponse(session.sessionId(), session.storageKey(), session.partSize(), session.totalParts(),
                Math.max(1, properties.getRecommendedConcurrency()), uploaded, uploadedBytes, resumed,
                session.completedTaskId());
    }

    private void verifyAllParts(UploadSession session, List<ObjectStorageService.UploadedPart> parts) {
        if (parts.size() != session.totalParts()) {
            throw new IllegalStateException("分片尚未全部上传: " + parts.size() + "/" + session.totalParts());
        }
        for (int i = 0; i < parts.size(); i++) {
            ObjectStorageService.UploadedPart part = parts.get(i);
            int expectedNumber = i + 1;
            if (part.partNumber() != expectedNumber) {
                throw new IllegalStateException("缺少分片: " + expectedNumber);
            }
            long expectedSize = expectedPartSize(session, expectedNumber);
            if (part.size() != expectedSize) {
                throw new IllegalStateException("分片大小不一致: part=" + expectedNumber
                        + ", expected=" + expectedSize + ", actual=" + part.size());
            }
        }
    }

    private long expectedPartSize(UploadSession session, int partNumber) {
        if (partNumber < session.totalParts()) return session.partSize();
        return session.fileSize() - session.partSize() * (session.totalParts() - 1L);
    }

    private long choosePartSize(long fileSize) {
        long configured = Math.max(S3_MIN_PART_SIZE, (long) Math.max(5, properties.getPartSizeMb()) * 1024 * 1024);
        long minForPartLimit = (fileSize + S3_MAX_PARTS - 1) / S3_MAX_PARTS;
        long required = Math.max(configured, minForPartLimit);
        long oneMiB = 1024L * 1024;
        return ((required + oneMiB - 1) / oneMiB) * oneMiB;
    }

    private String buildStorageKey(String sessionId, String filename) {
        LocalDate date = LocalDate.now(ZoneOffset.UTC);
        String safeName = filename == null ? "file.bin" : filename.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (safeName.length() > 120) safeName = safeName.substring(safeName.length() - 120);
        return "sha256/%04d/%02d/%02d/%s/%s".formatted(
                date.getYear(), date.getMonthValue(), date.getDayOfMonth(), sessionId, safeName);
    }

    private void validateInit(InitRequest request) {
        if (request == null) throw new IllegalArgumentException("请求不能为空");
        if (request.fileName() == null || request.fileName().isBlank()) throw new IllegalArgumentException("文件名不能为空");
        if (request.fileSize() <= 0) throw new IllegalArgumentException("空文件暂不使用 Multipart Upload");
        if (request.fingerprint() == null || request.fingerprint().isBlank()) throw new IllegalArgumentException("fingerprint 不能为空");
    }

    private String normalizeContentType(String value) {
        return value == null || value.isBlank() ? "application/octet-stream" : value;
    }

    private boolean isMissingMultipartUpload(Throwable error) {
        if (error instanceof NoSuchUploadException) return true;
        if (error instanceof S3Exception s3) return s3.statusCode() == 404;
        Throwable cause = error.getCause();
        return cause != null && cause != error && isMissingMultipartUpload(cause);
    }
}
