package cc.ivera.ragdemo.model.query;

public record RagUsage(
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens
) {
}
