package cc.ivera.ragdemo.model.query;

import java.time.LocalDateTime;

public record RagRetrievalEvaluationReportItem(
        Long knowledgeBaseId,
        String versionTag,
        String retrievalMode,
        long runCount,
        long totalCases,
        double avgHitRate,
        double avgMeanReciprocalRank,
        double avgMeanRecall,
        Long latestRunId,
        String latestRunNo,
        LocalDateTime latestCreatedAt
) {
}
