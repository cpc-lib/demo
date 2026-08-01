package cc.ivera.ragdemo.service.query;


import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.domain.rag.RagRerankCallLog;
import cc.ivera.ragdemo.mapper.MetricAggregationMapper;
import cc.ivera.ragdemo.mapper.RagRerankCallLogMapper;
import cc.ivera.ragdemo.model.query.RagRerankLatencyPercentiles;
import cc.ivera.ragdemo.model.query.RagRerankObservationDimensionItem;
import cc.ivera.ragdemo.model.query.RagRerankObservationTrendPoint;
import cc.ivera.ragdemo.service.ragops.TimeWindowAggregationPolicy;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class RerankObservationTrendService {

    private final RagRerankCallLogMapper mapper;
    private final TimeWindowAggregationPolicy timeWindowPolicy;
    private final RagProperties properties;
    private final MetricAggregationMapper metricAggregationMapper;

    public List<RagRerankObservationTrendPoint> trends(Long tenantId,
                                                       String provider,
                                                       String model,
                                                       String apiKeyHash,
                                                       String errorCode,
                                                       String degradedReason,
                                                       String window,
                                                       LocalDateTime from,
                                                       LocalDateTime to) {
        List<RagRerankObservationTrendPoint> materialized = materializedTrends(
                tenantId, provider, model, apiKeyHash, errorCode, degradedReason, window, from, to);
        if (!materialized.isEmpty()) {
            return materialized;
        }
        List<RagRerankCallLog> rows = rows(tenantId, provider, model, apiKeyHash, errorCode, degradedReason, from, to);
        Map<TrendKey, List<RagRerankCallLog>> grouped = rows.stream()
                .collect(Collectors.groupingBy(row -> new TrendKey(
                        timeWindowPolicy.bucket(row.getCreatedAt(), window),
                        timeWindowPolicy.normalize(window).name().toLowerCase(),
                        row.getProvider(),
                        row.getModel(),
                        row.getTenantId(),
                        row.getApiKeyHash()
                ), LinkedHashMap::new, Collectors.toList()));
        return grouped.entrySet().stream()
                .map(entry -> toTrendPoint(entry.getKey(), stats(entry.getValue())))
                .toList();
    }

    public List<RagRerankObservationDimensionItem> byDimension(String dimension,
                                                              Long tenantId,
                                                              String provider,
                                                              String model,
                                                              LocalDateTime from,
                                                              LocalDateTime to) {
        String dim = normalizeDimension(dimension);
        Function<RagRerankCallLog, String> extractor = dimensionExtractor(dim);
        Map<String, List<RagRerankCallLog>> grouped = rows(tenantId, provider, model, null, null, null, from, to).stream()
                .collect(Collectors.groupingBy(row -> stringValue(extractor.apply(row)), LinkedHashMap::new, Collectors.toList()));
        return grouped.entrySet().stream()
                .map(entry -> toDimensionItem(dim, entry.getKey(), stats(entry.getValue())))
                .toList();
    }

    public RagRerankLatencyPercentiles latencyPercentiles(Long tenantId,
                                                          String provider,
                                                          String model,
                                                          LocalDateTime from,
                                                          LocalDateTime to) {
        Stats stats = stats(rows(tenantId, provider, model, null, null, null, from, to));
        return new RagRerankLatencyPercentiles(
                stats.requestCount(),
                stats.p50LatencyMs(),
                stats.p90LatencyMs(),
                stats.p99LatencyMs()
        );
    }

    private List<RagRerankObservationTrendPoint> materializedTrends(Long tenantId,
                                                                    String provider,
                                                                    String model,
                                                                    String apiKeyHash,
                                                                    String errorCode,
                                                                    String degradedReason,
                                                                    String window,
                                                                    LocalDateTime from,
                                                                    LocalDateTime to) {
        if (!properties.getMetrics().isMaterializedEnabled() || !properties.getMetrics().isPreferMaterialized()) {
            return List.of();
        }
        String normalizedWindow = timeWindowPolicy.normalize(window).name();
        if (!"HOUR".equals(normalizedWindow)
                && !"DAY".equals(normalizedWindow)
                && !"WEEK".equals(normalizedWindow)
                && !"MONTH".equals(normalizedWindow)) {
            return List.of();
        }
        String table = "HOUR".equals(normalizedWindow)
                ? "rag_rerank_observation_metric_hourly"
                : "rag_rerank_observation_metric_daily";
        String sourceWindow = "HOUR".equals(normalizedWindow) ? "HOUR" : "DAY";
        try {
            return metricAggregationMapper.listMaterializedRerankObservationTrends(
                            table,
                            normalizedWindow,
                            sourceWindow,
                            tenantId,
                            normalize(provider),
                            normalize(model),
                            normalize(apiKeyHash),
                            normalize(errorCode),
                            normalize(degradedReason),
                            from,
                            to
                    ).stream()
                    .map(row -> {
                        long requestCount = longValue(row.get("request_count"));
                        long failureCount = longValue(row.get("failure_count"));
                        long fallbackCount = longValue(row.get("fallback_count"));
                        String rowErrorCode = stringValue(row.get("error_code"));
                        return new RagRerankObservationTrendPoint(
                                timeValue(row.get("bucket_start")),
                                String.valueOf(row.get("window_type")).toLowerCase(),
                                stringValue(row.get("provider")),
                                stringValue(row.get("model")),
                                boxedLong(row.get("tenant_id")),
                                stringValue(row.get("api_key_hash")),
                                rowErrorCode,
                                stringValue(row.get("degraded_reason")),
                                requestCount,
                                longValue(row.get("success_count")),
                                failureCount,
                                requestCount == 0 ? 0.0 : failureCount / (double) requestCount,
                                fallbackCount,
                                requestCount == 0 ? 0.0 : fallbackCount / (double) requestCount,
                                "TIMEOUT".equals(rowErrorCode) ? failureCount : 0,
                                "RATE_LIMITED".equals(rowErrorCode) ? failureCount : 0,
                                doubleValue(row.get("p50_latency_ms")),
                                doubleValue(row.get("p90_latency_ms")),
                                doubleValue(row.get("p99_latency_ms")),
                                0.0,
                                0.0,
                                longValue(row.get("total_tokens")),
                                decimalValue(row.get("estimated_cost")),
                                "MATERIALIZED"
                        );
                    })
                    .toList();
        } catch (DataAccessException ex) {
            return List.of();
        }
    }

    private List<RagRerankCallLog> rows(Long tenantId,
                                        String provider,
                                        String model,
                                        String apiKeyHash,
                                        String errorCode,
                                        String degradedReason,
                                        LocalDateTime from,
                                        LocalDateTime to) {
        return mapper.selectList(new LambdaQueryWrapper<RagRerankCallLog>()
                .eq(tenantId != null, RagRerankCallLog::getTenantId, tenantId)
                .eq(StringUtils.hasText(provider), RagRerankCallLog::getProvider, normalize(provider))
                .eq(StringUtils.hasText(model), RagRerankCallLog::getModel, normalize(model))
                .eq(StringUtils.hasText(apiKeyHash), RagRerankCallLog::getApiKeyHash, normalize(apiKeyHash))
                .eq(StringUtils.hasText(errorCode), RagRerankCallLog::getErrorCodeNormalized, normalize(errorCode))
                .eq(StringUtils.hasText(degradedReason), RagRerankCallLog::getDegradedReason, normalize(degradedReason))
                .ge(from != null, RagRerankCallLog::getCreatedAt, from)
                .le(to != null, RagRerankCallLog::getCreatedAt, to)
                .orderByAsc(RagRerankCallLog::getCreatedAt));
    }

    private RagRerankObservationTrendPoint toTrendPoint(TrendKey key, Stats stats) {
        return new RagRerankObservationTrendPoint(
                key.bucket(),
                key.window(),
                key.provider(),
                key.model(),
                key.tenantId(),
                key.apiKeyHash(),
                null,
                null,
                stats.requestCount(),
                stats.successCount(),
                stats.failureCount(),
                stats.failureRate(),
                stats.fallbackCount(),
                stats.fallbackRate(),
                stats.timeoutCount(),
                stats.rateLimitCount(),
                stats.p50LatencyMs(),
                stats.p90LatencyMs(),
                stats.p99LatencyMs(),
                stats.avgCandidateCount(),
                stats.avgInputTokens(),
                stats.totalTokens(),
                stats.estimatedCost(),
                "LIVE"
        );
    }

    private RagRerankObservationDimensionItem toDimensionItem(String dimension, String value, Stats stats) {
        return new RagRerankObservationDimensionItem(
                dimension,
                value,
                stats.requestCount(),
                stats.successCount(),
                stats.failureCount(),
                stats.failureRate(),
                stats.fallbackCount(),
                stats.fallbackRate(),
                stats.timeoutCount(),
                stats.rateLimitCount(),
                stats.p50LatencyMs(),
                stats.p90LatencyMs(),
                stats.p99LatencyMs(),
                stats.avgCandidateCount(),
                stats.avgInputTokens(),
                stats.totalTokens(),
                stats.estimatedCost()
        );
    }

    private Stats stats(List<RagRerankCallLog> rows) {
        if (rows == null || rows.isEmpty()) {
            return new Stats(0, 0, 0, 0, 0.0, 0, 0.0, 0,
                    0.0, 0.0, 0.0, 0.0, 0.0, 0, BigDecimal.ZERO);
        }
        long total = rows.size();
        long success = rows.stream().filter(row -> Boolean.TRUE.equals(row.getSuccess())).count();
        long fallback = rows.stream().filter(row -> Boolean.TRUE.equals(row.getFallback())).count();
        long timeout = rows.stream().filter(row -> "TIMEOUT".equals(row.getErrorCodeNormalized())).count();
        long rateLimit = rows.stream().filter(row -> "RATE_LIMITED".equals(row.getErrorCodeNormalized())).count();
        List<Long> latencies = rows.stream().map(RagRerankCallLog::getLatencyMs).toList();
        long totalTokens = rows.stream().map(RagRerankCallLog::getTotalTokens).filter(value -> value != null).mapToLong(Integer::longValue).sum();
        BigDecimal cost = rows.stream().map(RagRerankCallLog::getEstimatedCost).filter(value -> value != null).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Stats(
                total,
                success,
                total - success,
                fallback,
                (total - success) / (double) total,
                rateLimit,
                fallback / (double) total,
                timeout,
                timeWindowPolicy.percentile(latencies, 0.50),
                timeWindowPolicy.percentile(latencies, 0.90),
                timeWindowPolicy.percentile(latencies, 0.99),
                rows.stream().map(RagRerankCallLog::getCandidateCount).filter(value -> value != null).mapToInt(Integer::intValue).average().orElse(0.0),
                rows.stream().map(RagRerankCallLog::getInputTokens).filter(value -> value != null).mapToInt(Integer::intValue).average().orElse(0.0),
                totalTokens,
                cost
        );
    }

    private String normalizeDimension(String dimension) {
        if (!StringUtils.hasText(dimension)) {
            return "errorCode";
        }
        return switch (dimension.trim()) {
            case "apiKeyHash", "tenant", "tenantExternalId", "provider", "model", "degradedReason" -> dimension.trim();
            default -> "errorCode";
        };
    }

    private Function<RagRerankCallLog, String> dimensionExtractor(String dimension) {
        return switch (dimension) {
            case "apiKeyHash" -> RagRerankCallLog::getApiKeyHash;
            case "tenant" -> row -> stringValue(row.getTenantId());
            case "tenantExternalId" -> RagRerankCallLog::getTenantExternalId;
            case "provider" -> RagRerankCallLog::getProvider;
            case "model" -> RagRerankCallLog::getModel;
            case "degradedReason" -> RagRerankCallLog::getDegradedReason;
            default -> RagRerankCallLog::getErrorCodeNormalized;
        };
    }

    private LocalDateTime timeValue(Object value) {
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        return null;
    }

    private Long boxedLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    private long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return 0L;
    }

    private double doubleValue(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return 0.0;
    }

    private BigDecimal decimalValue(Object value) {
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return BigDecimal.ZERO;
    }

    private String stringValue(Object value) {
        return value == null || !StringUtils.hasText(String.valueOf(value)) ? "unknown" : String.valueOf(value);
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : value;
    }

    private record TrendKey(
            LocalDateTime bucket,
            String window,
            String provider,
            String model,
            Long tenantId,
            String apiKeyHash
    ) {
    }

    private record Stats(
            long requestCount,
            long successCount,
            long failureCount,
            long fallbackCount,
            double failureRate,
            long rateLimitCount,
            double fallbackRate,
            long timeoutCount,
            double p50LatencyMs,
            double p90LatencyMs,
            double p99LatencyMs,
            double avgCandidateCount,
            double avgInputTokens,
            long totalTokens,
            BigDecimal estimatedCost
    ) {
    }
}
