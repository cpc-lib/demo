package cc.ivera.ragdemo.service.query;


import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.domain.rag.RagQueryCostAnomaly;
import cc.ivera.ragdemo.domain.rag.RagQueryFeedback;
import cc.ivera.ragdemo.domain.rag.RagQueryLog;
import cc.ivera.ragdemo.mapper.MetricAggregationMapper;
import cc.ivera.ragdemo.mapper.RagQueryCostAnomalyMapper;
import cc.ivera.ragdemo.mapper.RagQueryFeedbackMapper;
import cc.ivera.ragdemo.mapper.RagQueryLogMapper;
import cc.ivera.ragdemo.model.query.RagQueryCostAnomalyItem;
import cc.ivera.ragdemo.model.query.RagQueryCostDimensionItem;
import cc.ivera.ragdemo.model.query.RagQueryCostTrendPoint;
import cc.ivera.ragdemo.service.ragops.QueryAuditPolicy;
import cc.ivera.ragdemo.service.ragops.QueryFeedbackPolicy;
import cc.ivera.ragdemo.service.ragops.TimeWindowAggregationPolicy;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class QueryCostAnalyticsService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(8, RoundingMode.HALF_UP);

    private final RagQueryLogMapper queryLogMapper;
    private final RagQueryFeedbackMapper feedbackMapper;
    private final RagQueryCostAnomalyMapper anomalyMapper;
    private final TimeWindowAggregationPolicy timeWindowPolicy;
    private final RagProperties properties;
    private final MetricAggregationMapper metricAggregationMapper;

    public List<RagQueryCostTrendPoint> trends(Long tenantId,
                                               String queryType,
                                               String retrievalMode,
                                               String status,
                                               String llmModel,
                                               String embeddingModel,
                                               Long knowledgeBaseId,
                                               String window,
                                               LocalDateTime from,
                                               LocalDateTime to) {
        List<RagQueryCostTrendPoint> materialized = materializedTrends(
                tenantId, queryType, retrievalMode, status, llmModel, embeddingModel, knowledgeBaseId, window, from, to);
        if (!materialized.isEmpty()) {
            return materialized;
        }
        List<RagQueryLog> rows = rows(tenantId, queryType, retrievalMode, status, llmModel, embeddingModel, knowledgeBaseId, from, to);
        Map<TrendKey, List<RagQueryLog>> grouped = rows.stream()
                .collect(Collectors.groupingBy(row -> new TrendKey(
                        timeWindowPolicy.bucket(row.getCreatedAt(), window),
                        timeWindowPolicy.normalize(window).name().toLowerCase(),
                        row.getTenantId(),
                        row.getQueryType(),
                        row.getRetrievalMode(),
                        row.getLlmModel(),
                        row.getEmbeddingModel(),
                        row.getStatus(),
                        knowledgeBaseId
                ), LinkedHashMap::new, Collectors.toList()));
        return grouped.entrySet().stream()
                .map(entry -> toTrend(entry.getKey(), stats(entry.getValue())))
                .toList();
    }

    public List<RagQueryCostDimensionItem> byDimension(String dimension,
                                                       Long tenantId,
                                                       String queryType,
                                                       String retrievalMode,
                                                       String status,
                                                       LocalDateTime from,
                                                       LocalDateTime to) {
        String dim = normalizeDimension(dimension);
        Function<RagQueryLog, String> extractor = dimensionExtractor(dim);
        Map<String, List<RagQueryLog>> grouped = rows(tenantId, queryType, retrievalMode, status, null, null, null, from, to).stream()
                .collect(Collectors.groupingBy(row -> stringValue(extractor.apply(row)), LinkedHashMap::new, Collectors.toList()));
        return grouped.entrySet().stream()
                .map(entry -> toDimension(dim, entry.getKey(), stats(entry.getValue())))
                .toList();
    }

    public List<RagQueryCostAnomalyItem> anomalies(Long tenantId,
                                                  LocalDateTime from,
                                                  LocalDateTime to,
                                                  Integer tokenThreshold,
                                                  BigDecimal dailyIncreaseRatio) {
        int threshold = tokenThreshold == null || tokenThreshold <= 0 ? 8000 : tokenThreshold;
        List<RagQueryCostAnomalyItem> tokenAnomalies = rows(tenantId, null, null, null, null, null, null, from, to).stream()
                .filter(row -> intValue(row.getTotalTokens()) > threshold)
                .map(row -> new RagQueryCostAnomalyItem(
                        "QUERY_TOKEN_SPIKE",
                        intValue(row.getTotalTokens()) > threshold * 2 ? "HIGH" : "MEDIUM",
                        "totalTokens",
                        BigDecimal.valueOf(intValue(row.getTotalTokens())),
                        BigDecimal.valueOf(threshold),
                        row.getCreatedAt(),
                        row.getCreatedAt(),
                        "queryLogId=" + row.getId()
                ))
                .toList();
        List<RagQueryCostAnomalyItem> persisted = anomalyMapper.selectList(new LambdaQueryWrapper<RagQueryCostAnomaly>()
                        .eq(tenantId != null, RagQueryCostAnomaly::getTenantId, tenantId)
                        .ge(from != null, RagQueryCostAnomaly::getWindowEnd, from)
                        .le(to != null, RagQueryCostAnomaly::getWindowStart, to)
                        .orderByDesc(RagQueryCostAnomaly::getCreatedAt))
                .stream()
                .map(row -> new RagQueryCostAnomalyItem(
                        row.getAnomalyType(),
                        row.getSeverity(),
                        row.getMetricName(),
                        row.getMetricValue(),
                        row.getBaselineValue(),
                        row.getWindowStart(),
                        row.getWindowEnd(),
                        row.getMetadataJson()
                ))
                .toList();
        return java.util.stream.Stream.concat(tokenAnomalies.stream(), persisted.stream()).toList();
    }

    public String exportCsv(Long tenantId,
                            String queryType,
                            String retrievalMode,
                            String status,
                            String window,
                            LocalDateTime from,
                            LocalDateTime to) {
        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append("bucket,window,tenantId,queryType,retrievalMode,llmModel,embeddingModel,status,queryCount,totalTokens,estimatedTotalCost,avgCostPerQuery\n");
        for (RagQueryCostTrendPoint row : trends(tenantId, queryType, retrievalMode, status, null, null, null, window, from, to)) {
            csv.append(csv(row.bucket())).append(',')
                    .append(csv(row.window())).append(',')
                    .append(csv(row.tenantId())).append(',')
                    .append(csv(row.queryType())).append(',')
                    .append(csv(row.retrievalMode())).append(',')
                    .append(csv(row.llmModel())).append(',')
                    .append(csv(row.embeddingModel())).append(',')
                    .append(csv(row.status())).append(',')
                    .append(csv(row.queryCount())).append(',')
                    .append(csv(row.totalTokens())).append(',')
                    .append(csv(row.estimatedTotalCost())).append(',')
                    .append(csv(row.avgCostPerQuery()))
                    .append('\n');
        }
        return csv.toString();
    }

    private List<RagQueryCostTrendPoint> materializedTrends(Long tenantId,
                                                            String queryType,
                                                            String retrievalMode,
                                                            String status,
                                                            String llmModel,
                                                            String embeddingModel,
                                                            Long knowledgeBaseId,
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
        String table = metricTable(normalizedWindow, "rag_query_cost_metric_hourly", "rag_query_cost_metric_daily");
        String sourceWindow = "HOUR".equals(normalizedWindow) ? "HOUR" : "DAY";
        try {
            return metricAggregationMapper.listMaterializedQueryCostTrends(
                            table,
                            normalizedWindow,
                            sourceWindow,
                            tenantId,
                            knowledgeBaseId == null ? 0L : knowledgeBaseId,
                            normalize(queryType),
                            clean(retrievalMode),
                            normalize(status),
                            clean(llmModel),
                            clean(embeddingModel),
                            from,
                            to
                    ).stream()
                    .map(row -> {
                        long queryCount = longValue(row.get("query_count"));
                        BigDecimal cost = decimalValue(row.get("estimated_total_cost"));
                        return new RagQueryCostTrendPoint(
                                timeValue(row.get("bucket_start")),
                                String.valueOf(row.get("window_type")).toLowerCase(),
                                boxedLong(row.get("tenant_id")),
                                stringValue(row.get("query_type")),
                                stringValue(row.get("retrieval_mode")),
                                stringValue(row.get("llm_model")),
                                stringValue(row.get("embedding_model")),
                                stringValue(row.get("status")),
                                boxedLong(row.get("knowledge_base_id")),
                                queryCount,
                                longValue(row.get("success_count")),
                                longValue(row.get("failed_count")),
                                longValue(row.get("prompt_tokens")),
                                longValue(row.get("completion_tokens")),
                                longValue(row.get("total_tokens")),
                                queryCount == 0 ? 0.0 : longValue(row.get("total_tokens")) / (double) queryCount,
                                doubleValue(row.get("p50_latency_ms")),
                                doubleValue(row.get("p90_latency_ms")),
                                cost,
                                divide(cost, queryCount),
                                ZERO,
                                ZERO,
                                "MATERIALIZED"
                        );
                    })
                    .toList();
        } catch (DataAccessException ex) {
            return List.of();
        }
    }

    private List<RagQueryLog> rows(Long tenantId,
                                   String queryType,
                                   String retrievalMode,
                                   String status,
                                   String llmModel,
                                   String embeddingModel,
                                   Long knowledgeBaseId,
                                   LocalDateTime from,
                                   LocalDateTime to) {
        return queryLogMapper.selectList(new LambdaQueryWrapper<RagQueryLog>()
                        .eq(tenantId != null, RagQueryLog::getTenantId, tenantId)
                        .eq(StringUtils.hasText(queryType), RagQueryLog::getQueryType, normalize(queryType))
                        .eq(StringUtils.hasText(retrievalMode), RagQueryLog::getRetrievalMode, clean(retrievalMode))
                        .eq(StringUtils.hasText(status), RagQueryLog::getStatus, normalize(status))
                        .eq(StringUtils.hasText(llmModel), RagQueryLog::getLlmModel, clean(llmModel))
                        .eq(StringUtils.hasText(embeddingModel), RagQueryLog::getEmbeddingModel, clean(embeddingModel))
                        .and(condition -> condition.eq(RagQueryLog::getDeleted, false).or().isNull(RagQueryLog::getDeleted))
                        .ge(from != null, RagQueryLog::getCreatedAt, from)
                        .le(to != null, RagQueryLog::getCreatedAt, to)
                        .orderByAsc(RagQueryLog::getCreatedAt))
                .stream()
                .filter(row -> knowledgeBaseId == null || containsKnowledgeBase(row.getKnowledgeBaseIdsJson(), knowledgeBaseId))
                .toList();
    }

    private CostStats stats(List<RagQueryLog> rows) {
        if (rows == null || rows.isEmpty()) {
            return new CostStats(0, 0, 0, 0, 0, 0, 0.0, 0.0, 0.0, ZERO, ZERO, ZERO, ZERO);
        }
        long count = rows.size();
        long success = rows.stream().filter(row -> QueryAuditPolicy.STATUS_SUCCESS.equals(row.getStatus())).count();
        long failed = rows.stream().filter(row -> QueryAuditPolicy.STATUS_FAILED.equals(row.getStatus())).count();
        long prompt = rows.stream().map(RagQueryLog::getPromptTokens).mapToLong(this::intValue).sum();
        long completion = rows.stream().map(RagQueryLog::getCompletionTokens).mapToLong(this::intValue).sum();
        long total = rows.stream().map(RagQueryLog::getTotalTokens).mapToLong(this::intValue).sum();
        double avgTokens = count == 0 ? 0.0 : total / (double) count;
        List<Long> latencies = rows.stream().map(RagQueryLog::getLatencyMs).toList();
        BigDecimal cost = rows.stream().map(RagQueryLog::getEstimatedTotalCost).filter(value -> value != null).reduce(ZERO, BigDecimal::add);
        long helpful = feedbackCount(rows, QueryFeedbackPolicy.RATING_HELPFUL);
        long hits = rows.stream().filter(row -> Boolean.TRUE.equals(row.getKnowledgeHit())).count();
        return new CostStats(
                count,
                success,
                failed,
                prompt,
                completion,
                total,
                avgTokens,
                timeWindowPolicy.percentile(latencies, 0.50),
                timeWindowPolicy.percentile(latencies, 0.90),
                cost,
                divide(cost, count),
                helpful == 0 ? ZERO : divide(cost, helpful),
                hits == 0 ? ZERO : divide(cost, hits)
        );
    }

    private long feedbackCount(List<RagQueryLog> rows, String rating) {
        List<Long> ids = rows.stream().map(RagQueryLog::getId).filter(id -> id != null).toList();
        if (ids.isEmpty()) {
            return 0;
        }
        return feedbackMapper.selectCount(new LambdaQueryWrapper<RagQueryFeedback>()
                .in(RagQueryFeedback::getQueryLogId, ids)
                .eq(RagQueryFeedback::getRating, rating));
    }

    private RagQueryCostTrendPoint toTrend(TrendKey key, CostStats stats) {
        return new RagQueryCostTrendPoint(
                key.bucket(),
                key.window(),
                key.tenantId(),
                key.queryType(),
                key.retrievalMode(),
                key.llmModel(),
                key.embeddingModel(),
                key.status(),
                key.knowledgeBaseId(),
                stats.queryCount(),
                stats.successCount(),
                stats.failedCount(),
                stats.totalPromptTokens(),
                stats.totalCompletionTokens(),
                stats.totalTokens(),
                stats.avgTokensPerQuery(),
                stats.p50LatencyMs(),
                stats.p90LatencyMs(),
                stats.estimatedTotalCost(),
                stats.avgCostPerQuery(),
                stats.costPerHelpfulFeedback(),
                stats.costPerKnowledgeHit(),
                "LIVE"
        );
    }

    private RagQueryCostDimensionItem toDimension(String dimension, String value, CostStats stats) {
        return new RagQueryCostDimensionItem(
                dimension,
                value,
                stats.queryCount(),
                stats.successCount(),
                stats.failedCount(),
                stats.totalPromptTokens(),
                stats.totalCompletionTokens(),
                stats.totalTokens(),
                stats.avgTokensPerQuery(),
                stats.p50LatencyMs(),
                stats.p90LatencyMs(),
                stats.estimatedTotalCost(),
                stats.avgCostPerQuery()
        );
    }

    private String normalizeDimension(String dimension) {
        if (!StringUtils.hasText(dimension)) {
            return "model";
        }
        return switch (dimension.trim()) {
            case "tenant", "queryType", "retrievalMode", "llmModel", "embeddingModel", "status" -> dimension.trim();
            default -> "model";
        };
    }

    private Function<RagQueryLog, String> dimensionExtractor(String dimension) {
        return switch (dimension) {
            case "tenant" -> row -> stringValue(row.getTenantId());
            case "queryType" -> RagQueryLog::getQueryType;
            case "retrievalMode" -> RagQueryLog::getRetrievalMode;
            case "embeddingModel" -> RagQueryLog::getEmbeddingModel;
            case "status" -> RagQueryLog::getStatus;
            default -> RagQueryLog::getLlmModel;
        };
    }

    private String metricTable(String window, String hourly, String daily) {
        return "HOUR".equals(window) ? hourly : daily;
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
            return BigDecimal.valueOf(number.doubleValue()).setScale(8, RoundingMode.HALF_UP);
        }
        return ZERO;
    }

    private boolean containsKnowledgeBase(String idsJson, Long knowledgeBaseId) {
        return StringUtils.hasText(idsJson) && idsJson.contains(String.valueOf(knowledgeBaseId));
    }

    private BigDecimal divide(BigDecimal value, long divisor) {
        if (divisor <= 0) {
            return ZERO;
        }
        return value.divide(BigDecimal.valueOf(divisor), 8, RoundingMode.HALF_UP);
    }

    private int intValue(Integer value) {
        return value == null ? 0 : value;
    }

    private String stringValue(Object value) {
        return value == null || !StringUtils.hasText(String.valueOf(value)) ? "unknown" : String.valueOf(value);
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase() : null;
    }

    private String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String csv(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        if (!text.isEmpty() && "=+-@".indexOf(text.charAt(0)) >= 0) {
            text = "'" + text;
        }
        return "\"" + text.replace("\"", "\"\"").replace("\r", " ").replace("\n", " ") + "\"";
    }

    private record TrendKey(
            LocalDateTime bucket,
            String window,
            Long tenantId,
            String queryType,
            String retrievalMode,
            String llmModel,
            String embeddingModel,
            String status,
            Long knowledgeBaseId
    ) {
    }

    private record CostStats(
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
            BigDecimal costPerKnowledgeHit
    ) {
    }
}
