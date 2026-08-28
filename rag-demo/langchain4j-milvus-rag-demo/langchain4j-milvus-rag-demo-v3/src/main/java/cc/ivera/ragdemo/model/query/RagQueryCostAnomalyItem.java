package cc.ivera.ragdemo.model.query;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RagQueryCostAnomalyItem(
        String anomalyType,
        String severity,
        String metricName,
        BigDecimal metricValue,
        BigDecimal baselineValue,
        LocalDateTime windowStart,
        LocalDateTime windowEnd,
        String metadata
) {
}
