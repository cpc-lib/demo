package cc.ivera.ragdemo.model.knowledge;

public record RagIngestionSubmitResponse(
        Long knowledgeBaseId,
        Long documentId,
        Long documentVersionId,
        Integer documentVersionNo,
        Long taskId,
        String taskNo,
        String documentUid,
        String objectKey,
        String fileHash,
        String taskStatus
) {
}
