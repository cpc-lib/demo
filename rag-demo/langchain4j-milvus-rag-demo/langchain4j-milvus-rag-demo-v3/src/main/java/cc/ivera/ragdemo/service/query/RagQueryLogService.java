package cc.ivera.ragdemo.service.query;


import cc.ivera.ragdemo.domain.rag.*;
import cc.ivera.ragdemo.mapper.*;
import cc.ivera.ragdemo.model.query.*;
import cc.ivera.ragdemo.service.ragops.QueryDeletePolicy;
import cc.ivera.ragdemo.service.ragops.QueryFeedbackPolicy;
import cc.ivera.ragdemo.service.ragops.QueryFeedbackWorkflowPolicy;
import cc.ivera.ragdemo.service.ragops.QueryRetentionPolicy;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class RagQueryLogService {

    private static final int DEFAULT_PAGE_NO = 1;
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 200;
    private static final int MAX_EXPORT_LIMIT = 5000;
    private static final int MAX_OPERATION_LIMIT = 10000;

    private final RagQueryLogMapper queryLogMapper;
    private final RagQueryHitMapper queryHitMapper;
    private final RagQueryFeedbackMapper queryFeedbackMapper;
    private final RagQueryFeedbackEventMapper queryFeedbackEventMapper;
    private final RagQueryLogDeleteAuditMapper deleteAuditMapper;
    private final RagQueryLogArchiveMapper archiveMapper;
    private final RagQueryRetentionPolicyMapper retentionPolicyMapper;
    private final ObjectMapper objectMapper;
    private final QueryFeedbackPolicy feedbackPolicy;
    private final QueryFeedbackWorkflowPolicy feedbackWorkflowPolicy;
    private final QueryDeletePolicy deletePolicy;
    private final QueryRetentionPolicy retentionPolicy;

    public List<RagQueryLog> listLogs(Long tenantId,
                                      String queryType,
                                      String status,
                                      String conversationId,
                                      String traceId,
                                      Integer limit) {
        return pageLogs(tenantId, queryType, status, conversationId, traceId, null, DEFAULT_PAGE_NO, limit).records();
    }

    public PageResponse<RagQueryLog> pageLogs(Long tenantId,
                                              String queryType,
                                              String status,
                                              String conversationId,
                                              String traceId,
                                              String queryText,
                                              Integer pageNo,
                                              Integer pageSize) {
        return pageLogs(tenantId, queryType, status, conversationId, traceId, queryText, pageNo, pageSize, null, null);
    }

    public PageResponse<RagQueryLog> pageLogs(Long tenantId,
                                              String queryType,
                                              String status,
                                              String conversationId,
                                              String traceId,
                                              String queryText,
                                              Integer pageNo,
                                              Integer pageSize,
                                              String sortBy,
                                              String sortDirection) {
        return pageLogs(tenantId, queryType, status, conversationId, traceId, queryText, pageNo, pageSize, sortBy, sortDirection, null);
    }

    public PageResponse<RagQueryLog> pageLogs(Long tenantId,
                                              String queryType,
                                              String status,
                                              String conversationId,
                                              String traceId,
                                              String queryText,
                                              Integer pageNo,
                                              Integer pageSize,
                                              String sortBy,
                                              String sortDirection,
                                              String visibility) {
        PageQuery pageQuery = PageQuery.of(pageNo, pageSize, DEFAULT_PAGE_SIZE, sortBy, sortDirection, MAX_PAGE_SIZE)
                .withDefaultSort("createdAt", "DESC");
        long total = queryLogMapper.selectCount(queryWrapper(
                tenantId,
                queryType,
                status,
                conversationId,
                traceId,
                queryText,
                false,
                0,
                0,
                pageQuery,
                visibility
        ));
        int safePageSize = pageQuery.effectivePageSize(safeTotal(total));
        int offset = Math.min((pageQuery.pageNo() - 1) * safePageSize, safeTotal(total));
        List<RagQueryLog> records = queryLogMapper.selectList(queryWrapper(
                tenantId,
                queryType,
                status,
                conversationId,
                traceId,
                queryText,
                true,
                offset,
                safePageSize,
                pageQuery,
                visibility
        ));
        return PageResponse.of(pageQuery, total, records);
    }

    public RagQueryLogDetailResponse getDetail(Long queryLogId) {
        RagQueryLog log = getRequiredLog(queryLogId);
        List<RagQueryHit> hits = queryHitMapper.selectList(new LambdaQueryWrapper<RagQueryHit>()
                .eq(RagQueryHit::getQueryLogId, queryLogId)
                .orderByAsc(RagQueryHit::getRankNo));
        List<RagQueryFeedback> feedbacks = queryFeedbackMapper.selectList(new LambdaQueryWrapper<RagQueryFeedback>()
                .eq(RagQueryFeedback::getQueryLogId, queryLogId)
                .orderByDesc(RagQueryFeedback::getCreatedAt));
        return new RagQueryLogDetailResponse(log, hits, feedbacks);
    }

    @Transactional
    public RagQueryFeedback submitFeedback(Long queryLogId, RagQueryFeedbackRequest request) {
        getRequiredLog(queryLogId);
        RagQueryFeedback feedback = new RagQueryFeedback();
        feedback.setQueryLogId(queryLogId);
        feedback.setRating(feedbackPolicy.normalizeRating(request.rating()));
        feedback.setCreatedBy(feedbackPolicy.cleanText(request.createdBy(), 128));
        feedback.setComment(feedbackPolicy.cleanText(request.comment(), 2000));
        feedback.setCorrectedAnswer(feedbackPolicy.cleanText(request.correctedAnswer(), 20000));
        feedback.setFeedbackStatus(feedbackWorkflowPolicy.defaultStatus(feedback.getRating()));
        feedback.setPriority(feedbackWorkflowPolicy.defaultPriority(feedback.getRating()));
        feedback.setReopenedCount(0);
        queryFeedbackMapper.insert(feedback);
        recordFeedbackEvent(
                feedback.getId(),
                QueryFeedbackWorkflowPolicy.EVENT_STATUS_CHANGED,
                null,
                feedback.getFeedbackStatus(),
                feedback.getCreatedBy(),
                "feedback submitted",
                Map.of("rating", feedback.getRating())
        );
        return feedback;
    }

    public RagFeedbackSummaryResponse feedbackSummary(Long tenantId,
                                                      String queryType,
                                                      String status,
                                                      String conversationId,
                                                      String traceId,
                                                      String rating,
                                                      String createdBy,
                                                      Integer limit) {
        String normalizedQueryType = normalize(queryType);
        String normalizedStatus = normalize(status);
        String normalizedRating = normalize(rating);
        String cleanCreatedBy = cleanFilter(createdBy);
        int safeLimit = safePageSize(limit);
        List<RagFeedbackRatingCount> counts = queryFeedbackMapper.summarizeFeedback(
                tenantId,
                normalizedQueryType,
                normalizedStatus,
                cleanFilter(conversationId),
                cleanFilter(traceId),
                normalizedRating,
                cleanCreatedBy
        );
        long helpful = countOf(counts, "HELPFUL");
        long notHelpful = countOf(counts, "NOT_HELPFUL");
        long correction = countOf(counts, "CORRECTION");
        long total = counts.stream().mapToLong(item -> item.getCount() == null ? 0L : item.getCount()).sum();
        return new RagFeedbackSummaryResponse(
                total,
                helpful,
                notHelpful,
                correction,
                counts,
                queryFeedbackMapper.listRecentFeedback(
                        tenantId,
                        normalizedQueryType,
                        normalizedStatus,
                        cleanFilter(conversationId),
                        cleanFilter(traceId),
                        normalizedRating,
                        cleanCreatedBy,
                        safeLimit
                )
        );
    }

    @Transactional
    public RagQueryLogDeleteResponse deleteLog(Long queryLogId) {
        return deleteLogs(List.of(queryLogId));
    }

    @Transactional
    public RagQueryLogDeleteResponse deleteLogs(Collection<Long> queryLogIds) {
        RagQueryLogOperationResponse response = softDelete(new RagQueryLogOperationRequest(
                deletePolicy.cleanIds(queryLogIds),
                null,
                null,
                null,
                null,
                null,
                null,
                "system",
                "legacy delete endpoint",
                null
        ));
        if (response.matchedCount() == 0) {
            return new RagQueryLogDeleteResponse(0, 0, 0, 0);
        }
        return new RagQueryLogDeleteResponse(response.matchedCount(), response.successCount(), 0, 0);
    }

    @Transactional
    public RagQueryLogOperationResponse softDelete(RagQueryLogOperationRequest request) {
        return operateLogs(QueryDeletePolicy.MODE_SOFT_DELETE, request);
    }

    @Transactional
    public RagQueryLogOperationResponse archive(RagQueryLogOperationRequest request) {
        return operateLogs(QueryDeletePolicy.MODE_ARCHIVE, request);
    }

    @Transactional
    public RagQueryLogOperationResponse restore(RagQueryLogOperationRequest request) {
        return operateLogs(QueryDeletePolicy.MODE_RESTORE, request);
    }

    @Transactional
    public RagQueryLogOperationResponse purge(RagQueryLogOperationRequest request) {
        return operateLogs(QueryDeletePolicy.MODE_PURGE, request);
    }

    public PageResponse<RagQueryLogDeleteAudit> pageDeleteAudits(String deleteNo,
                                                                 String mode,
                                                                 String operator,
                                                                 Integer pageNo,
                                                                 Integer pageSize) {
        PageQuery pageQuery = PageQuery.of(pageNo, pageSize, DEFAULT_PAGE_SIZE, "createdAt", "DESC", MAX_PAGE_SIZE)
                .withDefaultSort("createdAt", "DESC");
        LambdaQueryWrapper<RagQueryLogDeleteAudit> countQuery = deleteAuditQuery(deleteNo, mode, operator);
        long total = deleteAuditMapper.selectCount(countQuery);
        LambdaQueryWrapper<RagQueryLogDeleteAudit> rowsQuery = deleteAuditQuery(deleteNo, mode, operator);
        rowsQuery.orderByDesc(RagQueryLogDeleteAudit::getCreatedAt)
                .last("LIMIT " + pageQuery.offset(total) + ", " + pageQuery.effectivePageSize(total));
        return PageResponse.of(pageQuery, total, deleteAuditMapper.selectList(rowsQuery));
    }

    public RagQueryLogDeleteAudit getDeleteAudit(String deleteNo) {
        if (!StringUtils.hasText(deleteNo)) {
            throw new IllegalArgumentException("deleteNo is required");
        }
        RagQueryLogDeleteAudit audit = deleteAuditMapper.selectOne(new LambdaQueryWrapper<RagQueryLogDeleteAudit>()
                .eq(RagQueryLogDeleteAudit::getDeleteNo, deleteNo.trim())
                .last("LIMIT 1"));
        if (audit == null) {
            throw new IllegalArgumentException("Delete audit not found: " + deleteNo);
        }
        return audit;
    }

    public PageResponse<RagQueryRetentionPolicy> pageRetentionPolicies(Long tenantId,
                                                                       Boolean enabled,
                                                                       Integer pageNo,
                                                                       Integer pageSize) {
        PageQuery pageQuery = PageQuery.of(pageNo, pageSize, DEFAULT_PAGE_SIZE, "updatedAt", "DESC", MAX_PAGE_SIZE)
                .withDefaultSort("updatedAt", "DESC");
        LambdaQueryWrapper<RagQueryRetentionPolicy> query = retentionPolicyQuery(tenantId, enabled);
        long total = retentionPolicyMapper.selectCount(query);
        LambdaQueryWrapper<RagQueryRetentionPolicy> rowsQuery = retentionPolicyQuery(tenantId, enabled);
        rowsQuery.orderByDesc(RagQueryRetentionPolicy::getUpdatedAt)
                .last("LIMIT " + pageQuery.offset(total) + ", " + pageQuery.effectivePageSize(total));
        return PageResponse.of(pageQuery, total, retentionPolicyMapper.selectList(rowsQuery));
    }

    @Transactional
    public RagQueryRetentionPolicy createRetentionPolicy(RagQueryRetentionPolicyRequest request) {
        RagQueryRetentionPolicy policy = new RagQueryRetentionPolicy();
        applyRetentionPolicyRequest(policy, request);
        retentionPolicyMapper.insert(policy);
        return policy;
    }

    @Transactional
    public RagQueryRetentionPolicy updateRetentionPolicy(Long id, RagQueryRetentionPolicyRequest request) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("policy id is required");
        }
        RagQueryRetentionPolicy policy = retentionPolicyMapper.selectById(id);
        if (policy == null) {
            throw new IllegalArgumentException("Retention policy not found: " + id);
        }
        applyRetentionPolicyRequest(policy, request);
        retentionPolicyMapper.updateById(policy);
        return retentionPolicyMapper.selectById(id);
    }

    public String exportLogsCsv(Long tenantId,
                                String queryType,
                                String status,
                                String conversationId,
                                String traceId,
                                String queryText,
                                Integer limit) {
        int safeLimit = Math.max(1, Math.min(limit == null ? MAX_EXPORT_LIMIT : limit, MAX_EXPORT_LIMIT));
        List<RagQueryLog> logs = queryLogMapper.selectList(queryWrapper(
                tenantId,
                queryType,
                status,
                conversationId,
                traceId,
                queryText,
                true,
                0,
                safeLimit,
                PageQuery.of(DEFAULT_PAGE_NO, safeLimit, safeLimit, "createdAt", "DESC", MAX_EXPORT_LIMIT),
                null
        ));
        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append(String.join(",",
                "id",
                "tenantId",
                "traceId",
                "conversationId",
                "queryType",
                "status",
                "retrievalMode",
                "queryText",
                "knowledgeHit",
                "hitCount",
                "promptTokens",
                "completionTokens",
                "totalTokens",
                "latencyMs",
                "createdAt",
                "errorCode",
                "errorMessage"
        )).append('\n');
        for (RagQueryLog log : logs) {
            csv.append(csv(log.getId())).append(',')
                    .append(csv(log.getTenantId())).append(',')
                    .append(csv(log.getTraceId())).append(',')
                    .append(csv(log.getConversationId())).append(',')
                    .append(csv(log.getQueryType())).append(',')
                    .append(csv(log.getStatus())).append(',')
                    .append(csv(log.getRetrievalMode())).append(',')
                    .append(csv(log.getQueryText())).append(',')
                    .append(csv(log.getKnowledgeHit())).append(',')
                    .append(csv(log.getHitCount())).append(',')
                    .append(csv(log.getPromptTokens())).append(',')
                    .append(csv(log.getCompletionTokens())).append(',')
                    .append(csv(log.getTotalTokens())).append(',')
                    .append(csv(log.getLatencyMs())).append(',')
                    .append(csv(log.getCreatedAt())).append(',')
                    .append(csv(log.getErrorCode())).append(',')
                    .append(csv(log.getErrorMessage()))
                    .append('\n');
        }
        return csv.toString();
    }

    private LambdaQueryWrapper<RagQueryLog> queryWrapper(Long tenantId,
                                                        String queryType,
                                                        String status,
                                                        String conversationId,
                                                        String traceId,
                                                        String queryText,
                                                        boolean orderedAndLimited,
                                                        int offset,
                                                        int limit,
                                                        PageQuery pageQuery,
                                                        String visibility) {
        LambdaQueryWrapper<RagQueryLog> wrapper = new LambdaQueryWrapper<RagQueryLog>()
                .eq(tenantId != null, RagQueryLog::getTenantId, tenantId)
                .eq(StringUtils.hasText(queryType), RagQueryLog::getQueryType, normalize(queryType))
                .eq(StringUtils.hasText(status), RagQueryLog::getStatus, normalize(status))
                .eq(StringUtils.hasText(conversationId), RagQueryLog::getConversationId, cleanFilter(conversationId))
                .eq(StringUtils.hasText(traceId), RagQueryLog::getTraceId, cleanFilter(traceId))
                .like(StringUtils.hasText(queryText), RagQueryLog::getQueryText, cleanFilter(queryText));
        applyVisibility(wrapper, visibility);
        if (orderedAndLimited) {
            applyOrder(wrapper, pageQuery);
            wrapper.last("LIMIT " + Math.max(0, offset) + ", " + Math.max(1, limit));
        }
        return wrapper;
    }

    private void applyVisibility(LambdaQueryWrapper<RagQueryLog> wrapper, String visibility) {
        switch (normalizeVisibility(visibility)) {
            case "ALL" -> {
            }
            case "DELETED" -> wrapper.eq(RagQueryLog::getDeleted, true);
            case "ARCHIVED" -> wrapper.eq(RagQueryLog::getArchiveStatus, QueryRetentionPolicy.ARCHIVE_ARCHIVED);
            default -> wrapper.and(condition -> condition.eq(RagQueryLog::getDeleted, false).or().isNull(RagQueryLog::getDeleted))
                    .and(condition -> condition.eq(RagQueryLog::getArchiveStatus, QueryRetentionPolicy.ARCHIVE_ACTIVE)
                            .or()
                            .isNull(RagQueryLog::getArchiveStatus));
        }
    }

    private void applyOrder(LambdaQueryWrapper<RagQueryLog> wrapper, PageQuery pageQuery) {
        PageQuery safeQuery = pageQuery == null
                ? PageQuery.of(DEFAULT_PAGE_NO, DEFAULT_PAGE_SIZE, DEFAULT_PAGE_SIZE, "createdAt", "DESC", MAX_PAGE_SIZE)
                : pageQuery.withDefaultSort("createdAt", "DESC");
        boolean asc = safeQuery.ascending();
        switch (safeQuery.sortBy()) {
            case "id" -> wrapper.orderBy(true, asc, RagQueryLog::getId);
            case "hitCount" -> wrapper.orderBy(true, asc, RagQueryLog::getHitCount);
            case "latencyMs" -> wrapper.orderBy(true, asc, RagQueryLog::getLatencyMs);
            case "totalTokens" -> wrapper.orderBy(true, asc, RagQueryLog::getTotalTokens);
            case "createdAt" -> wrapper.orderBy(true, asc, RagQueryLog::getCreatedAt);
            default -> wrapper.orderByDesc(RagQueryLog::getCreatedAt);
        }
    }

    private int safePageSize(Integer pageSize) {
        if (pageSize == null) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.max(1, Math.min(MAX_PAGE_SIZE, pageSize));
    }

    private int safeTotal(long total) {
        if (total <= 0) {
            return 0;
        }
        return total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeVisibility(String value) {
        if (!StringUtils.hasText(value)) {
            return "ACTIVE";
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "ALL", "DELETED", "ARCHIVED" -> normalized;
            default -> "ACTIVE";
        };
    }

    private String cleanFilter(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private long countOf(List<RagFeedbackRatingCount> counts, String rating) {
        return counts.stream()
                .filter(item -> rating.equals(item.getRating()))
                .map(RagFeedbackRatingCount::getCount)
                .filter(count -> count != null)
                .findFirst()
                .orElse(0L);
    }

    private String csv(Object value) {
        if (value == null) {
            return "";
        }
        String text = value instanceof LocalDateTime dateTime ? dateTime.toString() : String.valueOf(value);
        if (!text.isEmpty() && "=+-@".indexOf(text.charAt(0)) >= 0) {
            text = "'" + text;
        }
        return "\"" + text.replace("\"", "\"\"").replace("\r", " ").replace("\n", " ") + "\"";
    }

    private RagQueryLog getRequiredLog(Long queryLogId) {
        RagQueryLog log = queryLogMapper.selectById(queryLogId);
        if (log == null) {
            throw new IllegalArgumentException("Query log not found: " + queryLogId);
        }
        return log;
    }

    private RagQueryLogOperationResponse operateLogs(String mode, RagQueryLogOperationRequest request) {
        String normalizedMode = deletePolicy.normalizeMode(mode);
        RagQueryLogOperationRequest safeRequest = request == null
                ? new RagQueryLogOperationRequest(null, null, null, null, null, null, null, null, null, null)
                : request;
        List<RagQueryLog> logs = operationLogs(normalizedMode, safeRequest);
        String deleteNo = deletePolicy.newDeleteNo(normalizedMode);
        int success = 0;
        List<Long> successIds = new ArrayList<>();
        List<Map<String, Object>> failures = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (RagQueryLog log : logs) {
            try {
                switch (normalizedMode) {
                    case QueryDeletePolicy.MODE_ARCHIVE -> archiveOne(log, deleteNo, now);
                    case QueryDeletePolicy.MODE_RESTORE -> restoreOne(log);
                    case QueryDeletePolicy.MODE_PURGE -> purgeOne(log, now);
                    default -> softDeleteOne(log, safeRequest, now);
                }
                success++;
                successIds.add(log.getId());
            } catch (RuntimeException ex) {
                failures.add(Map.of(
                        "queryLogId", log.getId(),
                        "reason", ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()
                ));
            }
        }
        RagQueryLogOperationResponse response = new RagQueryLogOperationResponse(
                deleteNo,
                normalizedMode,
                logs.size(),
                success,
                failures.size(),
                successIds
        );
        recordDeleteAudit(deleteNo, normalizedMode, safeRequest, logs, response, failures);
        return response;
    }

    private List<RagQueryLog> operationLogs(String mode, RagQueryLogOperationRequest request) {
        List<Long> ids = deletePolicy.cleanIds(request.ids());
        LambdaQueryWrapper<RagQueryLog> wrapper = new LambdaQueryWrapper<RagQueryLog>();
        if (!ids.isEmpty()) {
            wrapper.in(RagQueryLog::getId, ids);
        } else {
            if (!hasOperationSelector(request)) {
                throw new IllegalArgumentException("query log operation requires ids or at least one filter");
            }
            wrapper.eq(request.tenantId() != null, RagQueryLog::getTenantId, request.tenantId())
                    .eq(StringUtils.hasText(request.queryType()), RagQueryLog::getQueryType, normalize(request.queryType()))
                    .eq(StringUtils.hasText(request.status()), RagQueryLog::getStatus, normalize(request.status()))
                    .eq(StringUtils.hasText(request.conversationId()), RagQueryLog::getConversationId, cleanFilter(request.conversationId()))
                    .eq(StringUtils.hasText(request.traceId()), RagQueryLog::getTraceId, cleanFilter(request.traceId()))
                    .like(StringUtils.hasText(request.queryText()), RagQueryLog::getQueryText, cleanFilter(request.queryText()));
        }
        if (QueryDeletePolicy.MODE_RESTORE.equals(mode)) {
            wrapper.and(condition -> condition.eq(RagQueryLog::getDeleted, true)
                    .or()
                    .eq(RagQueryLog::getArchiveStatus, QueryRetentionPolicy.ARCHIVE_ARCHIVED)
                    .or()
                    .eq(RagQueryLog::getArchiveStatus, QueryRetentionPolicy.ARCHIVE_DELETE_PENDING));
        } else if (!QueryDeletePolicy.MODE_PURGE.equals(mode)) {
            wrapper.and(condition -> condition.eq(RagQueryLog::getDeleted, false).or().isNull(RagQueryLog::getDeleted));
        }
        wrapper.orderByAsc(RagQueryLog::getId).last("LIMIT " + MAX_OPERATION_LIMIT);
        return queryLogMapper.selectList(wrapper);
    }

    private boolean hasOperationSelector(RagQueryLogOperationRequest request) {
        return request.tenantId() != null
                || StringUtils.hasText(request.queryType())
                || StringUtils.hasText(request.status())
                || StringUtils.hasText(request.conversationId())
                || StringUtils.hasText(request.traceId())
                || StringUtils.hasText(request.queryText());
    }

    private void softDeleteOne(RagQueryLog log, RagQueryLogOperationRequest request, LocalDateTime now) {
        boolean hasFeedback = hasFeedback(log.getId());
        boolean hasCorrection = hasCorrection(log.getId());
        List<RagQueryRetentionPolicy> policies = retentionPolicies(log.getTenantId());
        LocalDateTime retentionUntil = request.retentionUntil() == null
                ? retentionPolicy.retentionUntil(log, policies, hasFeedback, hasCorrection, now)
                : request.retentionUntil();
        queryLogMapper.update(null, new UpdateWrapper<RagQueryLog>()
                .eq("id", log.getId())
                .set("is_deleted", true)
                .set("archive_status", QueryRetentionPolicy.ARCHIVE_DELETE_PENDING)
                .set("retention_until", retentionUntil)
                .set("deleted_at", now)
                .set("deleted_by", deletePolicy.cleanOperator(request.operator()))
                .set("delete_reason", deletePolicy.cleanReason(request.reason())));
    }

    private void archiveOne(RagQueryLog log, String deleteNo, LocalDateTime now) {
        RagQueryLogArchive archive = toArchive(log, deleteNo, now);
        archiveMapper.insert(archive);
        queryLogMapper.update(null, new UpdateWrapper<RagQueryLog>()
                .eq("id", log.getId())
                .set("archive_status", QueryRetentionPolicy.ARCHIVE_ARCHIVED));
    }

    private void restoreOne(RagQueryLog log) {
        queryLogMapper.update(null, new UpdateWrapper<RagQueryLog>()
                .eq("id", log.getId())
                .set("is_deleted", false)
                .set("archive_status", QueryRetentionPolicy.ARCHIVE_ACTIVE)
                .set("deleted_at", null)
                .set("deleted_by", null)
                .set("delete_reason", null));
    }

    private void purgeOne(RagQueryLog log, LocalDateTime now) {
        if (!retentionPolicy.canPurge(log, hasFeedback(log.getId()), now)) {
            throw new IllegalArgumentException("Query log has not reached retention deadline or still has feedback: " + log.getId());
        }
        queryHitMapper.delete(new LambdaQueryWrapper<RagQueryHit>().eq(RagQueryHit::getQueryLogId, log.getId()));
        queryFeedbackMapper.delete(new LambdaQueryWrapper<RagQueryFeedback>().eq(RagQueryFeedback::getQueryLogId, log.getId()));
        queryLogMapper.deleteById(log.getId());
    }

    private RagQueryLogArchive toArchive(RagQueryLog log, String deleteNo, LocalDateTime now) {
        RagQueryLogArchive archive = new RagQueryLogArchive();
        archive.setSourceQueryLogId(log.getId());
        archive.setDeleteNo(deleteNo);
        archive.setTenantId(log.getTenantId());
        archive.setTraceId(log.getTraceId());
        archive.setConversationId(log.getConversationId());
        archive.setQueryType(log.getQueryType());
        archive.setQueryText(log.getQueryText());
        archive.setRetrievalMode(log.getRetrievalMode());
        archive.setKnowledgeBaseIdsJson(log.getKnowledgeBaseIdsJson());
        archive.setTopK(log.getTopK());
        archive.setMinScore(log.getMinScore());
        archive.setContentTypesJson(log.getContentTypesJson());
        archive.setPermissionTagsJson(log.getPermissionTagsJson());
        archive.setPromptText(log.getPromptText());
        archive.setAnswerText(log.getAnswerText());
        archive.setKnowledgeHit(log.getKnowledgeHit());
        archive.setHitCount(log.getHitCount());
        archive.setPromptTokens(log.getPromptTokens());
        archive.setCompletionTokens(log.getCompletionTokens());
        archive.setTotalTokens(log.getTotalTokens());
        archive.setLlmProvider(log.getLlmProvider());
        archive.setLlmModel(log.getLlmModel());
        archive.setEmbeddingProvider(log.getEmbeddingProvider());
        archive.setEmbeddingModel(log.getEmbeddingModel());
        archive.setEstimatedInputCost(log.getEstimatedInputCost());
        archive.setEstimatedOutputCost(log.getEstimatedOutputCost());
        archive.setEstimatedEmbeddingCost(log.getEstimatedEmbeddingCost());
        archive.setEstimatedTotalCost(log.getEstimatedTotalCost());
        archive.setCostCurrency(log.getCostCurrency());
        archive.setLatencyMs(log.getLatencyMs());
        archive.setStatus(log.getStatus());
        archive.setArchiveStatus(QueryRetentionPolicy.ARCHIVE_ARCHIVED);
        archive.setRetentionUntil(log.getRetentionUntil());
        archive.setErrorCode(log.getErrorCode());
        archive.setErrorMessage(log.getErrorMessage());
        archive.setQueryCreatedAt(log.getCreatedAt());
        archive.setQueryUpdatedAt(log.getUpdatedAt());
        archive.setArchivedAt(now);
        return archive;
    }

    private boolean hasFeedback(Long queryLogId) {
        return queryFeedbackMapper.selectCount(new LambdaQueryWrapper<RagQueryFeedback>()
                .eq(RagQueryFeedback::getQueryLogId, queryLogId)) > 0;
    }

    private boolean hasCorrection(Long queryLogId) {
        return queryFeedbackMapper.selectCount(new LambdaQueryWrapper<RagQueryFeedback>()
                .eq(RagQueryFeedback::getQueryLogId, queryLogId)
                .eq(RagQueryFeedback::getRating, QueryFeedbackPolicy.RATING_CORRECTION)) > 0;
    }

    private List<RagQueryRetentionPolicy> retentionPolicies(Long tenantId) {
        return retentionPolicyMapper.selectList(new LambdaQueryWrapper<RagQueryRetentionPolicy>()
                .eq(RagQueryRetentionPolicy::getEnabled, true)
                .and(wrapper -> wrapper.eq(RagQueryRetentionPolicy::getTenantId, tenantId == null ? 0L : tenantId)
                        .or()
                        .eq(RagQueryRetentionPolicy::getTenantId, 0L)
                        .or()
                        .isNull(RagQueryRetentionPolicy::getTenantId)));
    }

    private void recordDeleteAudit(String deleteNo,
                                   String mode,
                                   RagQueryLogOperationRequest request,
                                   List<RagQueryLog> logs,
                                   RagQueryLogOperationResponse response,
                                   List<Map<String, Object>> failures) {
        RagQueryLogDeleteAudit audit = new RagQueryLogDeleteAudit();
        audit.setDeleteNo(deleteNo);
        audit.setDeleteMode(mode);
        audit.setOperator(deletePolicy.cleanOperator(request.operator()));
        audit.setReason(deletePolicy.cleanReason(request.reason()));
        audit.setQueryLogIdsJson(toJson(logs.stream().map(RagQueryLog::getId).filter(Objects::nonNull).toList()));
        audit.setMatchedCount(response.matchedCount());
        audit.setSuccessCount(response.successCount());
        audit.setFailedCount(response.failedCount());
        audit.setFilterJson(toJson(filterMap(request)));
        audit.setResultJson(toJson(Map.of(
                "successIds", response.queryLogIds(),
                "failures", failures
        )));
        deleteAuditMapper.insert(audit);
    }

    private Map<String, Object> filterMap(RagQueryLogOperationRequest request) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("ids", deletePolicy.cleanIds(request.ids()));
        values.put("tenantId", request.tenantId());
        values.put("queryType", cleanFilter(request.queryType()));
        values.put("status", cleanFilter(request.status()));
        values.put("conversationId", cleanFilter(request.conversationId()));
        values.put("traceId", cleanFilter(request.traceId()));
        values.put("queryText", cleanFilter(request.queryText()));
        return values;
    }

    private void recordFeedbackEvent(Long feedbackId,
                                     String eventType,
                                     String fromStatus,
                                     String toStatus,
                                     String operator,
                                     String comment,
                                     Map<String, Object> payload) {
        if (feedbackId == null) {
            return;
        }
        RagQueryFeedbackEvent event = new RagQueryFeedbackEvent();
        event.setFeedbackId(feedbackId);
        event.setEventType(eventType);
        event.setFromStatus(fromStatus);
        event.setToStatus(toStatus);
        event.setOperator(feedbackWorkflowPolicy.cleanOperator(operator));
        event.setComment(feedbackWorkflowPolicy.cleanText(comment, 2000));
        event.setPayloadJson(toJson(payload));
        queryFeedbackEventMapper.insert(event);
    }

    private LambdaQueryWrapper<RagQueryLogDeleteAudit> deleteAuditQuery(String deleteNo, String mode, String operator) {
        return new LambdaQueryWrapper<RagQueryLogDeleteAudit>()
                .eq(StringUtils.hasText(deleteNo), RagQueryLogDeleteAudit::getDeleteNo, cleanFilter(deleteNo))
                .eq(StringUtils.hasText(mode), RagQueryLogDeleteAudit::getDeleteMode, deletePolicy.normalizeMode(mode))
                .eq(StringUtils.hasText(operator), RagQueryLogDeleteAudit::getOperator, cleanFilter(operator));
    }

    private LambdaQueryWrapper<RagQueryRetentionPolicy> retentionPolicyQuery(Long tenantId, Boolean enabled) {
        return new LambdaQueryWrapper<RagQueryRetentionPolicy>()
                .eq(tenantId != null, RagQueryRetentionPolicy::getTenantId, tenantId)
                .eq(enabled != null, RagQueryRetentionPolicy::getEnabled, enabled);
    }

    private void applyRetentionPolicyRequest(RagQueryRetentionPolicy policy, RagQueryRetentionPolicyRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("retention policy request is required");
        }
        policy.setTenantId(request.tenantId() == null ? 0L : request.tenantId());
        policy.setPolicyName(requiredText(request.policyName(), 128, "policyName"));
        policy.setQueryType(normalizeAll(request.queryType(), "ALL"));
        policy.setStatusFilter(normalizeAll(request.statusFilter(), "ALL"));
        policy.setRetentionDays(request.retentionDays() == null ? 180 : Math.max(0, request.retentionDays()));
        policy.setArchiveBeforeDelete(request.archiveBeforeDelete() == null || request.archiveBeforeDelete());
        policy.setEnabled(request.enabled() == null || request.enabled());
    }

    private String requiredText(String value, int maxLength, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private String normalizeAll(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : fallback;
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }
}
