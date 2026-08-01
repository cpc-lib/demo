package cc.ivera.ragdemo.model.query;

public record RagRerankLatencyPercentiles(
        long requestCount,
        double p50LatencyMs,
        double p90LatencyMs,
        double p99LatencyMs
) {
}
