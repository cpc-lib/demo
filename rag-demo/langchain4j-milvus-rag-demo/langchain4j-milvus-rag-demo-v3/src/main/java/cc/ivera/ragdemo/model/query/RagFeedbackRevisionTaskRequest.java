package cc.ivera.ragdemo.model.query;

public record RagFeedbackRevisionTaskRequest(
        Long knowledgeBaseId,
        Long documentId,
        String chunkUid,
        String revisionType,
        String beforeSnapshotJson,
        String expectedFix,
        String verificationQuery,
        String createdBy,
        String assignee
) {
}
