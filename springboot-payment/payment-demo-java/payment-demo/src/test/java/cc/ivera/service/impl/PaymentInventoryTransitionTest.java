package cc.ivera.service.impl;

import cc.ivera.config.AlipayProperties;
import cc.ivera.config.PaymentAppConfig;
import cc.ivera.config.PaymentConfigLoader;
import cc.ivera.config.WxPayConfig;
import cc.ivera.controller.WxPayV2Controller;
import cc.ivera.entity.OrderInfo;
import cc.ivera.enums.OrderStatus;
import cc.ivera.enums.PayType;
import cc.ivera.exception.BizException;
import cc.ivera.exception.ConflictException;
import cc.ivera.lock.DistributedLockTemplate;
import cc.ivera.service.InventoryService;
import cc.ivera.service.OrderInfoService;
import cc.ivera.service.PaymentInfoService;
import cc.ivera.service.RefundInfoService;
import cc.ivera.service.impl.wxpay.WxPayHttpClient;
import cc.ivera.service.impl.wxpay.WxPayNotificationDecoder;
import cc.ivera.service.impl.wxpay.WxPayOrderService;
import cc.ivera.service.wxpay.WxPayOrderFacade;
import com.alipay.api.AlipayClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentInventoryTransitionTest {

    private OrderInfoService orderInfoService;
    private PaymentInfoService paymentInfoService;
    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        orderInfoService = mock(OrderInfoService.class);
        paymentInfoService = mock(PaymentInfoService.class);
        inventoryService = mock(InventoryService.class);
    }

    @Test
    void wxV3NotificationCommitsInventoryBeforeWritingPaymentInfo() {
        WxPayOrderService service = wxService();
        when(orderInfoService.getOrderByOrderNoForUpdate("ORDER-WX3"))
                .thenReturn(unpaidOrder("ORDER-WX3", PayType.WXPAY));
        when(orderInfoService.updateStatusByOrderNoIfStatus(
                "ORDER-WX3",
                OrderStatus.NOTPAY,
                OrderStatus.SUCCESS
        )).thenReturn(true);
        when(inventoryService.commitPayment("ORDER-WX3")).thenReturn(true);

        ReflectionTestUtils.invokeMethod(
                service,
                "doProcessOrderNotifyInTransaction",
                "ORDER-WX3",
                wxV3Result("ORDER-WX3"),
                "{\"out_trade_no\":\"ORDER-WX3\"}",
                "NOTIFY-1"
        );

        InOrder inOrder = inOrder(orderInfoService, inventoryService, paymentInfoService);
        inOrder.verify(orderInfoService).updateStatusByOrderNoIfStatus(
                "ORDER-WX3",
                OrderStatus.NOTPAY,
                OrderStatus.SUCCESS
        );
        inOrder.verify(inventoryService).commitPayment("ORDER-WX3");
        inOrder.verify(paymentInfoService).createPaymentInfo(anyString());
    }

    @Test
    void wxV3ConditionalStatusLoserDoesNotCommitInventory() {
        WxPayOrderService service = wxService();
        when(orderInfoService.getOrderByOrderNoForUpdate("ORDER-WX3"))
                .thenReturn(unpaidOrder("ORDER-WX3", PayType.WXPAY));
        when(orderInfoService.updateStatusByOrderNoIfStatus(
                "ORDER-WX3",
                OrderStatus.NOTPAY,
                OrderStatus.SUCCESS
        )).thenReturn(false);

        ReflectionTestUtils.invokeMethod(
                service,
                "doProcessOrderNotifyInTransaction",
                "ORDER-WX3",
                wxV3Result("ORDER-WX3"),
                "{}",
                "NOTIFY-2"
        );

        verify(inventoryService, never()).commitPayment(anyString());
        verify(paymentInfoService, never()).createPaymentInfo(anyString());
    }

    @Test
    void winningPaymentStatusRequiresARealInventoryTransition() {
        WxPayOrderService service = wxService();
        when(orderInfoService.getOrderByOrderNoForUpdate("ORDER-BROKEN"))
                .thenReturn(unpaidOrder("ORDER-BROKEN", PayType.WXPAY));
        when(orderInfoService.updateStatusByOrderNoIfStatus(
                "ORDER-BROKEN",
                OrderStatus.NOTPAY,
                OrderStatus.SUCCESS
        )).thenReturn(true);
        when(inventoryService.commitPayment("ORDER-BROKEN")).thenReturn(false);

        assertThrows(ConflictException.class, () -> ReflectionTestUtils.invokeMethod(
                service,
                "doProcessOrderNotifyInTransaction",
                "ORDER-BROKEN",
                wxV3Result("ORDER-BROKEN"),
                "{}",
                "NOTIFY-3"
        ));

        verify(paymentInfoService, never()).createPaymentInfo(anyString());
    }

    @Test
    void alipayNotificationCommitsInventoryBeforeWritingPaymentInfo() {
        AliPayServiceImpl service = aliService();
        when(orderInfoService.getOrderByOrderNoForUpdate("ORDER-ALI"))
                .thenReturn(unpaidOrder("ORDER-ALI", PayType.ALIPAY));
        when(orderInfoService.updateStatusByOrderNoIfStatus(
                "ORDER-ALI",
                OrderStatus.NOTPAY,
                OrderStatus.SUCCESS
        )).thenReturn(true);
        when(inventoryService.commitPayment("ORDER-ALI")).thenReturn(true);
        Map<String, String> params = new HashMap<>();
        params.put("out_trade_no", "ORDER-ALI");
        params.put("total_amount", "10.00");

        ReflectionTestUtils.invokeMethod(
                service,
                "doProcessAliPayNotifyInTransaction",
                params,
                "ORDER-ALI",
                "ALI-NOTIFY-1"
        );

        InOrder inOrder = inOrder(orderInfoService, inventoryService, paymentInfoService);
        inOrder.verify(orderInfoService).updateStatusByOrderNoIfStatus(
                "ORDER-ALI",
                OrderStatus.NOTPAY,
                OrderStatus.SUCCESS
        );
        inOrder.verify(inventoryService).commitPayment("ORDER-ALI");
        inOrder.verify(paymentInfoService).createPaymentInfoForAliPay(params);
    }

    @Test
    void wxV3NotificationWithoutAmountOrMerchantIdentityCannotCommitInventory() {
        WxPayOrderService service = wxService();
        when(orderInfoService.getOrderByOrderNoForUpdate("ORDER-WX3-MISSING"))
                .thenReturn(unpaidOrder("ORDER-WX3-MISSING", PayType.WXPAY));

        assertThrows(BizException.class, () -> ReflectionTestUtils.invokeMethod(
                service,
                "doProcessOrderNotifyInTransaction",
                "ORDER-WX3-MISSING",
                Collections.singletonMap("out_trade_no", "ORDER-WX3-MISSING"),
                "{}",
                "NOTIFY-MISSING"
        ));

        verify(orderInfoService, never()).updateStatusByOrderNoIfStatus(
                anyString(),
                any(OrderStatus.class),
                any(OrderStatus.class)
        );
        verify(inventoryService, never()).commitPayment(anyString());
    }

    @Test
    void alipayNotificationWithoutAmountCannotCommitInventory() {
        AliPayServiceImpl service = aliService();
        when(orderInfoService.getOrderByOrderNoForUpdate("ORDER-ALI-MISSING"))
                .thenReturn(unpaidOrder("ORDER-ALI-MISSING", PayType.ALIPAY));
        Map<String, String> params = Collections.singletonMap("out_trade_no", "ORDER-ALI-MISSING");

        assertThrows(BizException.class, () -> ReflectionTestUtils.invokeMethod(
                service,
                "doProcessAliPayNotifyInTransaction",
                params,
                "ORDER-ALI-MISSING",
                "ALI-NOTIFY-MISSING"
        ));

        verify(orderInfoService, never()).updateStatusByOrderNoIfStatus(
                anyString(),
                any(OrderStatus.class),
                any(OrderStatus.class)
        );
        verify(inventoryService, never()).commitPayment(anyString());
    }

    @Test
    void wxV2NotificationCommitsInventoryBeforeWritingPaymentInfo() {
        WxPayV2Controller controller = new WxPayV2Controller(
                mock(WxPayOrderFacade.class),
                mock(WxPayConfig.class),
                orderInfoService,
                paymentInfoService,
                inventoryService,
                mock(DistributedLockTemplate.class),
                mock(TransactionTemplate.class),
                mock(StringRedisTemplate.class)
        );
        OrderInfo order = unpaidOrder("ORDER-WX2", PayType.WXPAY);
        order.setTotalFee(1000);
        when(orderInfoService.getOrderByOrderNoForUpdate("ORDER-WX2")).thenReturn(order);
        when(orderInfoService.updateStatusByOrderNoIfStatus(
                "ORDER-WX2",
                OrderStatus.NOTPAY,
                OrderStatus.SUCCESS
        )).thenReturn(true);
        when(inventoryService.commitPayment("ORDER-WX2")).thenReturn(true);
        Map<String, String> notify = new HashMap<>();
        notify.put("out_trade_no", "ORDER-WX2");

        ReflectionTestUtils.invokeMethod(
                controller,
                "doProcessWxPayV2NotifyInTransaction",
                "ORDER-WX2",
                "TX-2",
                1000L,
                notify,
                "<xml/>"
        );

        InOrder inOrder = inOrder(orderInfoService, inventoryService, paymentInfoService);
        inOrder.verify(orderInfoService).updateStatusByOrderNoIfStatus(
                "ORDER-WX2",
                OrderStatus.NOTPAY,
                OrderStatus.SUCCESS
        );
        inOrder.verify(inventoryService).commitPayment("ORDER-WX2");
        inOrder.verify(paymentInfoService).createPaymentInfoForWxPayV2(notify, "<xml/>");
    }

    @Test
    void wxV2NotificationCannotCommitInventoryForAnAlipayOrder() {
        WxPayV2Controller controller = new WxPayV2Controller(
                mock(WxPayOrderFacade.class),
                mock(WxPayConfig.class),
                orderInfoService,
                paymentInfoService,
                inventoryService,
                mock(DistributedLockTemplate.class),
                mock(TransactionTemplate.class),
                mock(StringRedisTemplate.class)
        );
        OrderInfo order = unpaidOrder("ORDER-WRONG-CHANNEL", PayType.ALIPAY);
        when(orderInfoService.getOrderByOrderNoForUpdate("ORDER-WRONG-CHANNEL")).thenReturn(order);

        assertThrows(BizException.class, () -> ReflectionTestUtils.invokeMethod(
                controller,
                "doProcessWxPayV2NotifyInTransaction",
                "ORDER-WRONG-CHANNEL",
                "TX-WRONG",
                1000L,
                Collections.singletonMap("out_trade_no", "ORDER-WRONG-CHANNEL"),
                "<xml/>"
        ));

        verify(orderInfoService, never()).updateStatusByOrderNoIfStatus(
                anyString(),
                any(OrderStatus.class),
                any(OrderStatus.class)
        );
        verify(inventoryService, never()).commitPayment(anyString());
    }

    private WxPayOrderService wxService() {
        PaymentConfigLoader loader = mock(PaymentConfigLoader.class);
        PaymentAppConfig config = new PaymentAppConfig();
        config.setChannelCode(PaymentConfigLoader.CHANNEL_WXPAY);
        config.setAppid("wx-app-1");
        config.setMchId("wx-mch-1");
        when(loader.getRequiredAppConfig(9L)).thenReturn(config);
        return new WxPayOrderService(
                mock(WxPayConfig.class),
                loader,
                orderInfoService,
                paymentInfoService,
                inventoryService,
                mock(WxPayHttpClient.class),
                mock(WxPayNotificationDecoder.class),
                mock(DistributedLockTemplate.class),
                mock(TransactionTemplate.class)
        );
    }

    private AliPayServiceImpl aliService() {
        return new AliPayServiceImpl(
                orderInfoService,
                mock(AlipayClient.class),
                mock(AlipayProperties.class),
                mock(PaymentConfigLoader.class),
                paymentInfoService,
                inventoryService,
                mock(RefundInfoService.class),
                mock(DistributedLockTemplate.class),
                mock(TransactionTemplate.class)
        );
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

    private Map<String, Object> wxV3Result(String orderNo) {
        Map<String, Object> amount = new HashMap<>();
        amount.put("total", 1000);
        Map<String, Object> result = new HashMap<>();
        result.put("out_trade_no", orderNo);
        result.put("appid", "wx-app-1");
        result.put("mchid", "wx-mch-1");
        result.put("amount", amount);
        return result;
    }
}
