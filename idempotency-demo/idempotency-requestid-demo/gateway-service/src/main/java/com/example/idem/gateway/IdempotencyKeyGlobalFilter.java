package com.example.idem.gateway;

import com.example.idem.core.ServerIssuedIdempotency;
import org.springframework.cloud.gateway.filter.*;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

/**
 * Important distinction:
 * - X-Request-Id = tracing/observability id. Gateway/Nginx may generate it.
 * - Idempotency-Key = business command id. For /api/orders it MUST be pre-issued by backend
 *   and carried by the client. Gateway never invents a new one for a mutation.
 */
@Component
public class IdempotencyKeyGlobalFilter implements GlobalFilter, Ordered {

    private static final String TRACE_HEADER = "X-Request-Id";
    private static final Set<HttpMethod> COMMANDS = Set.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = exchange.getRequest().getHeaders().getFirst(TRACE_HEADER);
        if (!StringUtils.hasText(traceId)) traceId = UUID.randomUUID().toString();

        String path = exchange.getRequest().getURI().getPath();
        String idemKey = exchange.getRequest().getHeaders().getFirst(ServerIssuedIdempotency.HEADER);

        // Only business mutation route requires a pre-issued key.
        boolean businessCommand = path.startsWith("/api/orders")
                && COMMANDS.contains(exchange.getRequest().getMethod());

        if (businessCommand && !StringUtils.hasText(idemKey)) {
            byte[] json = ("{\"code\":\"IDEMPOTENCY_KEY_REQUIRED\","
                    + "\"message\":\"Call POST /api/idempotency/order-create first, then reuse the returned requestId as Idempotency-Key.\"}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponse().setStatusCode(HttpStatus.PRECONDITION_REQUIRED);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
            exchange.getResponse().getHeaders().set(TRACE_HEADER, traceId);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(json);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        }

        final String finalTraceId = traceId;
        ServerWebExchange mutated = exchange.mutate()
                .request(r -> r.headers(h -> h.set(TRACE_HEADER, finalTraceId)))
                .build();
        mutated.getResponse().getHeaders().set(TRACE_HEADER, finalTraceId);
        if (StringUtils.hasText(idemKey)) {
            mutated.getResponse().getHeaders().set(ServerIssuedIdempotency.HEADER, idemKey);
        }
        return chain.filter(mutated);
    }

    @Override
    public int getOrder() { return -100; }
}
