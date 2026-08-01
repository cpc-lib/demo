package cc.ivera.ragdemo.model.query;

import java.util.List;

public record RagRetrievalEvaluationCaseUpsertRequest(
        Long id,
        Long tenantId,
        Long knowledgeBaseId,
        String versionTag,
        String caseId,
        String query,
        String retrievalMode,
        String queryCategory,
        String difficultyLevel,
        String language,
        String expectedAnswerType,
        Integer topK,
        Double minScore,
        List<String> contentTypes,
        List<String> permissionTags,
        List<String> expectedChunkIds,
        Boolean enabled,
        String metadataJson
) {
}
