package com.example.fencing.repository;

import com.example.fencing.model.OperationRecord;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OperationRepository {
    private final JdbcTemplate jdbc;

    public OperationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<OperationRecord> find(String resourceId, String requestId) {
        return jdbc.query("""
                SELECT resource_id, request_id, delta_amount, applied_value, fence_token
                FROM fenced_operation WHERE resource_id = ? AND request_id = ?
                """, rs -> rs.next() ? Optional.of(new OperationRecord(
                rs.getString("resource_id"), rs.getString("request_id"),
                rs.getLong("delta_amount"), rs.getLong("applied_value"),
                rs.getLong("fence_token"))) : Optional.empty(),
                resourceId, requestId);
    }

    public void insert(String resourceId, String requestId, long delta,
            long appliedValue, long fenceToken) {
        jdbc.update("""
                INSERT INTO fenced_operation
                  (resource_id, request_id, delta_amount, applied_value, fence_token)
                VALUES (?, ?, ?, ?, ?)
                """, resourceId, requestId, delta, appliedValue, fenceToken);
    }
}
