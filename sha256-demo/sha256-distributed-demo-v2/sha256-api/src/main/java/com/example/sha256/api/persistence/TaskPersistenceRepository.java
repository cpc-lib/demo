package com.example.sha256.api.persistence;

import com.example.sha256.api.outbox.OutboxEvent;
import com.example.sha256.common.model.Sha256TaskMessage;
import com.example.sha256.common.model.Sha256TaskRecord;
import com.example.sha256.common.model.TaskStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TaskPersistenceRepository {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public TaskPersistenceRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void createTaskAndOutbox(Sha256TaskRecord task, Sha256TaskMessage message) {
        jdbcTemplate.update("""
                INSERT INTO sha256_task
                (task_id, original_filename, storage_key, storage_bucket, total_bytes, broker, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, 'QUEUED', ?, ?)
                """,
                task.getTaskId(), task.getOriginalFilename(), task.getStorageKey(), task.getStorageBucket(),
                task.getTotalBytes(), task.getBroker(), Timestamp.from(task.getCreatedAt()), Timestamp.from(task.getUpdatedAt()));

        jdbcTemplate.update("""
                INSERT INTO outbox_event
                (event_id, aggregate_id, event_type, broker, payload, status, retry_count, next_retry_at, created_at, updated_at)
                VALUES (?, ?, 'SHA256_TASK_CREATED', ?, ?, 'PENDING', 0, NOW(3), NOW(3), NOW(3))
                """,
                UUID.randomUUID().toString().replace("-", ""), task.getTaskId(), task.getBroker(), toJson(message));
    }

    public Optional<Sha256TaskRecord> findTask(String taskId) {
        List<Sha256TaskRecord> rows = jdbcTemplate.query("""
                SELECT task_id, original_filename, storage_key, storage_bucket, total_bytes, broker, status,
                       created_at, updated_at
                FROM sha256_task WHERE task_id = ?
                """, (rs, rowNum) -> {
            Sha256TaskRecord record = new Sha256TaskRecord();
            record.setTaskId(rs.getString("task_id"));
            record.setOriginalFilename(rs.getString("original_filename"));
            record.setStorageKey(rs.getString("storage_key"));
            record.setStorageBucket(rs.getString("storage_bucket"));
            record.setTotalBytes(rs.getLong("total_bytes"));
            record.setProcessedBytes(0);
            record.setProgress(0);
            record.setBroker(rs.getString("broker"));
            record.setStatus(TaskStatus.valueOf(rs.getString("status")));
            record.setRetryCount(0);
            record.setCreatedAt(rs.getTimestamp("created_at").toInstant());
            record.setUpdatedAt(rs.getTimestamp("updated_at").toInstant());
            return record;
        }, taskId);
        return rows.stream().findFirst();
    }

    @Transactional
    public List<OutboxEvent> claimBatch(int batchSize, long leaseSeconds) {
        jdbcTemplate.update("""
                UPDATE outbox_event
                   SET status = 'FAILED', last_error = 'Outbox dispatcher lease expired', next_retry_at = NOW(3), updated_at = NOW(3)
                 WHERE status = 'SENDING'
                   AND updated_at < TIMESTAMPADD(SECOND, ?, NOW(3))
                """, -leaseSeconds);

        List<OutboxEvent> events = jdbcTemplate.query("""
                SELECT id, event_id, aggregate_id, event_type, broker, payload, retry_count
                  FROM outbox_event
                 WHERE status IN ('PENDING', 'FAILED')
                   AND next_retry_at <= NOW(3)
                 ORDER BY id
                 LIMIT ?
                 FOR UPDATE SKIP LOCKED
                """, (rs, rowNum) -> new OutboxEvent(
                rs.getLong("id"), rs.getString("event_id"), rs.getString("aggregate_id"),
                rs.getString("event_type"), rs.getString("broker"), rs.getString("payload"),
                rs.getInt("retry_count")), batchSize);

        for (OutboxEvent event : events) {
            jdbcTemplate.update("UPDATE outbox_event SET status='SENDING', updated_at=NOW(3) WHERE id=?", event.id());
        }
        return events;
    }

    public void markSent(long id) {
        jdbcTemplate.update("""
                UPDATE outbox_event
                   SET status='SENT', sent_at=NOW(3), last_error=NULL, updated_at=NOW(3)
                 WHERE id=?
                """, id);
    }

    public void markRetry(long id, int currentRetryCount, String error, long delayMillis) {
        Instant next = Instant.now().plusMillis(Math.max(1000, delayMillis));
        jdbcTemplate.update("""
                UPDATE outbox_event
                   SET status='FAILED', retry_count=?, last_error=?, next_retry_at=?, updated_at=NOW(3)
                 WHERE id=?
                """, currentRetryCount + 1, truncate(error, 1900), Timestamp.from(next), id);
    }

    private String toJson(Sha256TaskMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("序列化 Outbox 消息失败", e);
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
