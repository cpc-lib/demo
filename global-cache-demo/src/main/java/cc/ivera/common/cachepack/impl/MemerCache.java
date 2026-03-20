package cc.ivera.common.cachepack.impl;

import cc.ivera.common.cachepack.CachePack;

import java.time.Duration;

/**
 * @author Administrator
 * @version 1.0
 * @description: TODO
 * @date 2021/9/12 16:34
 */
public class MemerCache implements CachePack {
    @Override
    public void writeCacheData(String redisKey, String redisValue, Duration duration) {
        System.err.println("向memacache中放置缓存");
    }

    @Override
    public String getCacheData(String redisKey) {
        System.err.println("向memacache中取缓存");
        return null;
    }
}
