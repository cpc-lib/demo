package cc.ivera.ragdemo.service.query;

import cc.ivera.ragdemo.model.query.*;
import cc.ivera.ragdemo.quota.TenantQuotaService;
import cc.ivera.ragdemo.service.MetricsService;
import cc.ivera.ragdemo.service.rag.PromptBuilder;
import cc.ivera.ragdemo.service.ragops.QueryAuditPolicy;
import cc.ivera.ragdemo.tenant.TenantScopedQueryService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class RagQueryService {

    private static final String NO_KNOWLEDGE_ANSWER = "我不知道";

    private final RagRetrievalService retrievalService;
    private final PromptBuilder promptBuilder;
    private final ChatLanguageModel chatLanguageModel;
    private final RagQueryAuditService auditService;
    private final QueryAuditPolicy auditPolicy;
    private final MetricsService metricsService;
    private TenantScopedQueryService tenantScopedQueryService;
    private TenantQuotaService tenantQuotaService;

    @Autowired(required = false)
    public void setTenantScopedQueryService(TenantScopedQueryService tenantScopedQueryService) {
        this.tenantScopedQueryService = tenantScopedQueryService;
    }

    @Autowired(required = false)
    public void setTenantQuotaService(TenantQuotaService tenantQuotaService) {
        this.tenantQuotaService = tenantQuotaService;
    }

    public RagSearchResponse search(RagSearchRequest request) {
        return search(request, null);
    }

    public RagSearchResponse search(RagSearchRequest request, String traceId) {
        long startedNanos = System.nanoTime();
        try {
            if (tenantQuotaService != null) {
                tenantQuotaService.assertQueryAllowed();
            }
            RagRetrievalCriteria criteria = criteria(request);
            List<RagSearchItem> items = retrievalService.retrieve(criteria);
            items = filterAuthorized(items, criteria);
            long latencyMs = auditPolicy.latencyMillis(startedNanos, System.nanoTime());
            Long queryLogId = auditService.recordSearch(request, items, latencyMs, traceId);
            metricsService.recordQuery(latencyMs, true, "search");
            return new RagSearchResponse(queryLogId, items);
        } catch (RuntimeException ex) {
            long latencyMs = auditPolicy.latencyMillis(startedNanos, System.nanoTime());
            auditService.recordSearchFailure(request, latencyMs, traceId, ex);
            metricsService.recordQuery(latencyMs, false, "search");
            throw ex;
        }
    }

    public RagQueryResponse query(RagQueryRequest request) {
        return query(request, null);
    }

    public RagQueryResponse query(RagQueryRequest request, String traceId) {
        long startedNanos = System.nanoTime();
        String conversationId = normalizeConversationId(request.conversationId());
        try {
            if (tenantQuotaService != null) {
                tenantQuotaService.assertQueryAllowed();
            }
            RagRetrievalCriteria criteria = criteria(request);
            List<RagSearchItem> sources = retrievalService.retrieve(criteria);
            sources = filterAuthorized(sources, criteria);
            boolean knowledgeHit = !sources.isEmpty();
            String prompt = knowledgeHit ? promptBuilder.build(questionForPrompt(request), contextChunks(sources)) : null;
            ModelAnswer modelAnswer = knowledgeHit ? generateAnswer(prompt) : noKnowledgeAnswer();
            long latencyMs = auditPolicy.latencyMillis(startedNanos, System.nanoTime());
            Long queryLogId = auditService.recordQuery(
                    request,
                    conversationId,
                    prompt,
                    modelAnswer.answer(),
                    knowledgeHit,
                    sources,
                    modelAnswer.usage(),
                    latencyMs,
                    traceId
            );
            metricsService.recordQuery(latencyMs, true, "chat");

            return new RagQueryResponse(
                    queryLogId,
                    conversationId,
                    modelAnswer.answer(),
                    knowledgeHit,
                    includeSources(request) ? sources : List.of(),
                    modelAnswer.usage()
            );
        } catch (RuntimeException ex) {
            long latencyMs = auditPolicy.latencyMillis(startedNanos, System.nanoTime());
            auditService.recordQueryFailure(request, conversationId, latencyMs, traceId, ex);
            metricsService.recordQuery(latencyMs, false, "chat");
            throw ex;
        }
    }

    private RagRetrievalCriteria criteria(RagSearchRequest request) {
        return tenantScopedQueryService == null ? RagRetrievalCriteria.from(request) : tenantScopedQueryService.criteria(request);
    }

    private RagRetrievalCriteria criteria(RagQueryRequest request) {
        return tenantScopedQueryService == null ? RagRetrievalCriteria.from(request) : tenantScopedQueryService.criteria(request);
    }

    private List<RagSearchItem> filterAuthorized(List<RagSearchItem> items, RagRetrievalCriteria criteria) {
        return tenantScopedQueryService == null ? items : tenantScopedQueryService.filterAuthorized(items, criteria);
    }

    private List<String> contextChunks(List<RagSearchItem> sources) {
        return sources.stream()
                .map(source -> "[source rank=%s score=%s kb=%s document=%s chunk=%s type=%s modality=%s retrieval=%s page=%s]\n%s".formatted(
                        source.rank(),
                        source.score(),
                        source.knowledgeBaseId(),
                        source.documentId(),
                        source.chunkId(),
                        source.contentType(),
                        source.modality(),
                        source.retrievalSource(),
                        source.pageNo(),
                        source.content()
                ))
                .toList();
    }

    private String questionForPrompt(RagQueryRequest request) {
        if (request.question() != null && !request.question().isBlank()) {
            return request.question();
        }
        return "Summarize the relevant image knowledge and explain why it matches the image query.";
    }

    private boolean includeSources(RagQueryRequest request) {
        return request.includeSources() == null || request.includeSources();
    }

    private String normalizeConversationId(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return "conv-" + UUID.randomUUID();
        }
        return conversationId.trim();
    }

    private ModelAnswer generateAnswer(String prompt) {
        Response<AiMessage> response = chatLanguageModel.generate(List.of(UserMessage.from(prompt)));
        AiMessage message = response.content();
        String answer = message == null ? "" : message.text();
        return new ModelAnswer(answer, usageFrom(response.tokenUsage()));
    }

    private ModelAnswer noKnowledgeAnswer() {
        return new ModelAnswer(NO_KNOWLEDGE_ANSWER, new RagUsage(0, 0, 0));
    }

    private RagUsage usageFrom(TokenUsage tokenUsage) {
        if (tokenUsage == null) {
            return new RagUsage(null, null, null);
        }
        return new RagUsage(
                tokenUsage.inputTokenCount(),
                tokenUsage.outputTokenCount(),
                tokenUsage.totalTokenCount()
        );
    }

    private record ModelAnswer(String answer, RagUsage usage) {
    }
}
