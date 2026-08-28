package cc.ivera.ragdemo.service.query;


import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.domain.rag.RagRerankCallLog;
import cc.ivera.ragdemo.mapper.RagRerankCallLogMapper;
import cc.ivera.ragdemo.model.query.PageQuery;
import cc.ivera.ragdemo.model.query.PageResponse;
import cc.ivera.ragdemo.model.query.RagRerankObservationSummary;
import cc.ivera.ragdemo.service.ragops.RagHashing;
import cc.ivera.ragdemo.service.ragops.TimeWindowAggregationPolicy;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class RerankObservationService {

    private static final Logger log = LoggerFactory.getLogger(RerankObservationService.class);
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 500;

    private final RagRerankCallLogMapper mapper;
    private final RagProperties ragProperties;
    private final RerankErrorClassifier errorClassifier;
    private final TimeWindowAggregationPolicy timeWindowPolicy;

    public void record(Long tenantId,
                       String query,
                       String provider,
                       String model,
                       int candidateCount,
                       int topK,
                       int inputTokens,
                       int outputTokens,
                       int totalTokens,
                       long latencyMs,
                       boolean success,
                       boolean fallback,
                       String errorCode,
                       String errorMessage) {
        if (!ragProperties.getRetrieval().isRerankObservationEnabled()) {
            return;
        }
        try {
            LocalDateTime now = LocalDateTime.now();
            RerankErrorClassifier.Classification classification = errorClassifier.classify(success, fallback, errorCode, errorMessage);
            RagRerankCallLog row = new RagRerankCallLog();
            row.setTenantId(tenantId == null ? 0L : tenantId);
            row.setProvider(provider);
            row.setModel(model);
            row.setQueryHash(RagHashing.sha256Hex(query == null ? "" : query));
            row.setApiKeyHash(apiKeyHash());
            row.setTenantExternalId(tenantId == null ? null : String.valueOf(tenantId));
            row.setRequestWindow(timeWindowPolicy.bucket(now, TimeWindowAggregationPolicy.Window.MINUTE));
            row.setCandidateCount(candidateCount);
            row.setTopK(topK);
            row.setInputTokens(inputTokens);
            row.setOutputTokens(outputTokens);
            row.setTotalTokens(totalTokens);
            row.setLatencyMs(Math.max(0L, latencyMs));
            row.setSuccess(success);
            row.setFallback(fallback);
            row.setEstimatedCost(estimateCost(totalTokens));
            row.setErrorCode(errorCode);
            row.setHttpStatus(classification.httpStatus());
            row.setErrorCodeNormalized(classification.errorCodeNormalized());
            row.setDegradedReason(classification.degradedReason());
            row.setRetryCount(0);
            row.setCacheHit(false);
            row.setErrorMessage(truncate(errorMessage, 1900));
            row.setCreatedAt(now);
            mapper.insert(row);
        } catch (Exception e) {
            log.warn("Failed to record rerank observation: {}", e.getMessage());
        }
    }

    public PageResponse<RagRerankCallLog> pageLogs(Long tenantId,
                                                   String provider,
                                                   String model,
                                                   Boolean success,
                                                   Boolean fallback,
                                                   Integer pageNo,
                                                   Integer pageSize,
                                                   String sortBy,
                                                   String sortDirection) {
        PageQuery pageQuery = PageQuery.of(pageNo, pageSize, DEFAULT_PAGE_SIZE, sortBy, sortDirection, MAX_PAGE_SIZE)
                .withDefaultSort("createdAt", "DESC");
        LambdaQueryWrapper<RagRerankCallLog> countQuery = query(tenantId, provider, model, success, fallback);
        long total = mapper.selectCount(countQuery);
        LambdaQueryWrapper<RagRerankCallLog> rowsQuery = query(tenantId, provider, model, success, fallback);
        applyOrder(rowsQuery, pageQuery);
        rowsQuery.last("LIMIT " + pageQuery.offset(total) + ", " + pageQuery.effectivePageSize(total));
        return PageResponse.of(pageQuery, total, mapper.selectList(rowsQuery));
    }

    public RagRerankObservationSummary summary(Long tenantId, String provider, String model) {
        List<RagRerankCallLog> rows = mapper.selectList(query(tenantId, provider, model, null, null));
        long total = rows.size();
        long successCount = rows.stream().filter(row -> Boolean.TRUE.equals(row.getSuccess())).count();
        long failedCount = rows.stream().filter(row -> !Boolean.TRUE.equals(row.getSuccess())).count();
        long degradedCount = rows.stream().filter(row -> Boolean.TRUE.equals(row.getFallback())).count();
        double failureRate = total == 0 ? 0.0 : failedCount / (double) total;
        double averageLatency = rows.stream()
                .map(RagRerankCallLog::getLatencyMs)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);
        long totalTokens = rows.stream()
                .map(RagRerankCallLog::getTotalTokens)
                .filter(Objects::nonNull)
                .mapToLong(Integer::longValue)
                .sum();
        BigDecimal cost = rows.stream()
                .map(RagRerankCallLog::getEstimatedCost)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new RagRerankObservationSummary(
                total,
                successCount,
                failedCount,
                degradedCount,
                failureRate,
                averageLatency,
                totalTokens,
                cost
        );
    }

    private LambdaQueryWrapper<RagRerankCallLog> query(Long tenantId,
                                                       String provider,
                                                       String model,
                                                       Boolean success,
                                                       Boolean fallback) {
        return new LambdaQueryWrapper<RagRerankCallLog>()
                .eq(tenantId != null, RagRerankCallLog::getTenantId, tenantId)
                .eq(StringUtils.hasText(provider), RagRerankCallLog::getProvider, normalize(provider))
                .eq(StringUtils.hasText(model), RagRerankCallLog::getModel, normalize(model))
                .eq(success != null, RagRerankCallLog::getSuccess, success)
                .eq(fallback != null, RagRerankCallLog::getFallback, fallback);
    }

    private void applyOrder(LambdaQueryWrapper<RagRerankCallLog> wrapper, PageQuery pageQuery) {
        boolean asc = pageQuery.ascending();
        switch (pageQuery.sortBy()) {
            case "id" -> wrapper.orderBy(true, asc, RagRerankCallLog::getId);
            case "latencyMs" -> wrapper.orderBy(true, asc, RagRerankCallLog::getLatencyMs);
            case "totalTokens" -> wrapper.orderBy(true, asc, RagRerankCallLog::getTotalTokens);
            case "estimatedCost" -> wrapper.orderBy(true, asc, RagRerankCallLog::getEstimatedCost);
            case "createdAt" -> wrapper.orderBy(true, asc, RagRerankCallLog::getCreatedAt);
            default -> wrapper.orderByDesc(RagRerankCallLog::getCreatedAt);
        }
    }

    private BigDecimal estimateCost(int totalTokens) {
        double unit = ragProperties.getRetrieval().getRerankCostPer1kTokens();
        if (unit <= 0 || totalTokens <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(totalTokens)
                .multiply(BigDecimal.valueOf(unit))
                .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP);
    }

    private String apiKeyHash() {
        String apiKey = ragProperties.getRetrieval().getRerankApiKey();
        if (!StringUtils.hasText(apiKey)) {
            return null;
        }
        return RagHashing.sha256Hex(apiKey).substring(0, 16);
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : value;
    }

    private String truncate(String value, int max) {
        if (!StringUtils.hasText(value) || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}
