package com.example.sha256.api.upload;

import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Repository
public class UploadSessionRepository {
    private static final String SESSION_PREFIX = "sha256:upload:session:";
    private static final String FINGERPRINT_PREFIX = "sha256:upload:fingerprint:";

    private final ReactiveStringRedisTemplate redis;
    private final ReactiveHashOperations<String, String, String> hashOps;
    private final Duration ttl;

    public UploadSessionRepository(ReactiveStringRedisTemplate redis, MultipartUploadProperties properties) {
        this.redis = redis;
        this.hashOps = redis.opsForHash();
        this.ttl = Duration.ofHours(Math.max(1, properties.getSessionTtlHours()));
    }

    public Mono<Void> save(UploadSession session) {
        String key = sessionKey(session.sessionId());
        Map<String, String> values = Map.ofEntries(
                Map.entry("sessionId", session.sessionId()),
                Map.entry("fingerprint", safe(session.fingerprint())),
                Map.entry("uploadId", safe(session.uploadId())),
                Map.entry("storageKey", safe(session.storageKey())),
                Map.entry("originalFilename", safe(session.originalFilename())),
                Map.entry("contentType", safe(session.contentType())),
                Map.entry("fileSize", String.valueOf(session.fileSize())),
                Map.entry("lastModified", String.valueOf(session.lastModified())),
                Map.entry("partSize", String.valueOf(session.partSize())),
                Map.entry("totalParts", String.valueOf(session.totalParts())),
                Map.entry("completedTaskId", safe(session.completedTaskId())),
                Map.entry("createdAt", session.createdAt().toString()),
                Map.entry("updatedAt", session.updatedAt().toString())
        );
        return hashOps.putAll(key, values)
                .then(redis.expire(key, ttl))
                .then(redis.opsForValue().set(fingerprintKey(session.fingerprint()), session.sessionId(), ttl))
                .then();
    }

    public Mono<Optional<UploadSession>> find(String sessionId) {
        return hashOps.entries(sessionKey(sessionId))
                .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                .map(map -> map.isEmpty() ? Optional.empty() : Optional.of(fromMap(map)));
    }

    public Mono<Optional<UploadSession>> findByFingerprint(String fingerprint) {
        return redis.opsForValue().get(fingerprintKey(fingerprint))
                .flatMap(this::find)
                .defaultIfEmpty(Optional.empty());
    }

    public Mono<Void> delete(UploadSession session) {
        return redis.delete(sessionKey(session.sessionId()))
                .then(redis.delete(fingerprintKey(session.fingerprint())))
                .then();
    }

    private UploadSession fromMap(Map<String, String> map) {
        return new UploadSession(
                map.get("sessionId"), map.get("fingerprint"), map.get("uploadId"), map.get("storageKey"),
                map.get("originalFilename"), map.getOrDefault("contentType", "application/octet-stream"),
                parseLong(map.get("fileSize")), parseLong(map.get("lastModified")), parseLong(map.get("partSize")),
                parseInt(map.get("totalParts")), blankToNull(map.get("completedTaskId")),
                parseInstant(map.get("createdAt")), parseInstant(map.get("updatedAt")));
    }

    private String sessionKey(String id) { return SESSION_PREFIX + id; }
    private String fingerprintKey(String fingerprint) { return FINGERPRINT_PREFIX + fingerprint; }
    private static String safe(String value) { return value == null ? "" : value; }
    private static String blankToNull(String value) { return value == null || value.isBlank() ? null : value; }
    private static long parseLong(String value) { try { return Long.parseLong(value); } catch (Exception e) { return 0; } }
    private static int parseInt(String value) { try { return Integer.parseInt(value); } catch (Exception e) { return 0; } }
    private static Instant parseInstant(String value) { try { return Instant.parse(value); } catch (Exception e) { return Instant.now(); } }
}
