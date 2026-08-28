package cc.ivera.ragdemo.model.query;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RagModelPricingRequest(
        String provider,
        String model,
        BigDecimal inputCostPer1kTokens,
        BigDecimal outputCostPer1kTokens,
        String currency,
        LocalDateTime effectiveFrom,
        LocalDateTime effectiveTo,
        Boolean enabled
) {
}
