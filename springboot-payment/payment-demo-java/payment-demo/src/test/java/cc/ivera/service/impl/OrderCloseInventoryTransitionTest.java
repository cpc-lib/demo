package cc.ivera.service.impl;

import cc.ivera.config.AlipayProperties;
import cc.ivera.config.PaymentAppConfig;
import cc.ivera.config.PaymentConfigLoader;
import cc.ivera.config.WxPayConfig;
import cc.ivera.entity.OrderInfo;
import cc.ivera.enums.OrderStatus;
import cc.ivera.enums.PayType;
import cc.ivera.exception.BizException;
import cc.ivera.lock.DistributedLockTemplate;
import cc.ivera.service.InventoryService;
import cc.ivera.service.OrderInfoService;
import cc.ivera.service.PaymentInfoService;
import cc.ivera.service.RefundInfoService;
import cc.ivera.service.impl.wxpay.WxPayHttpClient;
import cc.ivera.service.impl.wxpay.WxPayNotificationDecoder;
import cc.ivera.service.impl.wxpay.WxPayOrderService;
import com.alipay.api.AlipayClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderCloseInventoryTransitionTest {

    private OrderInfoService orderInfoService;
    private InventoryService inventoryService;
    private PaymentInfoService paymentInfoService;
    private DistributedLockTemplate lockTemplate;
    private TransactionTemplate transactionTemplate;
    private PaymentConfigLoader paymentConfigLoader;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        orderInfoService = mock(OrderInfoService.class);
        inventoryService = mock(InventoryService.class);
        paymentInfoService = mock(PaymentInfoService.class);
        lockTemplate = mock(DistributedLockTemplate.class);
        transactionTemplate = mock(TransactionTemplate.class);
        paymentConfigLoader = mock(PaymentConfigLoader.class);
        when(lockTemplate.execute(anyString(), anyLong(), anyLong(), any(Supplier.class)))
                .thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(3)).get());
        when(transactionTemplate.execute(any(TransactionCallback.class))).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });
    }

    @Test
    void alipayActiveQueryKeepsWaitingOrderOpenAndReserved() {
        AliPayServiceImpl service = spy(aliService());
        doReturn(aliResult("WAIT_BUYER_PAY")).when(service).queryOrder("ORDER-ALI-WAIT");

        assertDoesNotThrow(() -> service.checkOrderStatus("ORDER-ALI-WAIT"));

        verify(orderInfoService, never()).updateStatusByOrderNoIfStatus(
                anyString(),
                any(OrderStatus.class),
                any(OrderStatus.class)
        );
        verify(inventoryService, never()).releaseReservation(anyString());
    }

    @Test
    void alipayNullQueryIsUnknownAndNeverReleasesInventory() {
        AliPayServiceImpl service = spy(aliService());
        doReturn(null).when(service).queryOrder("ORDER-ALI-UNKNOWN");

        assertThrows(BizException.class, () -> service.checkOrderStatus("ORDER-ALI-UNKNOWN"));

        verify(orderInfoService, never()).updateStatusByOrderNoIfStatus(
                anyString(),
                any(OrderStatus.class),
                any(OrderStatus.class)
        );
        verify(inventoryService, never()).releaseReservation(anyString());
    }

    @Test
    void alipayExplicitClosedStateReleasesInventoryAfterWinningTheCas() {
        AliPayServiceImpl service = spy(aliService());
        doReturn(aliResult("TRADE_CLOSED")).when(service).queryOrder("ORDER-ALI-CLOSED");
        when(orderInfoService.getOrderByOrderNoForUpdate("ORDER-ALI-CLOSED"))
                .thenReturn(unpaidOrder("ORDER-ALI-CLOSED", PayType.ALIPAY));
        when(orderInfoService.updateStatusByOrderNoIfStatus(
                "ORDER-ALI-CLOSED",
                OrderStatus.NOTPAY,
                OrderStatus.CLOSED
        )).thenReturn(true);
        when(inventoryService.releaseReservation("ORDER-ALI-CLOSED")).thenReturn(true);

        service.checkOrderStatus("ORDER-ALI-CLOSED");

        verify(lockTemplate).execute(
                eq("payment:order:transition:ORDER-ALI-CLOSED"),
                anyLong(),
                anyLong(),
                any(Supplier.class)
        );
        verify(inventoryService).releaseReservation("ORDER-ALI-CLOSED");
    }

    @Test
    void alipayQueryCannotCommitInventoryForAWechatOrder() {
        AliPayServiceImpl service = spy(aliService());
        doReturn(aliResult("TRADE_SUCCESS")).when(service).queryOrder("ORDER-WRONG-CHANNEL");
        when(orderInfoService.getOrderByOrderNoForUpdate("ORDER-WRONG-CHANNEL"))
                .thenReturn(unpaidOrder("ORDER-WRONG-CHANNEL", PayType.WXPAY));

        assertThrows(BizException.class, () -> service.checkOrderStatus("ORDER-WRONG-CHANNEL"));

        verify(orderInfoService, never()).updateStatusByOrderNoIfStatus(
                anyString(),
                any(OrderStatus.class),
                any(OrderStatus.class)
        );
        verify(inventoryService, never()).commitPayment(anyString());
    }

    @Test
    void alipaySuccessfulQueryWithoutOrderOrAmountCannotCommitInventory() {
        AliPayServiceImpl service = spy(aliService());
        doReturn("{\"alipay_trade_query_response\":{\"trade_status\":\"TRADE_SUCCESS\"}}")
                .when(service).queryOrder("ORDER-ALI-INCOMPLETE");
        when(orderInfoService.getOrderByOrderNoForUpdate("ORDER-ALI-INCOMPLETE"))
                .thenReturn(unpaidOrder("ORDER-ALI-INCOMPLETE", PayType.ALIPAY));

        assertThrows(BizException.class, () -> service.checkOrderStatus("ORDER-ALI-INCOMPLETE"));

        verify(orderInfoService, never()).updateStatusByOrderNoIfStatus(
                anyString(),
                any(OrderStatus.class),
                any(OrderStatus.class)
        );
        verify(inventoryService, never()).commitPayment(anyString());
    }

    @Test
    void wxExplicitClosedStateReleasesInventoryAfterWinningTheCas() {
        WxPayOrderService service = spy(wxService());
        OrderInfo order = unpaidOrder("ORDER-WX-CLOSED", PayType.WXPAY);
        when(orderInfoService.getOrderByOrderNo("ORDER-WX-CLOSED")).thenReturn(order);
        when(orderInfoService.getOrderByOrderNoForUpdate("ORDER-WX-CLOSED")).thenReturn(order);
        when(orderInfoService.updateStatusByOrderNoIfStatus(
                "ORDER-WX-CLOSED",
                OrderStatus.NOTPAY,
                OrderStatus.CLOSED
        )).thenReturn(true);
        when(orderInfoService.getOrderStatus("ORDER-WX-CLOSED")).thenReturn(OrderStatus.CLOSED.getType());
        when(inventoryService.releaseReservation("ORDER-WX-CLOSED")).thenReturn(true);
        doReturn(wxResult("ORDER-WX-CLOSED", "CLOSED"))
                .when(service).queryOrder("ORDER-WX-CLOSED");

        service.queryPaymentStatus("ORDER-WX-CLOSED");

        verify(lockTemplate).execute(
                eq("payment:order:transition:ORDER-WX-CLOSED"),
                anyLong(),
                anyLong(),
                any(Supplier.class)
        );
        verify(inventoryService).releaseReservation("ORDER-WX-CLOSED");
    }

    @Test
    void delayedWxCloseTreatsUnknownChannelStateAsRetryable() {
        WxPayOrderService service = spy(wxService());
        doReturn("{\"trade_state\":\"USERPAYING\"}").when(service).queryOrder("ORDER-WX-UNKNOWN");

        assertThrows(BizException.class, () -> service.checkOrderStatus("ORDER-WX-UNKNOWN"));

        verify(orderInfoService, never()).updateStatusByOrderNoIfStatus(
                anyString(),
                any(OrderStatus.class),
                any(OrderStatus.class)
        );
        verify(inventoryService, never()).releaseReservation(anyString());
    }

    private AliPayServiceImpl aliService() {
        return new AliPayServiceImpl(
                orderInfoService,
                mock(AlipayClient.class),
                mock(AlipayProperties.class),
                paymentConfigLoader,
                paymentInfoService,
                inventoryService,
                mock(RefundInfoService.class),
                lockTemplate,
                transactionTemplate
        );
    }

    private WxPayOrderService wxService() {
        PaymentAppConfig config = new PaymentAppConfig();
        config.setChannelCode(PaymentConfigLoader.CHANNEL_WXPAY);
        config.setAppid("wx-app-1");
        config.setMchId("wx-mch-1");
        when(paymentConfigLoader.getRequiredAppConfig(9L)).thenReturn(config);
        return new WxPayOrderService(
                mock(WxPayConfig.class),
                paymentConfigLoader,
                orderInfoService,
                paymentInfoService,
                inventoryService,
                mock(WxPayHttpClient.class),
                mock(WxPayNotificationDecoder.class),
                lockTemplate,
                transactionTemplate
        );
    }

    private String aliResult(String tradeStatus) {
        String orderNo = "TRADE_CLOSED".equals(tradeStatus)
                ? "ORDER-ALI-CLOSED"
                : "ORDER-WRONG-CHANNEL";
        return "{\"alipay_trade_query_response\":{\"trade_status\":\""
                + tradeStatus
                + "\",\"out_trade_no\":\""
                + orderNo
                + "\",\"total_amount\":\"10.00\"}}";
    }

    private OrderInfo unpaidOrder(String orderNo, PayType payType) {
        OrderInfo order = new OrderInfo();
        order.setId(11L);
        order.setOrderNo(orderNo);
        order.setOrderStatus(OrderStatus.NOTPAY.getType());
        order.setPaymentType(payType.getType());
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
}
