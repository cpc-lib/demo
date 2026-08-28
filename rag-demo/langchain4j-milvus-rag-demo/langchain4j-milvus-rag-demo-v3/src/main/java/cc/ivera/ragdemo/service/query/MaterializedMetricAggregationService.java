package cc.ivera.ragdemo.service.query;


import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.mapper.MetricAggregationMapper;
import cc.ivera.ragdemo.model.query.FeedbackQualityMetricUpsertCommand;
import cc.ivera.ragdemo.model.query.QueryCostMetricUpsertCommand;
import cc.ivera.ragdemo.model.query.RerankObservationMetricUpsertCommand;
import cc.ivera.ragdemo.service.ragops.TimeWindowAggregationPolicy;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class MaterializedMetricAggregationService {

    private static final Long ALL_KNOWLEDGE_BASES = 0L;
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+");

    private final RagProperties properties;
    private final MetricAggregationMapper metricAggregationMapper;
    private final TimeWindowAggregationPolicy timeWindowPolicy;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${rag.metrics.fixed-delay-millis:900000}")
    public void scheduledAggregate() {
        if (!properties.getMetrics().isScheduledAggregationEnabled()) {
            return;
        }
        LocalDateTime to = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
        LocalDateTime from = to.minusHours(Math.max(1, properties.getMetrics().getLookbackHours())
                + Math.max(0, properties.getMetrics().getLateArrivalWindowHours()));
        aggregate(from, to);
    }

    @Transactional
    public Map<String, Object> aggregate(LocalDateTime from, LocalDateTime to) {
        LocalDateTime end = to == null ? LocalDateTime.now().truncatedTo(ChronoUnit.HOURS) : to;
        LocalDateTime start = from == null ? end.minusHours(Math.max(1, properties.getMetrics().getLookbackHours())
                + Math.max(0, properties.getMetrics().getLateArrivalWindowHours())) : from;
        int queryHourly = aggregateQueryCost("HOUR", start, end);
        int queryDaily = aggregateQueryCost("DAY", start, end);
        int feedbackHourly = aggregateFeedbackQuality("HOUR", start, end);
        int feedbackDaily = aggregateFeedbackQuality("DAY", start, end);
        int rerankHourly = aggregateRerank("HOUR", start, end);
        int rerankDaily = aggregateRerank("DAY", start, end);
        upsertWatermark("query_cost", end);
        upsertWatermark("feedback_quality", end);
        upsertWatermark("rerank_observation", end);
        return Map.of(
                "from", start,
                "to", end,
                "queryHourly", queryHourly,
                "queryDaily", queryDaily,
                "feedbackHourly", feedbackHourly,
                "feedbackDaily", feedbackDaily,
                "rerankHourly", rerankHourly,
                "rerankDaily", rerankDaily
        );
    }

    public Object watermarks() {
        return metricAggregationMapper.listWatermarks();
    }

    private int aggregateQueryCost(String window, LocalDateTime from, LocalDateTime to) {
        String table = metricTable(window, "rag_query_cost_metric_hourly", "rag_query_cost_metric_daily");
        List<Map<String, Object>> rows = metricAggregationMapper.listQueryCostAggregationRows(from, to);
        Map<QueryCostKey, QueryCostAccumulator> grouped = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            LocalDateTime createdAt = timeValue(row.get("created_at"));
            if (createdAt == null) {
                continue;
            }
            for (Long knowledgeBaseId : knowledgeBaseDimensionValues(row.get("knowledge_base_ids_json"))) {
                QueryCostKey key = new QueryCostKey(
                        longValue(row.get("tenant_id")),
                        knowledgeBaseId,
                        bucket(createdAt, window),
                        window,
                        normalize(row.get("query_type"), "UNKNOWN"),
                        normalize(row.get("retrieval_mode"), "unknown"),
                        normalize(row.get("llm_model"), "unknown"),
                        normalize(row.get("embedding_model"), "unknown"),
                        normalize(row.get("status"), "UNKNOWN")
                );
                grouped.computeIfAbsent(key, ignored -> new QueryCostAccumulator())
                        .add(row);
            }
        }
        int affected = 0;
        for (Map.Entry<QueryCostKey, QueryCostAccumulator> entry : grouped.entrySet()) {
            QueryCostKey key = entry.getKey();
            QueryCostAccumulator value = entry.getValue();
            affected += metricAggregationMapper.upsertQueryCostMetric(new QueryCostMetricUpsertCommand(
                    table,
                    key.tenantId(), key.knowledgeBaseId(), key.bucketStart(), key.windowType(), key.queryType(),
                    key.retrievalMode(), key.llmModel(), key.embeddingModel(), key.status(),
                    value.queryCount, value.successCount, value.failedCount, value.promptTokens,
                    value.completionTokens, value.totalTokens, value.estimatedTotalCost,
                    percentile(value.latencies, 0.50), percentile(value.latencies, 0.90),
                    percentile(value.latencies, 0.95), percentile(value.latencies, 0.99)
            ));
        }
        return affected;
    }

    private int aggregateFeedbackQuality(String window, LocalDateTime from, LocalDateTime to) {
        String table = metricTable(window, "rag_feedback_quality_metric_hourly", "rag_feedback_quality_metric_daily");
        List<Map<String, Object>> rows = metricAggregationMapper.listFeedbackQualityAggregationRows(from, to);
        Map<FeedbackKey, FeedbackAccumulator> grouped = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            LocalDateTime createdAt = timeValue(row.get("created_at"));
            if (createdAt == null) {
                continue;
            }
            for (Long knowledgeBaseId : knowledgeBaseDimensionValues(row.get("knowledge_base_ids_json"))) {
                FeedbackKey key = new FeedbackKey(
                        longValue(row.get("tenant_id")),
                        knowledgeBaseId,
                        bucket(createdAt, window),
                        window,
                        normalize(row.get("retrieval_mode"), "unknown"),
                        normalize(row.get("query_type"), "UNKNOWN"),
                        normalize(row.get("rating"), "UNKNOWN"),
                        normalize(row.get("feedback_status"), "UNKNOWN"),
                        normalize(row.get("assignee"), "unassigned")
                );
                grouped.computeIfAbsent(key, ignored -> new FeedbackAccumulator())
                        .add(row);
            }
        }
        int affected = 0;
        for (Map.Entry<FeedbackKey, FeedbackAccumulator> entry : grouped.entrySet()) {
            FeedbackKey key = entry.getKey();
            FeedbackAccumulator value = entry.getValue();
            affected += metricAggregationMapper.upsertFeedbackQualityMetric(new FeedbackQualityMetricUpsertCommand(
                    table,
                    key.tenantId(), key.bucketStart(), key.windowType(), key.knowledgeBaseId(), key.retrievalMode(),
                    key.queryType(), key.feedbackRating(), key.feedbackStatus(), key.assignee(),
                    value.queryIds.size(), value.feedbackCount, value.helpfulCount, value.notHelpfulCount,
                    value.correctionCount
            ));
        }
        return affected;
    }

    private int aggregateRerank(String window, LocalDateTime from, LocalDateTime to) {
        String table = metricTable(window, "rag_rerank_observation_metric_hourly", "rag_rerank_observation_metric_daily");
        List<Map<String, Object>> rows = metricAggregationMapper.listRerankObservationAggregationRows(from, to);
        Map<RerankKey, RerankAccumulator> grouped = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            LocalDateTime createdAt = timeValue(row.get("created_at"));
            if (createdAt == null) {
                continue;
            }
            RerankKey key = new RerankKey(
                    longValue(row.get("tenant_id")),
                    bucket(createdAt, window),
                    window,
                    normalize(row.get("provider"), "unknown"),
                    normalize(row.get("model"), "unknown"),
                    normalize(row.get("api_key_hash"), "unknown"),
                    normalize(row.get("error_code_normalized"), "none"),
                    normalize(row.get("degraded_reason"), "none")
            );
            grouped.computeIfAbsent(key, ignored -> new RerankAccumulator())
                    .add(row);
        }
        int affected = 0;
        for (Map.Entry<RerankKey, RerankAccumulator> entry : grouped.entrySet()) {
            RerankKey key = entry.getKey();
            RerankAccumulator value = entry.getValue();
            affected += metricAggregationMapper.upsertRerankObservationMetric(new RerankObservationMetricUpsertCommand(
                    table,
                    key.tenantId(), key.bucketStart(), key.windowType(), key.provider(), key.model(),
                    key.apiKeyHash(), key.errorCode(), key.degradedReason(), value.requestCount,
                    value.successCount, value.failureCount, value.fallbackCount, value.retryCount,
                    value.cacheHitCount, value.totalTokens, value.estimatedCost,
                    percentile(value.latencies, 0.50), percentile(value.latencies, 0.90),
                    percentile(value.latencies, 0.95), percentile(value.latencies, 0.99)
            ));
        }
        return affected;
    }

    private void upsertWatermark(String metric, LocalDateTime watermark) {
        metricAggregationMapper.upsertMetricAggregationWatermark(metric, watermark);
    }

    private String metricTable(String window, String hourly, String daily) {
        return "HOUR".equals(window) ? hourly : daily;
    }

    private LocalDateTime bucket(LocalDateTime createdAt, String window) {
        return timeWindowPolicy.bucket(createdAt, window);
    }

    private Set<Long> knowledgeBaseDimensionValues(Object value) {
        Set<Long> ids = new LinkedHashSet<>();
        ids.add(ALL_KNOWLEDGE_BASES);
        ids.addAll(parseKnowledgeBaseIds(value));
        return ids;
    }

    private List<Long> parseKnowledgeBaseIds(Object value) {
        if (value == null || !StringUtils.hasText(String.valueOf(value))) {
            return List.of();
        }
        String raw = String.valueOf(value);
        try {
            JsonNode node = objectMapper.readTree(raw);
            List<Long> ids = new ArrayList<>();
            collectKnowledgeBaseIds(node, ids);
            return ids.stream().filter(id -> id != null && id > 0).distinct().toList();
        } catch (Exception ignored) {
            List<Long> ids = new ArrayList<>();
            Matcher matcher = NUMBER_PATTERN.matcher(raw);
            while (matcher.find()) {
                try {
                    ids.add(Long.parseLong(matcher.group()));
                } catch (NumberFormatException ignoredNumber) {
                    // Ignore malformed fragments in legacy comma-separated metadata.
                }
            }
            return ids.stream().filter(id -> id > 0).distinct().toList();
        }
    }

    private void collectKnowledgeBaseIds(JsonNode node, List<Long> ids) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isIntegralNumber()) {
            ids.add(node.longValue());
            return;
        }
        if (node.isTextual()) {
            try {
                ids.add(Long.parseLong(node.asText()));
            } catch (NumberFormatException ignored) {
                // Ignore non-numeric textual values.
            }
            return;
        }
        if (node.isArray()) {
            node.forEach(child -> collectKnowledgeBaseIds(child, ids));
            return;
        }
        if (node.isObject()) {
            JsonNode direct = node.get("id");
            if (direct == null) {
                direct = node.get("knowledgeBaseId");
            }
            if (direct != null) {
                collectKnowledgeBaseIds(direct, ids);
            }
        }
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

    private Long longValue(Object value) {
        Long parsed = nullableLong(value);
        return parsed == null ? 0L : parsed;
    }

    private Long nullableLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value != null && StringUtils.hasText(String.valueOf(value))) {
            try {
                return Long.parseLong(String.valueOf(value));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private int intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null && StringUtils.hasText(String.valueOf(value))) {
            try {
                return Integer.parseInt(String.valueOf(value));
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private BigDecimal decimalValue(Object value) {
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        if (value != null && StringUtils.hasText(String.valueOf(value))) {
            try {
                return new BigDecimal(String.valueOf(value));
            } catch (NumberFormatException ignored) {
                return BigDecimal.ZERO;
            }
        }
        return BigDecimal.ZERO;
    }

    private boolean booleanValue(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        return value != null && Boolean.parseBoolean(String.valueOf(value));
    }

    private String normalize(Object value, String fallback) {
        if (value == null || !StringUtils.hasText(String.valueOf(value))) {
            return fallback;
        }
        return String.valueOf(value).trim();
    }

    private double percentile(List<Long> values, double percentile) {
        return timeWindowPolicy.percentile(values, percentile);
    }

    private final class QueryCostAccumulator {
        private long queryCount;
        private long successCount;
        private long failedCount;
        private long promptTokens;
        private long completionTokens;
        private long totalTokens;
        private BigDecimal estimatedTotalCost = BigDecimal.ZERO;
        private final List<Long> latencies = new ArrayList<>();

        private void add(Map<String, Object> row) {
            queryCount++;
            String status = normalize(row.get("status"), "");
            if ("SUCCESS".equals(status)) {
                successCount++;
            }
            if ("FAILED".equals(status)) {
                failedCount++;
            }
            promptTokens += intValue(row.get("prompt_tokens"));
            completionTokens += intValue(row.get("completion_tokens"));
            totalTokens += intValue(row.get("total_tokens"));
            estimatedTotalCost = estimatedTotalCost.add(decimalValue(row.get("estimated_total_cost")));
            Long latency = nullableLong(row.get("latency_ms"));
            if (latency != null && latency >= 0) {
                latencies.add(latency);
            }
        }
    }

    private final class FeedbackAccumulator {
        private final Set<Long> queryIds = new LinkedHashSet<>();
        private long feedbackCount;
        private long helpfulCount;
        private long notHelpfulCount;
        private long correctionCount;

        private void add(Map<String, Object> row) {
            queryIds.add(longValue(row.get("query_log_id")));
            feedbackCount++;
            String rating = normalize(row.get("rating"), "");
            if ("HELPFUL".equals(rating)) {
                helpfulCount++;
            }
            if ("NOT_HELPFUL".equals(rating)) {
                notHelpfulCount++;
            }
            if ("CORRECTION".equals(rating)) {
                correctionCount++;
            }
        }
    }

    private final class RerankAccumulator {
        private long requestCount;
        private long successCount;
        private long failureCount;
        private long fallbackCount;
        private long retryCount;
        private long cacheHitCount;
        private long totalTokens;
        private BigDecimal estimatedCost = BigDecimal.ZERO;
        private final List<Long> latencies = new ArrayList<>();

        private void add(Map<String, Object> row) {
            requestCount++;
            Boolean success = boolOrNull(row.get("success"));
            if (Boolean.TRUE.equals(success)) {
                successCount++;
            } else if (Boolean.FALSE.equals(success)) {
                failureCount++;
            }
            if (booleanValue(row.get("fallback"))) {
                fallbackCount++;
            }
            if (intValue(row.get("retry_count")) > 0) {
                retryCount++;
            }
            if (booleanValue(row.get("cache_hit"))) {
                cacheHitCount++;
            }
            totalTokens += intValue(row.get("total_tokens"));
            estimatedCost = estimatedCost.add(decimalValue(row.get("estimated_cost")));
            Long latency = nullableLong(row.get("latency_ms"));
            if (latency != null && latency >= 0) {
                latencies.add(latency);
            }
        }

        private Boolean boolOrNull(Object value) {
            if (value == null) {
                return null;
            }
            return booleanValue(value);
        }
    }

    private record QueryCostKey(Long tenantId,
                                Long knowledgeBaseId,
                                LocalDateTime bucketStart,
                                String windowType,
                                String queryType,
                                String retrievalMode,
                                String llmModel,
                                String embeddingModel,
                                String status) {
    }

    private record FeedbackKey(Long tenantId,
                               Long knowledgeBaseId,
                               LocalDateTime bucketStart,
                               String windowType,
                               String retrievalMode,
                               String queryType,
                               String feedbackRating,
                               String feedbackStatus,
                               String assignee) {
    }

    private record RerankKey(Long tenantId,
                             LocalDateTime bucketStart,
                             String windowType,
                             String provider,
                             String model,
                             String apiKeyHash,
                             String errorCode,
                             String degradedReason) {
    }
}
