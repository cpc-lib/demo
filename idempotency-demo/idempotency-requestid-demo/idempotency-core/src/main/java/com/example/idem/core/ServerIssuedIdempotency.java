package com.example.idem.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Server-issued idempotency token implementation.
 *
 * Flow:
 *   1) backend issue token -> DB row status=ISSUED
 *   2) client sends Idempotency-Key on mutation
 *   3) SELECT ... FOR UPDATE serializes duplicates
 *   4) business write + SUCCESS response are committed in the SAME transaction
 *   5) retry sees SUCCESS and replays the original response
 *
 * If the transaction crashes/rolls back, token remains ISSUED and a retry can execute again.
 */
public final class ServerIssuedIdempotency {

    public static final String HEADER = "Idempotency-Key";

    private ServerIssuedIdempotency() {}

    public static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public enum Status { ISSUED, PROCESSING, SUCCESS }

    public record IssuedToken(String requestId, Instant expiresAt) {}

    public record TokenRecord(
            long id,
            String tokenHash,
            String scope,
            String requestHash,
            Status status,
            String responseJson,
            String businessRef,
            Instant expiresAt) {}

    public record Result<T>(T value, boolean replayed) {}

    public static class IdempotencyException extends RuntimeException {
        private final String code;
        public IdempotencyException(String code, String message) {
            super(message);
            this.code = code;
        }
        public String getCode() { return code; }
    }

    public static final class TokenNotFoundException extends IdempotencyException {
        public TokenNotFoundException() {
            super("IDEMPOTENCY_TOKEN_NOT_FOUND", "The server-issued idempotency token does not exist.");
        }
    }

    public static final class TokenExpiredException extends IdempotencyException {
        public TokenExpiredException() {
            super("IDEMPOTENCY_TOKEN_EXPIRED", "The server-issued idempotency token has expired before first use.");
        }
    }

    public static final class TokenScopeMismatchException extends IdempotencyException {
        public TokenScopeMismatchException() {
            super("IDEMPOTENCY_TOKEN_SCOPE_MISMATCH", "The idempotency token was issued for another operation or tenant.");
        }
    }

    public static final class KeyReusedException extends IdempotencyException {
        public KeyReusedException() {
            super("IDEMPOTENCY_KEY_REUSED", "The same Idempotency-Key was used with a different request payload.");
        }
    }

    public static final class InvalidKeyException extends IdempotencyException {
        public InvalidKeyException() {
            super("INVALID_IDEMPOTENCY_KEY", "Idempotency-Key length must be between 8 and 128 characters.");
        }
    }

    public interface Repository {
        void insertIssued(String tokenHash, String scope, Instant expiresAt);
        Optional<TokenRecord> findForUpdate(String tokenHash);
        void markProcessing(long id, String requestHash);
        void markSuccess(long id, String responseJson, String businessRef);
    }

    public static final class JdbcRepository implements Repository {
        private final JdbcTemplate jdbc;
        public JdbcRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

        @Override
        public void insertIssued(String tokenHash, String scope, Instant expiresAt) {
            jdbc.update("""
                    INSERT INTO idempotency_token(
                      token_hash,scope,status,expires_at
                    ) VALUES(?,?,'ISSUED',?)
                    """, tokenHash, scope, Timestamp.from(expiresAt));
        }

        @Override
        public Optional<TokenRecord> findForUpdate(String tokenHash) {
            return jdbc.query("""
                    SELECT id,token_hash,scope,request_hash,status,
                           response_json,business_ref,expires_at
                    FROM idempotency_token
                    WHERE token_hash=?
                    FOR UPDATE
                    """,
                    (rs, n) -> new TokenRecord(
                            rs.getLong("id"),
                            rs.getString("token_hash"),
                            rs.getString("scope"),
                            rs.getString("request_hash"),
                            Status.valueOf(rs.getString("status")),
                            rs.getString("response_json"),
                            rs.getString("business_ref"),
                            rs.getTimestamp("expires_at").toInstant()),
                    tokenHash).stream().findFirst();
        }

        @Override
        public void markProcessing(long id, String requestHash) {
            jdbc.update("""
                    UPDATE idempotency_token
                    SET status='PROCESSING',request_hash=?,updated_at=CURRENT_TIMESTAMP(3)
                    WHERE id=?
                    """, requestHash, id);
        }

        @Override
        public void markSuccess(long id, String responseJson, String businessRef) {
            jdbc.update("""
                    UPDATE idempotency_token
                    SET status='SUCCESS',response_json=?,business_ref=?,
                        updated_at=CURRENT_TIMESTAMP(3)
                    WHERE id=?
                    """, responseJson, businessRef, id);
        }
    }

    public static final class Template {
        private final Repository repository;
        private final ObjectMapper mapper;
        private final ObjectMapper canonicalMapper;
        private final TransactionTemplate tx;

        public Template(Repository repository, ObjectMapper mapper, TransactionTemplate tx) {
            this.repository = repository;
            this.mapper = mapper;
            this.canonicalMapper = mapper.copy()
                    .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                    .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
            this.tx = tx;
        }

        /** Issue a one-time server-side idempotency token. Raw token is returned once; DB stores only SHA-256. */
        public IssuedToken issue(String scope, Duration ttl) {
            if (ttl == null || ttl.isZero() || ttl.isNegative()) {
                throw new IllegalArgumentException("ttl must be positive");
            }
            String raw = "idem_" + UUID.randomUUID().toString().replace("-", "");
            Instant expiresAt = Instant.now().plus(ttl);
            tx.executeWithoutResult(s -> repository.insertIssued(
                    sha256(raw), scope, expiresAt));
            return new IssuedToken(raw, expiresAt);
        }

        public <T> Result<T> execute(
                String scope,
                String rawKey,
                Object request,
                Class<T> responseType,
                Supplier<T> action) {

            validate(rawKey);
            String tokenHash = sha256(rawKey);
            String requestHash = sha256(writeCanonical(request));

            Result<T> result = tx.execute(status -> {
                TokenRecord row = repository.findForUpdate(tokenHash)
                        .orElseThrow(TokenNotFoundException::new);

                if (!row.scope().equals(scope)) {
                    throw new TokenScopeMismatchException();
                }

                // SUCCESS remains replayable even after the original issue TTL.
                // Retention/cleanup is a separate policy (e.g. keep SUCCESS 24h/7d).
                if (row.status() == Status.SUCCESS) {
                    verifyPayload(row, requestHash);
                    return new Result<>(deserialize(row.responseJson(), responseType), true);
                }

                if (Instant.now().isAfter(row.expiresAt())) {
                    throw new TokenExpiredException();
                }

                if (row.requestHash() != null && !row.requestHash().equals(requestHash)) {
                    throw new KeyReusedException();
                }

                repository.markProcessing(row.id(), requestHash);
                T value = action.get();
                repository.markSuccess(row.id(), serialize(value), businessRef(value));
                return new Result<>(value, false);
            });

            if (result == null) throw new IllegalStateException("Transaction returned null");
            return result;
        }

        private void verifyPayload(TokenRecord row, String requestHash) {
            if (row.requestHash() == null || !row.requestHash().equals(requestHash)) {
                throw new KeyReusedException();
            }
        }

        private String writeCanonical(Object request) {
            try {
                return canonicalMapper.writeValueAsString(request);
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Cannot serialize request", e);
            }
        }

        private String serialize(Object value) {
            try {
                return mapper.writeValueAsString(value);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Cannot serialize idempotent response", e);
            }
        }

        private <T> T deserialize(String json, Class<T> type) {
            try {
                return mapper.readValue(json, type);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Cannot deserialize stored idempotent response", e);
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
