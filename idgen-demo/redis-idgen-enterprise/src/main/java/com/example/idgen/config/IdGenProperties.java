package com.example.idgen.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Enterprise Redis segment ID generator properties (single app).
 */
@ConfigurationProperties(prefix = "idgen")
public class IdGenProperties {

    /**
     * Custom epoch (seconds) for time-part. Default: 2025-01-01 00:00:00 UTC.
     * You can change it, but DON'T change after going live.
     */
    private long epochSeconds = 1735689600L;

    /**
     * Bits allocation (sum must be 64):
     * timeBits + tenantBits + bizBits + seqBits = 64
     */
    private int timeBits = 30;     // ~34 years
    private int tenantBits = 12;   // 0-4095 tenants
    private int bizBits = 8;       // 0-255 biz types
    private int seqBits = 14;      // 0-16383 ids per second per tenant+biz

    /**
     * Redis key prefix.
     */
    private String redisPrefix = "idgen:";

    /**
     * Segment step (how many sequences to allocate each time from Redis) for per-second counters.
     * Keep it small enough to avoid wasting too much on second switching, but large enough to reduce Redis calls.
     */
    private int step = 2000;

    /**
     * Prefetch when remaining <= size * threshold.
     */
    private double prefetchThreshold = 0.2d;

    /**
     * Wait best-effort for prefetch future on switch; then fallback to sync fetch.
     */
    private Duration fetchTimeout = Duration.ofMillis(1500);

    /**
     * Emergency cached segments per key (tenant+biz+second). When Redis is healthy, prefetch & keep extra reserve.
     * When Redis is down, continue to use reserve segments.
     */
    private int emergencySegments = 2;

    /**
     * Financial-grade mode: NEVER generate IDs outside Redis-allocated segments.
     * If Redis is unavailable and emergency reserve is exhausted, FAIL FAST (throw exception).
     */
    private boolean failFast = true;

    /**
     * Node id for hard fallback mode when Redis down AND reserve segments are exhausted.
     * Range: [0, 1023] recommended. Used only in fallback IDs to avoid collision across nodes.
     */
    private int nodeId = 1;

    /**
     * Optional explicit bizKey -> bizId mapping (0..(2^bizBits-1)). Preferred for avoiding hash collisions.
     * Example:
     *   idgen.biz-map.order=1
     *   idgen.biz-map.refund=2
     */
    private Map<String, Integer> bizMap = new HashMap<>();

    // getters/setters

    public long getEpochSeconds() { return epochSeconds; }
    public void setEpochSeconds(long epochSeconds) { this.epochSeconds = epochSeconds; }

    public int getTimeBits() { return timeBits; }
    public void setTimeBits(int timeBits) { this.timeBits = timeBits; }

    public int getTenantBits() { return tenantBits; }
    public void setTenantBits(int tenantBits) { this.tenantBits = tenantBits; }

    public int getBizBits() { return bizBits; }
    public void setBizBits(int bizBits) { this.bizBits = bizBits; }

    public int getSeqBits() { return seqBits; }
    public void setSeqBits(int seqBits) { this.seqBits = seqBits; }

    public String getRedisPrefix() { return redisPrefix; }
    public void setRedisPrefix(String redisPrefix) { this.redisPrefix = redisPrefix; }

    public int getStep() { return step; }
    public void setStep(int step) { this.step = step; }

    public double getPrefetchThreshold() { return prefetchThreshold; }
    public void setPrefetchThreshold(double prefetchThreshold) { this.prefetchThreshold = prefetchThreshold; }

    public Duration getFetchTimeout() { return fetchTimeout; }
    public void setFetchTimeout(Duration fetchTimeout) { this.fetchTimeout = fetchTimeout; }

    public int getEmergencySegments() { return emergencySegments; }
    public void setEmergencySegments(int emergencySegments) { this.emergencySegments = emergencySegments; }

    public boolean isFailFast() { return failFast; }
    public void setFailFast(boolean failFast) { this.failFast = failFast; }

    public int getNodeId() { return nodeId; }
    public void setNodeId(int nodeId) { this.nodeId = nodeId; }

    public Map<String, Integer> getBizMap() { return bizMap; }
    public void setBizMap(Map<String, Integer> bizMap) { this.bizMap = bizMap; }
}
