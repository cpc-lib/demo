package cc.ivera.ragdemo.model.query;

public record RagFeedbackQualitySummary(
        long queryCount,
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
