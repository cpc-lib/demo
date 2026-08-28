package com.example.versionedcachemp.service;

import com.example.versionedcachemp.cache.UserCacheValue;
import com.example.versionedcachemp.config.MqConfig;
import com.example.versionedcachemp.domain.UserAccount;
import com.example.versionedcachemp.mapper.UserAccountMapper;
import com.example.versionedcachemp.mq.UserUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserAccountService {

    private final UserAccountMapper userMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final AmqpTemplate amqpTemplate;

    private String buildKey(Long id) {
        return "user:" + id;
    }

    /**
     * 读取用户（带缓存）
     */
    @Transactional(readOnly = true)
    public UserCacheValue getUser(Long id) {
        String key = buildKey(id);

        Object obj = redisTemplate.opsForValue().get(key);
        if (obj instanceof UserCacheValue) {
            UserCacheValue cache = (UserCacheValue) obj;
            log.info("[CACHE HIT] key={}, version={}", key, cache.getVersion());
            return cache;
        }

        log.info("[CACHE MISS] key={}", key);

        UserAccount user = userMapper.selectById(id);
        if (user == null) {
            throw new IllegalArgumentException("User not found: " + id);
        }

        UserCacheValue value = toCache(user);

        redisTemplate.opsForValue().set(key, value);
        log.info("[CACHE FILL] key={}, version={}", key, value.getVersion());

        return value;
    }

    /**
     * 更新余额 + 发送 MQ 👉 由消费者异步刷新缓存
     */
    @Transactional
    public UserCacheValue increaseBalance(Long id, BigDecimal amount) {
        int updated = userMapper.increaseBalance(id, amount);
        if (updated == 0) {
            throw new IllegalStateException("Update failed (maybe concurrent update)");
        }

        UserAccount user = userMapper.selectById(id);

        // MQ 通知异步刷新缓存
        UserUpdatedEvent event = new UserUpdatedEvent(user.getId(), user.getVersion());
        amqpTemplate.convertAndSend(
                MqConfig.EXCHANGE_USER,
                MqConfig.ROUTING_USER_UPDATED,
                event
        );

        log.info("[MQ SENT] id={}, version={}", user.getId(), user.getVersion());

        return toCache(user);
    }

    /**
     * Versioned Cache：比较版本号后写缓存
     */
    public void writeCacheWithVersionCheck(UserAccount latest) {
        String key = buildKey(latest.getId());

        Object obj = redisTemplate.opsForValue().get(key);
        long newVer = Optional.ofNullable(latest.getVersion()).orElse(0L);

        if (obj instanceof UserCacheValue) {
            UserCacheValue current = (UserCacheValue) obj;

            if (newVer < current.getVersion()) {
                log.warn("[CACHE SKIP OUTDATED] key={}, new={}, current={}",
                        key, newVer, current.getVersion());
                return;
            }

            log.info("[CACHE UPDATE] key={}, new={}, old={}", key, newVer, current.getVersion());
        } else {
            log.info("[CACHE WRITE] key={}, version={}", key, newVer);
        }

        redisTemplate.opsForValue().set(key, toCache(latest));
    }

    private UserCacheValue toCache(UserAccount user) {
        return new UserCacheValue(
                user.getId(),
                user.getUsername(),
                user.getBalance(),
                user.getVersion()
        );
    }
}
