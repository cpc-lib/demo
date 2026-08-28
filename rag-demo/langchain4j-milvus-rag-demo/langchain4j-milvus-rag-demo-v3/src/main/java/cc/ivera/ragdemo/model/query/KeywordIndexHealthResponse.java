package cc.ivera.ragdemo.model.query;

public record KeywordIndexHealthResponse(
        boolean enabled,
        String provider,
        String engineCompatible,
        String baseUrl,
        String indexName,
        String indexAlias,
        String indexVersion,
        String analyzerProfile,
        boolean templateManaged,
        boolean autoCreateIndex,
        String activeIndexTarget,
        String status
) {
}
