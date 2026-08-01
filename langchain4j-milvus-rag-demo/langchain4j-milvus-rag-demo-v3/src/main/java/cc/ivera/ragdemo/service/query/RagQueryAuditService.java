package cc.ivera.ragdemo.service.query;


import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.domain.rag.RagModelPricing;
import cc.ivera.ragdemo.domain.rag.RagQueryHit;
import cc.ivera.ragdemo.domain.rag.RagQueryLog;
import cc.ivera.ragdemo.mapper.RagModelPricingMapper;
import cc.ivera.ragdemo.mapper.RagQueryHitMapper;
import cc.ivera.ragdemo.mapper.RagQueryLogMapper;
import cc.ivera.ragdemo.model.query.RagQueryRequest;
import cc.ivera.ragdemo.model.query.RagSearchItem;
import cc.ivera.ragdemo.model.query.RagSearchRequest;
import cc.ivera.ragdemo.model.query.RagUsage;
import cc.ivera.ragdemo.service.ragops.QueryAuditPolicy;
import cc.ivera.ragdemo.service.ragops.QueryCostPolicy;
import cc.ivera.ragdemo.tenant.TenantContextHolder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@Slf4j
public class RagQueryAuditService {

    private static final int PROMPT_LIMIT = 20000;
    private static final int ANSWER_LIMIT = 20000;
    private static final int HIT_CONTENT_LIMIT = 2000;
    private static final int ERROR_LIMIT = 2000;

    private final RagQueryLogMapper queryLogMapper;
    private final RagQueryHitMapper queryHitMapper;
    private final RagModelPricingMapper modelPricingMapper;
    private final ObjectMapper objectMapper;
    private final QueryAuditPolicy policy;
    private final QueryCostPolicy costPolicy;
    private final RagProperties ragProperties;

    public Long recordSearch(RagSearchRequest request,
                             List<RagSearchItem> items,
                             long latencyMs,
                             String traceId) {
        try {
            RagQueryLog log = baseLog(
                    QueryAuditPolicy.TYPE_SEARCH,
                    effectiveTenantId(request.tenantId()),
                    traceId,
                    null,
                    request.query(),
                    request.retrievalMode(),
                    request.knowledgeBaseIds(),
                    request.topK(),
                    request.minScore(),
                    request.contentTypes(),
                    request.permissionTags(),
                    multimodalTrace(request)
            );
            log.setKnowledgeHit(policy.knowledgeHit(items));
            log.setHitCount(policy.hitCount(items));
            setModelMetadata(log, false);
            log.setLatencyMs(latencyMs);
            log.setStatus(QueryAuditPolicy.STATUS_SUCCESS);
            queryLogMapper.insert(log);
            insertHits(log.getId(), items);
            return log.getId();
        } catch (Exception ex) {
            log.warn("Failed to persist RAG search audit log, traceId={}", traceId, ex);
            return null;
        }
    }

    public Long recordQuery(RagQueryRequest request,
                            String conversationId,
                            String prompt,
                            String answer,
                            boolean knowledgeHit,
                            List<RagSearchItem> sources,
                            RagUsage usage,
                            long latencyMs,
                            String traceId) {
        try {
            RagQueryLog log = baseLog(
                    QueryAuditPolicy.TYPE_QUERY,
                    effectiveTenantId(request.tenantId()),
                    traceId,
                    conversationId,
                    request.question(),
                    request.retrievalMode(),
                    request.knowledgeBaseIds(),
                    request.topK(),
                    request.minScore(),
                    request.contentTypes(),
                    request.permissionTags(),
                    multimodalTrace(request)
            );
            log.setPromptText(policy.truncate(prompt, PROMPT_LIMIT));
            log.setAnswerText(policy.truncate(answer, ANSWER_LIMIT));
            log.setKnowledgeHit(knowledgeHit);
            log.setHitCount(policy.hitCount(sources));
            setUsage(log, usage);
            setModelMetadata(log, true);
            setEstimatedCost(log, usage);
            log.setLatencyMs(latencyMs);
            log.setStatus(QueryAuditPolicy.STATUS_SUCCESS);
            queryLogMapper.insert(log);
            insertHits(log.getId(), sources);
            return log.getId();
        } catch (Exception ex) {
            log.warn("Failed to persist RAG query audit log, traceId={}", traceId, ex);
            return null;
        }
    }

