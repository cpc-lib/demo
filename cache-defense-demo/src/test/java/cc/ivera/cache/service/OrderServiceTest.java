package cc.ivera.cache.service;

import cc.ivera.cache.entity.Order;
import cc.ivera.cache.mapper.OrderMapper;
import cc.ivera.cache.support.OrderBloomFilterSupport;
import com.alibaba.fastjson2.JSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class OrderServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock lock;

    @Mock
    private OrderBloomFilterSupport orderBloomFilter;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(redisTemplate, orderMapper, redissonClient, orderBloomFilter);
    }

    @Test
    void rejectsInvalidId() {
        assertThatThrownBy(() -> orderService.getById(0L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST")
                .hasMessageContaining("order id must be greater than 0");
        verifyNoInteractions(redisTemplate, orderMapper, redissonClient, orderBloomFilter);
    }

    @Test
    void throwsNotFoundWhenBloomRejectsId() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("order:99")).thenReturn(null);
        when(orderBloomFilter.mightContain(99L)).thenReturn(false);

        assertThatThrownBy(() -> orderService.getById(99L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND")
                .hasMessageContaining("order not found: 99");

        verifyNoInteractions(orderMapper, redissonClient);
    }

    @Test
    void returnsCachedOrderWhenCacheHit() {
        Order cached = order(1L, "订单1");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("order:1")).thenReturn(JSON.toJSONString(cached));

        Order result = orderService.getById(1L);

        assertThat(result).usingRecursiveComparison().isEqualTo(cached);
        verifyNoInteractions(orderMapper, redissonClient, orderBloomFilter);
    }

    @Test
    void throwsNotFoundWhenNullMarkerCached() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("order:2")).thenReturn("NULL");

        assertThatThrownBy(() -> orderService.getById(2L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND")
                .hasMessageContaining("order not found: 2");

        verifyNoInteractions(orderMapper, redissonClient, orderBloomFilter);
    }

    @Test
    void loadsFromDatabaseAndCachesResultOnMiss() {
        Order dbOrder = order(3L, "订单3");
        when(orderBloomFilter.mightContain(3L)).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("order:3")).thenReturn((String) null, (String) null);
        when(redissonClient.getLock("lock:3")).thenReturn(lock);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        when(orderMapper.selectById(3L)).thenReturn(dbOrder);

        Order result = orderService.getById(3L);

        assertThat(result).usingRecursiveComparison().isEqualTo(dbOrder);
        verify(lock).lock(10, TimeUnit.SECONDS);

        ArgumentCaptor<Long> ttlCaptor = ArgumentCaptor.forClass(Long.class);
        verify(valueOperations).set(eq("order:3"), eq(JSON.toJSONString(dbOrder)), ttlCaptor.capture(), eq(TimeUnit.MINUTES));
        assertThat(ttlCaptor.getValue()).isBetween(30L, 39L);
        verify(lock).unlock();
    }

    @Test
    void cachesNullMarkerAndThrowsNotFoundWhenDatabaseHasNoRecord() {
        when(orderBloomFilter.mightContain(4L)).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("order:4")).thenReturn((String) null, (String) null);
        when(redissonClient.getLock("lock:4")).thenReturn(lock);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        assertThatThrownBy(() -> orderService.getById(4L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND")
                .hasMessageContaining("order not found: 4");

        verify(valueOperations).set("order:4", "NULL", 120, TimeUnit.SECONDS);
        verify(lock).unlock();
    }

    @Test
    void createAddsIdToBloomAndCachesAfterCommit() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(orderMapper.insert(any(Order.class))).thenAnswer(invocation -> {
            Order inserted = invocation.getArgument(0);
            inserted.setId(5L);
            return 1;
        });

        TransactionSynchronizationManager.initSynchronization();
        try {
            Order result = orderService.create(order(null, " 新订单 "));

            assertThat(result.getId()).isEqualTo(5L);
            assertThat(result.getName()).isEqualTo("新订单");
            verify(orderBloomFilter).add(5L);
            verify(valueOperations, never()).set(eq("order:5"), any(String.class), any(Long.class), eq(TimeUnit.MINUTES));

            for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
                synchronization.afterCommit();
            }

            ArgumentCaptor<Long> ttlCaptor = ArgumentCaptor.forClass(Long.class);
            verify(valueOperations).set(eq("order:5"), eq(JSON.toJSONString(result)), ttlCaptor.capture(), eq(TimeUnit.MINUTES));
            assertThat(ttlCaptor.getValue()).isBetween(30L, 39L);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void createRejectsBlankName() {
        assertThatThrownBy(() -> orderService.create(order(null, "   ")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST");
    }

    private Order order(Long id, String name) {
        Order order = new Order();
        order.setId(id);
        order.setName(name);
        return order;
    }
}
