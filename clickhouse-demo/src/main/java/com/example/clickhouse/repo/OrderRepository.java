package com.example.clickhouse.repo;

import com.example.clickhouse.web.dto.OrderCreateRequest;
import com.example.clickhouse.web.dto.OrderResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class OrderRepository {

    private final JdbcTemplate jdbcTemplate;

    public OrderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String ping() {
        return jdbcTemplate.queryForObject("SELECT 'OK'", String.class);
    }

    public long countOrders() {
        Long cnt = jdbcTemplate.queryForObject("SELECT count() FROM t_order", Long.class);
        return cnt == null ? 0L : cnt;
    }

    public List<OrderResponse> listOrders(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 1000));
        String sql = "SELECT id, user_id, amount, status, created_at FROM t_order ORDER BY created_at DESC, id DESC LIMIT ?";
        return jdbcTemplate.query(sql, rowMapper(), safeLimit);
    }

    public void insertOrder(OrderCreateRequest req) {
        // created_at use ClickHouse now() to avoid timezone mismatch
        String sql = "INSERT INTO t_order (id, user_id, amount, status, created_at) VALUES (?, ?, ?, ?, now())";
        jdbcTemplate.update(sql, req.getId(), req.getUserId(), req.getAmount(), req.getStatus());
    }

    private RowMapper<OrderResponse> rowMapper() {
        return (rs, rowNum) -> {
            long id = rs.getLong("id");
            long userId = rs.getLong("user_id");
            java.math.BigDecimal amount = rs.getBigDecimal("amount");
            String status = rs.getString("status");
            Timestamp ts = rs.getTimestamp("created_at");
            LocalDateTime createdAt = ts == null ? null : ts.toLocalDateTime();
            return new OrderResponse(id, userId, amount, status, createdAt);
        };
    }
}
