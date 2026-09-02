package com.example.idem.order;

import com.example.idem.core.ServerIssuedIdempotency;
import com.example.idem.core.ServerIssuedIdempotency.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
class IdempotencyConfig {
    @Bean ServerIssuedIdempotency.Repository serverIssuedRepository(JdbcTemplate jdbc) {
        return new ServerIssuedIdempotency.JdbcRepository(jdbc);
    }

    @Bean ServerIssuedIdempotency.Template serverIssuedTemplate(
            ServerIssuedIdempotency.Repository repository,
            ObjectMapper mapper,
            PlatformTransactionManager txManager) {
        return new ServerIssuedIdempotency.Template(
                repository, mapper, new TransactionTemplate(txManager));
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

record IssueRequestIdResponse(String requestId, Instant expiresAt) {}

@Service
class OrderApplicationService {
    static final Duration ISSUE_TTL = Duration.ofMinutes(10);

    private final ServerIssuedIdempotency.Template template;
    private final JdbcTemplate jdbc;

    OrderApplicationService(ServerIssuedIdempotency.Template template, JdbcTemplate jdbc) {
        this.template = template;
        this.jdbc = jdbc;
    }

    IssueRequestIdResponse issueCreateOrderRequestId(String tenantId) {
        IssuedToken token = template.issue(scope(tenantId), ISSUE_TTL);
        return new IssueRequestIdResponse(token.requestId(), token.expiresAt());
    }

    CreateOrderResponse create(String tenantId, String key, CreateOrderRequest request) {
        Result<CreateOrderResponse> result = template.execute(
                scope(tenantId),
                key,
                request,
                CreateOrderResponse.class,
                () -> insertOrder(tenantId, key, request));

        return result.replayed() ? result.value().asReplay() : result.value();
    }

    private String scope(String tenantId) {
        return tenantId + ":order:create";
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
                request.amount(), ServerIssuedIdempotency.sha256(key));

        return new CreateOrderResponse(orderNo, "CREATED", false);
    }
}

/** Backend endpoint used BEFORE the mutating request. */
@RestController
@RequestMapping("/idempotency")
class RequestIdController {
    private final OrderApplicationService service;

    RequestIdController(OrderApplicationService service) {
        this.service = service;
    }

    @PostMapping("/order-create")
    IssueRequestIdResponse issue(
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "demo") String tenantId) {
        return service.issueCreateOrderRequestId(tenantId);
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
            @RequestHeader(ServerIssuedIdempotency.HEADER) String key,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "demo") String tenantId,
            @RequestHeader(value = "X-Demo-Feign-Fail-Once", defaultValue = "false")
            boolean failOnce,
            @Valid @RequestBody CreateOrderRequest request) {

        // Business + SUCCESS replay record have committed when this returns.
        CreateOrderResponse response = service.create(tenantId, key, request);

        // Fault injection: COMMIT succeeded, but caller observes 503 and retries.
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
    @ExceptionHandler(ServerIssuedIdempotency.IdempotencyException.class)
    ResponseEntity<?> handle(ServerIssuedIdempotency.IdempotencyException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(java.util.Map.of("code", ex.getCode(), "message", ex.getMessage()));
    }
}
