package cc.ivera.ragdemo.model.query;

import java.util.List;

public record RagRetrievalEvaluationResponse(
        Long runId,
        String runNo,
        Long knowledgeBaseId,
        String versionTag,
        String retrievalMode,
        int totalCases,
        double hitRate,
        double meanReciprocalRank,
        double meanRecall,
        List<RagRetrievalEvaluationCaseResult> results
) {
}
