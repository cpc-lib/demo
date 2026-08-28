package cc.ivera.ragdemo.model.query;

import java.time.LocalDateTime;

public record RagRetrievalEvaluationTrendPoint(
        LocalDateTime bucket,
        String window,
        Long knowledgeBaseId,
        String versionTag,
        String retrievalMode,
        String queryCategory,
        String language,
        String difficultyLevel,
        Integer topK,
        long totalCases,
        double hitRate,
        double meanReciprocalRank,
        double meanRecall,
        double failureRate,
        double rerankDropRate,
        double keywordOnlyHitRate,
        double vectorOnlyHitRate
) {
}
