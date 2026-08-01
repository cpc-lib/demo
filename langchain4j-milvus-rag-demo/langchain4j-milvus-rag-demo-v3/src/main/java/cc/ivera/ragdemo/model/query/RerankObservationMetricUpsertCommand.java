package cc.ivera.ragdemo.model.query;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class RerankObservationMetricUpsertCommand {

    private final String table;
    private final Long tenantId;
    private final LocalDateTime bucketStart;
    private final String windowType;
    private final String provider;
    private final String model;
    private final String apiKeyHash;
    private final String errorCode;
    private final String degradedReason;
    private final long requestCount;
    private final long successCount;
    private final long failureCount;
    private final long fallbackCount;
    private final long retryCount;
    private final long cacheHitCount;
    private final long totalTokens;
    private final BigDecimal estimatedCost;
    private final double p50LatencyMs;
    private final double p90LatencyMs;
    private final double p95LatencyMs;
    private final double p99LatencyMs;
}
