package cc.ivera.ragdemo.model.query;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RagQueryCostTrendPoint(
        LocalDateTime bucket,
        String window,
        Long tenantId,
        String queryType,
        String retrievalMode,
        String llmModel,
        String embeddingModel,
        String status,
        Long knowledgeBaseId,
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
        BigDecimal avgCostPerQuery,
        BigDecimal costPerHelpfulFeedback,
        BigDecimal costPerKnowledgeHit,
        String source
) {
}
