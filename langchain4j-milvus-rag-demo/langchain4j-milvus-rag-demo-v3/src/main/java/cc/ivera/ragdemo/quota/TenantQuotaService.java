package cc.ivera.ragdemo.quota;


import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.domain.tenant.RagTenantQuota;
import cc.ivera.ragdemo.domain.tenant.RagTenantUsageDaily;
import cc.ivera.ragdemo.mapper.RagTenantQuotaMapper;
import cc.ivera.ragdemo.mapper.RagTenantUsageDailyMapper;
import cc.ivera.ragdemo.service.ragops.TenantQuotaPolicy;
import cc.ivera.ragdemo.tenant.TenantContextHolder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class TenantQuotaService {

    private final RagTenantQuotaMapper quotaMapper;
    private final RagTenantUsageDailyMapper usageMapper;
    private final RagProperties properties;
    private final TenantQuotaPolicy policy = new TenantQuotaPolicy();

    public RagTenantQuota getQuota(Long tenantId) {
        Long effectiveTenantId = tenantId == null ? TenantContextHolder.requireTenantId() : tenantId;
        RagTenantQuota quota = quotaMapper.selectOne(new LambdaQueryWrapper<RagTenantQuota>()
                .eq(RagTenantQuota::getTenantId, effectiveTenantId)
                .last("LIMIT 1"));
        return quota == null ? defaultQuota(effectiveTenantId) : quota;
    }

    public RagTenantQuota updateQuota(Long tenantId, RagTenantQuota request) {
        Long effectiveTenantId = tenantId == null ? TenantContextHolder.requireTenantId() : tenantId;
        RagTenantQuota existing = quotaMapper.selectOne(new LambdaQueryWrapper<RagTenantQuota>()
                .eq(RagTenantQuota::getTenantId, effectiveTenantId)
                .last("LIMIT 1"));
        RagTenantQuota quota = existing == null ? new RagTenantQuota() : existing;
        quota.setTenantId(effectiveTenantId);
        quota.setMaxDocuments(policy.safeLimit(request.getMaxDocuments(), properties.getTenant().getDefaultMaxDocuments()));
        quota.setMaxStorageBytes(policy.safeLimit(request.getMaxStorageBytes(), properties.getTenant().getDefaultMaxStorageBytes()));
        quota.setMaxFileBytes(policy.safeLimit(request.getMaxFileBytes(), properties.getTenant().getDefaultMaxFileBytes()));
        quota.setDailyOcrLimit(policy.safeLimit(request.getDailyOcrLimit(), properties.getTenant().getDefaultDailyOcrLimit()));
        quota.setDailyEmbeddingTokens(policy.safeLimit(request.getDailyEmbeddingTokens(), properties.getTenant().getDefaultDailyEmbeddingTokens()));
        quota.setMaxConcurrentIngestionTasks(policy.safeLimit(request.getMaxConcurrentIngestionTasks(), properties.getTenant().getDefaultMaxConcurrentIngestionTasks()));
        quota.setDailyQueryLimit(policy.safeLimit(request.getDailyQueryLimit(), properties.getTenant().getDefaultDailyQueryLimit()));
        quota.setMonthlyBudgetCents(policy.safeLimit(request.getMonthlyBudgetCents(), properties.getTenant().getDefaultMonthlyBudgetCents()));
        quota.setEnabled(request.getEnabled() == null || request.getEnabled());
        quota.setUpdatedAt(LocalDateTime.now());
        if (existing == null) {
            quota.setCreatedAt(LocalDateTime.now());
            quotaMapper.insert(quota);
        } else {
            quotaMapper.updateById(quota);
        }
        return quota;
    }

    public RagTenantUsageDaily getUsage(Long tenantId, LocalDate date) {
        Long effectiveTenantId = tenantId == null ? TenantContextHolder.requireTenantId() : tenantId;
        LocalDate effectiveDate = date == null ? LocalDate.now() : date;
        RagTenantUsageDaily usage = usageMapper.selectOne(new LambdaQueryWrapper<RagTenantUsageDaily>()
                .eq(RagTenantUsageDaily::getTenantId, effectiveTenantId)
                .eq(RagTenantUsageDaily::getUsageDate, effectiveDate)
                .last("LIMIT 1"));
        return usage == null ? emptyUsage(effectiveTenantId, effectiveDate) : usage;
    }

    public void assertFileUploadAllowed(long fileSizeBytes) {
        Long tenantId = TenantContextHolder.requireTenantId();
        RagTenantQuota quota = getQuota(tenantId);
        RagTenantUsageDaily usage = getUsage(tenantId, LocalDate.now());
        policy.assertWithinLimit("file_size", fileSizeBytes, 0, quota.getMaxFileBytes());
        policy.assertWithinLimit("storage_bytes", fileSizeBytes, value(usage.getStorageBytes()), quota.getMaxStorageBytes());
    }

    public void assertQueryAllowed() {
        Long tenantId = TenantContextHolder.requireTenantId();
        RagTenantQuota quota = getQuota(tenantId);
        RagTenantUsageDaily usage = getUsage(tenantId, LocalDate.now());
        policy.assertWithinLimit("daily_query_count", 1, value(usage.getQueryCount()), quota.getDailyQueryLimit());
    }

    private RagTenantQuota defaultQuota(Long tenantId) {
        RagTenantQuota quota = new RagTenantQuota();
        quota.setTenantId(tenantId);
        quota.setMaxDocuments(properties.getTenant().getDefaultMaxDocuments());
        quota.setMaxStorageBytes(properties.getTenant().getDefaultMaxStorageBytes());
        quota.setMaxFileBytes(properties.getTenant().getDefaultMaxFileBytes());
        quota.setDailyOcrLimit(properties.getTenant().getDefaultDailyOcrLimit());
        quota.setDailyEmbeddingTokens(properties.getTenant().getDefaultDailyEmbeddingTokens());
        quota.setMaxConcurrentIngestionTasks(properties.getTenant().getDefaultMaxConcurrentIngestionTasks());
        quota.setDailyQueryLimit(properties.getTenant().getDefaultDailyQueryLimit());
        quota.setMonthlyBudgetCents(properties.getTenant().getDefaultMonthlyBudgetCents());
        quota.setEnabled(true);
        return quota;
    }

    private RagTenantUsageDaily emptyUsage(Long tenantId, LocalDate date) {
        RagTenantUsageDaily usage = new RagTenantUsageDaily();
        usage.setTenantId(tenantId);
        usage.setUsageDate(date);
        usage.setDocumentCount(0L);
        usage.setStorageBytes(0L);
        usage.setOcrCount(0L);
        usage.setEmbeddingTokens(0L);
        usage.setVectorCount(0L);
        usage.setQueryCount(0L);
        usage.setLlmTokens(0L);
        usage.setEstimatedCostCents(0L);
        return usage;
    }

    private long value(Long value) {
        return value == null ? 0L : value;
    }
}
