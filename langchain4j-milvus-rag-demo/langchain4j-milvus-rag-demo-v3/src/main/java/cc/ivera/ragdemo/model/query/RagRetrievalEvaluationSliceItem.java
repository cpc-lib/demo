package cc.ivera.ragdemo.model.query;

public record RagRetrievalEvaluationSliceItem(
        String dimension,
        String value,
        long totalCases,
        double hitRate,
        double meanReciprocalRank,
        double meanRecall,
        double failureRate
) {
}
