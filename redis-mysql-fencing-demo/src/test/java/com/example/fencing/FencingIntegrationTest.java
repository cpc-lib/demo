package com.example.fencing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.fencing.error.IdempotencyConflictException;
import com.example.fencing.error.StaleFenceException;
import com.example.fencing.model.MutationResponse;
import com.example.fencing.repository.CounterRepository;
import com.example.fencing.service.CounterMutationService;
import com.example.fencing.service.MySqlFenceIssuer;
import com.example.fencing.service.RedisLockCoordinator;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class FencingIntegrationTest {
    private static final String TEST_REDIS_PASSWORD = "integration-redis-secret";

    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("fencing_demo")
            .withUsername("fencing_app")
            .withPassword("integration-mysql-secret");

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>("redis:7.4")
            .withExposedPorts(6379)
            .withCommand("redis-server", "--requirepass", TEST_REDIS_PASSWORD);

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", mysql::getJdbcUrl);
        r.add("spring.datasource.username", mysql::getUsername);
        r.add("spring.datasource.password", mysql::getPassword);
        r.add("app.redis.mode", () -> "single");
        r.add("app.redis.nodes", () -> "redis://" + redis.getHost()
                + ":" + redis.getMappedPort(6379));
        r.add("app.redis.password", () -> TEST_REDIS_PASSWORD);
        r.add("app.lock.wait-seconds", () -> "15");
    }

    @Autowired MySqlFenceIssuer issuer;
    @Autowired CounterMutationService writer;
    @Autowired RedisLockCoordinator coordinator;
    @Autowired CounterRepository repository;

    @Test
    void staleWriterMustNotModifyAfterNewTokenCommitted() {
        String id = "t_" + UUID.randomUUID();
        long oldToken = issuer.issue(id);
        long newToken = issuer.issue(id);
        assertThat(newToken).isGreaterThan(oldToken);

        assertThatThrownBy(() -> writer.apply(id, "old_request", 9, oldToken))
                .isInstanceOf(StaleFenceException.class);
        assertThat(repository.find(id).orElseThrow().value()).isZero();

        MutationResponse result = writer.apply(id, "new_request", 7, newToken);
        assertThat(result.value()).isEqualTo(7);
        assertThat(repository.find(id).orElseThrow().fenceToken()).isEqualTo(newToken);
    }

    @Test
    void concurrentRequestsDoNotLoseUpdates() throws Exception {
        String id = "t_" + UUID.randomUUID();
        ExecutorService pool = Executors.newFixedThreadPool(6);
        try {
            List<Future<MutationResponse>> futures = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                final String requestId = "parallel_" + i;
                futures.add(pool.submit(() -> coordinator.add(id, requestId, 1)));
            }
            for (Future<MutationResponse> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }
            assertThat(repository.find(id).orElseThrow().value()).isEqualTo(6);
            assertThat(repository.find(id).orElseThrow().fenceToken()).isEqualTo(6);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void idempotencyKeyPreventsDuplicateMutations() {
        String id = "t_" + UUID.randomUUID();
        MutationResponse first = coordinator.add(id, "request_a", 5);
        MutationResponse repeated = coordinator.add(id, "request_a", 5);
        assertThat(first.replayed()).isFalse();
        assertThat(repeated.replayed()).isTrue();
        assertThat(repeated.value()).isEqualTo(5);
        assertThat(repository.find(id).orElseThrow().value()).isEqualTo(5);
        assertThat(repository.find(id).orElseThrow().fenceToken())
                .isEqualTo(first.fenceToken());
        assertThatThrownBy(() -> coordinator.add(id, "request_a", 8))
                .isInstanceOf(IdempotencyConflictException.class);
    }
}
