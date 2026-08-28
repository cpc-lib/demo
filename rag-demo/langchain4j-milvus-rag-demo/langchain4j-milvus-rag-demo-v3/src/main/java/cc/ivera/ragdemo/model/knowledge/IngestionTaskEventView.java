package cc.ivera.ragdemo.model.knowledge;

import java.time.LocalDateTime;

public record IngestionTaskEventView(
        Long id,
        Long taskId,
        String eventType,
        String stageCode,
        String shardKey,
        Integer progress,
        Integer stageProgress,
        String message,
        String payloadJson,
        LocalDateTime createdAt
) {
}
