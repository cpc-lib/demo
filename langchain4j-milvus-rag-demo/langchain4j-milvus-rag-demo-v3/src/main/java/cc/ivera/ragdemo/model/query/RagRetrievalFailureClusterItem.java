package cc.ivera.ragdemo.model.query;

import java.util.List;

public record RagRetrievalFailureClusterItem(
        String clusterKey,
        String clusterLabel,
        String failureType,
        String queryCategory,
        String retrievalMode,
        long caseCount,
        List<String> sampleCaseIds,
        String suggestion
) {
}
