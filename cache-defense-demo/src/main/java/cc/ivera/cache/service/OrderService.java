
package cc.ivera.cache.service;

import cc.ivera.cache.entity.Order;
import cc.ivera.cache.mapper.OrderMapper;
import cc.ivera.cache.support.OrderBloomFilterSupport;
import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final String NULL_CACHE_VALUE = "NULL";
    private static final int CACHE_TTL_MINUTES = 30;
    private static final int CACHE_TTL_JITTER_MINUTES = 10;

    private final StringRedisTemplate redisTemplate;
    private final OrderMapper orderMapper;
    private final RedissonClient redissonClient;
    private final OrderBloomFilterSupport orderBloomFilter;

    public Order getById(Long id) {

        //无效参数拒绝
        if (id == null || id <= 0) {
            throw new ResponseStatusException(BAD_REQUEST, "order id must be greater than 0");
        }

        String key = "order:" + id;
        String cache = redisTemplate.opsForValue().get(key);
        if (cache != null) {
            return parseCacheValue(cache, id);
        }

        //bloom filter不存在一定不存在，存在可能存在
        if (!orderBloomFilter.mightContain(id)) {
            throw orderNotFound(id);
        }

        //在获取缓存之前加锁，保护，先读取，无法确定处理顺序，依然可能请求到db，导致重复构建缓存
        RLock lock = redissonClient.getLock("lock:" + id);
        lock.lock(10, TimeUnit.SECONDS);
        try {

            cache = redisTemplate.opsForValue().get(key);
            if (cache != null) {
                return parseCacheValue(cache, id);
            }

            Order order = orderMapper.selectById(id);

            if (order == null) {
                //缓存空值（穿透处理方案，缓存空值）
                cacheNullValue(key);
                throw orderNotFound(id);
            }

            cacheOrder(order);

            return order;

        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Transactional
    public Order create(Order order) {
        if (order == null || !StringUtils.hasText(order.getName())) {
            throw new ResponseStatusException(BAD_REQUEST, "order name must not be blank");
        }

        Order newOrder = new Order();
        newOrder.setName(order.getName().trim());
        orderMapper.insert(newOrder);

        orderBloomFilter.add(newOrder.getId());

        //事务提交后将数据写入缓存
        cacheOrderAfterCommit(newOrder);

        return newOrder;
    }

    private Order parseCacheValue(String cache, Long id) {
        if (NULL_CACHE_VALUE.equals(cache)) {
            throw orderNotFound(id);
        }
        return JSON.parseObject(cache, Order.class);
    }

    private ResponseStatusException orderNotFound(Long id) {
        return new ResponseStatusException(NOT_FOUND, "order not found: " + id);
    }

    private void cacheOrderAfterCommit(Order order) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            cacheOrder(order);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                cacheOrder(order);
            }
        });
    }

    private void cacheOrder(Order order) {
        //随机ttl，避免大量key失效导致雪崩
        int ttl = CACHE_TTL_MINUTES + ThreadLocalRandom.current().nextInt(CACHE_TTL_JITTER_MINUTES);
        redisTemplate.opsForValue().set(
                "order:" + order.getId(),
                JSON.toJSONString(order),
                ttl,
                TimeUnit.MINUTES
        );
    }

    private void cacheNullValue(String key) {
        redisTemplate.opsForValue().set(key, NULL_CACHE_VALUE, 120, TimeUnit.SECONDS);
    }

}
