package cc.ivera.ragdemo.service.tenant;

import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.domain.tenant.RagTenantModelConfig;
import cc.ivera.ragdemo.tenant.TenantContextHolder;
import cc.ivera.ragdemo.util.ApiKeyEncryptor;
import cc.ivera.ragdemo.util.ApiKeyMasker;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Dynamic model factory that creates and caches AI models based on database configuration.
 * <p>
 * Models are created lazily when requested and cached per tenant and model type.
 * When the database configuration changes, cached models are invalidated and recreated on next use.
 * <p>
 * Supports:
 * - ChatLanguageModel (LLM)
 * - StreamingChatLanguageModel (LLM)
 * - EmbeddingModel (Embedding)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicModelFactory {

    private final ModelConfigService modelConfigService;
    private final ApiKeyEncryptor apiKeyEncryptor;
    private final RagProperties properties;

    /**
     * Cache for LLM chat models (non-streaming)
     * Key: tenantId
     */
    private final Map<Long, CachedModel<ChatLanguageModel>> llmCache = new ConcurrentHashMap<>();

    /**
     * Cache for streaming LLM chat models
     * Key: tenantId
     */
    private final Map<Long, CachedModel<StreamingChatLanguageModel>> streamingLlmCache = new ConcurrentHashMap<>();

    /**
     * Cache for embedding models
     * Key: tenantId
     */
    private final Map<Long, CachedModel<EmbeddingModel>> embeddingCache = new ConcurrentHashMap<>();

    // Cache statistics counters
    private final AtomicLong llmHits = new AtomicLong(0);
    private final AtomicLong llmMisses = new AtomicLong(0);
    private final AtomicLong llmCreations = new AtomicLong(0);
    private final AtomicLong llmExpirations = new AtomicLong(0);

    private final AtomicLong streamingLlmHits = new AtomicLong(0);
    private final AtomicLong streamingLlmMisses = new AtomicLong(0);
    private final AtomicLong streamingLlmCreations = new AtomicLong(0);
    private final AtomicLong streamingLlmExpirations = new AtomicLong(0);

    private final AtomicLong embeddingHits = new AtomicLong(0);
    private final AtomicLong embeddingMisses = new AtomicLong(0);
    private final AtomicLong embeddingCreations = new AtomicLong(0);
    private final AtomicLong embeddingExpirations = new AtomicLong(0);

    /**
     * Get the LLM chat model for the current tenant.
     * Creates or returns cached model based on database configuration.
     */
    public ChatLanguageModel getLlmModel() {
        Long tenantId = TenantContextHolder.currentTenantId().orElse(0L);
        return getLlmModel(tenantId);
    }

    /**
     * Get the LLM chat model for a specific tenant.
     */
    public ChatLanguageModel getLlmModel(Long tenantId) {
        return llmCache.compute(tenantId, (id, cached) -> {
            if (cached == null || cached.isStale()) {
                llmMisses.incrementAndGet();
                llmCreations.incrementAndGet();
                log.info("Creating new LLM model for tenant {}", tenantId);
                return new CachedModel<>(createLlmModel(tenantId), System.currentTimeMillis());
            }
            llmHits.incrementAndGet();
            return cached;
        }).model();
    }

    /**
     * Get the streaming LLM chat model for the current tenant.
     */
    public StreamingChatLanguageModel getStreamingLlmModel() {
        Long tenantId = TenantContextHolder.currentTenantId().orElse(0L);
        return getStreamingLlmModel(tenantId);
    }

    /**
     * Get the streaming LLM chat model for a specific tenant.
     */
    public StreamingChatLanguageModel getStreamingLlmModel(Long tenantId) {
        return streamingLlmCache.compute(tenantId, (id, cached) -> {
            if (cached == null || cached.isStale()) {
                streamingLlmMisses.incrementAndGet();
                streamingLlmCreations.incrementAndGet();
                log.info("Creating new streaming LLM model for tenant {}", tenantId);
                return new CachedModel<>(createStreamingLlmModel(tenantId), System.currentTimeMillis());
            }
            streamingLlmHits.incrementAndGet();
            return cached;
        }).model();
    }

    /**
     * Get the embedding model for the current tenant.
     */
    public EmbeddingModel getEmbeddingModel() {
        Long tenantId = TenantContextHolder.currentTenantId().orElse(0L);
        return getEmbeddingModel(tenantId);
    }

    /**
     * Get the embedding model for a specific tenant.
     */
    public EmbeddingModel getEmbeddingModel(Long tenantId) {
        return embeddingCache.compute(tenantId, (id, cached) -> {
            if (cached == null || cached.isStale()) {
                embeddingMisses.incrementAndGet();
                embeddingCreations.incrementAndGet();
                log.info("Creating new embedding model for tenant {}", tenantId);
                return new CachedModel<>(createEmbeddingModel(tenantId), System.currentTimeMillis());
            }
            embeddingHits.incrementAndGet();
            return cached;
        }).model();
    }

    /**
     * Invalidate all cached models for a specific tenant.
     */
    public void invalidateTenant(Long tenantId) {
        llmCache.remove(tenantId);
        streamingLlmCache.remove(tenantId);
        embeddingCache.remove(tenantId);
        log.info("Invalidated all model caches for tenant {}", tenantId);
    }

    /**
     * Invalidate all cached models globally.
     */
    public void invalidateAll() {
        llmCache.clear();
        streamingLlmCache.clear();
        embeddingCache.clear();
        log.info("Invalidated all model caches globally");
    }

    /**
     * Get cache statistics for monitoring.
     */
    public ModelCacheStats getCacheStats() {
        return new ModelCacheStats(
                llmCache.size(),
                streamingLlmCache.size(),
                embeddingCache.size(),
                llmHits.get(),
                llmMisses.get(),
                llmCreations.get(),
                llmExpirations.get(),
                streamingLlmHits.get(),
                streamingLlmMisses.get(),
                streamingLlmCreations.get(),
                streamingLlmExpirations.get(),
                embeddingHits.get(),
                embeddingMisses.get(),
                embeddingCreations.get(),
                embeddingExpirations.get()
        );
    }

    /**
     * Reset cache statistics counters.
     */
    public void resetStats() {
        llmHits.set(0);
        llmMisses.set(0);
        llmCreations.set(0);
        llmExpirations.set(0);
        streamingLlmHits.set(0);
        streamingLlmMisses.set(0);
        streamingLlmCreations.set(0);
        streamingLlmExpirations.set(0);
        embeddingHits.set(0);
        embeddingMisses.set(0);
        embeddingCreations.set(0);
        embeddingExpirations.set(0);
        log.info("Cache statistics reset");
    }

    /**
     * Handle model config change events.
     */
    @EventListener
    public void onModelConfigChanged(ModelConfigChangedEvent event) {
        log.info("Received model config change event: tenant={}, type={}",
                event.getTenantId(), event.getModelType());
        invalidateTenant(event.getTenantId());
    }

    /**
     * Scheduled cleanup of stale cache entries (every 10 minutes).
     */
    @Scheduled(fixedRate = 600_000)
    public void cleanupStaleCache() {
        int removed = 0;
        removed += removeStaleEntries(llmCache, llmExpirations);
        removed += removeStaleEntries(streamingLlmCache, streamingLlmExpirations);
        removed += removeStaleEntries(embeddingCache, embeddingExpirations);
        if (removed > 0) {
            log.info("Cleaned up {} stale model cache entries", removed);
        }
    }

    private <T> int removeStaleEntries(Map<Long, CachedModel<T>> cache, AtomicLong expirationCounter) {
        int removed = 0;
        for (Map.Entry<Long, CachedModel<T>> entry : cache.entrySet()) {
            if (entry.getValue().isStale()) {
                cache.remove(entry.getKey());
                removed++;
            }
        }
        expirationCounter.addAndGet(removed);
        return removed;
    }

    private ChatLanguageModel createLlmModel(Long tenantId) {
        RagTenantModelConfig config = modelConfigService.getActiveLlmConfig(tenantId);
        if (config == null) {
            log.warn("No LLM config found for tenant {}, using placeholder model", tenantId);
            return createPlaceholderLlm();
        }

        String apiKey = resolveApiKey(config);
        OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
                .baseUrl(config.getBaseUrl())
                .apiKey(apiKey)
                .modelName(config.getModelName());

        // 从数据库配置读取参数，不再使用硬编码值
        if (config.getTemperature() != null) {
            builder.temperature(config.getTemperature().doubleValue());
        }
        if (config.getMaxTokens() != null) {
            builder.maxTokens(config.getMaxTokens());
        }
        if (config.getFrequencyPenalty() != null) {
            builder.frequencyPenalty(config.getFrequencyPenalty().doubleValue());
        }
        if (config.getPresencePenalty() != null) {
            builder.presencePenalty(config.getPresencePenalty().doubleValue());
        }
        if (config.getTopP() != null) {
            builder.topP(config.getTopP().doubleValue());
        }
        if (config.getTimeoutSeconds() != null) {
            builder.timeout(Duration.ofSeconds(config.getTimeoutSeconds()));
        }
        if (config.getMaxRetries() != null) {
            builder.maxRetries(config.getMaxRetries());
        }

        log.info("Created LLM model: tenant={}, model={}, baseUrl={}, temperature={}, maxTokens={}, timeout={}s",
                tenantId, config.getModelName(), config.getBaseUrl(),
                config.getTemperature(), config.getMaxTokens(), config.getTimeoutSeconds());
        return builder.build();
    }

    private StreamingChatLanguageModel createStreamingLlmModel(Long tenantId) {
        RagTenantModelConfig config = modelConfigService.getActiveLlmConfig(tenantId);
        if (config == null) {
            log.warn("No LLM config found for tenant {}, using placeholder streaming model", tenantId);
            return createPlaceholderStreamingLlm();
        }

        String apiKey = resolveApiKey(config);
        OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder builder = OpenAiStreamingChatModel.builder()
                .baseUrl(config.getBaseUrl())
                .apiKey(apiKey)
                .modelName(config.getModelName());

        // 从数据库配置读取参数，不再使用硬编码值
        if (config.getTemperature() != null) {
            builder.temperature(config.getTemperature().doubleValue());
        }
        if (config.getMaxTokens() != null) {
            builder.maxTokens(config.getMaxTokens());
        }
        if (config.getFrequencyPenalty() != null) {
            builder.frequencyPenalty(config.getFrequencyPenalty().doubleValue());
        }
        if (config.getPresencePenalty() != null) {
            builder.presencePenalty(config.getPresencePenalty().doubleValue());
        }
        if (config.getTopP() != null) {
            builder.topP(config.getTopP().doubleValue());
        }
        if (config.getTimeoutSeconds() != null) {
            builder.timeout(Duration.ofSeconds(config.getTimeoutSeconds()));
        }
        // streaming model 不支持 maxRetries

        log.info("Created streaming LLM model: tenant={}, model={}, baseUrl={}, temperature={}, maxTokens={}, timeout={}s",
                tenantId, config.getModelName(), config.getBaseUrl(),
                config.getTemperature(), config.getMaxTokens(), config.getTimeoutSeconds());
        return builder.build();
    }

    private EmbeddingModel createEmbeddingModel(Long tenantId) {
        RagTenantModelConfig config = modelConfigService.getActiveEmbeddingConfig(tenantId);
        if (config == null) {
            log.warn("No embedding config found for tenant {}, using placeholder model", tenantId);
            return createPlaceholderEmbedding();
        }

        String apiKey = resolveApiKey(config);
        OpenAiEmbeddingModel.OpenAiEmbeddingModelBuilder builder = OpenAiEmbeddingModel.builder()
                .baseUrl(config.getBaseUrl())
                .apiKey(apiKey)
                .modelName(config.getModelName());

        // 从数据库配置读取参数，不再使用硬编码值
        if (config.getDimension() != null) {
            builder.dimensions(config.getDimension());
        }
        if (config.getTimeoutSeconds() != null) {
            builder.timeout(Duration.ofSeconds(config.getTimeoutSeconds()));
        }
        if (config.getMaxRetries() != null) {
            builder.maxRetries(config.getMaxRetries());
        }

        log.info("Created embedding model: tenant={}, model={}, baseUrl={}, dimension={}, timeout={}s",
                tenantId, config.getModelName(), config.getBaseUrl(),
                config.getDimension(), config.getTimeoutSeconds());
        return builder.build();
    }

    private String resolveApiKey(RagTenantModelConfig config) {
        String rawKey = config.getApiKeySecretRef();
        if (rawKey == null || rawKey.isBlank()) {
            log.warn("API key not configured for model {} (tenant {}), using placeholder",
                    config.getModelName(), config.getTenantId());
            return "placeholder-key";
        }
        String apiKey = apiKeyEncryptor.decrypt(rawKey);
        if (!apiKey.equals(rawKey)) {
            log.debug("Decrypted API key for model {} (tenant {}): {}",
                    config.getModelName(), config.getTenantId(), ApiKeyMasker.mask(apiKey));
        }
        return apiKey;
    }

    private ChatLanguageModel createPlaceholderLlm() {
        RagProperties.Llm llmProps = properties.getLlm();
        return OpenAiChatModel.builder()
                .baseUrl(llmProps.getBaseUrl() != null ? llmProps.getBaseUrl() : "https://api.openai.com/v1")
                .apiKey(llmProps.getApiKey() != null ? llmProps.getApiKey() : "placeholder-key")
                .modelName(llmProps.getModel() != null ? llmProps.getModel() : "gpt-3.5-turbo")
                .temperature(llmProps.getTemperature())
                .build();
    }

    private StreamingChatLanguageModel createPlaceholderStreamingLlm() {
        RagProperties.Llm llmProps = properties.getLlm();
        return OpenAiStreamingChatModel.builder()
                .baseUrl(llmProps.getBaseUrl() != null ? llmProps.getBaseUrl() : "https://api.openai.com/v1")
                .apiKey(llmProps.getApiKey() != null ? llmProps.getApiKey() : "placeholder-key")
                .modelName(llmProps.getModel() != null ? llmProps.getModel() : "gpt-3.5-turbo")
                .temperature(llmProps.getTemperature())
                .build();
    }

    private EmbeddingModel createPlaceholderEmbedding() {
        RagProperties.Embedding embedProps = properties.getEmbedding();
        return OpenAiEmbeddingModel.builder()
                .baseUrl(embedProps.getBaseUrl() != null ? embedProps.getBaseUrl() : "https://api.openai.com/v1")
                .apiKey(embedProps.getApiKey() != null ? embedProps.getApiKey() : "placeholder-key")
                .modelName(embedProps.getModel() != null ? embedProps.getModel() : "text-embedding-3-small")
                .dimensions(embedProps.getDimension())
                .build();
    }

    /**
     * Wrapper for cached models with timestamp for staleness checking.
     */
    private record CachedModel<T>(T model, long createdAt) {

        /**
         * Check if cache entry is stale (older than 10 minutes).
         */
        boolean isStale() {
            return System.currentTimeMillis() - createdAt > 600_000; // 10 minutes
        }
    }

    /**
     * Statistics object for cache monitoring.
     */
    @Getter
    public static class ModelCacheStats {
        // Cache sizes
        private final int llmCacheSize;
        private final int streamingLlmCacheSize;
        private final int embeddingCacheSize;

        // LLM cache statistics
        private final long llmHits;
        private final long llmMisses;
        private final long llmCreations;
        private final long llmExpirations;

        // Streaming LLM cache statistics
        private final long streamingLlmHits;
        private final long streamingLlmMisses;
        private final long streamingLlmCreations;
        private final long streamingLlmExpirations;

        // Embedding cache statistics
        private final long embeddingHits;
        private final long embeddingMisses;
        private final long embeddingCreations;
        private final long embeddingExpirations;

        public ModelCacheStats(int llmCacheSize, int streamingLlmCacheSize, int embeddingCacheSize,
                               long llmHits, long llmMisses, long llmCreations, long llmExpirations,
                               long streamingLlmHits, long streamingLlmMisses, long streamingLlmCreations, long streamingLlmExpirations,
                               long embeddingHits, long embeddingMisses, long embeddingCreations, long embeddingExpirations) {
            this.llmCacheSize = llmCacheSize;
            this.streamingLlmCacheSize = streamingLlmCacheSize;
            this.embeddingCacheSize = embeddingCacheSize;
            this.llmHits = llmHits;
            this.llmMisses = llmMisses;
            this.llmCreations = llmCreations;
            this.llmExpirations = llmExpirations;
            this.streamingLlmHits = streamingLlmHits;
            this.streamingLlmMisses = streamingLlmMisses;
            this.streamingLlmCreations = streamingLlmCreations;
            this.streamingLlmExpirations = streamingLlmExpirations;
            this.embeddingHits = embeddingHits;
            this.embeddingMisses = embeddingMisses;
            this.embeddingCreations = embeddingCreations;
            this.embeddingExpirations = embeddingExpirations;
        }

        /**
         * Calculate LLM cache hit rate.
         */
        public double getLlmHitRate() {
            long total = llmHits + llmMisses;
            return total > 0 ? (double) llmHits / total : 0.0;
        }

        /**
         * Calculate streaming LLM cache hit rate.
         */
        public double getStreamingLlmHitRate() {
            long total = streamingLlmHits + streamingLlmMisses;
            return total > 0 ? (double) streamingLlmHits / total : 0.0;
        }

        /**
         * Calculate embedding cache hit rate.
         */
        public double getEmbeddingHitRate() {
            long total = embeddingHits + embeddingMisses;
            return total > 0 ? (double) embeddingHits / total : 0.0;
        }

        /**
         * Get total cache size across all caches.
         */
        public int getTotalCacheSize() {
            return llmCacheSize + streamingLlmCacheSize + embeddingCacheSize;
        }

        /**
         * Get total hits across all caches.
         */
        public long getTotalHits() {
            return llmHits + streamingLlmHits + embeddingHits;
        }

        /**
         * Get total misses across all caches.
         */
        public long getTotalMisses() {
            return llmMisses + streamingLlmMisses + embeddingMisses;
        }

        /**
         * Get total hit rate across all caches.
         */
        public double getTotalHitRate() {
            long total = getTotalHits() + getTotalMisses();
            return total > 0 ? (double) getTotalHits() / total : 0.0;
        }
    }
}
