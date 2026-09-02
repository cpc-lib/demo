package com.example.idem.order;

import com.example.idem.core.IdempotencyCore;
import com.example.idem.core.IdempotencyCore.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.context.annotation.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
class IdempotencyConfig {
    @Bean Repository idemRepository(JdbcTemplate jdbc) {
        return new JdbcRepository(jdbc);
    }

    @Bean Mutex idemMutex(StringRedisTemplate redis) {
        return new RedisMutex(redis, Duration.ofSeconds(30));
    }

    @Bean Template idemTemplate(
            Repository repository,
            Mutex mutex,
            ObjectMapper mapper,
            PlatformTransactionManager txManager) {
        return new Template(
                repository, mutex, mapper,
                new TransactionTemplate(txManager),
                Duration.ofSeconds(2),
                Duration.ofMillis(50));
    }
}

record CreateOrderRequest(
        @NotNull Long userId,
        @NotBlank String itemName,
        @NotNull @DecimalMin("0.01") BigDecimal amount) {}

record CreateOrderResponse(String orderNo, String status, boolean replayed) {
    public String businessRef() { return orderNo; }
    CreateOrderResponse asReplay() {
        return new CreateOrderResponse(orderNo, status, true);
    }
}

@Service
class OrderApplicationService {
    private final Template template;
    private final JdbcTemplate jdbc;

    OrderApplicationService(Template template, JdbcTemplate jdbc) {
        this.template = template;
        this.jdbc = jdbc;
    }

    CreateOrderResponse create(String tenantId, String key, CreateOrderRequest request) {
        Result<CreateOrderResponse> result = template.execute(
                tenantId + ":order:create",
                key,
                request,
                CreateOrderResponse.class,
                () -> insertOrder(tenantId, key, request));

        return result.replayed() ? result.value().asReplay() : result.value();
    }

    private CreateOrderResponse insertOrder(
            String tenantId, String key, CreateOrderRequest request) {
        String orderNo = "ORD-" + UUID.randomUUID()
                .toString().replace("-", "").substring(0, 20);

        jdbc.update("""
                INSERT INTO biz_order(
                  order_no,tenant_id,user_id,item_name,amount,
                  idempotency_key_hash,status
                ) VALUES(?,?,?,?,?,?,'CREATED')
                """,
                orderNo, tenantId, request.userId(), request.itemName(),
                request.amount(), IdempotencyCore.sha256(key));

        return new CreateOrderResponse(orderNo, "CREATED", false);
    }
}

@RestController
@RequestMapping("/internal/orders")
class OrderController {
    private final OrderApplicationService service;
    private final Set<String> failedOnce = ConcurrentHashMap.newKeySet();

    OrderController(OrderApplicationService service) {
        this.service = service;
    }

    @PostMapping
    CreateOrderResponse create(
            @RequestHeader(IdempotencyCore.HEADER) String key,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "demo") String tenantId,
            @RequestHeader(value = "X-Demo-Feign-Fail-Once", defaultValue = "false")
            boolean failOnce,
            @Valid @RequestBody CreateOrderRequest request) {

        // Local idempotency+business transaction has committed when this returns.
        CreateOrderResponse response = service.create(tenantId, key, request);

        // Fault injection: simulate "DB COMMIT succeeded, HTTP response becomes 503".
        if (failOnce && failedOnce.add(key)) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "demo: committed but return 503 once");
        }
        return response;
    }
}

@RestControllerAdvice
class IdempotencyErrorHandler {
    @ExceptionHandler(IdempotencyException.class)
    ResponseEntity<?> handle(IdempotencyException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(java.util.Map.of("code", ex.getCode(), "message", ex.getMessage()));
    }
}