    public void recordSearchFailure(RagSearchRequest request,
                                    long latencyMs,
                                    String traceId,
                                    Throwable throwable) {
        try {
            RagQueryLog log = baseLog(
                    QueryAuditPolicy.TYPE_SEARCH,
                    effectiveTenantId(request.tenantId()),
                    traceId,
                    null,
                    request.query(),
                    request.retrievalMode(),
                    request.knowledgeBaseIds(),
                    request.topK(),
                    request.minScore(),
                    request.contentTypes(),
                    request.permissionTags(),
                    multimodalTrace(request)
            );
            setModelMetadata(log, false);
            applyFailure(log, latencyMs, throwable);
            queryLogMapper.insert(log);
        } catch (Exception ex) {
            log.warn("Failed to persist failed RAG search audit log, traceId={}", traceId, ex);
        }
    }

    public void recordQueryFailure(RagQueryRequest request,
                                   String conversationId,
                                   long latencyMs,
                                   String traceId,
                                   Throwable throwable) {
        try {
            RagQueryLog log = baseLog(
                    QueryAuditPolicy.TYPE_QUERY,
                    effectiveTenantId(request.tenantId()),
                    traceId,
                    conversationId,
                    request.question(),
                    request.retrievalMode(),
                    request.knowledgeBaseIds(),
                    request.topK(),
                    request.minScore(),
                    request.contentTypes(),
                    request.permissionTags(),
                    multimodalTrace(request)
            );
            setModelMetadata(log, true);
            applyFailure(log, latencyMs, throwable);
            queryLogMapper.insert(log);
        } catch (Exception ex) {
            log.warn("Failed to persist failed RAG query audit log, traceId={}", traceId, ex);
        }
    }

    private RagQueryLog baseLog(String queryType,
                                Long tenantId,
                                String traceId,
                                String conversationId,
                                String queryText,
                                String retrievalMode,
                                List<Long> knowledgeBaseIds,
                                Integer topK,
                                Double minScore,
                                List<String> contentTypes,
                                List<String> permissionTags,
                                String multimodalTraceJson) {
        RagQueryLog log = new RagQueryLog();
        log.setTenantId(tenantId == null ? 0L : tenantId);
        log.setTraceId(traceId);
        log.setConversationId(conversationId);
        log.setQueryType(queryType);
        log.setQueryText(queryText == null ? "" : queryText);
        log.setRetrievalMode(retrievalMode);
        log.setKnowledgeBaseIdsJson(toJson(knowledgeBaseIds));
        log.setTopK(topK);
        log.setMinScore(minScore);
        log.setContentTypesJson(toJson(contentTypes));
        log.setPermissionTagsJson(toJson(permissionTags));
        log.setMultimodalTraceJson(multimodalTraceJson);
        return log;
    }

    private Long effectiveTenantId(Long requestTenantId) {
        return TenantContextHolder.currentTenantId().orElse(requestTenantId);
    }

    private void insertHits(Long queryLogId, List<RagSearchItem> items) {
        if (queryLogId == null || items == null || items.isEmpty()) {
            return;
        }
        for (RagSearchItem item : items) {
            RagQueryHit hit = new RagQueryHit();
            hit.setQueryLogId(queryLogId);
            hit.setRankNo(item.rank());
            hit.setScore(item.score());
            hit.setKnowledgeBaseId(item.knowledgeBaseId());
            hit.setDocumentId(item.documentId());
            hit.setDocumentName(item.documentName());
            hit.setChunkId(item.chunkId());
            hit.setChunkVersion(item.version());
            hit.setContentType(item.contentType());
            hit.setModality(item.modality());
            hit.setRetrievalSource(item.retrievalSource());
            hit.setImageAssetId(item.imageAssetId());
            hit.setFusionScore(item.fusionScore());
            hit.setPageNo(item.pageNo());
            hit.setSectionTitle(item.sectionTitle());
            hit.setImageUrl(item.imageUrl());
            hit.setContentSnippet(policy.truncate(item.content(), HIT_CONTENT_LIMIT));
            hit.setMetadataJson(toMetadataJson(item.metadata()));
            queryHitMapper.insert(hit);
        }
    }

