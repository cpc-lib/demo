package cc.ivera.ragdemo.model.knowledge;

import java.util.List;

public record IngestionShardRetryRequest(
        List<Long> shardIds,
        String stageCode
) {
}
