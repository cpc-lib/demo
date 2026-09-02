package com.example.idem.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.*;
import java.util.function.Supplier;

public final class IdempotencyCore {

    public static final String HEADER = "Idempotency-Key";

    private IdempotencyCore() {}

    public static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public enum Status { PROCESSING, SUCCESS }

    public record Record(
            long id,
            String scope,
            String keyHash,
            String requestHash,
            Status status,
            String responseJson,
            String businessRef) {}

    public static class IdempotencyException extends RuntimeException {
        private final String code;
        public IdempotencyException(String code, String message) {
            super(message);
            this.code = code;
        }
        public String getCode() { return code; }
    }

    public static final class KeyReusedException extends IdempotencyException {
        public KeyReusedException() {
            super("IDEMPOTENCY_KEY_REUSED",
                    "The same Idempotency-Key was used with a different request payload.");
        }
    }

    public static final class InProgressException extends IdempotencyException {
        public InProgressException() {
            super("IDEMPOTENCY_REQUEST_IN_PROGRESS",
                    "The same idempotent request is still being processed.");
        }
    }

    public static final class InvalidKeyException extends IdempotencyException {
        public InvalidKeyException() {
            super("INVALID_IDEMPOTENCY_KEY",
                    "Idempotency-Key length must be between 8 and 128.");
        }
    }

    public interface Repository {
        Optional<Record> find(String scope, String keyHash);
        void insertProcessing(String scope, String keyHash, String requestHash);
        void markSuccess(String scope, String keyHash, String responseJson, String businessRef);
    }

    public static final class JdbcRepository implements Repository {
        private final JdbcTemplate jdbc;
        public JdbcRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

        @Override
        public Optional<Record> find(String scope, String keyHash) {
            return jdbc.query("""
                    SELECT id,scope,key_hash,request_hash,status,response_json,business_ref
                    FROM idempotency_record
                    WHERE scope=? AND key_hash=?
                    """,
                    (rs, n) -> new Record(
                            rs.getLong("id"),
                            rs.getString("scope"),
                            rs.getString("key_hash"),
                            rs.getString("request_hash"),
                            Status.valueOf(rs.getString("status")),
                            rs.getString("response_json"),
                            rs.getString("business_ref")),
                    scope, keyHash).stream().findFirst();
        }

        @Override
        public void insertProcessing(String scope, String keyHash, String requestHash) {
            jdbc.update("""
                    INSERT INTO idempotency_record(scope,key_hash,request_hash,status)
                    VALUES(?,?,?,'PROCESSING')
                    """, scope, keyHash, requestHash);
        }

        @Override
        public void markSuccess(String scope, String keyHash, String responseJson, String businessRef) {
            jdbc.update("""
                    UPDATE idempotency_record
                    SET status='SUCCESS',response_json=?,business_ref=?,
                        updated_at=CURRENT_TIMESTAMP(3)
                    WHERE scope=? AND key_hash=?
                    """, responseJson, businessRef, scope, keyHash);
        }
    }

    public record LockAttempt(boolean acquired, boolean redisAvailable, String token) {
        static LockAttempt acquired(String token) { return new LockAttempt(true, true, token); }
        static LockAttempt busy() { return new LockAttempt(false, true, null); }
        static LockAttempt degraded() { return new LockAttempt(false, false, null); }
    }

    public interface Mutex {
        LockAttempt tryAcquire(String lockKey);
        void release(String lockKey, String token);
    }

    public static final class RedisMutex implements Mutex {
        private static final Logger log = LoggerFactory.getLogger(RedisMutex.class);
        private static final DefaultRedisScript<Long> RELEASE =
                new DefaultRedisScript<>("""
                        if redis.call('get', KEYS[1]) == ARGV[1] then
                          return redis.call('del', KEYS[1])
                        end
                        return 0
                        """, Long.class);

        private final StringRedisTemplate redis;
        private final Duration lease;

        public RedisMutex(StringRedisTemplate redis, Duration lease) {
            this.redis = redis;
            this.lease = lease;
        }

        @Override
        public LockAttempt tryAcquire(String lockKey) {
            String token = UUID.randomUUID().toString();
            try {
                Boolean ok = redis.opsForValue().setIfAbsent(lockKey, token, lease);
                return Boolean.TRUE.equals(ok) ? LockAttempt.acquired(token) : LockAttempt.busy();
            } catch (DataAccessException ex) {
                log.warn("Redis unavailable, degrade idempotency to DB unique constraint. key={}", lockKey);
                return LockAttempt.degraded();
            }
        }

