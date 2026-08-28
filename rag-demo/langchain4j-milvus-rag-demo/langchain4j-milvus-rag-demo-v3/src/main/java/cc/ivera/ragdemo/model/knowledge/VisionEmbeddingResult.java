package cc.ivera.ragdemo.model.knowledge;

import java.util.List;

public record VisionEmbeddingResult(
        String status,
        List<Float> vector,
        String vectorId,
        String provider,
        String model,
        Integer dimension,
        Integer inputTokens,
        Long latencyMs,
        String errorMessage
) {

    public static VisionEmbeddingResult skipped(String provider, String model) {
        return new VisionEmbeddingResult("SKIPPED", List.of(), null, provider, model, null, null, 0L, null);
    }

    public static VisionEmbeddingResult failed(String provider,
                                               String model,
                                               Integer dimension,
                                               Long latencyMs,
                                               String errorMessage) {
        return new VisionEmbeddingResult("FAILED", List.of(), null, provider, model, dimension, null, latencyMs, errorMessage);
    }

    public boolean success() {
        return "SUCCESS".equals(status) && vector != null && !vector.isEmpty();
    }
}
