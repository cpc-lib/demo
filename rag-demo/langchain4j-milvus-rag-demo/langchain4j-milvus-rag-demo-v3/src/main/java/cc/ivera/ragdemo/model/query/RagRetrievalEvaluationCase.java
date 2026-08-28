package cc.ivera.ragdemo.model.query;

import java.util.List;

public record RagRetrievalEvaluationCase(
        String caseId,
        String query,
        Long tenantId,
        List<Long> knowledgeBaseIds,
        String retrievalMode,
        Integer topK,
        Double minScore,
        List<String> contentTypes,
        List<String> permissionTags,
        List<String> expectedChunkIds,
        Long caseDbId,
        Long knowledgeBaseId,
        String versionTag,
        String queryCategory,
        String difficultyLevel,
        String language,
        String expectedAnswerType
) {
    public RagRetrievalEvaluationCase(String caseId,
                                      String query,
                                      Long tenantId,
                                      List<Long> knowledgeBaseIds,
                                      String retrievalMode,
                                      Integer topK,
                                      Double minScore,
                                      List<String> contentTypes,
                                      List<String> permissionTags,
                                      List<String> expectedChunkIds,
                                      Long caseDbId,
                                      Long knowledgeBaseId,
                                      String versionTag) {
        this(caseId, query, tenantId, knowledgeBaseIds, retrievalMode, topK, minScore, contentTypes, permissionTags,
                expectedChunkIds, caseDbId, knowledgeBaseId, versionTag, null, null, null, null);
    }
}
