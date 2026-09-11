package cc.ivera.mq;

import cc.ivera.config.AlipayProperties;
import cc.ivera.config.PaymentAppConfig;
import cc.ivera.config.PaymentConfigLoader;
import cc.ivera.config.WxPayConfig;
import cc.ivera.controller.WxPayV2Controller;
import cc.ivera.entity.OrderInfo;
import cc.ivera.entity.RefundInfo;
import cc.ivera.enums.OrderStatus;
import cc.ivera.enums.PayType;
import cc.ivera.exception.ConflictException;
import cc.ivera.exception.BizException;
import cc.ivera.lock.DistributedLockTemplate;
import cc.ivera.service.InventoryService;
import cc.ivera.service.MessageConsumeClaim;
import cc.ivera.service.MessageConsumeLogService;
import cc.ivera.service.OrderInfoService;
import cc.ivera.service.PaymentInfoService;
import cc.ivera.service.RefundInfoService;
import cc.ivera.service.impl.AliPayServiceImpl;
import cc.ivera.service.impl.wxpay.WxPayHttpClient;
import cc.ivera.service.impl.wxpay.WxPayNotificationDecoder;
import cc.ivera.service.impl.wxpay.WxPayOrderService;
import cc.ivera.service.impl.wxpay.WxPayRefundService;
import cc.ivera.service.wxpay.WxPayOrderFacade;
import com.alipay.api.AlipayClient;
import com.github.wxpay.sdk.WXPayUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ChannelNotificationInboxTest {

    private OrderInfoService orderInfoService;
    private PaymentInfoService paymentInfoService;
    private InventoryService inventoryService;
    private MessageConsumeLogService consumeLogService;
    private DistributedLockTemplate lockTemplate;
    private TransactionTemplate transactionTemplate;
    private final AtomicBoolean insideTransaction = new AtomicBoolean();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        orderInfoService = mock(OrderInfoService.class);
        paymentInfoService = mock(PaymentInfoService.class);
        inventoryService = mock(InventoryService.class);
        consumeLogService = mock(MessageConsumeLogService.class);
        lockTemplate = mock(DistributedLockTemplate.class);
        transactionTemplate = mock(TransactionTemplate.class);
        when(lockTemplate.execute(anyString(), anyLong(), anyLong(), any(Supplier.class)))
                .thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(3)).get());
        when(transactionTemplate.execute(any(TransactionCallback.class))).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            insideTransaction.set(true);
            try {
                return callback.doInTransaction(mock(TransactionStatus.class));
            } finally {
                insideTransaction.set(false);
            }
        });
        doAnswer(invocation -> {
            assertTrue(insideTransaction.get(), "Inbox complete must join business transaction");
            return null;
        }).when(consumeLogService).complete(anyString(), anyString(), anyString());
        doAnswer(invocation -> {
            assertFalse(insideTransaction.get(), "Inbox fail must run after rollback");
            return null;
        }).when(consumeLogService).fail(anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    void wxV3BusyNotificationIsRetryableInsteadOfAcknowledged() throws Exception {
        WxPayNotificationDecoder decoder = mock(WxPayNotificationDecoder.class);
        when(decoder.decryptResource(any())).thenReturn(wxPaymentBody("ORDER-WX"));
        when(consumeLogService.tryStart(
                "notify-wx-1",
                "wxpay-payment-notify",
                "PAYMENT_NOTIFY_WX_V3",
                "ORDER-WX"
        )).thenReturn(MessageConsumeClaim.busy());

        WxPayOrderService service = wxService(decoder);

        assertThrows(ConflictException.class, () -> service.processOrder(singletonBody("notify-wx-1")));
        verifyNoInteractions(orderInfoService, paymentInfoService, inventoryService, lockTemplate);
    }

    @Test
    void wxV3CompletesInboxOnlyAfterLocalPaymentWork() throws Exception {
        WxPayNotificationDecoder decoder = mock(WxPayNotificationDecoder.class);
        when(decoder.decryptResource(any())).thenReturn(wxPaymentBody("ORDER-WX"));
        when(consumeLogService.tryStart(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(MessageConsumeClaim.claimed("lease-wx"));
        when(orderInfoService.getOrderByOrderNoForUpdate("ORDER-WX")).thenReturn(unpaidOrder("ORDER-WX", PayType.WXPAY));
        when(orderInfoService.updateStatusByOrderNoIfStatus("ORDER-WX", OrderStatus.NOTPAY, OrderStatus.SUCCESS))
                .thenReturn(true);
        when(inventoryService.commitPayment("ORDER-WX")).thenReturn(true);

        WxPayOrderService service = wxService(decoder);
        service.processOrder(singletonBody("notify-wx-1"));

        org.mockito.InOrder order = inOrder(paymentInfoService, consumeLogService);
        order.verify(paymentInfoService).createPaymentInfo(anyString());
        order.verify(consumeLogService).complete("notify-wx-1", "wxpay-payment-notify", "lease-wx");
    }

    @Test
    void alipayConsumedNotificationDoesNotRepeatLocalWork() {
        when(consumeLogService.tryStart(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(MessageConsumeClaim.consumed());

        aliService().processOrder(aliPaymentParams());

        verifyNoInteractions(orderInfoService, paymentInfoService, inventoryService, lockTemplate);
        verify(consumeLogService).tryStart(aliEventId(), "alipay-payment-notify", "PAYMENT_NOTIFY_ALIPAY", "ORDER-ALI");
    }

    @Test
    void failedAlipayNotificationReleasesInboxForRetry() {
        when(consumeLogService.tryStart(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(MessageConsumeClaim.claimed("lease-ali"));
        when(orderInfoService.getOrderByOrderNoForUpdate("ORDER-ALI"))
                .thenReturn(unpaidOrder("ORDER-ALI", PayType.ALIPAY));
        when(orderInfoService.updateStatusByOrderNoIfStatus("ORDER-ALI", OrderStatus.NOTPAY, OrderStatus.SUCCESS))
                .thenReturn(true);
        when(inventoryService.commitPayment("ORDER-ALI")).thenReturn(false);

        assertThrows(ConflictException.class, () -> aliService().processOrder(aliPaymentParams()));

        verify(consumeLogService).fail(
                eq(aliEventId()),
                eq("alipay-payment-notify"),
                eq("lease-ali"),
                anyString(),
                eq(null)
        );
        verify(consumeLogService, never()).complete(anyString(), anyString(), anyString());
    }

    @Test
    void alipayUsesTradeAndStatusIdentityEvenWhenNotificationIdChanges() {
        when(consumeLogService.tryStart(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(MessageConsumeClaim.consumed());
        Map<String, String> params = aliPaymentParams();
        aliService().processOrder(params);
        params.put("notify_id", "different-delivery-id");
        aliService().processOrder(params);
        verify(consumeLogService, org.mockito.Mockito.times(2)).tryStart(
                aliEventId(), "alipay-payment-notify", "PAYMENT_NOTIFY_ALIPAY", "ORDER-ALI");
    }

    @Test
    void alipayBusyNotificationDoesNotAcknowledgeOrWrite() {
        when(consumeLogService.tryStart(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(MessageConsumeClaim.busy());
        assertThrows(ConflictException.class, () -> aliService().processOrder(aliPaymentParams()));
        verifyNoInteractions(orderInfoService, paymentInfoService, inventoryService, lockTemplate);
    }

    @Test
    void alipaySuccessCompletesInsideBusinessTransaction() {
        when(consumeLogService.tryStart(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(MessageConsumeClaim.claimed("lease-ali"));
        when(orderInfoService.getOrderByOrderNoForUpdate("ORDER-ALI"))
                .thenReturn(unpaidOrder("ORDER-ALI", PayType.ALIPAY));
        when(orderInfoService.updateStatusByOrderNoIfStatus("ORDER-ALI", OrderStatus.NOTPAY, OrderStatus.SUCCESS))
                .thenReturn(true);
        when(inventoryService.commitPayment("ORDER-ALI")).thenReturn(true);
        aliService().processOrder(aliPaymentParams());
        verify(paymentInfoService).createPaymentInfoForAliPay(any());
        verify(consumeLogService).complete(aliEventId(), "alipay-payment-notify", "lease-ali");
    }

    @Test
    void wxV3ConsumedNotificationDoesNotWriteAgain() throws Exception {
        WxPayNotificationDecoder decoder = mock(WxPayNotificationDecoder.class);
        when(decoder.decryptResource(any())).thenReturn(wxPaymentBody("ORDER-WX"));
        when(consumeLogService.tryStart(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(MessageConsumeClaim.consumed());
        wxService(decoder).processOrder(singletonBody("notify-wx-1"));
        verifyNoInteractions(orderInfoService, paymentInfoService, inventoryService, lockTemplate);
    }

    @Test
    void wxV3LostInboxLeaseFailsAfterBusinessTransactionExits() throws Exception {
        WxPayNotificationDecoder decoder = mock(WxPayNotificationDecoder.class);
        when(decoder.decryptResource(any())).thenReturn(wxPaymentBody("ORDER-WX"));
        when(consumeLogService.tryStart(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(MessageConsumeClaim.claimed("lease-wx"));
        when(orderInfoService.getOrderByOrderNoForUpdate("ORDER-WX"))
                .thenReturn(unpaidOrder("ORDER-WX", PayType.WXPAY));
        doThrow(new ConflictException("lease expired")).when(consumeLogService)
                .complete(anyString(), anyString(), anyString());
        assertThrows(ConflictException.class, () -> wxService(decoder).processOrder(singletonBody("notify-wx-1")));
        verify(consumeLogService).fail(eq("notify-wx-1"), eq("wxpay-payment-notify"),
                eq("lease-wx"), eq("lease expired"), eq(null));
    }

    @Test
    void wxRefundNotificationUsesItsOwnInboxConsumer() throws Exception {
        WxPayNotificationDecoder decoder = mock(WxPayNotificationDecoder.class);
        when(decoder.decryptResource(any())).thenReturn(
                "{\"out_trade_no\":\"ORDER-1\",\"out_refund_no\":\"REFUND-1\","
                        + "\"refund_id\":\"WX-REFUND-1\",\"refund_status\":\"SUCCESS\","
                        + "\"amount\":{\"total\":1000,\"refund\":500}}"
        );
        when(consumeLogService.tryStart(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(MessageConsumeClaim.claimed("lease-refund"));
        RefundInfoService refundInfoService = mock(RefundInfoService.class);
        when(refundInfoService.getByRefundNo("REFUND-1")).thenReturn(new RefundInfo());

        WxPayRefundService service = new WxPayRefundService(
                mock(WxPayConfig.class),
                mock(PaymentConfigLoader.class),
                orderInfoService,
                refundInfoService,
                mock(WxPayHttpClient.class),
                decoder,
                lockTemplate,
                transactionTemplate,
                consumeLogService
        );
        service.processRefund(singletonBody("notify-refund-1"));

        verify(refundInfoService).syncRefundStatus(any());
        verify(consumeLogService).complete(
                "notify-refund-1",
                "wxpay-refund-notify",
                "lease-refund"
        );
    }

    @Test
    void wxRefundBusyDoesNotAcknowledgeOrWrite() throws Exception {
        RefundInfoService refunds = mock(RefundInfoService.class);
        when(consumeLogService.tryStart(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(MessageConsumeClaim.busy());
        WxPayRefundService service = wxRefundService(refunds);
        assertThrows(ConflictException.class, () -> service.processRefund(singletonBody("refund-event")));
        verifyNoInteractions(refunds, lockTemplate, transactionTemplate);
    }

    @Test
    void wxRefundConsumedDoesNotRepeatBusinessWork() throws Exception {
        RefundInfoService refunds = mock(RefundInfoService.class);
        when(consumeLogService.tryStart(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(MessageConsumeClaim.consumed());
        wxRefundService(refunds).processRefund(singletonBody("refund-event"));
        verifyNoInteractions(refunds, lockTemplate, transactionTemplate);
    }

    @Test
    void wxRefundLocalFailureIsRecordedOnlyAfterRollback() throws Exception {
        RefundInfoService refunds = mock(RefundInfoService.class);
        when(refunds.getByRefundNo("REFUND-1")).thenReturn(new RefundInfo());
        when(consumeLogService.tryStart(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(MessageConsumeClaim.claimed("refund-lease"));
        doThrow(new IllegalStateException("refund write failed")).when(refunds).syncRefundStatus(any());
        WxPayRefundService service = wxRefundService(refunds);
        assertThrows(IllegalStateException.class, () -> service.processRefund(singletonBody("refund-event")));
        verify(consumeLogService).fail(eq("refund-event"), eq("wxpay-refund-notify"),
                eq("refund-lease"), eq("refund write failed"), eq(null));
        verify(consumeLogService, never()).complete(anyString(), anyString(), anyString());
    }

    @Test
    void wxRefundUnknownLocalRefundMustNotBePermanentlyConsumed() throws Exception {
        RefundInfoService refunds = mock(RefundInfoService.class);
        when(consumeLogService.tryStart(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(MessageConsumeClaim.claimed("refund-lease"));
        WxPayRefundService service = wxRefundService(refunds);
        assertThrows(BizException.class, () -> service.processRefund(singletonBody("refund-event")));
        verify(consumeLogService, never()).complete(anyString(), anyString(), anyString());
        verify(consumeLogService).fail(eq("refund-event"), eq("wxpay-refund-notify"),
                eq("refund-lease"), anyString(), eq(null));
    }

    private WxPayRefundService wxRefundService(RefundInfoService refunds) throws Exception {
        WxPayNotificationDecoder decoder = mock(WxPayNotificationDecoder.class);
        when(decoder.decryptResource(any())).thenReturn(
                "{\"out_trade_no\":\"ORDER-1\",\"out_refund_no\":\"REFUND-1\","
                        + "\"refund_id\":\"WX-REFUND-1\",\"refund_status\":\"SUCCESS\","
                        + "\"amount\":{\"total\":1000,\"refund\":500}}");
        return new WxPayRefundService(mock(WxPayConfig.class), mock(PaymentConfigLoader.class),
                orderInfoService, refunds, mock(WxPayHttpClient.class), decoder, lockTemplate,
                transactionTemplate, consumeLogService);
    }

    @Test
    void wxV2BusyNotificationReturnsFailSoWechatRetries() throws Exception {
        String partnerKey = "test-partner-key";
        WxPayConfig wxPayConfig = mock(WxPayConfig.class);
        when(wxPayConfig.getPartnerKey()).thenReturn(partnerKey);
        when(consumeLogService.tryStart(
                "wx-v2-tx-1",
                "wxpay-v2-payment-notify",
                "PAYMENT_NOTIFY_WX_V2",
                "ORDER-WX2"
        )).thenReturn(MessageConsumeClaim.busy());
        WxPayV2Controller controller = new WxPayV2Controller(
                mock(WxPayOrderFacade.class),
                wxPayConfig,
                orderInfoService,
                paymentInfoService,
                inventoryService,
                lockTemplate,
                transactionTemplate,
                consumeLogService
        );
        Map<String, String> notify = new HashMap<>();
        notify.put("return_code", "SUCCESS");
        notify.put("result_code", "SUCCESS");
        notify.put("out_trade_no", "ORDER-WX2");
        notify.put("transaction_id", "wx-v2-tx-1");
        notify.put("total_fee", "1000");
        notify.put("nonce_str", "nonce-1");
        String xml = WXPayUtil.generateSignedXml(notify, partnerKey);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent(xml.getBytes(StandardCharsets.UTF_8));

        String response = controller.wxNotify(request);

        assertTrue(response.contains("FAIL"));
        verifyNoInteractions(orderInfoService, paymentInfoService, inventoryService, lockTemplate);
    }

    private WxPayOrderService wxService(WxPayNotificationDecoder decoder) {
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
                decoder,
                lockTemplate,
                transactionTemplate,
                consumeLogService
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
                lockTemplate,
                transactionTemplate,
                consumeLogService
        );
    }

    private Map<String, Object> singletonBody(String notifyId) {
        Map<String, Object> body = new HashMap<>();
        body.put("id", notifyId);
        return body;
    }

    private Map<String, String> aliPaymentParams() {
        Map<String, String> params = new HashMap<>();
        params.put("out_trade_no", "ORDER-ALI");
        params.put("notify_id", "notify-ali-1");
        params.put("trade_no", "ali-tx-1");
        params.put("trade_status", "TRADE_SUCCESS");
        params.put("total_amount", "10.00");
        return params;
    }

    private String aliEventId() {
        return InboxEventIds.resolve(null, "PAYMENT_NOTIFY_ALIPAY:ORDER-ALI:ali-tx-1:TRADE_SUCCESS");
    }

    private OrderInfo unpaidOrder(String orderNo, PayType payType) {
        OrderInfo order = new OrderInfo();
        order.setOrderNo(orderNo);
        order.setOrderStatus(OrderStatus.NOTPAY.getType());
        order.setPaymentType(payType.getType());
        order.setPaymentAppId(9L);
        order.setTotalFee(1000);
        return order;
    }

    private String wxPaymentBody(String orderNo) {
        return "{\"out_trade_no\":\"" + orderNo + "\",\"transaction_id\":\"wx-tx-1\","
                + "\"trade_state\":\"SUCCESS\",\"appid\":\"wx-app-1\",\"mchid\":\"wx-mch-1\","
                + "\"amount\":{\"total\":1000}}";
    }
}
