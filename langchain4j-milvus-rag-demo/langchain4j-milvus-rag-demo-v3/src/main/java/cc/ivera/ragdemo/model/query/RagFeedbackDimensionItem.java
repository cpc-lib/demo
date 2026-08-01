package cc.ivera.ragdemo.model.query;

public record RagFeedbackDimensionItem(
        String dimension,
        String value,
        long feedbackCount,
        double helpfulRate,
        double notHelpfulRate,
        double correctionRate,
        double avgTimeToResolveHours,
        long linkedRevisionCount,
        double verifiedFixRate
) {
}
