package cc.ivera.ragdemo.model.knowledge;

public record ChunkRedisRebuildResponse(
        int mysqlRows,
        int chunkCount,
        int activeCount,
        int deletedRedisKeys
) {
}
