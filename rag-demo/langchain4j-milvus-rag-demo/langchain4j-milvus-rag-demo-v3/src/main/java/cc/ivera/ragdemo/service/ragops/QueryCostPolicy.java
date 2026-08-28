package cc.ivera.ragdemo.service.ragops;

import cc.ivera.ragdemo.domain.rag.RagModelPricing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Component
public class QueryCostPolicy {

    @Autowired
    public QueryCostPolicy() {
    }

    private static final BigDecimal ONE_THOUSAND = BigDecimal.valueOf(1000L);

    public RagModelPricing effectivePricing(String provider,
                                            String model,
                                            LocalDateTime at,
                                            List<RagModelPricing> candidates) {
        if (!StringUtils.hasText(model) || candidates == null) {
            return null;
        }
        LocalDateTime timestamp = at == null ? LocalDateTime.now() : at;
        String normalizedProvider = normalize(provider);
        String normalizedModel = normalize(model);
        return candidates.stream()
                .filter(row -> !Boolean.FALSE.equals(row.getEnabled()))
                .filter(row -> normalizedProvider == null || normalize(row.getProvider()).equals(normalizedProvider))
                .filter(row -> normalize(row.getModel()).equals(normalizedModel))
                .filter(row -> row.getEffectiveFrom() == null || !row.getEffectiveFrom().isAfter(timestamp))
                .filter(row -> row.getEffectiveTo() == null || row.getEffectiveTo().isAfter(timestamp))
                .max(Comparator.comparing(row -> row.getEffectiveFrom() == null ? LocalDateTime.MIN : row.getEffectiveFrom()))
                .orElse(null);
    }

    public CostEstimate estimate(Integer promptTokens,
                                 Integer completionTokens,
                                 Integer embeddingTokens,
                                 RagModelPricing llmPricing,
                                 RagModelPricing embeddingPricing) {
        BigDecimal inputCost = tokenCost(promptTokens, llmPricing == null ? null : llmPricing.getInputCostPer1kTokens());
        BigDecimal outputCost = tokenCost(completionTokens, llmPricing == null ? null : llmPricing.getOutputCostPer1kTokens());
        BigDecimal embeddingCost = tokenCost(embeddingTokens, embeddingPricing == null ? null : embeddingPricing.getInputCostPer1kTokens());
        String currency = llmPricing != null && StringUtils.hasText(llmPricing.getCurrency())
                ? llmPricing.getCurrency()
                : embeddingPricing != null ? embeddingPricing.getCurrency() : "unknown";
        String warning = llmPricing == null ? "llm pricing not configured" : null;
        if (embeddingTokens != null && embeddingTokens > 0 && embeddingPricing == null) {
            warning = warning == null ? "embedding pricing not configured" : warning + "; embedding pricing not configured";
        }
        return new CostEstimate(
                inputCost,
                outputCost,
                embeddingCost,
                inputCost.add(outputCost).add(embeddingCost),
                StringUtils.hasText(currency) ? currency : "unknown",
                warning
        );
    }

    public BigDecimal tokenCost(Integer tokens, BigDecimal costPer1kTokens) {
        if (tokens == null || tokens <= 0 || costPer1kTokens == null) {
            return BigDecimal.ZERO.setScale(8, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(tokens)
                .multiply(costPer1kTokens)
                .divide(ONE_THOUSAND, 8, RoundingMode.HALF_UP);
    }

    public String normalizeProvider(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : null;
    }

    public String normalizeModel(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : "";
    }

    public record CostEstimate(
            BigDecimal inputCost,
            BigDecimal outputCost,
            BigDecimal embeddingCost,
            BigDecimal totalCost,
            String currency,
            String warning
    ) {
    }
}
