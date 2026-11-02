package com.example.fencing.repository;

import com.example.fencing.model.OrderRecord;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 秒杀幂等记录表：(product_id, request_id) 唯一。 */
@Repository
public class SeckillOperationRepository {
    private final JdbcTemplate jdbc;

    public SeckillOperationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<OrderRecord> find(String productId, String requestId) {
        return jdbc.query("""
                SELECT o.order_id, o.product_id, o.user_id, o.fence_token
                FROM seckill_operation op
                JOIN seckill_order o ON o.order_id = op.order_id
                WHERE op.product_id = ? AND op.request_id = ?
                """, rs -> rs.next() ? Optional.of(new OrderRecord(
                rs.getLong("order_id"),
                rs.getString("product_id"),
                rs.getString("user_id"),
                rs.getLong("fence_token"))) : Optional.empty(),
                productId, requestId);
    }

    public void insert(String productId, String requestId, String userId,
                       long fenceToken, long orderId) {
        jdbc.update("""
                INSERT INTO seckill_operation
                  (product_id, request_id, user_id, fence_token, order_id)
                VALUES (?, ?, ?, ?, ?)
                """, productId, requestId, userId, fenceToken, orderId);
    }
}
