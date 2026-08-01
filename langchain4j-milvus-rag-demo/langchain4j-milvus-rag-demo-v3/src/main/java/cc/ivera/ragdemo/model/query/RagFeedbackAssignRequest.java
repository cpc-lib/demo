package cc.ivera.ragdemo.model.query;

public record RagFeedbackAssignRequest(
        String assignee,
        String operator,
        String comment
) {
}
