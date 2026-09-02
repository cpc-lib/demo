package cc.ivera.service.impl;

import cc.ivera.config.PaymentAppConfig;
import cc.ivera.config.PaymentConfigLoader;
import cc.ivera.entity.Cart;
import cc.ivera.entity.CartItem;
import cc.ivera.entity.OrderInfo;
import cc.ivera.entity.OrderIdempotency;
import cc.ivera.entity.OrderItem;
import cc.ivera.entity.Product;
import cc.ivera.enums.InventoryStatus;
import cc.ivera.enums.OrderStatus;
import cc.ivera.enums.ProductStatus;
import cc.ivera.exception.BizException;
import cc.ivera.exception.ConflictException;
import cc.ivera.lock.DistributedLockTemplate;
import cc.ivera.mapper.CartItemMapper;
import cc.ivera.mapper.CartMapper;
import cc.ivera.mapper.OrderInfoMapper;
import cc.ivera.mapper.OrderItemMapper;
import cc.ivera.mapper.ProductMapper;
import cc.ivera.service.OrderCloseMessageService;
import cc.ivera.service.OrderIdempotencyService;
import cc.ivera.service.InventoryService;
import cc.ivera.vo.CheckoutResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CartCheckoutServiceTest {

    private CartMapper cartMapper;
    private CartItemMapper cartItemMapper;
    private ProductMapper productMapper;
    private OrderInfoMapper orderInfoMapper;
    private OrderItemMapper orderItemMapper;
    private PaymentConfigLoader paymentConfigLoader;
    private OrderCloseMessageService closeMessageService;
    private OrderIdempotencyService orderIdempotencyService;
    private InventoryService inventoryService;
    private DistributedLockTemplate lockTemplate;
    private CheckoutServiceImpl checkoutService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        cartMapper = mock(CartMapper.class);
        cartItemMapper = mock(CartItemMapper.class);
        productMapper = mock(ProductMapper.class);
        orderInfoMapper = mock(OrderInfoMapper.class);
        orderItemMapper = mock(OrderItemMapper.class);
        paymentConfigLoader = mock(PaymentConfigLoader.class);
        closeMessageService = mock(OrderCloseMessageService.class);
        orderIdempotencyService = mock(OrderIdempotencyService.class);
        inventoryService = mock(InventoryService.class);
        lockTemplate = mock(DistributedLockTemplate.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        when(lockTemplate.execute(anyString(), anyLong(), anyLong(), any(Supplier.class)))
                .thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(3)).get());
        when(transactionTemplate.execute(any(TransactionCallback.class))).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });
        checkoutService = new CheckoutServiceImpl(
                cartMapper,
                cartItemMapper,
                productMapper,
                orderInfoMapper,
                orderItemMapper,
                paymentConfigLoader,
                closeMessageService,
                orderIdempotencyService,
                inventoryService,
                lockTemplate,
                transactionTemplate
        );
    }

    @Test
    void checkoutSnapshotsCurrentPricesIntoOneOrderAndMultipleItemsThenClearsCart() {
        OrderIdempotency key = issuedKey(5L, 7L, "checkout-1");
        when(orderIdempotencyService.requireForUpdate(7L, "checkout-1", "fp-1")).thenReturn(key);
        PaymentAppConfig appConfig = new PaymentAppConfig();
        appConfig.setAppId(9L);
        appConfig.setChannelCode("WXPAY");
        when(paymentConfigLoader.getRequiredAppConfig(9L)).thenReturn(appConfig);
        Cart cart = cart(20L, 7L);
        when(cartMapper.selectByUserIdForUpdate(7L)).thenReturn(cart);
        when(cartItemMapper.selectByCartId(20L)).thenReturn(Arrays.asList(
                item(20L, 101L, 2),
                item(20L, 202L, 3)
        ));
        when(productMapper.selectByIdForUpdate(101L)).thenReturn(product(101L, "Java", 1000));
        when(productMapper.selectByIdForUpdate(202L)).thenReturn(product(202L, "Vue", 2500));
        when(orderInfoMapper.insert(any(OrderInfo.class))).thenAnswer(invocation -> {
            ((OrderInfo) invocation.getArgument(0)).setId(88L);
            return 1;
        });
        when(orderItemMapper.insert(any(OrderItem.class))).thenReturn(1);

        CheckoutResult result;
        TransactionSynchronizationManager.initSynchronization();
        try {
            result = checkoutService.checkout(7L, 9L, "checkout-1", "fp-1");
            verify(closeMessageService).sendCloseOrderMessage(anyString(), anyString());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        ArgumentCaptor<OrderInfo> orderCaptor = ArgumentCaptor.forClass(OrderInfo.class);
        verify(orderInfoMapper).insert(orderCaptor.capture());
        OrderInfo order = orderCaptor.getValue();
        assertEquals(9500, order.getTotalFee());
        assertEquals("Java等2种课程", order.getTitle());
        assertEquals(7L, order.getUserId());
        assertNull(order.getProductId());
        assertEquals("checkout-1", order.getCheckoutRequestId());
        assertEquals(OrderStatus.NOTPAY.getType(), order.getOrderStatus());
        assertEquals("微信", order.getPaymentType());
        assertEquals("WXPAY", order.getPaymentChannelCode());

        ArgumentCaptor<OrderItem> itemCaptor = ArgumentCaptor.forClass(OrderItem.class);
        verify(orderItemMapper, org.mockito.Mockito.times(2)).insert(itemCaptor.capture());
        List<OrderItem> savedItems = itemCaptor.getAllValues();
        assertEquals("Java", savedItems.get(0).getProductTitle());
        assertEquals(1000, savedItems.get(0).getUnitPrice());
        assertEquals(2, savedItems.get(0).getQuantity());
        assertEquals(2000, savedItems.get(0).getSubtotal());
        assertEquals(InventoryStatus.RESERVED, savedItems.get(0).getInventoryStatus());
        assertEquals(0, savedItems.get(0).getRefundedQuantity());
        assertEquals(7500, savedItems.get(1).getSubtotal());
        verify(inventoryService).reserve(order, savedItems);
        verify(cartItemMapper).deleteByCartId(20L);
        verify(orderIdempotencyService).complete(5L, "fp-1", 88L);
        verify(closeMessageService).sendCloseOrderMessage(order.getOrderNo(), "微信");
        assertEquals(order.getOrderNo(), result.getOrderNo());
    }

    @Test
    void completedKeyReturnsItsOrderWithoutTouchingCartOrProducts() {
        OrderIdempotency key = issuedKey(5L, 7L, "same-request");
        key.setStatus("COMPLETED");
        key.setOrderId(88L);
        key.setRequestFingerprint("fp-1");
        when(orderIdempotencyService.requireForUpdate(7L, "same-request", "fp-1")).thenReturn(key);
        OrderInfo existing = new OrderInfo();
        existing.setOrderNo("ORDER-EXISTING");
        existing.setUserId(7L);
        existing.setTotalFee(8800);
        existing.setPaymentAppId(9L);
        existing.setPaymentChannelCode("WXPAY");
        when(orderInfoMapper.selectById(88L)).thenReturn(existing);

        CheckoutResult result = checkoutService.checkout(7L, 9L, "same-request", "fp-1");

        assertEquals("ORDER-EXISTING", result.getOrderNo());
        verify(cartMapper, never()).selectByUserIdForUpdate(anyLong());
        verify(orderInfoMapper, never()).insert(any(OrderInfo.class));
        verifyNoInteractions(productMapper);
        verifyNoInteractions(paymentConfigLoader);
        verifyNoInteractions(inventoryService);
        verifyNoInteractions(closeMessageService);
        verify(orderIdempotencyService, never()).complete(anyLong(), anyString(), anyLong());
    }

    @Test
    void outboxFailureDoesNotClearCartOrCompleteTheKeyBeforeRollback() {
        when(orderIdempotencyService.requireForUpdate(7L, "outbox-failed", "fp-outbox"))
                .thenReturn(issuedKey(5L, 7L, "outbox-failed"));
        paymentApp("WXPAY");
        when(cartMapper.selectByUserIdForUpdate(7L)).thenReturn(cart(20L, 7L));
        when(cartItemMapper.selectByCartId(20L)).thenReturn(Collections.singletonList(
                item(20L, 101L, 1)
        ));
        when(productMapper.selectByIdForUpdate(101L)).thenReturn(product(101L, "Java", 1000));
        when(orderInfoMapper.insert(any(OrderInfo.class))).thenAnswer(invocation -> {
            ((OrderInfo) invocation.getArgument(0)).setId(88L);
            return 1;
        });
        when(orderItemMapper.insert(any(OrderItem.class))).thenReturn(1);
        doThrow(new ConflictException("outbox failed"))
                .when(closeMessageService).sendCloseOrderMessage(anyString(), anyString());

        assertThrows(ConflictException.class,
                () -> checkoutService.checkout(7L, 9L, "outbox-failed", "fp-outbox"));

        verify(cartItemMapper, never()).deleteByCartId(anyLong());
        verify(orderIdempotencyService, never()).complete(anyLong(), anyString(), anyLong());
    }

    @Test
    void differentRequestFingerprintIsRejectedWithoutTouchingCart() {
        when(orderIdempotencyService.requireForUpdate(7L, "same-key", "fp-2"))
                .thenThrow(new ConflictException("订单幂等键请求参数不一致"));

        assertThrows(ConflictException.class,
                () -> checkoutService.checkout(7L, 9L, "same-key", "fp-2"));

        verify(cartMapper, never()).selectByUserIdForUpdate(anyLong());
        verify(orderInfoMapper, never()).insert(any(OrderInfo.class));
    }

    @Test
    void emptyCartIsRejectedWithoutCreatingOrder() {
        when(orderIdempotencyService.requireForUpdate(7L, "empty-cart", "fp-empty"))
                .thenReturn(issuedKey(5L, 7L, "empty-cart"));
        PaymentAppConfig appConfig = new PaymentAppConfig();
        appConfig.setAppId(9L);
        appConfig.setChannelCode("ALIPAY");
        when(paymentConfigLoader.getRequiredAppConfig(9L)).thenReturn(appConfig);
        when(cartMapper.selectByUserIdForUpdate(7L)).thenReturn(cart(20L, 7L));
        when(cartItemMapper.selectByCartId(20L)).thenReturn(Collections.emptyList());

        assertThrows(BizException.class,
                () -> checkoutService.checkout(7L, 9L, "empty-cart", "fp-empty"));

        verify(orderInfoMapper, never()).insert(any(OrderInfo.class));
        verify(orderIdempotencyService, never()).complete(anyLong(), anyString(), anyLong());
    }

    @Test
    void checkoutRejectsAmountAboveOrderIntegerLimit() {
        when(orderIdempotencyService.requireForUpdate(7L, "too-large", "fp-large"))
                .thenReturn(issuedKey(5L, 7L, "too-large"));
        PaymentAppConfig appConfig = new PaymentAppConfig();
        appConfig.setAppId(9L);
        appConfig.setChannelCode("WXPAY");
        when(paymentConfigLoader.getRequiredAppConfig(9L)).thenReturn(appConfig);
        when(cartMapper.selectByUserIdForUpdate(7L)).thenReturn(cart(20L, 7L));
        when(cartItemMapper.selectByCartId(20L)).thenReturn(Collections.singletonList(
                item(20L, 101L, 2)
        ));
        when(productMapper.selectByIdForUpdate(101L))
                .thenReturn(product(101L, "Java", Integer.MAX_VALUE));

        assertThrows(BizException.class,
                () -> checkoutService.checkout(7L, 9L, "too-large", "fp-large"));

        verify(orderInfoMapper, never()).insert(any(OrderInfo.class));
        verify(orderIdempotencyService, never()).complete(anyLong(), anyString(), anyLong());
    }

    @Test
    void orderInsertFailureLeavesTheKeyIssued() {
        when(orderIdempotencyService.requireForUpdate(7L, "failed-order", "fp-failed"))
                .thenReturn(issuedKey(5L, 7L, "failed-order"));
        PaymentAppConfig appConfig = new PaymentAppConfig();
        appConfig.setAppId(9L);
        appConfig.setChannelCode("WXPAY");
        when(paymentConfigLoader.getRequiredAppConfig(9L)).thenReturn(appConfig);
        when(cartMapper.selectByUserIdForUpdate(7L)).thenReturn(cart(20L, 7L));
        when(cartItemMapper.selectByCartId(20L)).thenReturn(Collections.singletonList(
                item(20L, 101L, 1)
        ));
        when(productMapper.selectByIdForUpdate(101L)).thenReturn(product(101L, "Java", 1000));
        when(orderInfoMapper.insert(any(OrderInfo.class))).thenThrow(new BizException("insert failed"));

        assertThrows(BizException.class,
                () -> checkoutService.checkout(7L, 9L, "failed-order", "fp-failed"));

        verify(orderIdempotencyService, never()).complete(anyLong(), anyString(), anyLong());
    }

    @Test
    void checkoutSerializesByUserAndBackendKey() {
        when(orderIdempotencyService.requireForUpdate(7L, "key-1", "fp-1"))
                .thenThrow(new ConflictException("stop"));

        assertThrows(ConflictException.class,
                () -> checkoutService.checkout(7L, 9L, "key-1", "fp-1"));

        verify(lockTemplate).execute(
                org.mockito.ArgumentMatchers.eq("payment:order:create:7:key-1"),
                anyLong(),
                anyLong(),
                any(Supplier.class)
        );
        verify(orderIdempotencyService).expireIssuedKeyIfNeeded(7L, "key-1");
    }

    @Test
    void checkoutRejectsAnOffShelfProductInsideTheLockedTransaction() {
        when(orderIdempotencyService.requireForUpdate(7L, "off-shelf", "fp-off"))
                .thenReturn(issuedKey(5L, 7L, "off-shelf"));
        paymentApp("WXPAY");
        when(cartMapper.selectByUserIdForUpdate(7L)).thenReturn(cart(20L, 7L));
        when(cartItemMapper.selectByCartId(20L)).thenReturn(Collections.singletonList(
                item(20L, 101L, 1)
        ));
        Product product = product(101L, "Java", 1000);
        product.setStatus(ProductStatus.OFF_SHELF);
        when(productMapper.selectByIdForUpdate(101L)).thenReturn(product);

        assertThrows(ConflictException.class,
                () -> checkoutService.checkout(7L, 9L, "off-shelf", "fp-off"));

        verify(orderInfoMapper, never()).insert(any(OrderInfo.class));
    }

    @Test
    void checkoutRejectsInvalidQuantityOrInsufficientAvailableStock() {
        when(orderIdempotencyService.requireForUpdate(7L, "invalid-quantity", "fp-quantity"))
                .thenReturn(issuedKey(5L, 7L, "invalid-quantity"));
        paymentApp("WXPAY");
        when(cartMapper.selectByUserIdForUpdate(7L)).thenReturn(cart(20L, 7L));
        when(cartItemMapper.selectByCartId(20L)).thenReturn(Collections.singletonList(
                item(20L, 101L, 0)
        ));
        when(productMapper.selectByIdForUpdate(101L)).thenReturn(product(101L, "Java", 1000));

        assertThrows(ConflictException.class,
                () -> checkoutService.checkout(7L, 9L, "invalid-quantity", "fp-quantity"));

        when(orderIdempotencyService.requireForUpdate(7L, "insufficient", "fp-stock"))
                .thenReturn(issuedKey(6L, 7L, "insufficient"));
        when(cartItemMapper.selectByCartId(20L)).thenReturn(Collections.singletonList(
                item(20L, 101L, 2)
        ));
        Product insufficient = product(101L, "Java", 1000);
        insufficient.setAvailableStock(1);
        when(productMapper.selectByIdForUpdate(101L)).thenReturn(insufficient);

        assertThrows(ConflictException.class,
                () -> checkoutService.checkout(7L, 9L, "insufficient", "fp-stock"));

        verify(orderInfoMapper, never()).insert(any(OrderInfo.class));
    }

    @Test
    void checkoutLocksProductsInAscendingIdOrder() {
        when(orderIdempotencyService.requireForUpdate(7L, "ascending", "fp-ascending"))
                .thenReturn(issuedKey(5L, 7L, "ascending"));
        paymentApp("WXPAY");
        when(cartMapper.selectByUserIdForUpdate(7L)).thenReturn(cart(20L, 7L));
        when(cartItemMapper.selectByCartId(20L)).thenReturn(Arrays.asList(
                item(20L, 202L, 1),
                item(20L, 101L, 1)
        ));
        when(productMapper.selectByIdForUpdate(101L)).thenReturn(product(101L, "Java", 1000));
        when(productMapper.selectByIdForUpdate(202L)).thenReturn(product(202L, "Vue", 2500));
        when(orderInfoMapper.insert(any(OrderInfo.class))).thenAnswer(invocation -> {
            ((OrderInfo) invocation.getArgument(0)).setId(88L);
            return 1;
        });
        when(orderItemMapper.insert(any(OrderItem.class))).thenReturn(1);

        checkoutService.checkout(7L, 9L, "ascending", "fp-ascending");

        InOrder inOrder = inOrder(productMapper);
        inOrder.verify(productMapper).selectByIdForUpdate(101L);
        inOrder.verify(productMapper).selectByIdForUpdate(202L);
    }

    private Cart cart(Long id, Long userId) {
        Cart cart = new Cart();
        cart.setId(id);
        cart.setUserId(userId);
        return cart;
    }

    private CartItem item(Long cartId, Long productId, int quantity) {
        CartItem item = new CartItem();
        item.setCartId(cartId);
        item.setProductId(productId);
        item.setQuantity(quantity);
        return item;
    }

    private Product product(Long id, String title, int price) {
        Product product = new Product();
        product.setId(id);
        product.setTitle(title);
        product.setPrice(price);
        product.setStatus(ProductStatus.ON_SHELF);
        product.setAvailableStock(100);
        return product;
    }

    private void paymentApp(String channelCode) {
        PaymentAppConfig appConfig = new PaymentAppConfig();
        appConfig.setAppId(9L);
        appConfig.setChannelCode(channelCode);
        when(paymentConfigLoader.getRequiredAppConfig(9L)).thenReturn(appConfig);
    }

    private OrderIdempotency issuedKey(Long id, Long userId, String key) {
        OrderIdempotency record = new OrderIdempotency();
        record.setId(id);
        record.setUserId(userId);
        record.setIdempotencyKey(key);
        record.setStatus("ISSUED");
        record.setExpiresAt(new java.util.Date(System.currentTimeMillis() + 60_000L));
        return record;
    }
}
