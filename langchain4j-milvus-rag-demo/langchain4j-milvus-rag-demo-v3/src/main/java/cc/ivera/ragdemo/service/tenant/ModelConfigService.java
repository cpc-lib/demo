package cc.ivera.ragdemo.service.tenant;

import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.domain.tenant.RagTenantModelConfig;
import cc.ivera.ragdemo.mapper.RagTenantModelConfigMapper;
import cc.ivera.ragdemo.model.query.ModelConfigUpsertRequest;
import cc.ivera.ragdemo.tenant.TenantContextHolder;
import cc.ivera.ragdemo.util.ApiKeyEncryptor;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides AI model configuration with MySQL source + Redis cache-aside.
 * <p>
 * Read path: Redis cache -> MySQL tenant-specific -> MySQL global (tenant_id=0) -> yml fallback.
 * Write path: MySQL insert/update + Redis cache eviction.
 * <p>
 * Model types: "LLM" for chat models, "EMBEDDING" for embedding models, "IMAGE" for text-to-image models.
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class ModelConfigService {

    public static final String MODEL_TYPE_LLM = "LLM";
    public static final String MODEL_TYPE_EMBEDDING = "EMBEDDING";
    public static final String MODEL_TYPE_IMAGE = "IMAGE";

    private static final String CACHE_KEY_PREFIX = "rag:%d:model-config:%s";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private final RagTenantModelConfigMapper configMapper;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final ObjectProvider<RagProperties> propertiesProvider;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final ApiKeyEncryptor apiKeyEncryptor;

    /**
     * Get the active LLM config for the current tenant.
     * Falls back to global (tenant_id=0), then to yml configuration.
     */
    public RagTenantModelConfig getActiveLlmConfig() {
        Long tenantId = TenantContextHolder.currentTenantId().orElse(0L);
        return getActiveLlmConfig(tenantId);
    }

    public RagTenantModelConfig getActiveLlmConfig(Long tenantId) {
        return getActiveConfig(tenantId, MODEL_TYPE_LLM);
    }

    /**
     * Get the active embedding config for the current tenant.
     */
    public RagTenantModelConfig getActiveEmbeddingConfig() {
        Long tenantId = TenantContextHolder.currentTenantId().orElse(0L);
        return getActiveEmbeddingConfig(tenantId);
    }

    public RagTenantModelConfig getActiveEmbeddingConfig(Long tenantId) {
        return getActiveConfig(tenantId, MODEL_TYPE_EMBEDDING);
    }

    /**
     * Get the active text-to-image config for the current tenant.
     */
    public RagTenantModelConfig getActiveImageConfig() {
        Long tenantId = TenantContextHolder.currentTenantId().orElse(0L);
        return getActiveImageConfig(tenantId);
    }

    public RagTenantModelConfig getActiveImageConfig(Long tenantId) {
        return getActiveConfig(tenantId, MODEL_TYPE_IMAGE);
    }

    /**
     * Get active config with cache-aside pattern.
     * Fallback chain: Redis -> MySQL tenant -> MySQL global -> yml default.
     */
    public RagTenantModelConfig getActiveConfig(Long tenantId, String modelType) {
        requireSupportedModelType(modelType);
        String cacheKey = CACHE_KEY_PREFIX.formatted(tenantId, modelType);
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();

        // Try Redis cache
        if (redisTemplate != null) {
            try {
                String cached = redisTemplate.opsForValue().get(cacheKey);
                if (cached != null) {
                    return objectMapper.readValue(cached, RagTenantModelConfig.class);
                }
            } catch (Exception e) {
                log.warn("Redis cache read failed for model config key={}, falling back to MySQL", cacheKey, e);
            }
        }

        // Cache miss - load from MySQL
        RagTenantModelConfig config = loadFromMysql(tenantId, modelType);

        // Write to cache
        if (redisTemplate != null && config != null) {
            try {
                redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(config), CACHE_TTL);
            } catch (Exception e) {
                log.warn("Redis cache write failed for model config key={}", cacheKey, e);
            }
        }

        return config;
    }

    /**
     * List all model configs for a tenant (admin view).
     */
    public List<RagTenantModelConfig> listConfigs(Long tenantId) {
        return TenantContextHolder.callWithBypass(() -> {
            List<RagTenantModelConfig> ownedConfigs = configMapper.selectList(
                    new LambdaQueryWrapper<RagTenantModelConfig>()
                            .eq(RagTenantModelConfig::getTenantId, tenantId)
                            .orderByAsc(RagTenantModelConfig::getModelType));
            List<RagTenantModelConfig> result = new ArrayList<>(ownedConfigs);
            appendEffectiveFallbackIfMissing(result, tenantId, MODEL_TYPE_LLM);
            appendEffectiveFallbackIfMissing(result, tenantId, MODEL_TYPE_EMBEDDING);
            appendEffectiveFallbackIfMissing(result, tenantId, MODEL_TYPE_IMAGE);
            return result;
        });
    }

    /**
     * Get one model config by id for the current tenant.
     */
    public RagTenantModelConfig getConfig(Long tenantId, Long configId) {
        return TenantContextHolder.callWithBypass(() -> requireOwnedConfig(tenantId, configId));
    }

    /**
     * Create or update a model config.
     * If an enabled config with the same model_type exists, it will be disabled first.
     * If a config with the same (tenant_id, model_type, provider, model_name) exists, it will be updated.
     */
    public RagTenantModelConfig upsertConfig(Long tenantId, String modelType, String provider,
                                             String modelName, String baseUrl, String apiKeySecretRef,
                                             BigDecimal temperature, Integer dimension,
                                             String imageSize, String imageQuality, Integer pollIntervalMillis,
                                             Integer rateLimitQps, Long monthlyBudgetCents,
                                             Integer timeoutSeconds, Integer maxRetries, Integer maxTokens,
                                             BigDecimal frequencyPenalty, BigDecimal presencePenalty, BigDecimal topP,
                                             Boolean enabled) {
        return TenantContextHolder.callWithBypass(() -> {
            requireSupportedModelType(modelType);
            String normalizedProvider = provider != null ? provider : "openai-compatible";
            boolean requestedEnabled = enabled == null || enabled;

            // Check for existing config with same (tenant_id, model_type, provider, model_name)
            RagTenantModelConfig existingSameConfig = configMapper.selectOne(new LambdaQueryWrapper<RagTenantModelConfig>()
                    .eq(RagTenantModelConfig::getTenantId, tenantId)
                    .eq(RagTenantModelConfig::getModelType, modelType)
                    .eq(RagTenantModelConfig::getProvider, normalizedProvider)
                    .eq(RagTenantModelConfig::getModelName, modelName)
                    .eq(RagTenantModelConfig::getIsDeleted, 0));

            if (requestedEnabled) {
                disableOtherEnabledConfigs(tenantId, modelType, existingSameConfig == null ? null : existingSameConfig.getId());
            }

            RagTenantModelConfig config;
            if (existingSameConfig != null) {
                // Update existing config
                config = existingSameConfig;
                config.setBaseUrl(baseUrl);
                config.setApiKeySecretRef(encryptApiKey(apiKeySecretRef));
                config.setTemperature(temperature);
                config.setDimension(dimension);
                config.setImageSize(trimToNull(imageSize));
                config.setImageQuality(trimToNull(imageQuality));
                config.setPollIntervalMillis(pollIntervalMillis);
                config.setRateLimitQps(rateLimitQps);
                config.setMonthlyBudgetCents(monthlyBudgetCents);
                // 从请求配置读取新增参数，不再使用硬编码值
                config.setTimeoutSeconds(timeoutSeconds);
                config.setMaxRetries(maxRetries);
                config.setMaxTokens(maxTokens);
                config.setFrequencyPenalty(frequencyPenalty);
                config.setPresencePenalty(presencePenalty);
                config.setTopP(topP);
                config.setEnabled(requestedEnabled);
                config.setUpdatedAt(LocalDateTime.now());
                configMapper.updateById(config);
            } else {
                // Insert new config
                config = new RagTenantModelConfig();
                config.setTenantId(tenantId);
                config.setModelType(modelType);
                config.setProvider(normalizedProvider);
                config.setModelName(modelName);
                config.setBaseUrl(baseUrl);
                config.setApiKeySecretRef(encryptApiKey(apiKeySecretRef));
                config.setTemperature(temperature);
                config.setDimension(dimension);
                config.setImageSize(trimToNull(imageSize));
                config.setImageQuality(trimToNull(imageQuality));
                config.setPollIntervalMillis(pollIntervalMillis);
                config.setRateLimitQps(rateLimitQps);
                config.setMonthlyBudgetCents(monthlyBudgetCents);
                // 从请求配置读取新增参数，不再使用硬编码值
                config.setTimeoutSeconds(timeoutSeconds);
                config.setMaxRetries(maxRetries);
                config.setMaxTokens(maxTokens);
                config.setFrequencyPenalty(frequencyPenalty);
                config.setPresencePenalty(presencePenalty);
                config.setTopP(topP);
                config.setEnabled(requestedEnabled);
                configMapper.insert(config);
            }

            // Evict cache
            evictCache(tenantId, modelType);

            // Publish config change event
            eventPublisher.publishEvent(new ModelConfigChangedEvent(this, tenantId, modelType));

            log.info("Model config upserted: tenant={}, type={}, model={}, timeout={}s, maxRetries={}, maxTokens={}", 
                    tenantId, modelType, modelName, timeoutSeconds, maxRetries, maxTokens);
            return config;
        });
    }

    /**
     * Update an existing model config by id.
     * <p>
     * A blank apiKeySecretRef means "keep the existing key" so edit forms do not need to
     * receive or resubmit encrypted secrets.
     */
    public RagTenantModelConfig updateConfig(Long tenantId, Long configId, ModelConfigUpsertRequest request) {
        return TenantContextHolder.callWithBypass(() -> {
            RagTenantModelConfig config = requireOwnedConfig(tenantId, configId);
            String previousType = config.getModelType();
            String newType = request.getModelType();
            boolean wasEnabled = Boolean.TRUE.equals(config.getEnabled());

            if (wasEnabled) {
                throw new IllegalStateException("Model config must be disabled before editing: " + configId);
            }
            requireSupportedModelType(newType);

            config.setModelType(newType);
            config.setProvider(request.getProvider() != null ? request.getProvider() : "openai-compatible");
            config.setModelName(request.getModelName());
            config.setBaseUrl(request.getBaseUrl());
            if (request.getApiKeySecretRef() != null && !request.getApiKeySecretRef().isBlank()) {
                config.setApiKeySecretRef(encryptApiKey(request.getApiKeySecretRef()));
            }
            config.setTemperature(request.getTemperature());
            config.setDimension(request.getDimension());
            config.setImageSize(trimToNull(request.getImageSize()));
            config.setImageQuality(trimToNull(request.getImageQuality()));
            config.setPollIntervalMillis(request.getPollIntervalMillis());
            config.setRateLimitQps(request.getRateLimitQps());
            config.setMonthlyBudgetCents(request.getMonthlyBudgetCents());
            config.setTimeoutSeconds(request.getTimeoutSeconds());
            config.setMaxRetries(request.getMaxRetries());
            config.setMaxTokens(request.getMaxTokens());
            config.setFrequencyPenalty(request.getFrequencyPenalty());
            config.setPresencePenalty(request.getPresencePenalty());
            config.setTopP(request.getTopP());
            if (Boolean.TRUE.equals(request.getEnabled())) {
                disableOtherEnabledConfigs(tenantId, newType, configId);
                config.setEnabled(true);
            } else if (request.getEnabled() != null) {
                config.setEnabled(false);
            }
            config.setUpdatedAt(LocalDateTime.now());
            configMapper.updateById(config);

            evictCache(tenantId, previousType);
            if (newType != null && !newType.equals(previousType)) {
                evictCache(tenantId, newType);
            }
            eventPublisher.publishEvent(new ModelConfigChangedEvent(this, tenantId, newType));
            log.info("Model config updated: id={}, tenant={}, type={}, model={}", configId, tenantId, newType, request.getModelName());
            return config;
        });
    }

    /**
     * Reveal the stored API key or secret reference for an owned model config.
     */
    public String revealApiKey(Long tenantId, Long configId) {
        return TenantContextHolder.callWithBypass(() -> decryptApiKey(requireOwnedConfig(tenantId, configId).getApiKeySecretRef()));
    }

    /**
     * Reveal the API key used by the active config of the requested model type.
     * This can be a tenant-owned config or the current fallback config.
     */
    public String revealActiveApiKey(Long tenantId, String modelType) {
        return TenantContextHolder.callWithBypass(() -> {
            requireSupportedModelType(modelType);
            RagTenantModelConfig active = loadFromMysql(tenantId, modelType);
            return active == null ? null : decryptApiKey(active.getApiKeySecretRef());
        });
    }

    /**
     * Replace only the API key for an owned model config. Enabled configs stay enabled.
     */
    public RagTenantModelConfig updateApiKey(Long tenantId, Long configId, String apiKeySecretRef) {
        return TenantContextHolder.callWithBypass(() -> {
            RagTenantModelConfig config = requireOwnedConfig(tenantId, configId);
            config.setApiKeySecretRef(encryptApiKey(apiKeySecretRef));
            config.setUpdatedAt(LocalDateTime.now());
            configMapper.updateById(config);

            evictCache(config.getTenantId(), config.getModelType());
            eventPublisher.publishEvent(new ModelConfigChangedEvent(this, config.getTenantId(), config.getModelType()));
            log.info("Model config API key updated: id={}, tenant={}, type={}", configId, config.getTenantId(), config.getModelType());
            return config;
        });
    }

    /**
     * Replace the API key for the active model. If the active config is only a fallback,
     * create a tenant-owned enabled config copied from that fallback and store the new key there.
     */
    public RagTenantModelConfig updateActiveApiKey(Long tenantId, String modelType, String apiKeySecretRef) {
        return TenantContextHolder.callWithBypass(() -> {
            requireSupportedModelType(modelType);
            RagTenantModelConfig enabledTenantConfig = findEnabled(tenantId, modelType);
            if (enabledTenantConfig != null) {
                return updateApiKey(tenantId, enabledTenantConfig.getId(), apiKeySecretRef);
            }

            RagTenantModelConfig source = null;
            if (tenantId != 0) {
                source = findEnabled(0L, modelType);
            }
            if (source == null) {
                source = fallbackFromYml(modelType);
            }
            if (source == null) {
                throw new IllegalStateException("Active model config not found: " + modelType);
            }

            RagTenantModelConfig config = copyForTenant(source, tenantId);
            config.setApiKeySecretRef(encryptApiKey(apiKeySecretRef));
            config.setEnabled(true);
            config.setCreatedAt(LocalDateTime.now());
            config.setUpdatedAt(LocalDateTime.now());
            configMapper.insert(config);

            evictCache(tenantId, modelType);
            eventPublisher.publishEvent(new ModelConfigChangedEvent(this, tenantId, modelType));
            log.info("Tenant model config created from active fallback with new API key: tenant={}, type={}, model={}",
                    tenantId, modelType, config.getModelName());
            return config;
        });
    }

    /**
     * Enable a specific config and disable others of the same type.
     */
    public RagTenantModelConfig enableConfig(Long tenantId, Long configId) {
        return TenantContextHolder.callWithBypass(() -> {
            RagTenantModelConfig config = requireOwnedConfig(tenantId, configId);

            disableOtherEnabledConfigs(config.getTenantId(), config.getModelType(), configId);

            // Enable target
            config.setEnabled(true);
            config.setUpdatedAt(LocalDateTime.now());
            configMapper.updateById(config);

            evictCache(config.getTenantId(), config.getModelType());
            eventPublisher.publishEvent(new ModelConfigChangedEvent(this, config.getTenantId(), config.getModelType()));
            log.info("Model config enabled: id={}, tenant={}, type={}", configId, config.getTenantId(), config.getModelType());
            return config;
        });
    }

    /**
     * Disable a specific config before it can be edited.
     */
    public RagTenantModelConfig disableConfig(Long tenantId, Long configId) {
        return TenantContextHolder.callWithBypass(() -> {
            RagTenantModelConfig config = requireOwnedConfig(tenantId, configId);
            if (!Boolean.FALSE.equals(config.getEnabled())) {
                config.setEnabled(false);
                config.setUpdatedAt(LocalDateTime.now());
                configMapper.updateById(config);
                evictCache(config.getTenantId(), config.getModelType());
                eventPublisher.publishEvent(new ModelConfigChangedEvent(this, config.getTenantId(), config.getModelType()));
                log.info("Model config disabled: id={}, tenant={}, type={}", configId, config.getTenantId(), config.getModelType());
            }
            return config;
        });
    }

    /**
     * Delete (soft delete) a model config.
     */
    public void deleteConfig(Long tenantId, Long configId) {
        TenantContextHolder.callWithBypass(() -> {
            RagTenantModelConfig config = requireOwnedConfig(tenantId, configId);
            configMapper.deleteById(configId);
            evictCache(config.getTenantId(), config.getModelType());
            eventPublisher.publishEvent(new ModelConfigChangedEvent(this, config.getTenantId(), config.getModelType()));
            log.info("Model config deleted: id={}, tenant={}, type={}", configId, config.getTenantId(), config.getModelType());
            return null;
        });
    }

    /**
     * Evict cached model config for a tenant and model type.
     */
    public void evictCache(Long tenantId, String modelType) {
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate != null) {
            try {
                redisTemplate.delete(CACHE_KEY_PREFIX.formatted(tenantId, modelType));
            } catch (Exception e) {
                log.warn("Redis cache eviction failed for tenant={}, type={}", tenantId, modelType, e);
            }
        }
    }

    /**
     * Reload all caches for a tenant (admin operation).
     */
    public void reloadTenant(Long tenantId) {
        evictCache(tenantId, MODEL_TYPE_LLM);
        evictCache(tenantId, MODEL_TYPE_EMBEDDING);
        evictCache(tenantId, MODEL_TYPE_IMAGE);
    }

    private RagTenantModelConfig loadFromMysql(Long tenantId, String modelType) {
        return TenantContextHolder.callWithBypass(() -> {
            // Try tenant-specific
            RagTenantModelConfig config = findEnabled(tenantId, modelType);
            if (config != null) {
                return config;
            }
            // Fall back to global (tenant_id=0)
            if (tenantId != 0) {
                config = findEnabled(0L, modelType);
                if (config != null) {
                    return config;
                }
            }
            // Ultimate fallback - create from yml
            return fallbackFromYml(modelType);
        });
    }

    private RagTenantModelConfig findEnabled(Long tenantId, String modelType) {
        return configMapper.selectOne(new LambdaQueryWrapper<RagTenantModelConfig>()
                .eq(RagTenantModelConfig::getTenantId, tenantId)
                .eq(RagTenantModelConfig::getModelType, modelType)
                .eq(RagTenantModelConfig::getEnabled, true)
                .last("LIMIT 1"));
    }

    private void disableOtherEnabledConfigs(Long tenantId, String modelType, Long keepConfigId) {
        List<RagTenantModelConfig> enabledConfigs = configMapper.selectList(new LambdaQueryWrapper<RagTenantModelConfig>()
                .eq(RagTenantModelConfig::getTenantId, tenantId)
                .eq(RagTenantModelConfig::getModelType, modelType)
                .eq(RagTenantModelConfig::getEnabled, true));
        for (RagTenantModelConfig enabledConfig : enabledConfigs) {
            if (keepConfigId != null && keepConfigId.equals(enabledConfig.getId())) {
                continue;
            }
            enabledConfig.setEnabled(false);
            enabledConfig.setUpdatedAt(LocalDateTime.now());
            configMapper.updateById(enabledConfig);
        }
    }

    private void appendEffectiveFallbackIfMissing(List<RagTenantModelConfig> configs, Long tenantId, String modelType) {
        boolean hasTenantEnabled = configs.stream()
                .anyMatch(config -> tenantId.equals(config.getTenantId())
                        && modelType.equals(config.getModelType())
                        && Boolean.TRUE.equals(config.getEnabled()));
        if (hasTenantEnabled) {
            return;
        }
        RagTenantModelConfig active = loadFromMysql(tenantId, modelType);
        if (active == null) {
            return;
        }
        boolean alreadyIncluded = active.getId() != null && configs.stream()
                .anyMatch(config -> active.getId().equals(config.getId()));
        if (alreadyIncluded) {
            return;
        }
        if (tenantId.equals(active.getTenantId())) {
            configs.add(active);
            return;
        }
        configs.add(copyAsFallbackListItem(active));
    }

    private RagTenantModelConfig copyAsFallbackListItem(RagTenantModelConfig source) {
        RagTenantModelConfig config = copyForTenant(source, source.getTenantId());
        config.setId(null);
        config.setEnabled(source.getEnabled());
        config.setApiKeySecretRef(source.getApiKeySecretRef());
        config.setCreatedAt(source.getCreatedAt());
        config.setUpdatedAt(source.getUpdatedAt());
        config.setIsDeleted(source.getIsDeleted());
        return config;
    }

    private RagTenantModelConfig requireOwnedConfig(Long tenantId, Long configId) {
        RagTenantModelConfig config = configMapper.selectById(configId);
        if (config == null || config.getIsDeleted() != null && config.getIsDeleted() != 0) {
            throw new IllegalArgumentException("Model config not found: " + configId);
        }
        if (!tenantId.equals(config.getTenantId())) {
            throw new IllegalArgumentException("Model config not found: " + configId);
        }
        return config;
    }

    private void requireSupportedModelType(String modelType) {
        if (!MODEL_TYPE_LLM.equals(modelType)
                && !MODEL_TYPE_EMBEDDING.equals(modelType)
                && !MODEL_TYPE_IMAGE.equals(modelType)) {
            throw new IllegalArgumentException("Unsupported model type: " + modelType);
        }
    }

    private RagTenantModelConfig copyForTenant(RagTenantModelConfig source, Long tenantId) {
        RagTenantModelConfig config = new RagTenantModelConfig();
        config.setTenantId(tenantId);
        config.setModelType(source.getModelType());
        config.setProvider(source.getProvider());
        config.setModelName(source.getModelName());
        config.setBaseUrl(source.getBaseUrl());
        config.setTemperature(source.getTemperature());
        config.setDimension(source.getDimension());
        config.setImageSize(source.getImageSize());
        config.setImageQuality(source.getImageQuality());
        config.setPollIntervalMillis(source.getPollIntervalMillis());
        config.setRateLimitQps(source.getRateLimitQps());
        config.setMonthlyBudgetCents(source.getMonthlyBudgetCents());
        config.setTimeoutSeconds(source.getTimeoutSeconds());
        config.setMaxRetries(source.getMaxRetries());
        config.setMaxTokens(source.getMaxTokens());
        config.setFrequencyPenalty(source.getFrequencyPenalty());
        config.setPresencePenalty(source.getPresencePenalty());
        config.setTopP(source.getTopP());
        return config;
    }

    private RagTenantModelConfig fallbackFromYml(String modelType) {
        RagProperties props = propertiesProvider.getIfAvailable();
        if (props == null) {
            return null;
        }
        RagTenantModelConfig config = new RagTenantModelConfig();
        config.setTenantId(0L);
        config.setEnabled(true);
        config.setProvider("openai-compatible");

        if (MODEL_TYPE_LLM.equals(modelType)) {
            RagProperties.Llm llm = props.getLlm();
            config.setModelType(MODEL_TYPE_LLM);
            config.setModelName(llm.getModel());
            config.setBaseUrl(llm.getBaseUrl());
            config.setApiKeySecretRef(encryptApiKey(llm.getApiKey()));
            config.setTemperature(BigDecimal.valueOf(llm.getTemperature()));
        } else if (MODEL_TYPE_EMBEDDING.equals(modelType)) {
            RagProperties.Embedding embedding = props.getEmbedding();
            config.setModelType(MODEL_TYPE_EMBEDDING);
            config.setModelName(embedding.getModel());
            config.setBaseUrl(embedding.getBaseUrl());
            config.setApiKeySecretRef(encryptApiKey(embedding.getApiKey()));
            config.setDimension(embedding.getDimension());
        } else if (MODEL_TYPE_IMAGE.equals(modelType)) {
            RagProperties.Image image = props.getImage();
            RagProperties.Llm llm = props.getLlm();
            config.setModelType(MODEL_TYPE_IMAGE);
            config.setModelName(image.getModel());
            config.setBaseUrl(StringUtils.hasText(image.getBaseUrl()) ? image.getBaseUrl() : llm.getBaseUrl());
            config.setApiKeySecretRef(encryptApiKey(StringUtils.hasText(image.getApiKey()) ? image.getApiKey() : llm.getApiKey()));
            config.setImageSize(image.getSize());
            config.setImageQuality(image.getQuality());
            config.setTimeoutSeconds(image.getTimeoutSeconds());
            config.setPollIntervalMillis(image.getPollIntervalMillis());
        }
        return config;
    }

    private String decryptApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        return apiKeyEncryptor.decrypt(apiKey);
    }

    /**
     * Encrypt API key before storage if encryption is enabled.
     * Returns original value if encryption is disabled or value is null/empty.
     */
    private String encryptApiKey(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            return apiKey;
        }
        RagProperties props = propertiesProvider.getIfAvailable();
        if (props != null && !props.getSecurity().isApiKeyEncryptionEnabled()) {
            return apiKey;
        }
        return apiKeyEncryptor.encrypt(apiKey);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
