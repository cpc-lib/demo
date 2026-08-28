package cc.ivera.ragdemo.model.query;

public record KeywordReindexJobRequest(
        Long tenantId,
        String sourceIndex,
        String targetIndex,
        String aliasName,
        String templateVersion
) {
}
