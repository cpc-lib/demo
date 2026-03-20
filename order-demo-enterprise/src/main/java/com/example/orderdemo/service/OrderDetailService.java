package com.example.orderdemo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.orderdemo.domain.dto.OrderDetailDTO;
import com.example.orderdemo.domain.dto.OrderItemDTO;
import com.example.orderdemo.domain.entity.OrderEntity;
import com.example.orderdemo.domain.entity.OrderItemEntity;
import com.example.orderdemo.infrastructure.cache.OrderCacheKeys;
import com.example.orderdemo.mapper.OrderItemMapper;
import com.example.orderdemo.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderDetailService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final long ttlSeconds;

    public OrderDetailService(OrderMapper orderMapper,
                              OrderItemMapper orderItemMapper,
                              RedisTemplate<String, Object> redisTemplate,
                              @Value("${app.cache.order-detail-ttl-seconds}") long ttlSeconds) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.redisTemplate = redisTemplate;
        this.ttlSeconds = ttlSeconds;
    }

    public OrderDetailDTO getOrderDetail(long orderId) {
        // 1) version key：不存在则初始化为 1（Uber 版本缓存核心）
        String vKey = OrderCacheKeys.versionKey(orderId);
        Boolean ok = redisTemplate.opsForValue().setIfAbsent(vKey, 1L, Duration.ofDays(30));
        Object obj = redisTemplate.opsForValue().get(vKey);
        Long ver = (obj == null) ? null : ((Number) obj).longValue();
        if (ver == null) {
            // 极端并发兜底
            redisTemplate.opsForValue().set(vKey, 1L, Duration.ofDays(30));
            ver = 1L;
        }

        // 2) 带版本的缓存 key
        String dKey = OrderCacheKeys.detailKey(orderId, ver);
        Object cached = redisTemplate.opsForValue().get(dKey);
        if (cached instanceof OrderDetailDTO) {
            return (OrderDetailDTO) cached;
        }

        // 3) 回源 DB
        OrderDetailDTO fresh = loadFromDb(orderId);
        // 4) 写入当前版本 key（不删旧缓存，靠 TTL 自然淘汰 + version bump 切换）
        redisTemplate.opsForValue().set(dKey, fresh, Duration.ofSeconds(ttlSeconds));
        return fresh;
    }

    @Transactional
    public OrderDetailDTO loadFromDb(long orderId) {
        OrderEntity order = orderMapper.selectById(orderId);
        if (order == null) throw new IllegalArgumentException("order not found: " + orderId);

        List<OrderItemEntity> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItemEntity>()
                .eq(OrderItemEntity::getOrderId, orderId));

        OrderDetailDTO dto = new OrderDetailDTO();
        dto.setOrderId(order.getId());
        dto.setUserId(order.getUserId());
        dto.setStatus(order.getStatus());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());
        dto.setItems(items.stream().map(x -> {
            OrderItemDTO i = new OrderItemDTO();
            i.setSkuId(x.getSkuId());
            i.setTitle(x.getTitle());
            i.setPrice(x.getPrice());
            i.setQuantity(x.getQuantity());
            return i;
        }).collect(Collectors.toList()));

        return dto;
    }

    /**
     * 更新后调用：version bump（切换到新 key，避免脏读/并发覆盖）
     */
    public long bumpVersion(long orderId) {
        String vKey = OrderCacheKeys.versionKey(orderId);
        Long ver = redisTemplate.opsForValue().increment(vKey);
        if (ver == null) throw new IllegalStateException("version increment failed");
        return ver;
    }
}
