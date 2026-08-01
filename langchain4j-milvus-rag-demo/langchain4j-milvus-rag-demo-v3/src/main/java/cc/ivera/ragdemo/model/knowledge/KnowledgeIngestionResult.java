package cc.ivera.ragdemo.model.knowledge;

import java.util.List;

public record KnowledgeIngestionResult(
        int chunks,
        List<KnowledgeChunkRecord> chunkRecords
) {
}
