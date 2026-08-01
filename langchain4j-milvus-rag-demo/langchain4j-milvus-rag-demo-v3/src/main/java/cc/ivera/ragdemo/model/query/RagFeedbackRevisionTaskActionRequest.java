package cc.ivera.ragdemo.model.query;

public record RagFeedbackRevisionTaskActionRequest(
        String operator,
        String comment,
        String afterSnapshotJson,
        String verificationResultJson,
        Boolean verified
) {
}
