package cc.ivera.ragdemo.model.knowledge;

public record IngestionShardSummary(
        long total,
        long pending,
        long running,
        long success,
        long failedRetryable,
        long failedFinal,
        long cancelled
) {
}
