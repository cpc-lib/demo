package com.example.fencing.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The authoritative monotonic token issuer is MySQL, NOT Redis INCR.
 * The issued token must commit before the business method executes.
 */
@Service
public class MySqlFenceIssuer {
    private final JdbcTemplate jdbc;

    public MySqlFenceIssuer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public long issue(String resourceId) {
        // Never delete/recreate rows: that could reset tokens and break monotonicity.
        jdbc.update("""
                INSERT IGNORE INTO fenced_counter(resource_id, counter_value, fence_token)
                VALUES (?, 0, 0)
                """, resourceId);
        int affected = jdbc.update("""
                UPDATE fenced_counter SET fence_token = fence_token + 1
                WHERE resource_id = ? AND fence_token < 9223372036854775807
                """, resourceId);
        if (affected != 1) {
            throw new IllegalStateException("Fencing token exhausted or resource missing");
        }
        return jdbc.queryForObject("""
                SELECT fence_token FROM fenced_counter WHERE resource_id = ?
                """, Long.class, resourceId);
    }
}
