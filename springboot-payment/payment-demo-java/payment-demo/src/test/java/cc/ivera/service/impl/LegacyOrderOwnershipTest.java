package cc.ivera.service.impl;

import cc.ivera.entity.OrderIdempotency;
import cc.ivera.entity.OrderInfo;
import cc.ivera.entity.OrderItem;
import cc.ivera.entity.Product;
import cc.ivera.enums.InventoryStatus;
import cc.ivera.enums.ProductStatus;
import cc.ivera.enums.UserRole;
import cc.ivera.exception.BizException;
import cc.ivera.exception.ConflictException;
import cc.ivera.lock.DistributedLockTemplate;
import cc.ivera.mapper.OrderInfoMapper;
import cc.ivera.mapper.OrderItemMapper;
import cc.ivera.mapper.ProductMapper;
import cc.ivera.security.AuthContext;
import cc.ivera.security.AuthUser;
import cc.ivera.service.OrderCloseMessageService;
import cc.ivera.service.OrderIdempotencyService;
import cc.ivera.service.InventoryService;
import cc.ivera.util.OrderRequestFingerprint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Date;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LegacyOrderOwnershipTest {

    private ProductMapper productMapper;
    private OrderInfoMapper orderInfoMapper;
    private OrderItemMapper orderItemMapper;
    private OrderIdempotencyService idempotencyService;
    private DistributedLockTemplate lockTemplate;
    private InventoryService inventoryService;
    private OrderCloseMessageService closeMessageService;
    private OrderInfoServiceImpl service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        AuthContext.setUser(new AuthUser(55L, "alice", UserRole.USER));
        productMapper = mock(ProductMapper.class);
        orderInfoMapper = mock(OrderInfoMapper.class);
        orderItemMapper = mock(OrderItemMapper.class);
        idempotencyService = mock(OrderIdempotencyService.class);
        lockTemplate = mock(DistributedLockTemplate.class);
        inventoryService = mock(InventoryService.class);
        closeMessageService = mock(OrderCloseMessageService.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        when(lockTemplate.execute(anyString(), anyLong(), anyLong(), any(Supplier.class)))
                .thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(3)).get());
        when(transactionTemplate.execute(any(TransactionCallback.class))).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });
        service = new OrderInfoServiceImpl(
                productMapper,
                closeMessageService,
                lockTemplate,
                transactionTemplate,
                orderItemMapper,
                idempotencyService,
                inventoryService
        );
        ReflectionTestUtils.setField(service, "baseMapper", orderInfoMapper);
    }

    @AfterEach
    void clearContext() {
        AuthContext.clear();
    }

    @Test
    void directPurchaseCreatesOneUserOrderAndSnapshotForTheIssuedKey() {
        String fingerprint = OrderRequestFingerprint.directBuy(101L, 1, 9L, "WXPAY");
        OrderIdempotency key = issuedKey(5L, "key-1");
        when(idempotencyService.requireForUpdate(55L, "key-1", fingerprint)).thenReturn(key);
        when(productMapper.selectByIdForUpdate(101L)).thenReturn(product());
        when(orderInfoMapper.insert(any(OrderInfo.class))).thenAnswer(invocation -> {
            ((OrderInfo) invocation.getArgument(0)).setId(88L);
            return 1;
        });
        when(orderItemMapper.insert(any(OrderItem.class))).thenReturn(1);

        OrderInfo result;
        TransactionSynchronizationManager.initSynchronization();
        try {
            result = service.createOrReuseOrder(101L, "微信", 9L, "WXPAY", "key-1");
            verify(closeMessageService).sendCloseOrderMessage(anyString(), anyString());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        ArgumentCaptor<OrderInfo> orderCaptor = ArgumentCaptor.forClass(OrderInfo.class);
        verify(orderInfoMapper).insert(orderCaptor.capture());
        assertEquals(55L, orderCaptor.getValue().getUserId());
        assertEquals("key-1", orderCaptor.getValue().getCheckoutRequestId());
        ArgumentCaptor<OrderItem> itemCaptor = ArgumentCaptor.forClass(OrderItem.class);
        verify(orderItemMapper).insert(itemCaptor.capture());
        assertEquals(88L, itemCaptor.getValue().getOrderId());
        assertEquals(101L, itemCaptor.getValue().getProductId());
        assertEquals(1, itemCaptor.getValue().getQuantity());
        assertEquals(1000, itemCaptor.getValue().getSubtotal());
        assertEquals(InventoryStatus.RESERVED, itemCaptor.getValue().getInventoryStatus());
        assertEquals(0, itemCaptor.getValue().getRefundedQuantity());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<OrderItem>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(inventoryService).reserve(
                org.mockito.ArgumentMatchers.same(orderCaptor.getValue()),
                itemsCaptor.capture()
        );
        assertEquals(1, itemsCaptor.getValue().size());
        assertEquals(101L, itemsCaptor.getValue().get(0).getProductId());
        verify(idempotencyService).complete(5L, fingerprint, 88L);
        verify(closeMessageService).sendCloseOrderMessage(orderCaptor.getValue().getOrderNo(), "微信");
        assertEquals(88L, result.getId());
    }

    @Test
    void completedDirectKeyReturnsOnlyItsBoundOrderWithoutReadingProduct() {
        String fingerprint = OrderRequestFingerprint.directBuy(101L, 1, 9L, "WXPAY");
        OrderIdempotency key = issuedKey(5L, "key-1");
        key.setStatus("COMPLETED");
        key.setRequestFingerprint(fingerprint);
        key.setOrderId(88L);
        when(idempotencyService.requireForUpdate(55L, "key-1", fingerprint)).thenReturn(key);
        OrderInfo existing = new OrderInfo();
        existing.setId(88L);
        existing.setUserId(55L);
        existing.setOrderNo("ORDER-88");
        when(orderInfoMapper.selectById(88L)).thenReturn(existing);

        OrderInfo result = service.createOrReuseOrder(101L, "微信", 9L, "WXPAY", "key-1");

        assertEquals("ORDER-88", result.getOrderNo());
        verifyNoInteractions(productMapper);
        verifyNoInteractions(inventoryService);
        verifyNoInteractions(closeMessageService);
        verify(idempotencyService, never()).complete(anyLong(), anyString(), anyLong());
    }

    @Test
    void failedDirectOrderInsertDoesNotCompleteTheKey() {
        String fingerprint = OrderRequestFingerprint.directBuy(101L, 1, 9L, "WXPAY");
        when(idempotencyService.requireForUpdate(55L, "key-1", fingerprint))
                .thenReturn(issuedKey(5L, "key-1"));
        when(productMapper.selectByIdForUpdate(101L)).thenReturn(product());
        when(orderInfoMapper.insert(any(OrderInfo.class))).thenThrow(new BizException("insert failed"));

        assertThrows(BizException.class,
                () -> service.createOrReuseOrder(101L, "微信", 9L, "WXPAY", "key-1"));

        verify(idempotencyService, never()).complete(anyLong(), anyString(), anyLong());
    }

    @Test
    void directPurchaseOutboxFailureDoesNotCompleteTheKeyBeforeRollback() {
        String fingerprint = OrderRequestFingerprint.directBuy(101L, 1, 9L, "WXPAY");
        when(idempotencyService.requireForUpdate(55L, "key-1", fingerprint))
                .thenReturn(issuedKey(5L, "key-1"));
        when(productMapper.selectByIdForUpdate(101L)).thenReturn(product());
        when(orderInfoMapper.insert(any(OrderInfo.class))).thenAnswer(invocation -> {
            ((OrderInfo) invocation.getArgument(0)).setId(88L);
            return 1;
        });
        when(orderItemMapper.insert(any(OrderItem.class))).thenReturn(1);
        doThrow(new ConflictException("outbox failed"))
                .when(closeMessageService).sendCloseOrderMessage(anyString(), anyString());

        assertThrows(ConflictException.class,
                () -> service.createOrReuseOrder(101L, "微信", 9L, "WXPAY", "key-1"));

        verify(idempotencyService, never()).complete(anyLong(), anyString(), anyLong());
    }

    @Test
    void directPurchaseUsesTheCommonUserAndKeyLock() {
        String fingerprint = OrderRequestFingerprint.directBuy(101L, 1, 9L, "WXPAY");
        when(idempotencyService.requireForUpdate(55L, "key-1", fingerprint))
                .thenThrow(new BizException("stop"));

        assertThrows(BizException.class,
                () -> service.createOrReuseOrder(101L, "微信", 9L, "WXPAY", "key-1"));

        verify(lockTemplate).execute(
                org.mockito.ArgumentMatchers.eq("payment:order:create:55:key-1"),
                anyLong(),
                anyLong(),
                any(Supplier.class)
        );
        verify(idempotencyService).expireIssuedKeyIfNeeded(55L, "key-1");
    }

    private OrderIdempotency issuedKey(Long id, String keyValue) {
        OrderIdempotency key = new OrderIdempotency();
        key.setId(id);
        key.setUserId(55L);
        key.setIdempotencyKey(keyValue);
        key.setStatus("ISSUED");
        key.setExpiresAt(new Date(System.currentTimeMillis() + 60_000L));
        return key;
    }

    private Product product() {
        Product product = new Product();
        product.setId(101L);
        product.setTitle("Java");
        product.setPrice(1000);
        product.setStatus(ProductStatus.ON_SHELF);
        product.setAvailableStock(3);
        return product;
    }
}
