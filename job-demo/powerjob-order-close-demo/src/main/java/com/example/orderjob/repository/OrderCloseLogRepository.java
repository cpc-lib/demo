package com.example.orderjob.repository;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OrderCloseLogRepository {

    private final JdbcTemplate jdbcTemplate;

    public OrderCloseLogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * uk_order_close_log_order_id 保证每个订单只保留一条“关闭成功”审计记录。
     */
    public void insertOnce(Long orderId, String orderNo, String triggerSource, String instanceId) {
        try {
            jdbcTemplate.update("""
                    INSERT INTO order_close_log(
                        order_id, order_no, trigger_source, scheduler_instance_id, created_at
                    ) VALUES (?, ?, ?, ?, NOW())
                    """, orderId, orderNo, triggerSource, instanceId);
        } catch (DuplicateKeyException ignored) {
            // 重试或重复调度时已存在日志，按幂等成功处理。
        }
    }

    public int countByOrderId(Long orderId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM order_close_log WHERE order_id = ?",
                Integer.class,
                orderId
        );
        return count == null ? 0 : count;
    }
}
