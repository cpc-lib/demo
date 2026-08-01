package cc.ivera.ragdemo.model.query;

import java.util.List;

public record RagFeedbackSummaryResponse(
        long totalFeedbacks,
        long helpfulCount,
        long notHelpfulCount,
        long correctionCount,
        List<RagFeedbackRatingCount> ratingCounts,
        List<RagFeedbackSummaryItem> recentFeedbacks
) {
}
