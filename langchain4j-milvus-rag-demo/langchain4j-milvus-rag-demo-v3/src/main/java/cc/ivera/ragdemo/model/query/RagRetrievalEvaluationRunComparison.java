package cc.ivera.ragdemo.model.query;

public record RagRetrievalEvaluationRunComparison(
        Long leftRunId,
        Long rightRunId,
        int leftTotalCases,
        int rightTotalCases,
        double leftHitRate,
        double rightHitRate,
        double hitRateDelta,
        double leftMeanReciprocalRank,
        double rightMeanReciprocalRank,
        double meanReciprocalRankDelta,
        double leftMeanRecall,
        double rightMeanRecall,
        double meanRecallDelta
) {
}
