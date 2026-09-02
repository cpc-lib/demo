package com.example.idem.order;

import com.example.idem.core.ServerIssuedIdempotency;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class OrderApplicationServiceTest {

    private JdbcTemplate jdbc;
    private OrderApplicationService service;

    @BeforeEach
    void setUp() {
        var ds = new DriverManagerDataSource(
                "jdbc:h2:mem:order_server_issued;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000", "sa", "");
        jdbc = new JdbcTemplate(ds);
        jdbc.execute("DROP TABLE IF EXISTS biz_order");
        jdbc.execute("DROP TABLE IF EXISTS idempotency_token");

        jdbc.execute("""
            CREATE TABLE idempotency_token(
              id BIGINT AUTO_INCREMENT PRIMARY KEY,
              token_hash CHAR(64) NOT NULL UNIQUE,
              scope VARCHAR(160) NOT NULL,
              request_hash CHAR(64),
              status VARCHAR(16) NOT NULL,
              response_json CLOB,
              business_ref VARCHAR(128),
              expires_at TIMESTAMP NOT NULL,
              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
              updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
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

        var tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        var template = new ServerIssuedIdempotency.Template(
                new ServerIssuedIdempotency.JdbcRepository(jdbc),
                new ObjectMapper(), tx);

        service = new OrderApplicationService(template, jdbc);
    }

    @Test
    void backendIssuedRequestIdShouldCreateOnlyOneOrderAndReplay() {
        var token = service.issueCreateOrderRequestId("demo");
        var req = new CreateOrderRequest(
                1001L, "keyboard", new BigDecimal("299.00"));

        var first = service.create("demo", token.requestId(), req);
        var second = service.create("demo", token.requestId(), req);

        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM biz_order", Integer.class);
        assertThat(first.replayed()).isFalse();
        assertThat(second.replayed()).isTrue();
        assertThat(second.orderNo()).isEqualTo(first.orderNo());
        assertThat(count).isEqualTo(1);
    }

    @Test
    void requestIdCannotCrossTenantScope() {
        var token = service.issueCreateOrderRequestId("tenant-a");
        var req = new CreateOrderRequest(1001L, "keyboard", new BigDecimal("299.00"));

        assertThatThrownBy(() -> service.create("tenant-b", token.requestId(), req))
                .isInstanceOf(ServerIssuedIdempotency.TokenScopeMismatchException.class);
    }
}
