package com.example.sha256.common.repository;

import com.example.sha256.common.model.Sha256TaskRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Optional;

@Repository
public class RedisTaskRepository {
    private static final String TASK_PREFIX = "sha256:task:";
    private static final String LOCK_PREFIX = "sha256:lock:";

    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration taskTtl;
    private final Duration lockTtl;

    public RedisTaskRepository(ReactiveStringRedisTemplate redisTemplate,
                               ObjectMapper objectMapper,
                               @Value("${sha256.task.ttl-minutes:30}") long taskTtlMinutes,
                               @Value("${sha256.task.lock-ttl-minutes:60}") long lockTtlMinutes) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.taskTtl = Duration.ofMinutes(taskTtlMinutes);
        this.lockTtl = Duration.ofMinutes(lockTtlMinutes);
    }

    public Mono<Void> save(Sha256TaskRecord record) {
        return redisTemplate.opsForValue()
                .set(taskKey(record.getTaskId()), toJson(record), taskTtl)
                .then();
    }

    public Mono<Optional<Sha256TaskRecord>> find(String taskId) {
        return redisTemplate.opsForValue()
                .get(taskKey(taskId))
                .map(this::fromJson)
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty());
    }

    public Mono<Boolean> tryLock(String taskId) {
        return redisTemplate.opsForValue()
                .setIfAbsent(lockKey(taskId), "1", lockTtl)
                .defaultIfEmpty(false);
    }

    public Mono<Boolean> unlock(String taskId) {
        return redisTemplate.delete(lockKey(taskId)).map(count -> count > 0);
    }

    private String taskKey(String taskId) {
        return TASK_PREFIX + taskId;
    }

    private String lockKey(String taskId) {
        return LOCK_PREFIX + taskId;
    }

    private String toJson(Sha256TaskRecord record) {
        try {
            return objectMapper.writeValueAsString(record);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize SHA-256 task", e);
        }
    }

    private Sha256TaskRecord fromJson(String json) {
        try {
            return objectMapper.readValue(json, Sha256TaskRecord.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize SHA-256 task", e);
        }
    }
}
