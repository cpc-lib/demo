package cc.ivera.service.impl;

import cc.ivera.config.PaymentAppConfig;
import cc.ivera.config.PaymentConfigLoader;
import cc.ivera.config.WxPayConfig;
import cc.ivera.entity.OrderInfo;
import cc.ivera.enums.OrderStatus;
import cc.ivera.enums.PayType;
import cc.ivera.lock.DistributedLockTemplate;
import cc.ivera.service.InventoryService;
import cc.ivera.service.OrderInfoService;
import cc.ivera.service.PaymentInfoService;
import cc.ivera.service.impl.wxpay.WxPayHttpClient;
import cc.ivera.service.impl.wxpay.WxPayNotificationDecoder;
import cc.ivera.service.impl.wxpay.WxPayOrderService;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class OrderTransitionConcurrencyTest {

    @Test
    void paymentAndCloseRaceProducesExactlyOneInventoryTransition() throws Exception {
        for (int iteration = 0; iteration < 20; iteration++) {
            runOneRace(iteration);
        }
    }

    @SuppressWarnings("unchecked")
    private void runOneRace(int iteration) throws Exception {
        String orderNo = "ORDER-RACE-" + iteration;
        AtomicReference<String> orderStatus = new AtomicReference<>(OrderStatus.NOTPAY.getType());
        AtomicInteger commits = new AtomicInteger();
        AtomicInteger releases = new AtomicInteger();
        OrderInfoService orderInfoService = mock(OrderInfoService.class);
        when(orderInfoService.getOrderByOrderNo(orderNo))
                .thenAnswer(invocation -> order(orderNo, orderStatus.get()));
        when(orderInfoService.getOrderByOrderNoForUpdate(orderNo))
                .thenAnswer(invocation -> order(orderNo, orderStatus.get()));
        when(orderInfoService.getOrderStatus(orderNo)).thenAnswer(invocation -> orderStatus.get());
        when(orderInfoService.updateStatusByOrderNoIfStatus(
                org.mockito.ArgumentMatchers.eq(orderNo),
                any(OrderStatus.class),
                any(OrderStatus.class)
        )).thenAnswer(invocation -> {
            OrderStatus expected = invocation.getArgument(1);
            OrderStatus target = invocation.getArgument(2);
            return orderStatus.compareAndSet(expected.getType(), target.getType());
        });

        InventoryService inventoryService = mock(InventoryService.class);
        when(inventoryService.commitPayment(orderNo)).thenAnswer(invocation -> {
            commits.incrementAndGet();
            return true;
        });
        when(inventoryService.releaseReservation(orderNo)).thenAnswer(invocation -> {
            releases.incrementAndGet();
            return true;
        });

        PaymentConfigLoader loader = mock(PaymentConfigLoader.class);
        PaymentAppConfig config = new PaymentAppConfig();
        config.setChannelCode(PaymentConfigLoader.CHANNEL_WXPAY);
        config.setAppid("wx-app-1");
        config.setMchId("wx-mch-1");
        when(loader.getRequiredAppConfig(9L)).thenReturn(config);
        WxPayNotificationDecoder decoder = mock(WxPayNotificationDecoder.class);
        when(decoder.decryptResource(any())).thenReturn(wxResult(orderNo, "SUCCESS"));
        LocalOrderLock lock = new LocalOrderLock();
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        when(transactionTemplate.execute(any(TransactionCallback.class))).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });
        WxPayOrderService service = spy(new WxPayOrderService(
                mock(WxPayConfig.class),
                loader,
                orderInfoService,
                mock(PaymentInfoService.class),
                inventoryService,
                mock(WxPayHttpClient.class),
                decoder,
                lock,
                transactionTemplate
        ));
        doReturn(wxResult(orderNo, "CLOSED")).when(service).queryOrder(orderNo);

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> payment = pool.submit(() -> {
                await(start);
                service.processOrder(Collections.<String, Object>singletonMap(
                        "id",
                        "NOTIFY-" + iteration
                ));
            });
            Future<?> close = pool.submit(() -> {
                await(start);
                service.queryPaymentStatus(orderNo);
            });
            start.countDown();
            payment.get(5, TimeUnit.SECONDS);
            close.get(5, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
        }

        assertEquals(1, commits.get() + releases.get());
        assertTrue(OrderStatus.SUCCESS.getType().equals(orderStatus.get())
                || OrderStatus.CLOSED.getType().equals(orderStatus.get()));
        assertEquals(Collections.singleton("payment:order:transition:" + orderNo), lock.keys());
    }

    private void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private OrderInfo order(String orderNo, String status) {
        OrderInfo order = new OrderInfo();
        order.setId(11L);
        order.setOrderNo(orderNo);
        order.setOrderStatus(status);
        order.setPaymentType(PayType.WXPAY.getType());
        order.setPaymentAppId(9L);
        order.setTotalFee(1000);
        return order;
    }

    private String wxResult(String orderNo, String tradeState) {
        return "{\"trade_state\":\"" + tradeState
                + "\",\"out_trade_no\":\"" + orderNo
                + "\",\"appid\":\"wx-app-1\",\"mchid\":\"wx-mch-1\""
                + ",\"amount\":{\"total\":1000}}";
    }

    private static final class LocalOrderLock implements DistributedLockTemplate {

        private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();
        private final Set<String> keys = ConcurrentHashMap.newKeySet();

        @Override
        public <T> T execute(
                String lockKey,
                long waitTimeMillis,
                long leaseTimeMillis,
                Supplier<T> supplier
        ) {
            keys.add(lockKey);
            ReentrantLock lock = locks.computeIfAbsent(lockKey, ignored -> new ReentrantLock());
            lock.lock();
            try {
                return supplier.get();
            } finally {
                lock.unlock();
            }
        }

        private Set<String> keys() {
            return keys;
        }
    }
}
