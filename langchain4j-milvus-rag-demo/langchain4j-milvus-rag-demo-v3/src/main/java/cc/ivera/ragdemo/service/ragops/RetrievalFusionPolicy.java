package cc.ivera.ragdemo.service.ragops;

import cc.ivera.ragdemo.config.RagProperties;
import org.springframework.stereotype.Component;

@Component
public class RetrievalFusionPolicy {

    private static final double DEFAULT_VECTOR_WEIGHT = 0.65;
    private static final double DEFAULT_KEYWORD_WEIGHT = 0.35;
    private static final double MIN_RRF_K = 1.0;
    private static final double MAX_RRF_K = 1000.0;

    public double vectorWeight(RagProperties.Retrieval retrieval) {
        double[] normalized = normalizedWeights(retrieval);
        return normalized[0];
    }

    public double keywordWeight(RagProperties.Retrieval retrieval) {
        double[] normalized = normalizedWeights(retrieval);
        return normalized[1];
    }

    public double rrfK(RagProperties.Retrieval retrieval) {
        double value = retrieval == null ? 60.0 : retrieval.getRrfK();
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 60.0;
        }
        return Math.max(MIN_RRF_K, Math.min(MAX_RRF_K, value));
    }

    private double[] normalizedWeights(RagProperties.Retrieval retrieval) {
        double vector = retrieval == null ? DEFAULT_VECTOR_WEIGHT : retrieval.getHybridVectorWeight();
        double keyword = retrieval == null ? DEFAULT_KEYWORD_WEIGHT : retrieval.getHybridKeywordWeight();
        if (!positiveFinite(vector) || !positiveFinite(keyword)) {
            return new double[]{DEFAULT_VECTOR_WEIGHT, DEFAULT_KEYWORD_WEIGHT};
        }
        double total = vector + keyword;
        return new double[]{vector / total, keyword / total};
    }

    private boolean positiveFinite(double value) {
        return value > 0 && !Double.isNaN(value) && !Double.isInfinite(value);
    }
}
