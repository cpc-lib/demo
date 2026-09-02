package cc.ivera.service.impl;

import cc.ivera.entity.InventoryOperation;
import cc.ivera.entity.OrderInfo;
import cc.ivera.entity.OrderItem;
import cc.ivera.entity.Product;
import cc.ivera.enums.InventoryStatus;
import cc.ivera.enums.OrderStatus;
import cc.ivera.enums.ProductStatus;
import cc.ivera.exception.ConflictException;
import cc.ivera.mapper.InventoryOperationMapper;
import cc.ivera.mapper.OrderInfoMapper;
import cc.ivera.mapper.OrderItemMapper;
import cc.ivera.mapper.ProductMapper;
import cc.ivera.service.InventoryService;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InventoryConcurrencyTest {

    @Test
    void tenUnitsAllowExactlyTenOfFiftyConcurrentReservations() throws Exception {
        OrderInfoMapper orderInfoMapper = mock(OrderInfoMapper.class);
        OrderItemMapper orderItemMapper = mock(OrderItemMapper.class);
        ProductMapper productMapper = mock(ProductMapper.class);
        InventoryOperationMapper operationMapper = mock(InventoryOperationMapper.class);
        InventoryService inventoryService = new InventoryServiceImpl(
                orderInfoMapper,
                orderItemMapper,
                productMapper,
                operationMapper
        );
        AtomicInteger available = new AtomicInteger(10);
        when(productMapper.selectByIdForUpdate(7L)).thenAnswer(invocation -> productWithTenAvailable());
        when(productMapper.reserveStock(7L, 1)).thenAnswer(invocation -> reserveOne(available));
        when(operationMapper.insert(any(InventoryOperation.class))).thenReturn(1);

        int attempts = 50;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(16);
        try {
            List<Callable<Boolean>> calls = IntStream.range(0, attempts)
                    .mapToObj(index -> (Callable<Boolean>) () -> {
                        start.await();
                        try {
                            inventoryService.reserve(
                                    order((long) index),
                                    Collections.singletonList(item((long) index))
                            );
                            return true;
                        } catch (ConflictException expected) {
                            return false;
                        }
                    })
                    .collect(Collectors.toList());
            List<Future<Boolean>> futures = calls.stream().map(pool::submit).collect(Collectors.toList());
            start.countDown();

            int successes = 0;
            for (Future<Boolean> future : futures) {
                if (future.get(10, TimeUnit.SECONDS)) {
                    successes++;
                }
            }

            assertEquals(10, successes);
            assertEquals(0, available.get());
            verify(productMapper, times(attempts)).reserveStock(7L, 1);
            verify(operationMapper, times(10)).insert(any(InventoryOperation.class));
        } finally {
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    private int reserveOne(AtomicInteger available) {
        while (true) {
            int current = available.get();
            if (current < 1) {
                return 0;
            }
            if (available.compareAndSet(current, current - 1)) {
                return 1;
            }
        }
    }

    private OrderInfo order(Long index) {
        OrderInfo order = new OrderInfo();
        order.setId(index + 1);
        order.setOrderNo("ORDER-" + index);
        order.setOrderStatus(OrderStatus.NOTPAY.getType());
        return order;
    }

    private OrderItem item(Long index) {
        OrderItem item = new OrderItem();
        item.setId(index + 1);
        item.setOrderId(index + 1);
        item.setProductId(7L);
        item.setQuantity(1);
        item.setInventoryStatus(InventoryStatus.RESERVED);
        item.setRefundedQuantity(0);
        return item;
    }

    private Product productWithTenAvailable() {
        Product product = new Product();
        product.setId(7L);
        product.setStatus(ProductStatus.ON_SHELF);
        product.setAvailableStock(10);
        product.setLockedStock(0);
        product.setSoldStock(0);
        return product;
    }
}
