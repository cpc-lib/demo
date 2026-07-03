package cc.ivera.gray.gateway;

import cc.ivera.gray.common.GrayMatchRequest;
import cc.ivera.gray.common.GrayMatchResult;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
public class ProxyController {
    private static final String SERVICE_ID = "demo-order-service";

    private final WebClient webClient;
    private final String adminBaseUrl;
    private final String orderV1Url;
    private final String orderV2Url;

    public ProxyController(WebClient.Builder builder,
                           @Value("${gray.admin.base-url}") String adminBaseUrl,
                           @Value("${gray.upstream.demo-order-service.v1}") String orderV1Url,
                           @Value("${gray.upstream.demo-order-service.v2}") String orderV2Url) {
        this.webClient = builder.build();
        this.adminBaseUrl = adminBaseUrl;
        this.orderV1Url = orderV1Url;
        this.orderV2Url = orderV2Url;
    }

    @RequestMapping("/api/order/**")
    public Mono<ResponseEntity<byte[]>> proxy(ServerWebExchange exchange,
                                             @RequestBody(required = false) Mono<byte[]> body) {
        GrayMatchRequest matchRequest = buildMatchRequest(exchange);
        Mono<GrayMatchResult> match = webClient.post()
                .uri(adminBaseUrl + "/api/internal/match")
                .bodyValue(matchRequest)
                .retrieve()
                .bodyToMono(GrayMatchResult.class)
                .onErrorReturn(GrayMatchResult.defaultVersion(SERVICE_ID, "v1"));

        Mono<byte[]> bodyMono = body == null ? Mono.just(new byte[0]) : body.defaultIfEmpty(new byte[0]);
        return match.zipWith(bodyMono)
                .flatMap(tuple -> forward(exchange, tuple.getT1(), tuple.getT2()));
    }

    private Mono<ResponseEntity<byte[]>> forward(ServerWebExchange exchange, GrayMatchResult result, byte[] body) {
        String baseUrl = "v2".equalsIgnoreCase(result.getTargetVersion()) ? orderV2Url : orderV1Url;
        URI targetUri = buildTargetUri(exchange, baseUrl);
        HttpMethod method = exchange.getRequest().getMethod();
        String traceId = traceId(exchange);

        WebClient.RequestBodySpec spec = webClient.method(method)
                .uri(targetUri)
                .headers(headers -> copyHeaders(exchange.getRequest().getHeaders(), headers))
                .header("X-Trace-Id", traceId)
                .header("X-Gray-Version", result.getTargetVersion())
                .header("X-Gray-Rule", result.getRuleName() == null ? "default" : result.getRuleName());

        if (body.length == 0 || method == HttpMethod.GET || method == HttpMethod.DELETE) {
            return spec.retrieve().toEntity(byte[].class).map(response -> withTrace(response, traceId));
        }
        return spec.body(BodyInserters.fromValue(body)).retrieve().toEntity(byte[].class)
                .map(response -> withTrace(response, traceId));
    }

    private ResponseEntity<byte[]> withTrace(ResponseEntity<byte[]> response, String traceId) {
        HttpHeaders headers = new HttpHeaders();
        headers.putAll(response.getHeaders());
        headers.set("X-Trace-Id", traceId);
        return new ResponseEntity<>(response.getBody(), headers, response.getStatusCode());
    }

    private String traceId(ServerWebExchange exchange) {
        String traceId = exchange.getRequest().getHeaders().getFirst("X-Trace-Id");
        return traceId == null || traceId.isBlank() ? UUID.randomUUID().toString().replace("-", "") : traceId;
    }

    private URI buildTargetUri(ServerWebExchange exchange, String baseUrl) {
        String rawPath = exchange.getRequest().getURI().getRawPath();
        String rawQuery = exchange.getRequest().getURI().getRawQuery();
        String uri = baseUrl + rawPath + (rawQuery == null ? "" : "?" + rawQuery);
        return URI.create(uri);
    }

    private GrayMatchRequest buildMatchRequest(ServerWebExchange exchange) {
        HttpHeaders headers = exchange.getRequest().getHeaders();
        GrayMatchRequest request = new GrayMatchRequest();
        request.setServiceId(SERVICE_ID);
        request.setUserId(first(headers, "X-User-Id"));
        request.setTenantId(first(headers, "X-Tenant-Id"));
        request.setAppVersion(first(headers, "X-App-Version"));
        request.setRegion(first(headers, "X-Region"));
        request.setIp(clientIp(exchange));
        request.setHeaders(flatHeaders(headers));
        request.setCookies(flatCookies(exchange));
        return request;
    }

    private String first(HttpHeaders headers, String key) {
        return headers.getFirst(key);
    }

    private String clientIp(ServerWebExchange exchange) {
        String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return exchange.getRequest().getRemoteAddress() == null
                ? null
                : exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
    }

    private Map<String, String> flatHeaders(HttpHeaders headers) {
        Map<String, String> values = new HashMap<>();
        headers.forEach((key, value) -> {
            if (!value.isEmpty()) {
                values.put(key, value.get(0));
            }
        });
        return values;
    }

    private Map<String, String> flatCookies(ServerWebExchange exchange) {
        Map<String, String> values = new HashMap<>();
        exchange.getRequest().getCookies().forEach((key, cookies) -> {
            if (!cookies.isEmpty()) {
                values.put(key, cookies.get(0).getValue());
            }
        });
        return values;
    }

    private void copyHeaders(HttpHeaders source, HttpHeaders target) {
        source.forEach((key, value) -> {
            if (!HttpHeaders.HOST.equalsIgnoreCase(key) && !HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase(key)) {
                target.put(key, value);
            }
        });
    }
}