        @Override
        public void release(String lockKey, String token) {
            if (token == null) return;
            try {
                redis.execute(RELEASE, List.of(lockKey), token);
            } catch (DataAccessException ex) {
                log.warn("Redis unlock failed; lease will expire. key={}", lockKey);
            }
        }
    }

    public record Result<T>(T value, boolean replayed) {}

    public static final class Template {
        private final Repository repository;
        private final Mutex mutex;
        private final ObjectMapper mapper;
        private final ObjectMapper canonicalMapper;
        private final TransactionTemplate tx;
        private final Duration waitTimeout;
        private final Duration pollInterval;

        public Template(
                Repository repository,
                Mutex mutex,
                ObjectMapper mapper,
                TransactionTemplate tx,
                Duration waitTimeout,
                Duration pollInterval) {
            this.repository = repository;
            this.mutex = mutex;
            this.mapper = mapper;
            this.canonicalMapper = mapper.copy()
                    .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                    .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
            this.tx = tx;
            this.waitTimeout = waitTimeout;
            this.pollInterval = pollInterval;
        }

        public <T> Result<T> execute(
                String scope,
                String rawKey,
                Object request,
                Class<T> responseType,
                Supplier<T> action) {

            validate(rawKey);

            String keyHash = sha256(rawKey);
            String requestHash = sha256(writeCanonical(request));
            String lockKey = "idem:lock:" + sha256(scope) + ":" + keyHash;

            Optional<T> replayBeforeLock = readReplay(scope, keyHash, requestHash, responseType);
            if (replayBeforeLock.isPresent()) return new Result<>(replayBeforeLock.get(), true);

            LockAttempt lock = mutex.tryAcquire(lockKey);

            if (!lock.acquired() && lock.redisAvailable()) {
                return new Result<>(waitForReplay(scope, keyHash, requestHash, responseType), true);
            }

            try {
                try {
                    T value = tx.execute(status ->
                            executeInTx(scope, keyHash, requestHash, responseType, action));
                    if (value == null) throw new IllegalStateException("Transaction returned null");
                    return new Result<>(value, false);
                } catch (DuplicateKeyException ex) {
                    return new Result<>(waitForReplay(scope, keyHash, requestHash, responseType), true);
                }
            } finally {
                if (lock.acquired()) mutex.release(lockKey, lock.token());
            }
        }

        private <T> T executeInTx(
                String scope,
                String keyHash,
                String requestHash,
                Class<T> responseType,
                Supplier<T> action) {

            Optional<T> replay = readReplay(scope, keyHash, requestHash, responseType);
            if (replay.isPresent()) return replay.get();

            Optional<Record> existing = repository.find(scope, keyHash);
            if (existing.isPresent()) {
                verifyRequest(existing.get(), requestHash);
                throw new InProgressException();
            }

            repository.insertProcessing(scope, keyHash, requestHash);
            T result = action.get();

            try {
                String responseJson = mapper.writeValueAsString(result);
                repository.markSuccess(scope, keyHash, responseJson, businessRef(result));
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Cannot serialize idempotent response", e);
            }
            return result;
        }

        private <T> Optional<T> readReplay(
                String scope, String keyHash, String requestHash, Class<T> type) {
            Optional<Record> row = repository.find(scope, keyHash);
            if (row.isEmpty()) return Optional.empty();

            verifyRequest(row.get(), requestHash);
            if (row.get().status() != Status.SUCCESS) return Optional.empty();

            try {
                return Optional.of(mapper.readValue(row.get().responseJson(), type));
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Cannot deserialize stored idempotent response", e);
            }
        }

        private <T> T waitForReplay(
                String scope, String keyHash, String requestHash, Class<T> type) {
            long deadline = System.nanoTime() + waitTimeout.toNanos();
            while (System.nanoTime() < deadline) {
                Optional<T> replay = readReplay(scope, keyHash, requestHash, type);
                if (replay.isPresent()) return replay.get();
                try {
                    Thread.sleep(pollInterval);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            throw new InProgressException();
        }

        private void verifyRequest(Record row, String requestHash) {
            if (!row.requestHash().equals(requestHash)) throw new KeyReusedException();
        }

        private String writeCanonical(Object request) {
            try {
                return canonicalMapper.writeValueAsString(request);
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Cannot serialize request", e);
            }
        }

        private void validate(String key) {
            if (key == null || key.length() < 8 || key.length() > 128) {
                throw new InvalidKeyException();
            }
        }

        private String businessRef(Object value) {
            try {
                Object ref = value.getClass().getMethod("businessRef").invoke(value);
                return ref == null ? null : ref.toString();
            } catch (ReflectiveOperationException ignored) {
                return null;
            }
        }
    }
}
