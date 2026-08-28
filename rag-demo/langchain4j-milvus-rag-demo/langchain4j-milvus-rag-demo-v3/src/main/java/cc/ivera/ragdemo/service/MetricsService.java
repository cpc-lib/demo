package cc.ivera.ragdemo.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 集中式指标监控服务
 * 封装 Micrometer 的 Timer/Counter/Gauge 埋点
 * 所有指标统一管理，确保命名和 tag 一致性
 */
@Slf4j
@Service
public class MetricsService {

    private final MeterRegistry meterRegistry;

    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        log.info("MetricsService initialized with meter registry: {}", meterRegistry.getClass().getSimpleName());
    }

    // ==================== RAG 查询指标 ====================

    /**
     * 记录 RAG 查询延迟和调用次数
     */
    public void recordQuery(long latencyMs, boolean success, String queryType) {
        Tags tags = Tags.of("success", String.valueOf(success))
                .and("type", queryType != null ? queryType : "unknown");

        Timer.builder("rag.query.duration")
                .tags(tags)
                .register(meterRegistry)
                .record(latencyMs, TimeUnit.MILLISECONDS);

        Counter.builder("rag.query.count")
                .tags(tags)
                .register(meterRegistry)
                .increment();
    }

    // ==================== Rerank 指标 ====================

    /**
     * 记录 Rerank 调用延迟和次数
     */
    public void recordRerank(long latencyMs, boolean success) {
        Tags tags = Tags.of("success", String.valueOf(success));

        Timer.builder("rag.rerank.duration")
                .tags(tags)
                .register(meterRegistry)
                .record(latencyMs, TimeUnit.MILLISECONDS);

        Counter.builder("rag.rerank.count")
                .tags(tags)
                .register(meterRegistry)
                .increment();
    }

    // ==================== Embedding 指标 ====================

    /**
     * 记录 Embedding 调用延迟和次数
     */
    public void recordEmbedding(long latencyMs, boolean success, String embeddingType) {
        Tags tags = Tags.of("success", String.valueOf(success))
                .and("type", embeddingType != null ? embeddingType : "unknown");

        Timer.builder("rag.embedding.duration")
                .tags(tags)
                .register(meterRegistry)
                .record(latencyMs, TimeUnit.MILLISECONDS);

        Counter.builder("rag.embedding.count")
                .tags(tags)
                .register(meterRegistry)
                .increment();
    }

    // ==================== 文档摄取指标 ====================

    /**
     * 记录文档摄取延迟和次数
     */
    public void recordIngestion(long latencyMs, boolean success) {
        Tags tags = Tags.of("success", String.valueOf(success));

        Timer.builder("rag.ingestion.duration")
                .tags(tags)
                .register(meterRegistry)
                .record(latencyMs, TimeUnit.MILLISECONDS);

        Counter.builder("rag.ingestion.count")
                .tags(tags)
                .register(meterRegistry)
                .increment();
    }

    // ==================== LLM 调用指标 ====================

    /**
     * 记录 LLM 调用延迟和次数
     */
    public void recordLlmCall(long latencyMs, boolean success, String model) {
        Tags tags = Tags.of("success", String.valueOf(success))
                .and("model", model != null ? model : "unknown");

        Timer.builder("rag.llm.duration")
                .tags(tags)
                .register(meterRegistry)
                .record(latencyMs, TimeUnit.MILLISECONDS);

        Counter.builder("rag.llm.count")
                .tags(tags)
                .register(meterRegistry)
                .increment();
    }

    // ==================== Token 使用量指标 ====================

    private final AtomicLong totalInputTokens = new AtomicLong(0);
    private final AtomicLong totalOutputTokens = new AtomicLong(0);

    /**
     * 记录 LLM Token 使用量
     */
    public void recordTokenUsage(long inputTokens, long outputTokens) {
        totalInputTokens.addAndGet(inputTokens);
        totalOutputTokens.addAndGet(outputTokens);

        Counter.builder("rag.llm.tokens")
                .tag("direction", "input")
                .register(meterRegistry)
                .increment(inputTokens);

        Counter.builder("rag.llm.tokens")
                .tag("direction", "output")
                .register(meterRegistry)
                .increment(outputTokens);
    }

    // ==================== 缓存指标 ====================

    /**
     * 注册 Gauge 监控模型缓存大小
     */
    public void registerCacheSizeGauge(String cacheName, java.util.function.Supplier<Number> supplier) {
        meterRegistry.gauge("rag.cache.size",
                Tags.of("cache", cacheName),
                supplier,
                s -> s.get().doubleValue());
    }

    // ==================== 限流指标 ====================

    /**
     * 记录限流触发次数
     */
    public void recordRateLimitTriggered(String limitType) {
        Counter.builder("rag.ratelimit.triggered")
                .tag("type", limitType != null ? limitType : "unknown")
                .register(meterRegistry)
                .increment();
    }

    // ==================== Redis 操作指标 ====================

    /**
     * 记录 Redis 操作延迟
     */
    public void recordRedisOperation(long latencyMs, String operation, boolean success) {
        Tags tags = Tags.of("operation", operation != null ? operation : "unknown")
                .and("success", String.valueOf(success));

        Timer.builder("rag.redis.duration")
                .tags(tags)
                .register(meterRegistry)
                .record(latencyMs, TimeUnit.MILLISECONDS);
    }
}