    private String multimodalTrace(RagSearchRequest request) {
        return toJson(Map.of(
                "imageUrlProvided", hasText(request.imageUrl()),
                "imageAssetId", request.imageAssetId() == null ? "" : request.imageAssetId(),
                "imageBase64Provided", hasText(request.imageBase64()),
                "modalities", request.modalities() == null ? List.of() : request.modalities(),
                "textVectorWeight", request.textVectorWeight() == null ? "" : request.textVectorWeight(),
                "imageVectorWeight", request.imageVectorWeight() == null ? "" : request.imageVectorWeight(),
                "keywordWeight", request.keywordWeight() == null ? "" : request.keywordWeight(),
                "includeReviewPending", Boolean.TRUE.equals(request.includeReviewPending())
        ));
    }

    private String multimodalTrace(RagQueryRequest request) {
        return toJson(Map.of(
                "imageUrlProvided", hasText(request.imageUrl()),
                "imageAssetId", request.imageAssetId() == null ? "" : request.imageAssetId(),
                "imageBase64Provided", hasText(request.imageBase64()),
                "modalities", request.modalities() == null ? List.of() : request.modalities(),
                "textVectorWeight", request.textVectorWeight() == null ? "" : request.textVectorWeight(),
                "imageVectorWeight", request.imageVectorWeight() == null ? "" : request.imageVectorWeight(),
                "keywordWeight", request.keywordWeight() == null ? "" : request.keywordWeight(),
                "includeReviewPending", Boolean.TRUE.equals(request.includeReviewPending())
        ));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void setUsage(RagQueryLog log, RagUsage usage) {
        if (usage == null) {
            return;
        }
        log.setPromptTokens(usage.promptTokens());
        log.setCompletionTokens(usage.completionTokens());
        log.setTotalTokens(usage.totalTokens());
    }

    private void setModelMetadata(RagQueryLog log, boolean includeLlm) {
        log.setEmbeddingProvider(costPolicy.normalizeProvider(ragProperties.getEmbedding().getProvider()));
        log.setEmbeddingModel(costPolicy.normalizeModel(ragProperties.getEmbedding().getModel()));
        if (includeLlm) {
            log.setLlmProvider(costPolicy.normalizeProvider(ragProperties.getLlm().getProvider()));
            log.setLlmModel(costPolicy.normalizeModel(ragProperties.getLlm().getModel()));
        }
    }

    private void setEstimatedCost(RagQueryLog log, RagUsage usage) {
        if (usage == null) {
            return;
        }
        List<RagModelPricing> prices = modelPricingMapper.selectList(new LambdaQueryWrapper<RagModelPricing>()
                .eq(RagModelPricing::getEnabled, true));
        RagModelPricing llmPricing = costPolicy.effectivePricing(
                log.getLlmProvider(),
                log.getLlmModel(),
                log.getCreatedAt(),
                prices
        );
        QueryCostPolicy.CostEstimate estimate = costPolicy.estimate(
                usage.promptTokens(),
                usage.completionTokens(),
                null,
                llmPricing,
                null
        );
        log.setEstimatedInputCost(estimate.inputCost());
        log.setEstimatedOutputCost(estimate.outputCost());
        log.setEstimatedEmbeddingCost(estimate.embeddingCost());
        log.setEstimatedTotalCost(estimate.totalCost());
        log.setCostCurrency(estimate.currency());
    }

    private void applyFailure(RagQueryLog log, long latencyMs, Throwable throwable) {
        log.setKnowledgeHit(false);
        log.setHitCount(0);
        log.setLatencyMs(latencyMs);
        log.setStatus(QueryAuditPolicy.STATUS_FAILED);
        log.setErrorCode(policy.errorCode(throwable));
        log.setErrorMessage(policy.errorMessage(throwable, ERROR_LIMIT));
    }

    private String toMetadataJson(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "{}";
        }
        return toJson(metadata);
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
