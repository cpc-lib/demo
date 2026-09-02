package com.example.idem.core;

import com.example.idem.core.IdempotencyCore.*;
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

class IdempotencyCoreTest {

    private Template template;

    @BeforeEach
    void setUp() {
        var ds = new DriverManagerDataSource(
                "jdbc:h2:mem:idem;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        var jdbc = new JdbcTemplate(ds);
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

        Mutex mutex = new Mutex() {
            @Override public LockAttempt tryAcquire(String k) {
                return new LockAttempt(false, false, null); // simulate Redis unavailable
            }
            @Override public void release(String k, String t) {}
        };

        template = new Template(
                new JdbcRepository(jdbc),
                mutex,
                new ObjectMapper(),
                new TransactionTemplate(new DataSourceTransactionManager(ds)),
                Duration.ofMillis(300),
                Duration.ofMillis(10));
    }

    @Test
    void sameKeySamePayloadShouldReplayWithoutExecutingTwice() {
        AtomicInteger executions = new AtomicInteger();
        var command = new Command(1L, "keyboard");

        var first = template.execute("demo:order:create", "request-00000001",
                command, Response.class, () -> {
                    executions.incrementAndGet();
                    return new Response("ORD-1");
                });

        var second = template.execute("demo:order:create", "request-00000001",
                command, Response.class, () -> {
                    executions.incrementAndGet();
                    return new Response("ORD-2");
                });

        assertThat(first.replayed()).isFalse();
        assertThat(second.replayed()).isTrue();
        assertThat(second.value().orderNo()).isEqualTo("ORD-1");
        assertThat(executions).hasValue(1);
    }

    @Test
    void sameKeyDifferentPayloadShouldReject() {
        template.execute("demo:order:create", "request-00000002",
                new Command(1L, "keyboard"), Response.class,
                () -> new Response("ORD-1"));

        assertThatThrownBy(() ->
                template.execute("demo:order:create", "request-00000002",
                        new Command(1L, "monitor"), Response.class,
                        () -> new Response("ORD-2")))
                .isInstanceOf(KeyReusedException.class);
    }

    @Test
    void failedBusinessTransactionShouldNotCacheSuccess() {
        AtomicInteger executions = new AtomicInteger();

        assertThatThrownBy(() ->
                template.execute("demo:order:create", "request-00000003",
                        new Command(1L, "keyboard"), Response.class, () -> {
                            executions.incrementAndGet();
                            throw new IllegalStateException("boom");
                        }))
                .isInstanceOf(IllegalStateException.class);

        var retry = template.execute("demo:order:create", "request-00000003",
                new Command(1L, "keyboard"), Response.class, () -> {
                    executions.incrementAndGet();
                    return new Response("ORD-OK");
                });

        assertThat(retry.value().orderNo()).isEqualTo("ORD-OK");
        assertThat(executions).hasValue(2);
    }

    record Command(Long userId, String item) {}
    record Response(String orderNo) {}
}
