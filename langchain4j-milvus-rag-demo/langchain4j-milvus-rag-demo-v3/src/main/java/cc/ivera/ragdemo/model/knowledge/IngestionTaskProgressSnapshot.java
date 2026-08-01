package cc.ivera.ragdemo.model.knowledge;

import cc.ivera.ragdemo.domain.rag.RagIngestionTask;
import cc.ivera.ragdemo.domain.rag.RagIngestionTaskStage;

import java.util.List;

public record IngestionTaskProgressSnapshot(
        Long taskId,
        Integer taskStatus,
        Integer progress,
        String currentStage,
        Integer stageProgress,
        Boolean cancelRequested,
        Long lastEventId,
        RagIngestionTask task,
        List<RagIngestionTaskStage> stages,
        IngestionShardSummary shardSummary
) {
}
