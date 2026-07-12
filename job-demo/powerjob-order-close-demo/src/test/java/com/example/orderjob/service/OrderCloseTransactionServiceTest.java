package com.example.orderjob.service;

import com.example.orderjob.domain.CloseOutcome;
import com.example.orderjob.domain.CloseResult;
import com.example.orderjob.domain.OrderStatus;
import com.example.orderjob.repository.OrderCloseLogRepository;
import com.example.orderjob.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@Import({OrderRepository.class, OrderCloseLogRepository.class, OrderCloseTransactionService.class})
class OrderCloseTransactionServiceTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderCloseLogRepository closeLogRepository;

    @Autowired
    private OrderCloseTransactionService service;

    @Test
    void repeatedCloseMustBeIdempotent() {
        jdbcTemplate.update("""
                INSERT INTO biz_order(
                    order_no, status, amount, created_at, expire_time,
                    version, created_by, updated_at
                ) VALUES ('TEST-001', 0, 10.00, DATEADD('MINUTE', -30, CURRENT_TIMESTAMP),
                          DATEADD('MINUTE', -10, CURRENT_TIMESTAMP), 0, 'test', CURRENT_TIMESTAMP)
                """);
        Long orderId = jdbcTemplate.queryForObject(
                "SELECT id FROM biz_order WHERE order_no='TEST-001'", Long.class);

        CloseResult first = service.closeOne(orderId, "TEST", "instance-1");
        CloseResult second = service.closeOne(orderId, "TEST", "instance-2");

        assertThat(first.outcome()).isEqualTo(CloseOutcome.CLOSED);
        assertThat(second.outcome()).isEqualTo(CloseOutcome.ALREADY_HANDLED);
        assertThat(orderRepository.findById(orderId).orElseThrow().status()).isEqualTo(OrderStatus.CLOSED);
        assertThat(closeLogRepository.countByOrderId(orderId)).isEqualTo(1);
    }

    @Test
    void paidOrderMustNeverBeClosed() {
        jdbcTemplate.update("""
                INSERT INTO biz_order(
                    order_no, status, amount, created_at, expire_time,
                    pay_time, version, created_by, updated_at
                ) VALUES ('TEST-PAID', 1, 20.00, DATEADD('MINUTE', -60, CURRENT_TIMESTAMP),
                          DATEADD('MINUTE', -30, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP,
                          1, 'test', CURRENT_TIMESTAMP)
                """);
        Long orderId = jdbcTemplate.queryForObject(
                "SELECT id FROM biz_order WHERE order_no='TEST-PAID'", Long.class);

        CloseResult result = service.closeOne(orderId, "TEST", "instance-1");

        assertThat(result.outcome()).isEqualTo(CloseOutcome.ALREADY_HANDLED);
        assertThat(orderRepository.findById(orderId).orElseThrow().status()).isEqualTo(OrderStatus.PAID);
        assertThat(closeLogRepository.countByOrderId(orderId)).isZero();
    }
}
