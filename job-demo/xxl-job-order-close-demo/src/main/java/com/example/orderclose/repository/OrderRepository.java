package com.example.orderclose.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class OrderRepository {

    private static final int STATUS_UNPAID = 0;
    private static final int STATUS_CLOSED = 2;

    private final JdbcTemplate jdbcTemplate;

    public OrderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Keyset pagination avoids OFFSET performance degradation and keeps the timeout index usable.
     */
    public List<Long> findExpiredUnpaidOrderIds(long lastId, int limit) {
        String sql = """
                SELECT id
                FROM biz_order
                WHERE status = ?
                  AND expire_time <= CURRENT_TIMESTAMP(3)
                  AND id > ?
                ORDER BY id
                LIMIT ?
                """;
        return jdbcTemplate.queryForList(sql, Long.class, STATUS_UNPAID, lastId, limit);
    }

    /**
     * Atomic compare-and-set close operation.
     *
     * The status and expire_time conditions are deliberately repeated in the UPDATE statement.
     * If a payment callback wins the race and changes status to PAID first, this update affects 0 rows.
     * Repeated XXL-JOB executions also affect 0 rows after the first successful close.
     */
    public int closeIfStillExpiredAndUnpaid(long orderId) {
        String sql = """
                UPDATE biz_order
                SET status = ?,
                    close_time = CURRENT_TIMESTAMP(3),
                    close_reason = 'PAY_TIMEOUT',
                    version = version + 1,
                    updated_at = CURRENT_TIMESTAMP(3)
                WHERE id = ?
                  AND status = ?
                  AND expire_time <= CURRENT_TIMESTAMP(3)
                """;
        return jdbcTemplate.update(sql, STATUS_CLOSED, orderId, STATUS_UNPAID);
    }

    public int countExpiredUnpaidOrders() {
        String sql = """
                SELECT COUNT(*)
                FROM biz_order
                WHERE status = ?
                  AND expire_time <= CURRENT_TIMESTAMP(3)
                """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, STATUS_UNPAID);
        return count == null ? 0 : count;
    }
}
