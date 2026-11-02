package com.example.fencing.repository;

import com.example.fencing.model.OrderRecord;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OrderRepository {
    private final JdbcTemplate jdbc;

    public OrderRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 返回 1 表示用户对该商品已有订单，0 表示没有。 */
    public int countByProductAndUser(String productId, String userId) {
        Integer n = jdbc.queryForObject("""
                SELECT COUNT(*) FROM seckill_order WHERE product_id = ? AND user_id = ?
                """, Integer.class, productId, userId);
        return n == null ? 0 : n;
    }

    public Optional<OrderRecord> find(String productId, String userId) {
        return jdbc.query("""
                SELECT order_id, product_id, user_id, fence_token
                FROM seckill_order WHERE product_id = ? AND user_id = ?
                """, rs -> rs.next() ? Optional.of(new OrderRecord(
                rs.getLong("order_id"),
                rs.getString("product_id"),
                rs.getString("user_id"),
                rs.getLong("fence_token"))) : Optional.empty(),
                productId, userId);
    }

    /** 插入订单；唯一索引冲突时抛 DuplicateKeyException（上层转 AlreadyPurchasedException）。 */
    public OrderRecord insert(String productId, String userId, long fenceToken) {
        try {
            jdbc.update("""
                    INSERT INTO seckill_order(product_id, user_id, fence_token)
                    VALUES (?, ?, ?)
                    """, productId, userId, fenceToken);
        } catch (DuplicateKeyException dke) {
            throw dke; // 让上层决定怎么处理
        }
        Long orderId = jdbc.queryForObject("""
                SELECT LAST_INSERT_ID()
                """, Long.class);
        return new OrderRecord(orderId, productId, userId, fenceToken);
    }
}
