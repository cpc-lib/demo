package com.example.scanlogin.infrastructure;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

@Repository
public class RefreshTokenRepository {
    private final JdbcTemplate jdbc;

    public RefreshTokenRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void create(long userId, String tokenHash, String client, Instant expiresAt) {
        jdbc.update("INSERT INTO refresh_token(user_id, token_hash, client, expires_at) VALUES (?,?,?,?)",
                userId, tokenHash, client, Timestamp.from(expiresAt));
    }

    /** Revoked rows are kept for audit and reuse detection, never deleted. */
    public Optional<Row> find(String tokenHash) {
        return jdbc.query("SELECT user_id, client, expires_at, revoked FROM refresh_token WHERE token_hash=?",
                        (rs, row) -> new Row(rs.getLong("user_id"), rs.getString("client"),
                                rs.getTimestamp("expires_at").toInstant(), rs.getBoolean("revoked")),
                        tokenHash)
                .stream().findFirst();
    }

    public void revoke(String tokenHash) {
        jdbc.update("UPDATE refresh_token SET revoked=1 WHERE token_hash=?", tokenHash);
    }

    /** Presenting a rotated token hints at theft: kill every live token of that client. */
    public void revokeAllActive(long userId, String client) {
        jdbc.update("UPDATE refresh_token SET revoked=1 WHERE user_id=? AND client=? AND revoked=0",
                userId, client);
    }

    /** Kick: kill every live token of the user across all clients. */
    public void revokeAllActiveForUser(long userId) {
        jdbc.update("UPDATE refresh_token SET revoked=1 WHERE user_id=? AND revoked=0", userId);
    }

    public record Row(long userId, String client, Instant expiresAt, boolean revoked) {
    }
}
