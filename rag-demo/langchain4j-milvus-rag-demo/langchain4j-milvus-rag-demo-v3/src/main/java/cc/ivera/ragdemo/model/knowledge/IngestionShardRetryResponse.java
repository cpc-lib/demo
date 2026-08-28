package cc.ivera.ragdemo.model.knowledge;

public record IngestionShardRetryResponse(
        Long taskId,
        int requested,
        int resetCount,
        boolean published
) {
}
