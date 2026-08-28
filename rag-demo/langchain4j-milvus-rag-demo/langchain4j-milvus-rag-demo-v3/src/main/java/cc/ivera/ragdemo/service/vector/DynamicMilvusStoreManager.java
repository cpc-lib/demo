package cc.ivera.ragdemo.service.vector;


import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.tenant.TenantContextHolder;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class DynamicMilvusStoreManager {

    private static final String ACTIVE_ALIAS_KEY = "rag:milvus:active";
    private static final String ALIAS_SET_KEY = "rag:milvus:aliases";
    private static final String CONFIG_KEY_PREFIX = "rag:milvus:config:";

    private final RagProperties props;
    private final StringRedisTemplate redisTemplate;
    private final TenantMilvusCollectionResolver collectionResolver;

    private final Map<String, EmbeddingStore<TextSegment>> storeCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void initDefaultConfig() {
        String defaultAlias = props.getMilvus().getDefaultAlias();
        if (!exists(defaultAlias)) {
            saveOrUpdate(MilvusStoreConfig.builder()
                    .alias(defaultAlias)
                    .host(props.getMilvus().getHost())
                    .port(props.getMilvus().getPort())
                    .collection(props.getMilvus().getCollection())
                    .topK(props.getMilvus().getTopK())
                    .minScore(props.getMilvus().getMinScore())
                    .username(props.getMilvus().getUsername())
                    .password(props.getMilvus().getPassword())
                    .build());
        }

        String activeAlias = redisTemplate.opsForValue().get(ACTIVE_ALIAS_KEY);
        if (!StringUtils.hasText(activeAlias)) {
            redisTemplate.opsForValue().set(ACTIVE_ALIAS_KEY, defaultAlias);
        }
    }

    public ActiveMilvusContext current() {
        String alias = currentAlias();
        return context(alias);
    }

    public ActiveMilvusContext context(String alias) {
        if (!StringUtils.hasText(alias)) {
            return current();
        }
        MilvusStoreConfig config = tenantScopedConfig(load(alias));
        EmbeddingStore<TextSegment> store = storeCache.computeIfAbsent(storeCacheKey(config), k -> createStore(config));
        return new ActiveMilvusContext(alias, config, store);
    }

    public ActiveMilvusContext context(String alias, String collection) {
        String resolvedAlias = StringUtils.hasText(alias) ? alias.trim() : currentAlias();
        MilvusStoreConfig baseConfig = load(resolvedAlias);
        MilvusStoreConfig config = StringUtils.hasText(collection)
                ? copyWithCollection(baseConfig, collection.trim())
                : tenantScopedConfig(baseConfig);
        EmbeddingStore<TextSegment> store = storeCache.computeIfAbsent(storeCacheKey(config), k -> createStore(config));
        return new ActiveMilvusContext(resolvedAlias, config, store);
    }

    public String currentAlias() {
        String alias = redisTemplate.opsForValue().get(ACTIVE_ALIAS_KEY);
        if (!StringUtils.hasText(alias)) {
            alias = props.getMilvus().getDefaultAlias();
            redisTemplate.opsForValue().set(ACTIVE_ALIAS_KEY, alias);
        }
        return alias;
    }

    public void switchTo(String alias) {
        if (!exists(alias)) {
            throw new IllegalArgumentException("Milvus config not found for alias: " + alias);
        }
        redisTemplate.opsForValue().set(ACTIVE_ALIAS_KEY, alias);
    }

    public void saveOrUpdate(MilvusStoreConfig config) {
        validate(config);
        String key = configKey(config.getAlias());
        redisTemplate.opsForHash().put(key, "host", config.getHost());
        redisTemplate.opsForHash().put(key, "port", String.valueOf(config.getPort()));
        redisTemplate.opsForHash().put(key, "collection", config.getCollection());
        redisTemplate.opsForHash().put(key, "topK", String.valueOf(config.getTopK()));
        redisTemplate.opsForHash().put(key, "minScore", String.valueOf(config.getMinScore()));
        if (config.getUsername() != null) {
            redisTemplate.opsForHash().put(key, "username", config.getUsername());
        }
        if (config.getPassword() != null) {
            redisTemplate.opsForHash().put(key, "password", config.getPassword());
        }
        redisTemplate.opsForSet().add(ALIAS_SET_KEY, config.getAlias());
        storeCache.keySet().removeIf(cacheKey -> cacheKey.equals(config.getAlias()) || cacheKey.startsWith(config.getAlias() + "\u001F"));
    }

    public MilvusStoreConfig load(String alias) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(configKey(alias));
        if (entries == null || entries.isEmpty()) {
            throw new IllegalArgumentException("Milvus config not found for alias: " + alias);
        }

        return MilvusStoreConfig.builder()
                .alias(alias)
                .host(stringValue(entries.get("host")))
                .port(intValue(entries.get("port"), props.getMilvus().getPort()))
                .collection(stringValue(entries.get("collection")))
                .topK(intValue(entries.get("topK"), props.getMilvus().getTopK()))
                .minScore(doubleValue(entries.get("minScore"), props.getMilvus().getMinScore()))
                .username(stringValue(entries.get("username")))
                .password(stringValue(entries.get("password")))
                .build();
    }

    public boolean exists(String alias) {
        Boolean exists = redisTemplate.hasKey(configKey(alias));
        return Boolean.TRUE.equals(exists);
    }

    public List<MilvusStoreConfig> listAll() {
        Set<String> aliases = redisTemplate.opsForSet().members(ALIAS_SET_KEY);
        if (aliases == null || aliases.isEmpty()) {
            return Collections.emptyList();
        }

        List<MilvusStoreConfig> configs = new ArrayList<>();
        for (String alias : aliases) {
            if (exists(alias)) {
                configs.add(load(alias));
            }
        }
        return configs;
    }

    private EmbeddingStore<TextSegment> createStore(MilvusStoreConfig config) {
        MilvusEmbeddingStore.Builder builder = MilvusEmbeddingStore.builder()
                .host(config.getHost())
                .port(config.getPort())
                .collectionName(config.getCollection())
                .dimension(props.getEmbedding().getDimension());
        if (StringUtils.hasText(config.getUsername())) {
            builder.username(config.getUsername());
        }
        if (StringUtils.hasText(config.getPassword())) {
            builder.password(config.getPassword());
        }
        return builder.build();
    }

    MilvusStoreConfig tenantScopedConfig(MilvusStoreConfig config) {
        Long tenantId = TenantContextHolder.currentTenantId().orElse(null);
        return copyWithCollection(config, collectionResolver.collectionForTenant(config.getCollection(), tenantId));
    }

    private MilvusStoreConfig copyWithCollection(MilvusStoreConfig config, String collection) {
        return MilvusStoreConfig.builder()
                .alias(config.getAlias())
                .host(config.getHost())
                .port(config.getPort())
                .collection(collection)
                .topK(config.getTopK())
                .minScore(config.getMinScore())
                .username(config.getUsername())
                .password(config.getPassword())
                .build();
    }

    private String storeCacheKey(MilvusStoreConfig config) {
        return config.getAlias() + "\u001F" + config.getCollection();
    }

    private void validate(MilvusStoreConfig config) {
        if (!StringUtils.hasText(config.getAlias())) {
            throw new IllegalArgumentException("alias must not be blank");
        }
        if (!StringUtils.hasText(config.getHost())) {
            throw new IllegalArgumentException("host must not be blank");
        }
        if (!StringUtils.hasText(config.getCollection())) {
            throw new IllegalArgumentException("collection must not be blank");
        }
        if (config.getPort() <= 0) {
            throw new IllegalArgumentException("port must be greater than 0");
        }
        if (config.getTopK() <= 0) {
            throw new IllegalArgumentException("topK must be greater than 0");
        }
    }

    private String configKey(String alias) {
        return CONFIG_KEY_PREFIX + alias;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private int intValue(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private double doubleValue(Object value, double defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return Double.parseDouble(String.valueOf(value));
    }
}
