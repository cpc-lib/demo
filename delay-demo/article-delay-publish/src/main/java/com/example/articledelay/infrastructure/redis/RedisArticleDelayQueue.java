package com.example.articledelay.infrastructure.redis;

import com.example.articledelay.domain.DelayTask;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class RedisArticleDelayQueue {

    static final String SCHEDULED_KEY = "article:delay:scheduled";
    static final String PROCESSING_KEY = "article:delay:processing";

    private static final DefaultRedisScript<Long> ENQUEUE_IF_UNKNOWN_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('ZSCORE', KEYS[1], ARGV[1]) or redis.call('ZSCORE', KEYS[2], ARGV[1]) then
                return 0
            end
            redis.call('ZADD', KEYS[1], ARGV[2], ARGV[1])
            return 1
            """, Long.class);

    @SuppressWarnings("rawtypes")
    private static final DefaultRedisScript<List> CLAIM_SCRIPT = new DefaultRedisScript<>("""
            local items = redis.call('ZRANGEBYSCORE', KEYS[1], '-inf', ARGV[1], 'LIMIT', 0, ARGV[2])
            local claimed = {}
            for _, member in ipairs(items) do
                if redis.call('ZREM', KEYS[1], member) == 1 then
                    redis.call('ZADD', KEYS[2], ARGV[3], member)
                    table.insert(claimed, member)
                end
            end
            return claimed
            """, List.class);

    private static final DefaultRedisScript<Long> ACK_SCRIPT = new DefaultRedisScript<>("""
            redis.call('ZREM', KEYS[1], ARGV[1])
            redis.call('ZREM', KEYS[2], ARGV[1])
            return 1
            """, Long.class);

    private static final DefaultRedisScript<Long> NACK_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('ZREM', KEYS[2], ARGV[1]) == 1 then
                redis.call('ZADD', KEYS[1], ARGV[2], ARGV[1])
                return 1
            end
            return 0
            """, Long.class);

    @SuppressWarnings("rawtypes")
    private static final DefaultRedisScript<List> RECOVER_EXPIRED_SCRIPT = new DefaultRedisScript<>("""
            local items = redis.call('ZRANGEBYSCORE', KEYS[2], '-inf', ARGV[1], 'LIMIT', 0, ARGV[2])
            local recovered = {}
            for _, member in ipairs(items) do
                if redis.call('ZREM', KEYS[2], member) == 1 then
                    redis.call('ZADD', KEYS[1], ARGV[1], member)
                    table.insert(recovered, member)
                end
            end
            return recovered
            """, List.class);

    private final StringRedisTemplate redisTemplate;

    public RedisArticleDelayQueue(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean enqueueIfUnknown(DelayTask task) {
        Long result = redisTemplate.execute(
                ENQUEUE_IF_UNKNOWN_SCRIPT,
                List.of(SCHEDULED_KEY, PROCESSING_KEY),
                task.member(),
                Long.toString(task.publishAt().toEpochMilli())
        );
        return Long.valueOf(1L).equals(result);
    }

    public void removeScheduled(DelayTask task) {
        redisTemplate.opsForZSet().remove(SCHEDULED_KEY, task.member());
    }

    public List<DelayTask> claimDue(Instant now, int limit, Duration lease) {
        long leaseUntil = now.plus(lease).toEpochMilli();
        List<String> members = executeList(
                CLAIM_SCRIPT,
                List.of(SCHEDULED_KEY, PROCESSING_KEY),
                Long.toString(now.toEpochMilli()),
                Integer.toString(limit),
                Long.toString(leaseUntil)
        );
        return decode(members);
    }

    public void ack(DelayTask task) {
        redisTemplate.execute(
                ACK_SCRIPT,
                List.of(PROCESSING_KEY, SCHEDULED_KEY),
                task.member()
        );
    }

    public void nack(DelayTask task, Instant retryAt) {
        redisTemplate.execute(
                NACK_SCRIPT,
                List.of(SCHEDULED_KEY, PROCESSING_KEY),
                task.member(),
                Long.toString(retryAt.toEpochMilli())
        );
    }

    public List<DelayTask> recoverExpired(Instant now, int limit) {
        List<String> members = executeList(
                RECOVER_EXPIRED_SCRIPT,
                List.of(SCHEDULED_KEY, PROCESSING_KEY),
                Long.toString(now.toEpochMilli()),
                Integer.toString(limit)
        );
        return decode(members);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private List<String> executeList(DefaultRedisScript<List> script, List<String> keys, String... args) {
        List raw = redisTemplate.execute(script, keys, (Object[]) args);
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>(raw.size());
        for (Object item : raw) {
            result.add(String.valueOf(item));
        }
        return result;
    }

    private List<DelayTask> decode(List<String> members) {
        return members.stream().map(DelayTask::fromMember).toList();
    }
}
