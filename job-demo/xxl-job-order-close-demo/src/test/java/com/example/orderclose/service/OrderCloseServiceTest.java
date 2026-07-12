package com.example.orderclose.service;

import com.example.orderclose.repository.OrderRepository;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class OrderCloseServiceTest {

    private JdbcTemplate jdbcTemplate;
    private OrderCloseService service;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:orderdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("DROP TABLE IF EXISTS biz_order");
        jdbcTemplate.execute("""
                CREATE TABLE biz_order (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    order_no VARCHAR(64) NOT NULL UNIQUE,
                    status TINYINT NOT NULL DEFAULT 0,
                    amount DECIMAL(18,2) NOT NULL,
                    created_at TIMESTAMP(3) NOT NULL,
                    expire_time TIMESTAMP(3) NOT NULL,
                    pay_time TIMESTAMP(3) NULL,
                    close_time TIMESTAMP(3) NULL,
                    close_reason VARCHAR(64) NULL,
                    version INT NOT NULL DEFAULT 0,
                    created_by VARCHAR(64) NULL,
                    updated_at TIMESTAMP(3) NOT NULL
                )
                """);

        OrderRepository repository = new OrderRepository(jdbcTemplate);
        service = new OrderCloseService(repository);
    }

    @Test
    void shouldCloseOnlyExpiredUnpaidOrdersAndRemainIdempotent() {
        insert("EXPIRED_UNPAID", 0, "DATEADD('MINUTE', -30, CURRENT_TIMESTAMP)");
        insert("FUTURE_UNPAID", 0, "DATEADD('MINUTE', 30, CURRENT_TIMESTAMP)");
        insert("EXPIRED_PAID", 1, "DATEADD('MINUTE', -30, CURRENT_TIMESTAMP)");
        insert("EXPIRED_CLOSED", 2, "DATEADD('MINUTE', -30, CURRENT_TIMESTAMP)");

        OrderCloseResult first = service.closeTimeoutOrders(100, 10);
        OrderCloseResult second = service.closeTimeoutOrders(100, 10);

        assertThat(first.scanned()).isEqualTo(1);
        assertThat(first.closed()).isEqualTo(1);
        assertThat(first.skipped()).isZero();
        assertThat(second.scanned()).isZero();
        assertThat(second.closed()).isZero();

        MapRow closed = load("EXPIRED_UNPAID");
        assertThat(closed.status()).isEqualTo(2);
        assertThat(closed.closeReason()).isEqualTo("PAY_TIMEOUT");
        assertThat(closed.version()).isEqualTo(1);

        assertThat(load("FUTURE_UNPAID").status()).isZero();
        assertThat(load("EXPIRED_PAID").status()).isEqualTo(1);
        assertThat(load("EXPIRED_CLOSED").status()).isEqualTo(2);
    }

    @Test
    void paymentCallbackWinningTheRaceMustPreventClose() {
        insert("RACE_ORDER", 0, "DATEADD('MINUTE', -1, CURRENT_TIMESTAMP)");

        int paidRows = jdbcTemplate.update("""
                UPDATE biz_order
                SET status = 1, pay_time = CURRENT_TIMESTAMP, version = version + 1
                WHERE order_no = 'RACE_ORDER' AND status = 0
                """);

        OrderCloseResult result = service.closeTimeoutOrders(100, 10);

        assertThat(paidRows).isEqualTo(1);
        assertThat(result.closed()).isZero();
        assertThat(load("RACE_ORDER").status()).isEqualTo(1);
    }

    private void insert(String orderNo, int status, String expireExpression) {
        jdbcTemplate.execute("""
                INSERT INTO biz_order (
                    order_no, status, amount, created_at, expire_time,
                    pay_time, close_time, close_reason, version, created_by, updated_at
                ) VALUES (
                    '%s', %d, 99.00, CURRENT_TIMESTAMP, %s,
                    NULL, NULL, NULL, 0, 'test', CURRENT_TIMESTAMP
                )
                """.formatted(orderNo, status, expireExpression));
    }

    private MapRow load(String orderNo) {
        return jdbcTemplate.queryForObject("""
                SELECT status, close_reason, version
                FROM biz_order
                WHERE order_no = ?
                """, (rs, rowNum) -> new MapRow(
                rs.getInt("status"),
                rs.getString("close_reason"),
                rs.getInt("version")
        ), orderNo);
    }

    private record MapRow(int status, String closeReason, int version) {
    }
}
