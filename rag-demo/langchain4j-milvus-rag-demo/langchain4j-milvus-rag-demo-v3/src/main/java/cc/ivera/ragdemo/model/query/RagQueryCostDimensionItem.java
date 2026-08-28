package cc.ivera.ragdemo.model.query;

import java.math.BigDecimal;

public record RagQueryCostDimensionItem(
        String dimension,
        String value,
        long queryCount,
        long successCount,
        long failedCount,
        long totalPromptTokens,
        long totalCompletionTokens,
        long totalTokens,
        double avgTokensPerQuery,
        double p50LatencyMs,
        double p90LatencyMs,
        BigDecimal estimatedTotalCost,
        BigDecimal avgCostPerQuery
) {
}
