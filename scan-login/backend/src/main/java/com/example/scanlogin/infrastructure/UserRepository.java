package com.example.scanlogin.infrastructure;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public class UserRepository {
    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static String truncate(String v, int max) {
        return v == null ? null : v.substring(0, Math.min(max, v.length()));
    }

    public long create(String username, String hash) {
        var key = new org.springframework.jdbc.support.GeneratedKeyHolder();
        jdbc.update(con -> {
            var statement = con.prepareStatement(
                    "INSERT INTO app_user(username, password_hash, enabled) VALUES (?, ?, 1)",
                    java.sql.Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, username);
            statement.setString(2, hash);
            return statement;
        }, key);
        return java.util.Objects.requireNonNull(key.getKey()).longValue();
    }

    public Optional<User> findByUsername(String username) {
        return jdbc.query("SELECT id, username, password_hash, enabled FROM app_user WHERE username=?",
                        (rs, row) -> new User(rs.getLong("id"), rs.getString("username"),
                                rs.getString("password_hash"), rs.getBoolean("enabled")), username)
                .stream().findFirst();
    }

    public Optional<User> findById(long userId) {
        return jdbc.query("SELECT id, username, password_hash, enabled FROM app_user WHERE id=?",
                        (rs, row) -> new User(rs.getLong("id"), rs.getString("username"),
                                rs.getString("password_hash"), rs.getBoolean("enabled")), userId)
                .stream().findFirst();
    }

    /** Admin console listing; includes the live refresh-token count per user. */
    public java.util.List<UserSummary> listSummaries() {
        return jdbc.query("""
                        SELECT u.id, u.username, u.enabled, u.token_version, u.created_at,
                               (SELECT COUNT(*) FROM refresh_token r
                                WHERE r.user_id = u.id AND r.revoked = 0) AS live_tokens
                        FROM app_user u ORDER BY u.id""",
                (rs, row) -> new UserSummary(rs.getLong("id"), rs.getString("username"),
                        rs.getBoolean("enabled"), rs.getLong("token_version"),
                        rs.getTimestamp("created_at").toInstant(), rs.getLong("live_tokens")));
    }

    public record UserSummary(long id, String username, boolean enabled, long tokenVersion,
                              Instant createdAt, long activeRefreshTokens) {
    }

    public void audit(long userId, String event, String ip, String userAgent) {
        jdbc.update("INSERT INTO login_audit(user_id, event_type, ip, user_agent) VALUES (?,?,?,?)",
                userId, event, truncate(ip, 45), truncate(userAgent, 255));
    }

    /** Current session version; -1 when the user no longer exists (rejects their tokens). */
    public long tokenVersion(long userId) {
        var rows = jdbc.queryForList("SELECT token_version FROM app_user WHERE id=?", Long.class, userId);
        return rows.isEmpty() ? -1L : rows.get(0);
    }

    /** Kick primitive: invalidates every token ever issued for this user. */
    public void bumpTokenVersion(long userId) {
        jdbc.update("UPDATE app_user SET token_version = token_version + 1 WHERE id=?", userId);
    }

    public record User(long id, String username, String passwordHash, boolean enabled) {
    }
}
