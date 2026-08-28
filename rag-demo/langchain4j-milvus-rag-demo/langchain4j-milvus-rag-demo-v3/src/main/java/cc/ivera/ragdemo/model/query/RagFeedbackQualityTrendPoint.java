package cc.ivera.ragdemo.model.query;

import java.time.LocalDateTime;

public record RagFeedbackQualityTrendPoint(
        LocalDateTime bucket,
        String window,
        Long tenantId,
        Long knowledgeBaseId,
        String retrievalMode,
        String queryType,
        String feedbackRating,
        String feedbackStatus,
        String assignee,
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
        double verifiedFixRate,
        String source
) {
}
