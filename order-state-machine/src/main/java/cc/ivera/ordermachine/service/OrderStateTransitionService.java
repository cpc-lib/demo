package cc.ivera.ordermachine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cc.ivera.ordermachine.domain.entity.OrderStateTransition;
import cc.ivera.ordermachine.mapper.OrderStateTransitionMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class OrderStateTransitionService {

    private final OrderStateTransitionMapper transitionMapper;

    private final Cache<String, OrderStateTransition> transitionCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(10))
            .build();

    public OrderStateTransition getTransition(String businessType, String currentStatus, String event) {
        String cacheKey = buildKey(businessType, currentStatus, event);
        return transitionCache.get(cacheKey, key -> transitionMapper.selectOne(
                new LambdaQueryWrapper<OrderStateTransition>()
                        .eq(OrderStateTransition::getBusinessType, businessType)
                        .eq(OrderStateTransition::getCurrentStatus, currentStatus)
                        .eq(OrderStateTransition::getEvent, event)
                        .eq(OrderStateTransition::getIsEnabled, 1)
                        .last("limit 1")
        ));
    }

    public void evict(String businessType, String currentStatus, String event) {
        transitionCache.invalidate(buildKey(businessType, currentStatus, event));
    }

    public void clearAll() {
        transitionCache.invalidateAll();
    }

    private String buildKey(String businessType, String currentStatus, String event) {
        return businessType + ":" + currentStatus + ":" + event;
    }
}
