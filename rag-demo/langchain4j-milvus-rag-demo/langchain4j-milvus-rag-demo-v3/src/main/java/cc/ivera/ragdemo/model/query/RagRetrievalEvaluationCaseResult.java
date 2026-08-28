package cc.ivera.ragdemo.model.query;

import java.util.List;

public record RagRetrievalEvaluationCaseResult(
        String caseId,
        String query,
        int topK,
        List<String> expectedChunkIds,
        List<String> retrievedChunkIds,
        boolean hit,
        double reciprocalRank,
        double recall,
        String failureType,
        String failureReason,
        String retrievalTraceJson,
        String clusterKey
) {
    public RagRetrievalEvaluationCaseResult(String caseId,
                                            String query,
                                            int topK,
                                            List<String> expectedChunkIds,
                                            List<String> retrievedChunkIds,
                                            boolean hit,
                                            double reciprocalRank,
                                            double recall) {
        this(caseId, query, topK, expectedChunkIds, retrievedChunkIds, hit, reciprocalRank, recall, null, null, null, null);
    }
}
