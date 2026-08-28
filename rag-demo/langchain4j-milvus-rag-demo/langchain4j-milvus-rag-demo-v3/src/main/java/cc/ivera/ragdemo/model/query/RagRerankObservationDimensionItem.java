package cc.ivera.ragdemo.model.query;

import java.math.BigDecimal;

public record RagRerankObservationDimensionItem(
        String dimension,
        String value,
        long requestCount,
        long successCount,
        long failureCount,
        double failureRate,
        long fallbackCount,
        double fallbackRate,
        long timeoutCount,
        long rateLimitCount,
        double p50LatencyMs,
        double p90LatencyMs,
        double p99LatencyMs,
        double avgCandidateCount,
        double avgInputTokens,
        long totalTokens,
        BigDecimal estimatedCost
) {
}
