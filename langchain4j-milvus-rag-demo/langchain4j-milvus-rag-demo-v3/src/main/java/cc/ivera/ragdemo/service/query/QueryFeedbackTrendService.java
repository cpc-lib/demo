package cc.ivera.ragdemo.service.query;


import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.domain.rag.RagFeedbackRevisionTask;
import cc.ivera.ragdemo.domain.rag.RagQueryFeedback;
import cc.ivera.ragdemo.domain.rag.RagQueryLog;
import cc.ivera.ragdemo.mapper.MetricAggregationMapper;
import cc.ivera.ragdemo.mapper.RagFeedbackRevisionTaskMapper;
import cc.ivera.ragdemo.mapper.RagQueryFeedbackMapper;
import cc.ivera.ragdemo.mapper.RagQueryLogMapper;
import cc.ivera.ragdemo.model.query.RagFeedbackDimensionItem;
import cc.ivera.ragdemo.model.query.RagFeedbackQualitySummary;
import cc.ivera.ragdemo.model.query.RagFeedbackQualityTrendPoint;
import cc.ivera.ragdemo.service.ragops.FeedbackRevisionPolicy;
import cc.ivera.ragdemo.service.ragops.QueryFeedbackPolicy;
import cc.ivera.ragdemo.service.ragops.QueryFeedbackWorkflowPolicy;
import cc.ivera.ragdemo.service.ragops.TimeWindowAggregationPolicy;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class QueryFeedbackTrendService {

    private final RagQueryLogMapper queryLogMapper;
    private final RagQueryFeedbackMapper feedbackMapper;
    private final RagFeedbackRevisionTaskMapper revisionTaskMapper;
    private final TimeWindowAggregationPolicy timeWindowPolicy;
    private final RagProperties properties;
    private final MetricAggregationMapper metricAggregationMapper;

    public List<RagFeedbackQualityTrendPoint> trends(Long tenantId,
                                                     Long knowledgeBaseId,
                                                     String retrievalMode,
                                                     String queryType,
                                                     String feedbackRating,
                                                     String feedbackStatus,
                                                     String assignee,
                                                     String window,
                                                     LocalDateTime from,
                                                     LocalDateTime to) {
        List<RagFeedbackQualityTrendPoint> materialized = materializedTrends(
                tenantId, knowledgeBaseId, retrievalMode, queryType, feedbackRating, feedbackStatus, assignee, window, from, to);
        if (!materialized.isEmpty()) {
            return materialized;
        }
        FeedbackRows rows = rows(tenantId, knowledgeBaseId, retrievalMode, queryType, feedbackRating, feedbackStatus, assignee, from, to);
        Map<TrendKey, List<FeedbackRow>> grouped = rows.feedbackRows().stream()
                .collect(Collectors.groupingBy(row -> new TrendKey(
                        timeWindowPolicy.bucket(row.feedback().getCreatedAt(), window),
                        timeWindowPolicy.normalize(window).name().toLowerCase(),
                        row.log().getTenantId(),
                        knowledgeBaseId,
                        row.log().getRetrievalMode(),
                        row.log().getQueryType(),
                        row.feedback().getRating(),
                        row.feedback().getFeedbackStatus(),
                        row.feedback().getAssignee()
                ), LinkedHashMap::new, Collectors.toList()));
        return grouped.entrySet().stream()
                .map(entry -> {
                    Metrics metrics = metrics(entry.getValue(), rows.tasksByFeedbackId(), queryCountForBucket(rows.logs(), entry.getKey().bucket(), window));
                    TrendKey key = entry.getKey();
                    return new RagFeedbackQualityTrendPoint(
                            key.bucket(),
                            key.window(),
                            key.tenantId(),
                            key.knowledgeBaseId(),
                            key.retrievalMode(),
                            key.queryType(),
                            key.feedbackRating(),
                            key.feedbackStatus(),
                            key.assignee(),
                            metrics.feedbackCount(),
                            metrics.feedbackRate(),
                            metrics.helpfulRate(),
                            metrics.notHelpfulRate(),
                            metrics.correctionRate(),
                            metrics.correctionAcceptedRate(),
                            metrics.avgTimeToFirstReviewHours(),
                            metrics.avgTimeToResolveHours(),
                            metrics.reopenedCount(),
                            metrics.linkedRevisionCount(),
                            metrics.verifiedFixRate(),
                            "LIVE"
                    );
                })
                .toList();
    }

    public RagFeedbackQualitySummary qualitySummary(Long tenantId,
                                                    Long knowledgeBaseId,
                                                    String retrievalMode,
                                                    String queryType,
                                                    String feedbackRating,
                                                    String feedbackStatus,
                                                    String assignee,
                                                    LocalDateTime from,
                                                    LocalDateTime to) {
        FeedbackRows rows = rows(tenantId, knowledgeBaseId, retrievalMode, queryType, feedbackRating, feedbackStatus, assignee, from, to);
        Metrics metrics = metrics(rows.feedbackRows(), rows.tasksByFeedbackId(), rows.logs().size());
        return new RagFeedbackQualitySummary(
                rows.logs().size(),
                metrics.feedbackCount(),
                metrics.feedbackRate(),
                metrics.helpfulRate(),
                metrics.notHelpfulRate(),
                metrics.correctionRate(),
                metrics.correctionAcceptedRate(),
                metrics.avgTimeToFirstReviewHours(),
                metrics.avgTimeToResolveHours(),
                metrics.reopenedCount(),
                metrics.linkedRevisionCount(),
                metrics.verifiedFixRate()
        );
    }

    public List<RagFeedbackDimensionItem> byDimension(String dimension,
                                                      Long tenantId,
                                                      Long knowledgeBaseId,
                                                      String retrievalMode,
                                                      String queryType,
                                                      LocalDateTime from,
                                                      LocalDateTime to) {
        String dim = normalizeDimension(dimension);
        Function<FeedbackRow, String> extractor = dimensionExtractor(dim);
        FeedbackRows rows = rows(tenantId, knowledgeBaseId, retrievalMode, queryType, null, null, null, from, to);
        return rows.feedbackRows().stream()
                .collect(Collectors.groupingBy(row -> stringValue(extractor.apply(row)), LinkedHashMap::new, Collectors.toList()))
                .entrySet()
                .stream()
                .map(entry -> {
                    Metrics metrics = metrics(entry.getValue(), rows.tasksByFeedbackId(), rows.logs().size());
                    return new RagFeedbackDimensionItem(
                            dim,
                            entry.getKey(),
                            metrics.feedbackCount(),
                            metrics.helpfulRate(),
                            metrics.notHelpfulRate(),
                            metrics.correctionRate(),
                            metrics.avgTimeToResolveHours(),
                            metrics.linkedRevisionCount(),
                            metrics.verifiedFixRate()
                    );
                })
                .toList();
    }

    public String exportCsv(Long tenantId,
                            Long knowledgeBaseId,
                            String retrievalMode,
                            String queryType,
                            String window,
                            LocalDateTime from,
                            LocalDateTime to) {
        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append("bucket,window,tenantId,knowledgeBaseId,retrievalMode,queryType,feedbackRating,feedbackStatus,assignee,feedbackCount,feedbackRate,helpfulRate,notHelpfulRate,correctionRate,verifiedFixRate\n");
        for (RagFeedbackQualityTrendPoint row : trends(tenantId, knowledgeBaseId, retrievalMode, queryType, null, null, null, window, from, to)) {
            csv.append(csv(row.bucket())).append(',')
                    .append(csv(row.window())).append(',')
                    .append(csv(row.tenantId())).append(',')
                    .append(csv(row.knowledgeBaseId())).append(',')
                    .append(csv(row.retrievalMode())).append(',')
                    .append(csv(row.queryType())).append(',')
                    .append(csv(row.feedbackRating())).append(',')
                    .append(csv(row.feedbackStatus())).append(',')
                    .append(csv(row.assignee())).append(',')
                    .append(csv(row.feedbackCount())).append(',')
                    .append(csv(row.feedbackRate())).append(',')
                    .append(csv(row.helpfulRate())).append(',')
                    .append(csv(row.notHelpfulRate())).append(',')
                    .append(csv(row.correctionRate())).append(',')
                    .append(csv(row.verifiedFixRate()))
                    .append('\n');
        }
        return csv.toString();
    }

    private List<RagFeedbackQualityTrendPoint> materializedTrends(Long tenantId,
                                                                  Long knowledgeBaseId,
                                                                  String retrievalMode,
                                                                  String queryType,
                                                                  String feedbackRating,
                                                                  String feedbackStatus,
                                                                  String assignee,
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
                ? "rag_feedback_quality_metric_hourly"
                : "rag_feedback_quality_metric_daily";
        String sourceWindow = "HOUR".equals(normalizedWindow) ? "HOUR" : "DAY";
        try {
            return metricAggregationMapper.listMaterializedFeedbackQualityTrends(
                            table,
                            normalizedWindow,
                            sourceWindow,
                            tenantId,
                            knowledgeBaseId == null ? 0L : knowledgeBaseId,
                            clean(retrievalMode),
                            normalize(queryType),
                            normalize(feedbackRating),
                            normalize(feedbackStatus),
                            clean(assignee),
                            from,
                            to
                    ).stream()
                    .map(row -> {
                        long queryCount = longValue(row.get("query_count"));
                        long feedbackCount = longValue(row.get("feedback_count"));
                        long helpful = longValue(row.get("helpful_count"));
                        long notHelpful = longValue(row.get("not_helpful_count"));
                        long correction = longValue(row.get("correction_count"));
                        return new RagFeedbackQualityTrendPoint(
                                timeValue(row.get("bucket_start")),
                                String.valueOf(row.get("window_type")).toLowerCase(),
                                boxedLong(row.get("tenant_id")),
                                boxedLong(row.get("knowledge_base_id")),
                                stringValue(row.get("retrieval_mode")),
                                stringValue(row.get("query_type")),
                                stringValue(row.get("feedback_rating")),
                                stringValue(row.get("feedback_status")),
                                stringValue(row.get("assignee")),
                                feedbackCount,
                                queryCount == 0 ? 0.0 : feedbackCount / (double) queryCount,
                                feedbackCount == 0 ? 0.0 : helpful / (double) feedbackCount,
                                feedbackCount == 0 ? 0.0 : notHelpful / (double) feedbackCount,
                                feedbackCount == 0 ? 0.0 : correction / (double) feedbackCount,
                                0.0,
                                0.0,
                                0.0,
                                0,
                                0,
                                0.0,
                                "MATERIALIZED"
                        );
                    })
                    .toList();
        } catch (DataAccessException ex) {
            return List.of();
        }
    }

    private FeedbackRows rows(Long tenantId,
                              Long knowledgeBaseId,
                              String retrievalMode,
                              String queryType,
                              String feedbackRating,
                              String feedbackStatus,
                              String assignee,
                              LocalDateTime from,
                              LocalDateTime to) {
        List<RagQueryLog> logs = queryLogMapper.selectList(new LambdaQueryWrapper<RagQueryLog>()
                        .eq(tenantId != null, RagQueryLog::getTenantId, tenantId)
                        .eq(StringUtils.hasText(retrievalMode), RagQueryLog::getRetrievalMode, clean(retrievalMode))
                        .eq(StringUtils.hasText(queryType), RagQueryLog::getQueryType, normalize(queryType))
                        .and(condition -> condition.eq(RagQueryLog::getDeleted, false).or().isNull(RagQueryLog::getDeleted))
                        .ge(from != null, RagQueryLog::getCreatedAt, from)
                        .le(to != null, RagQueryLog::getCreatedAt, to)
                        .orderByAsc(RagQueryLog::getCreatedAt))
                .stream()
                .filter(log -> knowledgeBaseId == null || containsKnowledgeBase(log.getKnowledgeBaseIdsJson(), knowledgeBaseId))
                .toList();
        List<Long> logIds = logs.stream().map(RagQueryLog::getId).filter(id -> id != null).toList();
        if (logIds.isEmpty()) {
            return new FeedbackRows(logs, List.of(), Map.of());
        }
        Map<Long, RagQueryLog> logsById = logs.stream().collect(Collectors.toMap(RagQueryLog::getId, Function.identity()));
        List<FeedbackRow> feedbackRows = feedbackMapper.selectList(new LambdaQueryWrapper<RagQueryFeedback>()
                        .in(RagQueryFeedback::getQueryLogId, logIds)
                        .eq(StringUtils.hasText(feedbackRating), RagQueryFeedback::getRating, normalize(feedbackRating))
                        .eq(StringUtils.hasText(feedbackStatus), RagQueryFeedback::getFeedbackStatus, normalize(feedbackStatus))
                        .eq(StringUtils.hasText(assignee), RagQueryFeedback::getAssignee, clean(assignee))
                        .orderByAsc(RagQueryFeedback::getCreatedAt))
                .stream()
                .map(feedback -> new FeedbackRow(feedback, logsById.get(feedback.getQueryLogId())))
                .filter(row -> row.log() != null)
                .toList();
        List<Long> feedbackIds = feedbackRows.stream().map(row -> row.feedback().getId()).filter(id -> id != null).toList();
        Map<Long, List<RagFeedbackRevisionTask>> tasks = feedbackIds.isEmpty()
                ? Map.of()
                : revisionTaskMapper.selectList(new LambdaQueryWrapper<RagFeedbackRevisionTask>()
                                .in(RagFeedbackRevisionTask::getFeedbackId, feedbackIds))
                        .stream()
                        .collect(Collectors.groupingBy(RagFeedbackRevisionTask::getFeedbackId));
        return new FeedbackRows(logs, feedbackRows, tasks);
    }

    private Metrics metrics(List<FeedbackRow> feedbackRows,
                            Map<Long, List<RagFeedbackRevisionTask>> tasksByFeedbackId,
                            long queryCount) {
        if (feedbackRows == null || feedbackRows.isEmpty()) {
            return new Metrics(0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0, 0, 0.0);
        }
        long count = feedbackRows.size();
        long helpful = ratingCount(feedbackRows, QueryFeedbackPolicy.RATING_HELPFUL);
        long notHelpful = ratingCount(feedbackRows, QueryFeedbackPolicy.RATING_NOT_HELPFUL);
        long correction = ratingCount(feedbackRows, QueryFeedbackPolicy.RATING_CORRECTION);
        long validCorrections = feedbackRows.stream()
                .filter(row -> QueryFeedbackPolicy.RATING_CORRECTION.equals(row.feedback().getRating()))
                .filter(row -> QueryFeedbackWorkflowPolicy.REVIEW_VALID.equals(row.feedback().getReviewResult()))
                .count();
        long reopened = feedbackRows.stream().map(row -> row.feedback().getReopenedCount()).mapToLong(value -> value == null ? 0L : value).sum();
        long linkedRevisions = feedbackRows.stream()
                .map(row -> tasksByFeedbackId.getOrDefault(row.feedback().getId(), List.of()).size())
                .mapToLong(Integer::longValue)
                .sum();
        long verifiedRevisions = feedbackRows.stream()
                .flatMap(row -> tasksByFeedbackId.getOrDefault(row.feedback().getId(), List.of()).stream())
                .filter(task -> FeedbackRevisionPolicy.STATUS_VERIFIED.equals(task.getRevisionStatus()))
                .count();
        return new Metrics(
                count,
                queryCount <= 0 ? 0.0 : count / (double) queryCount,
                helpful / (double) count,
                notHelpful / (double) count,
                correction / (double) count,
                correction == 0 ? 0.0 : validCorrections / (double) correction,
                avgHours(feedbackRows, false),
                avgHours(feedbackRows, true),
                reopened,
                linkedRevisions,
                linkedRevisions == 0 ? 0.0 : verifiedRevisions / (double) linkedRevisions
        );
    }

    private long queryCountForBucket(List<RagQueryLog> logs, LocalDateTime bucket, String window) {
        return logs.stream()
                .filter(log -> bucket.equals(timeWindowPolicy.bucket(log.getCreatedAt(), window)))
                .count();
    }

    private double avgHours(List<FeedbackRow> rows, boolean resolvedOnly) {
        return rows.stream()
                .map(FeedbackRow::feedback)
                .filter(feedback -> feedback.getCreatedAt() != null)
                .filter(feedback -> resolvedOnly ? feedback.getResolvedAt() != null : feedback.getUpdatedAt() != null && feedback.getReviewResult() != null)
                .mapToDouble(feedback -> {
                    LocalDateTime end = resolvedOnly ? feedback.getResolvedAt() : feedback.getUpdatedAt();
                    return Duration.between(feedback.getCreatedAt(), end).toMinutes() / 60.0;
                })
                .average()
                .orElse(0.0);
    }

    private long ratingCount(List<FeedbackRow> rows, String rating) {
        return rows.stream().filter(row -> rating.equals(row.feedback().getRating())).count();
    }

    private String normalizeDimension(String dimension) {
        if (!StringUtils.hasText(dimension)) {
            return "assignee";
        }
        return switch (dimension.trim()) {
            case "knowledgeBase", "assignee", "feedbackStatus", "feedbackRating", "queryType", "retrievalMode" -> dimension.trim();
            default -> "assignee";
        };
    }

    private Function<FeedbackRow, String> dimensionExtractor(String dimension) {
        return switch (dimension) {
            case "knowledgeBase" -> row -> row.log().getKnowledgeBaseIdsJson();
            case "feedbackStatus" -> row -> row.feedback().getFeedbackStatus();
            case "feedbackRating" -> row -> row.feedback().getRating();
            case "queryType" -> row -> row.log().getQueryType();
            case "retrievalMode" -> row -> row.log().getRetrievalMode();
            default -> row -> row.feedback().getAssignee();
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

    private boolean containsKnowledgeBase(String idsJson, Long knowledgeBaseId) {
        return StringUtils.hasText(idsJson) && idsJson.contains(String.valueOf(knowledgeBaseId));
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase() : null;
    }

    private String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String stringValue(Object value) {
        return value == null || !StringUtils.hasText(String.valueOf(value)) ? "unknown" : String.valueOf(value);
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
            Long knowledgeBaseId,
            String retrievalMode,
            String queryType,
            String feedbackRating,
            String feedbackStatus,
            String assignee
    ) {
    }

    private record FeedbackRows(
            List<RagQueryLog> logs,
            List<FeedbackRow> feedbackRows,
            Map<Long, List<RagFeedbackRevisionTask>> tasksByFeedbackId
    ) {
    }

    private record FeedbackRow(
            RagQueryFeedback feedback,
            RagQueryLog log
    ) {
    }

    private record Metrics(
            long feedbackCount,
            double feedbackRate,
            double helpfulRate,
            double notHelpfulRate,
            double correctionRate,
            double correctionAcceptedRate,
            double avgTimeToFirstReviewHours,
            double avgTimeToResolveHours,
            long reopenedCount,
            long linkedRevisionCount,
            double verifiedFixRate
    ) {
    }
}
