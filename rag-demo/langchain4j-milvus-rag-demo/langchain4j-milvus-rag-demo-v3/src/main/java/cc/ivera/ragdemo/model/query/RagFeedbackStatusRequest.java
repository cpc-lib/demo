package cc.ivera.ragdemo.model.query;

public record RagFeedbackStatusRequest(
        String status,
        String operator,
        String comment,
        Boolean linkedRevision
) {
}
