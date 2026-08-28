package cc.ivera.ragdemo.model.query;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class QueryCostMetricUpsertCommand {

    private final String table;
    private final Long tenantId;
    private final Long knowledgeBaseId;
    private final LocalDateTime bucketStart;
    private final String windowType;
    private final String queryType;
    private final String retrievalMode;
    private final String llmModel;
    private final String embeddingModel;
    private final String status;
    private final long queryCount;
    private final long successCount;
    private final long failedCount;
    private final long promptTokens;
    private final long completionTokens;
    private final long totalTokens;
    private final BigDecimal estimatedTotalCost;
    private final double p50LatencyMs;
    private final double p90LatencyMs;
    private final double p95LatencyMs;
    private final double p99LatencyMs;
}
