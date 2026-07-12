package com.example.orderjob.repository;

import com.example.orderjob.domain.OrderSnapshot;
import com.example.orderjob.domain.OrderStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class OrderRepository {

    private static final String BASE_COLUMNS = """
            id, order_no, status, amount, created_at, expire_time,
            pay_time, close_time, version, close_reason
            """;

    private static final RowMapper<OrderSnapshot> ORDER_MAPPER = (rs, rowNum) -> new OrderSnapshot(
            rs.getLong("id"),
            rs.getString("order_no"),
            OrderStatus.fromCode(rs.getInt("status")),
            rs.getBigDecimal("amount"),
            toLocalDateTime(rs.getTimestamp("created_at")),
            toLocalDateTime(rs.getTimestamp("expire_time")),
            toLocalDateTime(rs.getTimestamp("pay_time")),
            toLocalDateTime(rs.getTimestamp("close_time")),
            rs.getInt("version"),
            rs.getString("close_reason")
    );

    private final JdbcTemplate jdbcTemplate;

    public OrderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Long> findExpiredUnpaidIds(long lastId, int limit) {
        return jdbcTemplate.queryForList("""
                SELECT id
                FROM biz_order
                WHERE id > ?
                  AND status = ?
                  AND expire_time <= NOW()
                ORDER BY id ASC
                LIMIT ?
                """, Long.class, lastId, OrderStatus.UNPAID.getCode(), limit);
    }

    /**
     * 核心幂等 CAS：仅允许“仍是未支付且确实已超时”的订单被关闭。
     */
    public int closeExpiredUnpaid(Long orderId, String reason) {
        return jdbcTemplate.update("""
                UPDATE biz_order
                SET status = ?,
                    close_time = NOW(),
                    close_reason = ?,
                    version = version + 1,
                    updated_at = NOW()
                WHERE id = ?
                  AND status = ?
                  AND expire_time <= NOW()
                """,
                OrderStatus.CLOSED.getCode(),
                reason,
                orderId,
                OrderStatus.UNPAID.getCode());
    }

    /**
     * 支付回调也采用 CAS。支付和关闭并发时，只有一个 UPDATE 能成功。
     */
    public int markPaid(Long orderId) {
        return jdbcTemplate.update("""
                UPDATE biz_order
                SET status = ?,
                    pay_time = NOW(),
                    version = version + 1,
                    updated_at = NOW()
                WHERE id = ?
                  AND status = ?
                """,
                OrderStatus.PAID.getCode(),
                orderId,
                OrderStatus.UNPAID.getCode());
    }

    public Optional<OrderSnapshot> findById(Long orderId) {
        List<OrderSnapshot> list = jdbcTemplate.query(
                "SELECT " + BASE_COLUMNS + " FROM biz_order WHERE id = ?",
                ORDER_MAPPER,
                orderId
        );
        return list.stream().findFirst();
    }

    public List<OrderSnapshot> findLatest(int limit) {
        return jdbcTemplate.query(
                "SELECT " + BASE_COLUMNS + " FROM biz_order ORDER BY id DESC LIMIT ?",
                ORDER_MAPPER,
                limit
        );
    }

    public long createDemoOrder(BigDecimal amount, LocalDateTime expireTime) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        String orderNo = "DEMO" + System.currentTimeMillis();
        int updated = jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO biz_order(
                        order_no, status, amount, created_at, expire_time,
                        version, created_by, updated_at
                    ) VALUES (?, ?, ?, NOW(), ?, 0, 'demo-api', NOW())
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, orderNo);
            statement.setInt(2, OrderStatus.UNPAID.getCode());
            statement.setBigDecimal(3, amount);
            statement.setTimestamp(4, Timestamp.valueOf(expireTime));
            return statement;
        }, keyHolder);
        if (updated != 1 || keyHolder.getKey() == null) {
            throw new IllegalStateException("Failed to create demo order");
        }
        return keyHolder.getKey().longValue();
    }

    private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
