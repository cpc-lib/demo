package cc.ivera.ragdemo.model.knowledge;

public record RagIngestionTaskMessage(
        Long tenantId,
        Long taskId,
        Long documentId,
        Long knowledgeBaseId,
        Long documentVersionId,
        String taskNo,
        String traceId
) {
    public RagIngestionTaskMessage(Long taskId, Long documentId, Long knowledgeBaseId, String taskNo) {
        this(null, taskId, documentId, knowledgeBaseId, null, taskNo, null);
    }

    public RagIngestionTaskMessage(Long tenantId, Long taskId, Long documentId, Long knowledgeBaseId, Long documentVersionId, String taskNo) {
        this(tenantId, taskId, documentId, knowledgeBaseId, documentVersionId, taskNo, null);
    }
}
