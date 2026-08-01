package cc.ivera.ragdemo.model.query;

import java.util.List;

public record RagRetrievalEvaluationRequest(
        Long tenantId,
        Long knowledgeBaseId,
        String versionTag,
        String retrievalMode,
        List<Long> caseIds,
        List<RagRetrievalEvaluationCase> cases
) {
}
