package cc.ivera.ragdemo.admin;


import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.domain.tenant.TenantDataDeletionTask;
import cc.ivera.ragdemo.service.ragops.RedisPatternKeyDeletionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class RedisTenantDeletionWorker implements TenantDeletionWorker {

    private final RagProperties properties;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final RedisPatternKeyDeletionService redisPatternKeyDeletionService;

    @Override
    public String stageCode() {
        return TenantDeletionStageCode.REDIS.name();
    }

    @Override
    public TenantDeletionStageResult dryRun(TenantDataDeletionTask task) {
        int keys = countKeys(task);
        return TenantDeletionStageResult.success(stageCode(), keys, "{\"keys\":" + keys + "}");
    }

    @Override
    public TenantDeletionStageResult execute(TenantDataDeletionTask task) {
        if (!properties.getTenantDeletion().isRedisDeleteEnabled()) {
            return TenantDeletionStageResult.skipped(stageCode(), "redis-delete-disabled");
        }
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return TenantDeletionStageResult.skipped(stageCode(), "redis-unavailable");
        }
        int deleted = redisPatternKeyDeletionService.deleteByPattern(tenantKeyPattern(task));
        return TenantDeletionStageResult.success(stageCode(), deleted, "{\"deletedKeys\":" + deleted + "}");
    }

    @Override
    public TenantDeletionStageResult verify(TenantDataDeletionTask task) {
        int keys = countKeys(task);
        return keys == 0 || !properties.getTenantDeletion().isRedisDeleteEnabled()
                ? TenantDeletionStageResult.success(stageCode(), keys, "{\"remainingKeys\":" + keys + "}")
                : TenantDeletionStageResult.failed(stageCode(), "REDIS_KEYS_REMAIN", "Remaining tenant Redis keys: " + keys);
    }

    private int countKeys(TenantDataDeletionTask task) {
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return 0;
        }
        return redisPatternKeyDeletionService.countByPattern(tenantKeyPattern(task));
    }

    private String tenantKeyPattern(TenantDataDeletionTask task) {
        return "rag:" + task.getTenantId() + ":*";
    }
}
