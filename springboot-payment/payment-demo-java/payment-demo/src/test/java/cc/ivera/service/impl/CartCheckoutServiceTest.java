package cc.ivera.service.impl;

import cc.ivera.config.PaymentAppConfig;
import cc.ivera.config.PaymentConfigLoader;
import cc.ivera.entity.Cart;
import cc.ivera.entity.CartItem;
import cc.ivera.entity.OrderInfo;
import cc.ivera.entity.OrderItem;
import cc.ivera.entity.Product;
import cc.ivera.enums.OrderStatus;
import cc.ivera.exception.BizException;
import cc.ivera.lock.DistributedLockTemplate;
import cc.ivera.mapper.CartItemMapper;
import cc.ivera.mapper.CartMapper;
import cc.ivera.mapper.OrderInfoMapper;
import cc.ivera.mapper.OrderItemMapper;
import cc.ivera.mapper.ProductMapper;
import cc.ivera.service.OrderCloseMessageService;
import cc.ivera.vo.CheckoutResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CartCheckoutServiceTest {

    private CartMapper cartMapper;
    private CartItemMapper cartItemMapper;
    private ProductMapper productMapper;
    private OrderInfoMapper orderInfoMapper;
    private OrderItemMapper orderItemMapper;
    private PaymentConfigLoader paymentConfigLoader;
    private OrderCloseMessageService closeMessageService;
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
        DistributedLockTemplate lockTemplate = mock(DistributedLockTemplate.class);
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
                lockTemplate,
                transactionTemplate
        );
    }

    @Test
    void checkoutSnapshotsCurrentPricesIntoOneOrderAndMultipleItemsThenClearsCart() {
        PaymentAppConfig appConfig = new PaymentAppConfig();
        appConfig.setAppId(9L);
        appConfig.setChannelCode("WXPAY");
        when(paymentConfigLoader.getRequiredAppConfig(9L)).thenReturn(appConfig);
        when(orderInfoMapper.selectByCheckoutKey(7L, "checkout-1")).thenReturn(null);
        Cart cart = cart(20L, 7L);
        when(cartMapper.selectByUserIdForUpdate(7L)).thenReturn(cart);
        when(cartItemMapper.selectByCartId(20L)).thenReturn(Arrays.asList(
                item(20L, 101L, 2),
                item(20L, 202L, 3)
        ));
        when(productMapper.selectBatchIds(any())).thenReturn(Arrays.asList(
                product(101L, "Java", 1000),
                product(202L, "Vue", 2500)
        ));
        when(orderInfoMapper.insert(any(OrderInfo.class))).thenAnswer(invocation -> {
            ((OrderInfo) invocation.getArgument(0)).setId(88L);
            return 1;
        });

        CheckoutResult result = checkoutService.checkout(7L, 9L, "checkout-1");

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
        assertEquals(7500, savedItems.get(1).getSubtotal());
        verify(cartItemMapper).deleteByCartId(20L);
        verify(closeMessageService).sendCloseOrderMessage(order.getOrderNo(), "微信");
        assertEquals(order.getOrderNo(), result.getOrderNo());
    }

    @Test
    void sameCheckoutRequestReturnsExistingOrderWithoutTouchingCart() {
        OrderInfo existing = new OrderInfo();
        existing.setOrderNo("ORDER-EXISTING");
        existing.setTotalFee(8800);
        existing.setPaymentAppId(9L);
        existing.setPaymentChannelCode("WXPAY");
        when(orderInfoMapper.selectByCheckoutKey(7L, "same-request")).thenReturn(existing);

        CheckoutResult result = checkoutService.checkout(7L, 9L, "same-request");

        assertEquals("ORDER-EXISTING", result.getOrderNo());
        verify(cartMapper, never()).selectByUserIdForUpdate(anyLong());
        verify(orderInfoMapper, never()).insert(any(OrderInfo.class));
    }

    @Test
    void emptyCartIsRejectedWithoutCreatingOrder() {
        PaymentAppConfig appConfig = new PaymentAppConfig();
        appConfig.setAppId(9L);
        appConfig.setChannelCode("ALIPAY");
        when(paymentConfigLoader.getRequiredAppConfig(9L)).thenReturn(appConfig);
        when(orderInfoMapper.selectByCheckoutKey(7L, "empty-cart")).thenReturn(null);
        when(cartMapper.selectByUserIdForUpdate(7L)).thenReturn(cart(20L, 7L));
        when(cartItemMapper.selectByCartId(20L)).thenReturn(Collections.emptyList());

        assertThrows(BizException.class, () -> checkoutService.checkout(7L, 9L, "empty-cart"));

        verify(orderInfoMapper, never()).insert(any(OrderInfo.class));
    }

    @Test
    void checkoutRejectsAmountAboveOrderIntegerLimit() {
        PaymentAppConfig appConfig = new PaymentAppConfig();
        appConfig.setAppId(9L);
        appConfig.setChannelCode("WXPAY");
        when(paymentConfigLoader.getRequiredAppConfig(9L)).thenReturn(appConfig);
        when(orderInfoMapper.selectByCheckoutKey(7L, "too-large")).thenReturn(null);
        when(cartMapper.selectByUserIdForUpdate(7L)).thenReturn(cart(20L, 7L));
        when(cartItemMapper.selectByCartId(20L)).thenReturn(Collections.singletonList(
                item(20L, 101L, 2)
        ));
        when(productMapper.selectBatchIds(any())).thenReturn(Collections.singletonList(
                product(101L, "Java", Integer.MAX_VALUE)
        ));

        assertThrows(BizException.class, () -> checkoutService.checkout(7L, 9L, "too-large"));

        verify(orderInfoMapper, never()).insert(any(OrderInfo.class));
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
        return product;
    }
}
