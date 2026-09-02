package com.example.idem.gateway;

import com.example.idem.core.IdempotencyCore;
import org.springframework.cloud.gateway.filter.*;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.UUID;

@Component
public class IdempotencyKeyGlobalFilter implements GlobalFilter, Ordered {

    private static final Set<HttpMethod> COMMANDS =
            Set.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!COMMANDS.contains(exchange.getRequest().getMethod())) {
            return chain.filter(exchange);
        }

        String incoming =
                exchange.getRequest().getHeaders().getFirst(IdempotencyCore.HEADER);
        String key = StringUtils.hasText(incoming)
                ? incoming
                : UUID.randomUUID().toString();

        ServerWebExchange mutated = exchange.mutate()
                .request(r -> r.headers(h -> h.set(IdempotencyCore.HEADER, key)))
                .build();

        mutated.getResponse().getHeaders().set(IdempotencyCore.HEADER, key);
        return chain.filter(mutated);
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
