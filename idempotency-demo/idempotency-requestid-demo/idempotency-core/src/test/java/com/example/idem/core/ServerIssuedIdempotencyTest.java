package com.example.idem.core;

import com.example.idem.core.ServerIssuedIdempotency.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

class ServerIssuedIdempotencyTest {

    private JdbcTemplate jdbc;
    private Template template;

    record Cmd(String item, int qty) {}
    record Resp(String id) {
        public String businessRef() { return id; }
    }

    @BeforeEach
    void setUp() {
        var ds = new DriverManagerDataSource(
                "jdbc:h2:mem:server_issued;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000", "sa", "");
        jdbc = new JdbcTemplate(ds);
        jdbc.execute("DROP TABLE IF EXISTS idempotency_token");
        jdbc.execute("""
            CREATE TABLE idempotency_token(
              id BIGINT AUTO_INCREMENT PRIMARY KEY,
              token_hash CHAR(64) NOT NULL,
              scope VARCHAR(160) NOT NULL,
              request_hash CHAR(64),
              status VARCHAR(16) NOT NULL,
              response_json CLOB,
              business_ref VARCHAR(128),
              expires_at TIMESTAMP NOT NULL,
              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
              updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
              CONSTRAINT uk_token_hash UNIQUE(token_hash)
            )
            """);
        template = new Template(
                new JdbcRepository(jdbc),
                new ObjectMapper(),
                new TransactionTemplate(new DataSourceTransactionManager(ds)));
    }

    @Test
    void issuedTokenExecutesOnceThenReplays() {
        IssuedToken token = template.issue("demo:order:create", Duration.ofMinutes(5));
        AtomicInteger calls = new AtomicInteger();
        Cmd cmd = new Cmd("keyboard", 1);

        Result<Resp> first = template.execute(
                "demo:order:create", token.requestId(), cmd, Resp.class,
                () -> { calls.incrementAndGet(); return new Resp("ORD-1"); });
        Result<Resp> second = template.execute(
                "demo:order:create", token.requestId(), cmd, Resp.class,
                () -> { calls.incrementAndGet(); return new Resp("ORD-2"); });

        assertThat(first.replayed()).isFalse();
        assertThat(second.replayed()).isTrue();
        assertThat(second.value().id()).isEqualTo("ORD-1");
        assertThat(calls).hasValue(1);
    }

    @Test
    void sameTokenWithDifferentPayloadIsRejected() {
        IssuedToken token = template.issue("demo:order:create", Duration.ofMinutes(5));
        template.execute("demo:order:create", token.requestId(),
                new Cmd("keyboard", 1), Resp.class, () -> new Resp("ORD-1"));

        assertThatThrownBy(() -> template.execute(
                "demo:order:create", token.requestId(),
                new Cmd("monitor", 1), Resp.class, () -> new Resp("ORD-2")))
                .isInstanceOf(KeyReusedException.class);
    }

    @Test
    void failedBusinessTransactionRollsTokenBackToReusableState() {
        IssuedToken token = template.issue("demo:order:create", Duration.ofMinutes(5));
        Cmd cmd = new Cmd("keyboard", 1);

        assertThatThrownBy(() -> template.execute(
                "demo:order:create", token.requestId(), cmd, Resp.class,
                () -> { throw new IllegalStateException("boom"); }))
                .isInstanceOf(IllegalStateException.class);

        Result<Resp> retry = template.execute(
                "demo:order:create", token.requestId(), cmd, Resp.class,
                () -> new Resp("ORD-OK"));

        assertThat(retry.replayed()).isFalse();
        assertThat(retry.value().id()).isEqualTo("ORD-OK");
    }

    @Test
    void unknownTokenIsRejected() {
        assertThatThrownBy(() -> template.execute(
                "demo:order:create", "idem_12345678901234567890",
                new Cmd("keyboard", 1), Resp.class, () -> new Resp("ORD-X")))
                .isInstanceOf(TokenNotFoundException.class);
    }
}
