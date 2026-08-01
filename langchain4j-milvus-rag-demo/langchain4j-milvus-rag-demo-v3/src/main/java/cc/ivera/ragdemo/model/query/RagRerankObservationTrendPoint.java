package cc.ivera.ragdemo.model.query;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RagRerankObservationTrendPoint(
        LocalDateTime bucket,
        String window,
        String provider,
        String model,
        Long tenantId,
        String apiKeyHash,
        String errorCode,
        String degradedReason,
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
        BigDecimal estimatedCost,
        String source
) {
}
