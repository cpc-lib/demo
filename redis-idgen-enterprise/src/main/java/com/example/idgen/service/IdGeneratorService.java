package com.example.idgen.service;

import com.example.idgen.config.IdGenProperties;
import com.example.idgen.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Enterprise-grade service:
 * 1) Trend-ordered ID = time(seconds) + tenantId + bizId + seq
 * 2) Redis segment per (tenantId, bizKey, epochSecond)
 * 3) Emergency reserve segments kept when Redis is healthy
 * 4) Hard fallback (Redis down + reserve exhausted): local nodeId-based generator (still unique across nodes if nodeId is unique)
 */
@Service
public class IdGeneratorService {
    private static final Logger log = LoggerFactory.getLogger(IdGeneratorService.class);

    private final RedisSegmentAllocator allocator;
    private final IdComposer composer;
    private final IdGenProperties props;
    private final Clock clock = Clock.systemUTC();

    private final ExecutorService prefetchPool;

    /**
     * Buffers per key: tenant|biz|epochSecond
     */
    private final ConcurrentHashMap<String, SegmentBuffer> buffers = new ConcurrentHashMap<>();

    /**
     * Emergency reserve per key: tenant|biz (cross seconds) - keep a couple segments ready for current second when healthy.
     * We'll store per second key reserves too; simplest keep per full counterKey.
     */
    private final ConcurrentHashMap<String, EmergencyReserve> reserves = new ConcurrentHashMap<>();


    public IdGeneratorService(RedisSegmentAllocator allocator, IdComposer composer, IdGenProperties props) {
        this.allocator = Objects.requireNonNull(allocator, "allocator");
        this.composer = Objects.requireNonNull(composer, "composer");
        this.props = Objects.requireNonNull(props, "props");

        this.prefetchPool = new ThreadPoolExecutor(
                2, 8, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(2000),
                r -> {
                    Thread t = new Thread(r);
                    t.setName("idgen-prefetch-" + t.getId());
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    public long nextId(int tenantId, String bizKey) {
        long nowSec = clock.instant().getEpochSecond();

        String counterKey = counterKey(tenantId, bizKey, nowSec);

        long seq = nextSeqWithDegrade(counterKey);
        return composer.compose(nowSec, tenantId, bizKey, seq);
    }

    private long nextSeqWithDegrade(String counterKey) {
        int step = props.getStep();
        double threshold = props.getPrefetchThreshold();
        Duration wait = props.getFetchTimeout();

        SegmentBuffer buf = buffers.computeIfAbsent(counterKey, k -> new SegmentBuffer(prefetchPool));
        EmergencyReserve reserve = reserves.computeIfAbsent(counterKey, k -> new EmergencyReserve(props.getEmergencySegments()));

        try {
            // Allocate from Redis segments
            return buf.nextSeq(() -> {
                Segment seg = allocator.allocate(counterKey, step);

                // When Redis is healthy, keep extra reserve by allocating one more segment occasionally
                // Strategy: every successful allocation, try to top-up reserve if not full.
                if (reserve.size() < props.getEmergencySegments()) {
                    // best-effort: allocate another segment as reserve
                    try {
                        Segment extra = allocator.allocate(counterKey, step);
                        reserve.offer(extra);
                    } catch (Exception ignore) {
                        // ignore reserve failure
                    }
                }
                return seg;
            }, threshold, wait);
        } catch (Exception redisErr) {
            // Redis allocation failed; try reserve
            Segment reserved = reserve.poll();
            if (reserved != null) {
                long v = reserved.next();
                if (v != -1L) {
                    log.warn("Redis down/slow. Using emergency reserve for key={}, reserveLeft={}", counterKey, reserve.size());
                    return v;
                }
            }

            // Financial-grade: never generate IDs outside Redis-allocated segments.
            // When Redis is unavailable AND reserve is exhausted, FAIL FAST.
            throw new IdGenUnavailableException("ID generation unavailable: Redis is down and emergency reserve exhausted for key=" + counterKey, redisErr);
        }
    }

    private long seed() {
        // Randomize start to reduce collision risk within a node (still unique with nodeId).
        return ThreadLocalRandom.current().nextInt(0, 1 << 16);
    }

    private String counterKey(int tenantId, String bizKey, long epochSecond) {
        // per-second counter key for trend order and avoiding per-ms Redis load
        // idgen:{tenantId}:{bizKey}:{epochSecond}:counter
        return props.getRedisPrefix() + tenantId + ":" + bizKey + ":" + epochSecond + ":counter";
    }
}
