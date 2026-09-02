package com.example.sha256.common.repository;

import com.example.sha256.common.model.ClaimResult;
import com.example.sha256.common.model.Sha256TaskMessage;
import com.example.sha256.common.model.Sha256TaskRecord;
import com.example.sha256.common.model.TaskStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class RedisTaskRepository {
    private static final String TASK_PREFIX = "sha256:task:";
    private static final String LOCK_PREFIX = "sha256:lock:";

    private final ReactiveStringRedisTemplate redisTemplate;
    private final ReactiveHashOperations<String, String, String> hashOps;
    private final long taskTtlSeconds;
    private final long lockTtlMillis;

    private final DefaultRedisScript<Long> createTask = script("redis/create_task.lua");
    private final DefaultRedisScript<Long> claimTask = script("redis/claim_task.lua");
    private final DefaultRedisScript<Long> updateProgress = script("redis/update_progress.lua");
    private final DefaultRedisScript<Long> markSuccess = script("redis/mark_success.lua");
    private final DefaultRedisScript<Long> markRetrying = script("redis/mark_retrying.lua");
    private final DefaultRedisScript<Long> markFailed = script("redis/mark_failed.lua");
    private final DefaultRedisScript<Long> markDeadLettered = script("redis/mark_dead_lettered.lua");
    private final DefaultRedisScript<Long> releaseLock = script("redis/release_lock.lua");

    public RedisTaskRepository(ReactiveStringRedisTemplate redisTemplate,
                               @Value("${sha256.task.ttl-minutes:30}") long taskTtlMinutes,
                               @Value("${sha256.task.lock-ttl-minutes:60}") long lockTtlMinutes) {
        this.redisTemplate = redisTemplate;
        this.hashOps = redisTemplate.opsForHash();
        this.taskTtlSeconds = Duration.ofMinutes(taskTtlMinutes).toSeconds();
        this.lockTtlMillis = Duration.ofMinutes(lockTtlMinutes).toMillis();
    }

    public Mono<Boolean> createQueued(Sha256TaskRecord record) {
        return execute(createTask, List.of(taskKey(record.getTaskId())), List.of(
                record.getTaskId(), safe(record.getOriginalFilename()), safe(record.getStorageKey()),
                safe(record.getStorageBucket()), String.valueOf(record.getTotalBytes()), safe(record.getBroker()),
                instant(record.getCreatedAt()), String.valueOf(taskTtlSeconds)))
                .map(value -> value == 1L);
    }

    public Mono<Boolean> ensureQueued(Sha256TaskMessage message) {
        Sha256TaskRecord record = Sha256TaskRecord.queued(
                message.taskId(), message.originalFilename(), message.storageKey(), message.storageBucket(),
                message.totalBytes(), message.broker());
        return createQueued(record);
    }

    public Mono<Optional<Sha256TaskRecord>> find(String taskId) {
        return hashOps.entries(taskKey(taskId))
                .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                .map(map -> map.isEmpty() ? Optional.empty() : Optional.of(fromMap(map)));
    }

    public Mono<ClaimResult> claim(String taskId, String workerToken) {
        return execute(claimTask, List.of(taskKey(taskId), lockKey(taskId)), List.of(
                workerToken, String.valueOf(lockTtlMillis), Instant.now().toString(), String.valueOf(taskTtlSeconds)))
                .map(ClaimResult::fromCode);
    }

    public Mono<Boolean> updateProgress(String taskId, String workerToken, long processedBytes, int progress) {
        return execute(updateProgress, List.of(taskKey(taskId), lockKey(taskId)), List.of(
                workerToken, String.valueOf(processedBytes), String.valueOf(progress), Instant.now().toString(),
                String.valueOf(taskTtlSeconds), String.valueOf(lockTtlMillis)))
                .map(value -> value == 1L);
    }

    public Mono<Boolean> markSuccess(String taskId, String workerToken, String sha256) {
        return execute(markSuccess, List.of(taskKey(taskId), lockKey(taskId)), List.of(
                workerToken, sha256, Instant.now().toString(), String.valueOf(taskTtlSeconds)))
                .map(value -> value == 1L);
    }

    public Mono<Integer> markRetrying(String taskId, String workerToken, String error) {
        return execute(markRetrying, List.of(taskKey(taskId), lockKey(taskId)), List.of(
                workerToken, safe(error), Instant.now().toString(), String.valueOf(taskTtlSeconds)))
                .map(Long::intValue);
    }

    public Mono<Boolean> markFailed(String taskId, String workerToken, String error) {
        return execute(markFailed, List.of(taskKey(taskId), lockKey(taskId)), List.of(
                safe(workerToken), safe(error), Instant.now().toString(), String.valueOf(taskTtlSeconds)))
                .map(value -> value == 1L);
    }

    public Mono<Boolean> markDeadLettered(String taskId, String error) {
        return execute(markDeadLettered, List.of(taskKey(taskId), lockKey(taskId)), List.of(
                safe(error), Instant.now().toString(), String.valueOf(taskTtlSeconds)))
                .map(value -> value == 1L);
    }

    public Mono<Boolean> releaseLock(String taskId, String workerToken) {
        return execute(releaseLock, List.of(lockKey(taskId)), List.of(workerToken))
                .map(value -> value == 1L);
    }

    private Mono<Long> execute(DefaultRedisScript<Long> script, List<String> keys, List<?> args) {
        return redisTemplate.execute(script, keys, args).next().defaultIfEmpty(0L);
    }

    private Sha256TaskRecord fromMap(Map<String, String> map) {
        Sha256TaskRecord record = new Sha256TaskRecord();
        record.setTaskId(map.get("taskId"));
        record.setOriginalFilename(map.get("originalFilename"));
        record.setStorageKey(map.get("storageKey"));
        record.setStorageBucket(map.get("storageBucket"));
        record.setTotalBytes(parseLong(map.get("totalBytes")));
        record.setProcessedBytes(parseLong(map.get("processedBytes")));
        record.setProgress(parseInt(map.get("progress")));
        record.setStatus(TaskStatus.valueOf(map.getOrDefault("status", "QUEUED")));
        record.setSha256(blankToNull(map.get("sha256")));
        record.setError(blankToNull(map.get("error")));
        record.setBroker(map.get("broker"));
        record.setRetryCount(parseInt(map.get("retryCount")));
        record.setCreatedAt(parseInstant(map.get("createdAt")));
        record.setUpdatedAt(parseInstant(map.get("updatedAt")));
        return record;
    }

    private static DefaultRedisScript<Long> script(String path) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource(path));
        script.setResultType(Long.class);
        return script;
    }

    private String taskKey(String taskId) { return TASK_PREFIX + taskId; }
    private String lockKey(String taskId) { return LOCK_PREFIX + taskId; }
    private String safe(String value) { return value == null ? "" : value; }
    private String instant(Instant value) { return (value == null ? Instant.now() : value).toString(); }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value; }
    private long parseLong(String value) { try { return Long.parseLong(value); } catch (Exception e) { return 0L; } }
    private int parseInt(String value) { try { return Integer.parseInt(value); } catch (Exception e) { return 0; } }
    private Instant parseInstant(String value) { try { return Instant.parse(value); } catch (Exception e) { return Instant.now(); } }
}
