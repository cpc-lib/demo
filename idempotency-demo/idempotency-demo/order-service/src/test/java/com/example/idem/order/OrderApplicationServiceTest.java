package com.example.idem.order;

import com.example.idem.core.IdempotencyCore.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class OrderApplicationServiceTest {

    private JdbcTemplate jdbc;
    private OrderApplicationService service;

    @BeforeEach
    void setUp() {
        var ds = new DriverManagerDataSource(
                "jdbc:h2:mem:order;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(ds);
        jdbc.execute("DROP TABLE IF EXISTS biz_order");
        jdbc.execute("DROP TABLE IF EXISTS idempotency_record");

        jdbc.execute("""
            CREATE TABLE idempotency_record(
              id BIGINT AUTO_INCREMENT PRIMARY KEY,
              scope VARCHAR(160) NOT NULL,
              key_hash CHAR(64) NOT NULL,
              request_hash CHAR(64) NOT NULL,
              status VARCHAR(16) NOT NULL,
              response_json CLOB,
              business_ref VARCHAR(128),
              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
              updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
              CONSTRAINT uk_scope_key UNIQUE(scope,key_hash)
            )
            """);

        jdbc.execute("""
            CREATE TABLE biz_order(
              id BIGINT AUTO_INCREMENT PRIMARY KEY,
              order_no VARCHAR(64) NOT NULL UNIQUE,
              tenant_id VARCHAR(64) NOT NULL,
              user_id BIGINT NOT NULL,
              item_name VARCHAR(128) NOT NULL,
              amount DECIMAL(18,2) NOT NULL,
              idempotency_key_hash CHAR(64) NOT NULL,
              status VARCHAR(32) NOT NULL,
              CONSTRAINT uk_order_idem UNIQUE(tenant_id,idempotency_key_hash)
            )
            """);

        Mutex noRedis = new Mutex() {
            @Override public LockAttempt tryAcquire(String k) {
                return new LockAttempt(false, false, null);
            }
            @Override public void release(String k, String t) {}
        };

        var template = new Template(
                new JdbcRepository(jdbc),
                noRedis,
                new ObjectMapper(),
                new TransactionTemplate(new DataSourceTransactionManager(ds)),
                Duration.ofMillis(300),
                Duration.ofMillis(10));

        service = new OrderApplicationService(template, jdbc);
    }

    @Test
    void sameRequestShouldCreateOnlyOneOrderAndReplay() {
        var req = new CreateOrderRequest(
                1001L, "keyboard", new BigDecimal("299.00"));

        var first = service.create("demo", "order-key-000001", req);
        var second = service.create("demo", "order-key-000001", req);

        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM biz_order", Integer.class);

        assertThat(second.orderNo()).isEqualTo(first.orderNo());
        assertThat(first.replayed()).isFalse();
        assertThat(second.replayed()).isTrue();
        assertThat(count).isEqualTo(1);
    }
}
