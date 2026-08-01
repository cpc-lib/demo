package cc.ivera.ragdemo.controller;

import cc.ivera.ragdemo.domain.tenant.RagTenantModelConfig;
import cc.ivera.ragdemo.model.dto.EntityDtoConverter;
import cc.ivera.ragdemo.model.dto.RagTenantModelConfigDto;
import cc.ivera.ragdemo.model.query.ModelConfigApiKeyResponse;
import cc.ivera.ragdemo.model.query.ModelConfigApiKeyUpdateRequest;
import cc.ivera.ragdemo.model.query.ModelConfigUpsertRequest;
import cc.ivera.ragdemo.model.query.RagApiResponse;
import cc.ivera.ragdemo.service.tenant.DynamicModelFactory;
import cc.ivera.ragdemo.service.tenant.ModelConfigService;
import cc.ivera.ragdemo.tenant.TenantContextHolder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin API for managing AI model configurations (LLM + Embedding + Image).
 * <p>
 * Model configs are tenant-scoped with global fallback (tenant_id=0).
 * Supports cache-aside pattern with Redis and dynamic model factory.
 */
@RestController
@RequestMapping("/api/admin/model-configs")
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class ModelConfigController {

    private final ModelConfigService modelConfigService;
    private final DynamicModelFactory dynamicModelFactory;
    private final EntityDtoConverter converter;

    /**
     * List all model configs for the current tenant.
     */
    @GetMapping
    public RagApiResponse<List<RagTenantModelConfigDto>> listConfigs() {
        Long tenantId = TenantContextHolder.requireTenantId();
        return RagApiResponse.ok(converter.toTenantModelConfigDtoList(modelConfigService.listConfigs(tenantId)));
    }

    /**
     * Get one model config by id for the current tenant.
     */
    @GetMapping("/{id}")
    public RagApiResponse<RagTenantModelConfigDto> getConfig(@PathVariable Long id) {
        Long tenantId = TenantContextHolder.requireTenantId();
        return RagApiResponse.ok(converter.toDto(modelConfigService.getConfig(tenantId, id)));
    }

    /**
     * Get the active LLM config for the current tenant.
     */
    @GetMapping("/llm")
    public RagApiResponse<RagTenantModelConfigDto> getActiveLlm() {
        Long tenantId = TenantContextHolder.requireTenantId();
        return RagApiResponse.ok(converter.toDto(modelConfigService.getActiveLlmConfig(tenantId)));
    }

    /**
     * Get the active embedding config for the current tenant.
     */
    @GetMapping("/embedding")
    public RagApiResponse<RagTenantModelConfigDto> getActiveEmbedding() {
        Long tenantId = TenantContextHolder.requireTenantId();
        return RagApiResponse.ok(converter.toDto(modelConfigService.getActiveEmbeddingConfig(tenantId)));
    }

    /**
     * Get the active text-to-image config for the current tenant.
     */
    @GetMapping("/image")
    public RagApiResponse<RagTenantModelConfigDto> getActiveImage() {
        Long tenantId = TenantContextHolder.requireTenantId();
        return RagApiResponse.ok(converter.toDto(modelConfigService.getActiveImageConfig(tenantId)));
    }

    /**
     * Reveal the API key used by the active config for a model type.
     */
    @GetMapping("/active/{modelType}/api-key")
    public RagApiResponse<ModelConfigApiKeyResponse> getActiveApiKey(@PathVariable String modelType) {
        Long tenantId = TenantContextHolder.requireTenantId();
        RagTenantModelConfig config = modelConfigService.getActiveConfig(tenantId, modelType);
        String apiKey = modelConfigService.revealActiveApiKey(tenantId, modelType);
        return RagApiResponse.ok(toApiKeyResponse(config, modelType, apiKey));
    }

    /**
     * Replace only the active API key. If active config is fallback-only, creates a tenant-owned override.
     */
    @PutMapping("/active/{modelType}/api-key")
    public RagApiResponse<RagTenantModelConfigDto> updateActiveApiKey(@PathVariable String modelType,
                                                                      @Valid @RequestBody ModelConfigApiKeyUpdateRequest request) {
        Long tenantId = TenantContextHolder.requireTenantId();
        return RagApiResponse.ok(converter.toDto(modelConfigService.updateActiveApiKey(
                tenantId,
                modelType,
                request.apiKeySecretRef()
        )));
    }

    /**
     * Create or update a model config.
     * If an enabled config of the same type exists, it will be disabled first.
     */
    @PostMapping
    public RagApiResponse<RagTenantModelConfigDto> upsert(@Valid @RequestBody ModelConfigUpsertRequest request) {
        Long tenantId = TenantContextHolder.requireTenantId();

        RagTenantModelConfig result = modelConfigService.upsertConfig(
                tenantId,
                request.getModelType(),
                request.getProvider() != null ? request.getProvider() : "openai-compatible",
                request.getModelName(),
                request.getBaseUrl(),
                request.getApiKeySecretRef(),
                request.getTemperature(),
                request.getDimension(),
                request.getImageSize(),
                request.getImageQuality(),
                request.getPollIntervalMillis(),
                request.getRateLimitQps(),
                request.getMonthlyBudgetCents(),
                request.getTimeoutSeconds(),
                request.getMaxRetries(),
                request.getMaxTokens(),
                request.getFrequencyPenalty(),
                request.getPresencePenalty(),
                request.getTopP(),
                request.getEnabled());
        return RagApiResponse.ok(converter.toDto(result));
    }

    /**
     * Update an existing model config by id.
     */
    @PutMapping("/{id}")
    public RagApiResponse<RagTenantModelConfigDto> update(@PathVariable Long id,
                                                          @Valid @RequestBody ModelConfigUpsertRequest request) {
        Long tenantId = TenantContextHolder.requireTenantId();
        return RagApiResponse.ok(converter.toDto(modelConfigService.updateConfig(tenantId, id, request)));
    }

    /**
     * Reveal the API key for an owned model config.
     */
    @GetMapping("/{id}/api-key")
    public RagApiResponse<ModelConfigApiKeyResponse> getApiKey(@PathVariable Long id) {
        Long tenantId = TenantContextHolder.requireTenantId();
        RagTenantModelConfig config = modelConfigService.getConfig(tenantId, id);
        String apiKey = modelConfigService.revealApiKey(tenantId, id);
        return RagApiResponse.ok(toApiKeyResponse(config, config.getModelType(), apiKey));
    }

    /**
     * Replace only the API key for an owned model config. Enabled configs remain enabled.
     */
    @PutMapping("/{id}/api-key")
    public RagApiResponse<RagTenantModelConfigDto> updateApiKey(@PathVariable Long id,
                                                                @Valid @RequestBody ModelConfigApiKeyUpdateRequest request) {
        Long tenantId = TenantContextHolder.requireTenantId();
        return RagApiResponse.ok(converter.toDto(modelConfigService.updateApiKey(tenantId, id, request.apiKeySecretRef())));
    }

    /**
     * Enable a specific config (disables others of the same type).
     */
    @PutMapping("/{id}/enable")
    public RagApiResponse<RagTenantModelConfigDto> enable(@PathVariable Long id) {
        Long tenantId = TenantContextHolder.requireTenantId();
        return RagApiResponse.ok(converter.toDto(modelConfigService.enableConfig(tenantId, id)));
    }

    /**
     * Disable a specific config so it can be edited.
     */
    @PutMapping("/{id}/disable")
    public RagApiResponse<RagTenantModelConfigDto> disable(@PathVariable Long id) {
        Long tenantId = TenantContextHolder.requireTenantId();
        return RagApiResponse.ok(converter.toDto(modelConfigService.disableConfig(tenantId, id)));
    }

    /**
     * Delete (soft delete) a model config.
     */
    @DeleteMapping("/{id}")
    public RagApiResponse<Void> delete(@PathVariable Long id) {
        Long tenantId = TenantContextHolder.requireTenantId();
        modelConfigService.deleteConfig(tenantId, id);
        return RagApiResponse.ok(null);
    }

    /**
     * Reload model config caches for the current tenant.
     */
    @PostMapping("/reload")
    public RagApiResponse<Void> reload() {
        Long tenantId = TenantContextHolder.requireTenantId();
        modelConfigService.reloadTenant(tenantId);
        return RagApiResponse.ok(null);
    }

    /**
     * Invalidate dynamic model caches for the current tenant.
     * This forces models to be recreated from database configuration on next use.
     */
    @PostMapping("/invalidate-models")
    public RagApiResponse<Void> invalidateModels() {
        Long tenantId = TenantContextHolder.requireTenantId();
        dynamicModelFactory.invalidateTenant(tenantId);
        return RagApiResponse.ok(null);
    }

    /**
     * Invalidate all dynamic model caches globally.
     */
    @PostMapping("/invalidate-models/all")
    public RagApiResponse<Void> invalidateAllModels() {
        dynamicModelFactory.invalidateAll();
        return RagApiResponse.ok(null);
    }

    /**
     * Get dynamic model cache statistics.
     */
    @GetMapping("/cache-stats")
    public RagApiResponse<DynamicModelFactory.ModelCacheStats> getCacheStats() {
        return RagApiResponse.ok(dynamicModelFactory.getCacheStats());
    }

    /**
     * Reset dynamic model cache statistics counters.
     */
    @PostMapping("/cache-stats/reset")
    public RagApiResponse<DynamicModelFactory.ModelCacheStats> resetCacheStats() {
        dynamicModelFactory.resetStats();
        return RagApiResponse.ok(dynamicModelFactory.getCacheStats());
    }

    private ModelConfigApiKeyResponse toApiKeyResponse(RagTenantModelConfig config, String modelType, String apiKey) {
        return new ModelConfigApiKeyResponse(
                config == null ? null : config.getId(),
                config == null ? modelType : config.getModelType(),
                config == null ? null : config.getEnabled(),
                apiKey != null && !apiKey.isBlank(),
                apiKey
        );
    }
}
