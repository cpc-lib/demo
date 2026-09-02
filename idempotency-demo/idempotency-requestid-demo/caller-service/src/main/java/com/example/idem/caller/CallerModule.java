package com.example.idem.caller;

import com.example.idem.core.ServerIssuedIdempotency;
import feign.RetryableException;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

record CreateOrderRequest(Long userId, String itemName, BigDecimal amount) {}
record CreateOrderResponse(String orderNo, String status, boolean replayed) {}

class DemoFeignRetryConfig {
    @Bean
    Retryer retryer() {
        return new Retryer.Default(100, 300, 2);
    }

    @Bean
    ErrorDecoder errorDecoder() {
        ErrorDecoder defaultDecoder = new ErrorDecoder.Default();
        return (methodKey, response) -> {
            if (response.status() == 503) {
                return new RetryableException(
                        response.status(),
                        "demo retry on 503",
                        response.request().httpMethod(),
                        100L,
                        response.request());
            }
            return defaultDecoder.decode(methodKey, response);
        };
    }
}

@FeignClient(name = "order-service", configuration = DemoFeignRetryConfig.class)
interface OrderFeignClient {
    @PostMapping("/internal/orders")
    CreateOrderResponse create(
            @RequestHeader(ServerIssuedIdempotency.HEADER) String key,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-Demo-Feign-Fail-Once") boolean feignFailOnce,
            @RequestBody CreateOrderRequest request);
}

@RestController
@RequestMapping("/orders")
class CallerController {
    private final OrderFeignClient client;
    private final Set<String> failedOnce = ConcurrentHashMap.newKeySet();

    CallerController(OrderFeignClient client) {
        this.client = client;
    }

    @PostMapping
    CreateOrderResponse create(
            @RequestHeader(ServerIssuedIdempotency.HEADER) String key,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "demo") String tenantId,
            @RequestHeader(value = "X-Demo-Gateway-Fail-Once", defaultValue = "false")
            boolean gatewayFailOnce,
            @RequestHeader(value = "X-Demo-Feign-Fail-Once", defaultValue = "false")
            boolean feignFailOnce,
            @RequestBody CreateOrderRequest request) {

        CreateOrderResponse response =
                client.create(key, tenantId, feignFailOnce, request);

        // Downstream already succeeded; simulate caller returning 500 once.
        if (gatewayFailOnce && failedOnce.add(key)) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "demo: downstream succeeded but caller returns 500 once");
        }
        return response;
    }
}
