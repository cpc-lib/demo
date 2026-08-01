package cc.ivera.ragdemo.model.query;

import java.math.BigDecimal;

public record RagRerankObservationSummary(
        long totalRequests,
        long successCount,
        long failedCount,
        long degradedCount,
        double failureRate,
        double averageLatencyMs,
        long totalTokens,
        BigDecimal estimatedCost
) {
}
