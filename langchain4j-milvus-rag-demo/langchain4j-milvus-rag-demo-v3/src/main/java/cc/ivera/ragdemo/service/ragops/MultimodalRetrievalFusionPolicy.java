package cc.ivera.ragdemo.service.ragops;

import cc.ivera.ragdemo.config.RagProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class MultimodalRetrievalFusionPolicy {

    @Autowired
    public MultimodalRetrievalFusionPolicy() {
    }

    public Map<String, Double> weights(RagProperties.Retrieval retrieval, boolean imageOnly) {
        if (imageOnly) {
            return normalize(Map.of(
                    "image_vector", safe(retrieval == null ? 0.80D : retrieval.getImageOnlyVectorWeight(), 0.80D),
                    "keyword", safe(retrieval == null ? 0.20D : retrieval.getImageOnlyKeywordWeight(), 0.20D)
            ));
        }
        return normalize(Map.of(
                "text_vector", safe(retrieval == null ? 0.40D : retrieval.getMultimodalTextVectorWeight(), 0.40D),
                "image_vector", safe(retrieval == null ? 0.40D : retrieval.getMultimodalImageVectorWeight(), 0.40D),
                "keyword", safe(retrieval == null ? 0.20D : retrieval.getMultimodalKeywordWeight(), 0.20D)
        ));
    }

    public double weight(Map<String, Double> weights, String source) {
        if (weights == null || source == null) {
            return 1.0D;
        }
        return weights.getOrDefault(source, 1.0D);
    }

    private Map<String, Double> normalize(Map<String, Double> raw) {
        Map<String, Double> normalized = new LinkedHashMap<>();
        Map<String, Double> effective = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : raw.entrySet()) {
            double value = entry.getValue() == null || entry.getValue() <= 0 ? 1.0D : entry.getValue();
            effective.put(entry.getKey(), value);
        }
        double total = effective.values().stream().mapToDouble(Double::doubleValue).sum();
        for (Map.Entry<String, Double> entry : effective.entrySet()) {
            double value = entry.getValue();
            normalized.put(entry.getKey(), value / total);
        }
        return normalized;
    }

    private double safe(double value, double fallback) {
        if (value <= 0 || Double.isNaN(value) || Double.isInfinite(value)) {
            return fallback;
        }
        return value;
    }
}
