package cc.ivera.ragdemo.service.vector;

import cc.ivera.ragdemo.config.RagProperties;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class DynamicMilvusStoreManager {

    private static final String ACTIVE_ALIAS_KEY = "rag:milvus:active";
    private static final String ALIAS_SET_KEY = "rag:milvus:aliases";
    private static final String CONFIG_KEY_PREFIX = "rag:milvus:config:";

    private final RagProperties props;
    private final StringRedisTemplate redisTemplate;

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
                    .build());
        }

        String activeAlias = redisTemplate.opsForValue().get(ACTIVE_ALIAS_KEY);
        if (!StringUtils.hasText(activeAlias)) {
            redisTemplate.opsForValue().set(ACTIVE_ALIAS_KEY, defaultAlias);
        }
    }

    public ActiveMilvusContext current() {
        String alias = currentAlias();
        MilvusStoreConfig config = load(alias);
        EmbeddingStore<TextSegment> store = storeCache.computeIfAbsent(alias, k -> createStore(config));
        return new ActiveMilvusContext(alias, config, store);
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
        redisTemplate.opsForSet().add(ALIAS_SET_KEY, config.getAlias());
        storeCache.remove(config.getAlias());
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
        return MilvusEmbeddingStore.builder()
                .host(config.getHost())
                .port(config.getPort())
                .collectionName(config.getCollection())
                .dimension(props.getEmbedding().getDimension())
                .build();
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
