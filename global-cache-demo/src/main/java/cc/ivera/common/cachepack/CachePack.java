package cc.ivera.common.cachepack;

import java.time.Duration;

public interface CachePack {
    /**
     * 向缓存写入数据
     * @param cacheKey
     * @param cacheValue
     * @param duration 缓存时间
     */
    void writeCacheData(String cacheKey, String cacheValue, Duration duration);

    /**
     * 缓存获取数据
     * @param cacheKey
     * @return
     */
    String getCacheData(String cacheKey);
}
