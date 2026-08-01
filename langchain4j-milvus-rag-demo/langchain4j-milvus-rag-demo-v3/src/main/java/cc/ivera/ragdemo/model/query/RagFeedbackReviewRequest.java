package cc.ivera.ragdemo.model.query;

public record RagFeedbackReviewRequest(
        String reviewResult,
        String reviewComment,
        String operator
) {
}
