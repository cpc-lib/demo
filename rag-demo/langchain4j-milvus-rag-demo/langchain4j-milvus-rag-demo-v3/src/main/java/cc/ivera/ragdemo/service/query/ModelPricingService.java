package cc.ivera.ragdemo.service.query;


import cc.ivera.ragdemo.domain.rag.RagModelPricing;
import cc.ivera.ragdemo.mapper.RagModelPricingMapper;
import cc.ivera.ragdemo.model.query.PageQuery;
import cc.ivera.ragdemo.model.query.PageResponse;
import cc.ivera.ragdemo.model.query.RagModelPricingRequest;
import cc.ivera.ragdemo.service.ragops.QueryCostPolicy;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class ModelPricingService {

    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 200;

    private final RagModelPricingMapper mapper;
    private final QueryCostPolicy costPolicy;

    public PageResponse<RagModelPricing> page(String provider,
                                              String model,
                                              Boolean enabled,
                                              Integer pageNo,
                                              Integer pageSize) {
        PageQuery pageQuery = PageQuery.of(pageNo, pageSize, DEFAULT_PAGE_SIZE, "id", "DESC", MAX_PAGE_SIZE)
                .withDefaultSort("id", "DESC");
        LambdaQueryWrapper<RagModelPricing> query = query(provider, model, enabled);
        long total = mapper.selectCount(query);
        LambdaQueryWrapper<RagModelPricing> rowsQuery = query(provider, model, enabled);
        rowsQuery.orderByDesc(RagModelPricing::getId)
                .last("LIMIT " + pageQuery.offset(total) + ", " + pageQuery.effectivePageSize(total));
        return PageResponse.of(pageQuery, total, mapper.selectList(rowsQuery));
    }

    @Transactional
    public RagModelPricing create(RagModelPricingRequest request) {
        RagModelPricing pricing = new RagModelPricing();
        apply(pricing, request);
        mapper.insert(pricing);
        return pricing;
    }

    @Transactional
    public RagModelPricing update(Long id, RagModelPricingRequest request) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("pricing id is required");
        }
        RagModelPricing pricing = mapper.selectById(id);
        if (pricing == null) {
            throw new IllegalArgumentException("Model pricing not found: " + id);
        }
        apply(pricing, request);
        mapper.updateById(pricing);
        return mapper.selectById(id);
    }

    private LambdaQueryWrapper<RagModelPricing> query(String provider, String model, Boolean enabled) {
        return new LambdaQueryWrapper<RagModelPricing>()
                .eq(StringUtils.hasText(provider), RagModelPricing::getProvider, costPolicy.normalizeProvider(provider))
                .eq(StringUtils.hasText(model), RagModelPricing::getModel, costPolicy.normalizeModel(model))
                .eq(enabled != null, RagModelPricing::getEnabled, enabled);
    }

    private void apply(RagModelPricing pricing, RagModelPricingRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("model pricing request is required");
        }
        pricing.setProvider(required(request.provider(), "provider").toLowerCase());
        pricing.setModel(required(request.model(), "model"));
        pricing.setInputCostPer1kTokens(nonNegative(request.inputCostPer1kTokens()));
        pricing.setOutputCostPer1kTokens(nonNegative(request.outputCostPer1kTokens()));
        pricing.setCurrency(StringUtils.hasText(request.currency()) ? request.currency().trim().toUpperCase() : "USD");
        pricing.setEffectiveFrom(request.effectiveFrom());
        pricing.setEffectiveTo(request.effectiveTo());
        pricing.setEnabled(request.enabled() == null || request.enabled());
    }

    private String required(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private BigDecimal nonNegative(BigDecimal value) {
        if (value == null || value.signum() < 0) {
            return BigDecimal.ZERO;
        }
        return value;
    }
}
