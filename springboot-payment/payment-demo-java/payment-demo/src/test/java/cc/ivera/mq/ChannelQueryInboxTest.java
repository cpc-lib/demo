package cc.ivera.mq;

import cc.ivera.config.*;
import cc.ivera.entity.OrderInfo;
import cc.ivera.entity.RefundInfo;
import cc.ivera.enums.OrderStatus;
import cc.ivera.enums.PayType;
import cc.ivera.enums.RefundApprovalStatus;
import cc.ivera.enums.RefundStatus;
import cc.ivera.exception.ConflictException;
import cc.ivera.lock.DistributedLockTemplate;
import cc.ivera.service.*;
import cc.ivera.service.impl.AliPayServiceImpl;
import cc.ivera.service.impl.RefundApplicationServiceImpl;
import cc.ivera.service.impl.wxpay.*;
import cc.ivera.service.refund.OrderRefundStatusService;
import cc.ivera.service.refund.RefundStatusSyncResult;
import cc.ivera.service.wxpay.WxPayRefundFacade;
import com.alipay.api.AlipayClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChannelQueryInboxTest {
    private final AtomicBoolean insideTransaction = new AtomicBoolean();
    private MessageConsumeLogService inbox;
    private OrderInfoService orders;
    private PaymentInfoService payments;
    private InventoryService inventory;
    private RefundInfoService refunds;
    private WxPayRefundFacade wxRefunds;
    private AliPayService aliRefunds;
    private DistributedLockTemplate locks;
    private TransactionTemplate transactions;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        inbox = mock(MessageConsumeLogService.class);
        orders = mock(OrderInfoService.class);
        payments = mock(PaymentInfoService.class);
        inventory = mock(InventoryService.class);
        refunds = mock(RefundInfoService.class);
        wxRefunds = mock(WxPayRefundFacade.class);
        aliRefunds = mock(AliPayService.class);
        locks = mock(DistributedLockTemplate.class);
        transactions = mock(TransactionTemplate.class);
        when(locks.execute(anyString(), anyLong(), anyLong(), any(Supplier.class)))
                .thenAnswer(i -> ((Supplier<?>) i.getArgument(3)).get());
        when(transactions.execute(any(TransactionCallback.class))).thenAnswer(i -> {
            insideTransaction.set(true);
            try {
                return ((TransactionCallback<?>) i.getArgument(0)).doInTransaction(mock(TransactionStatus.class));
            } finally {
                insideTransaction.set(false);
            }
        });
        when(inbox.tryStart(anyString(), anyString(), anyString(), anyString())).thenAnswer(i -> {
            assertFalse(insideTransaction.get(), "Claim must commit before business starts");
            return MessageConsumeClaim.claimed("lease");
        });
        doAnswer(i -> {
            assertTrue(insideTransaction.get(), "Inbox complete must join local writes");
            return null;
        }).when(inbox).complete(anyString(), anyString(), anyString());
        doAnswer(i -> {
            assertFalse(insideTransaction.get(), "Fail must follow transaction rollback");
            return null;
        }).when(inbox).fail(anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    void wxPaymentQueryCompletesIndependentInboxWithTheInventoryAndLedger() {
        WxPayOrderService service = wxPaymentService();
        prepareUnpaidOrder(PayType.WXPAY);
        doAnswer(i -> {
            assertFalse(insideTransaction.get(), "Remote query must not hold transaction");
            return wxPaymentJson();
        }).when(service).queryOrder("ORDER-1");
        service.checkOrderStatus("ORDER-1");
        verify(payments).createPaymentInfo(wxPaymentJson());
        verify(inbox).complete(paymentEvent("PAYMENT_QUERY_WX", "wx-tx", "SUCCESS"),
                "wxpay-payment-query", "lease");
    }

    @Test
    void wxPaymentQueryBusyMustBeRetriedWithoutLocalWrites() {
        WxPayOrderService service = wxPaymentService();
        prepareUnpaidOrder(PayType.WXPAY);
        doReturn(wxPaymentJson()).when(service).queryOrder("ORDER-1");
        when(inbox.tryStart(anyString(), anyString(), anyString(), anyString())).thenReturn(MessageConsumeClaim.busy());
        assertThrows(ConflictException.class, () -> service.checkOrderStatus("ORDER-1"));
        verifyNoInteractions(orders, inventory, payments, transactions);
    }

    @Test
    void aliPaymentQueryConsumedDoesNotWriteAgain() {
        AliPayServiceImpl service = aliPaymentService();
        prepareUnpaidOrder(PayType.ALIPAY);
        doReturn(aliPaymentJson()).when(service).queryOrder("ORDER-1");
        when(inbox.tryStart(anyString(), anyString(), anyString(), anyString())).thenReturn(MessageConsumeClaim.consumed());
        service.checkOrderStatus("ORDER-1");
        verifyNoInteractions(orders, inventory, payments, transactions);
    }

    @Test
    void aliPaymentQueryCompletesInsideTheShortTransaction() {
        AliPayServiceImpl service = aliPaymentService();
        prepareUnpaidOrder(PayType.ALIPAY);
        doAnswer(i -> {
            assertFalse(insideTransaction.get());
            return aliPaymentJson();
        }).when(service).queryOrder("ORDER-1");
        service.checkOrderStatus("ORDER-1");
        verify(payments).createPaymentInfoForAliPay(any());
        verify(inbox).complete(paymentEvent("PAYMENT_QUERY_ALIPAY", "ali-tx", "TRADE_SUCCESS"),
                "alipay-payment-query", "lease");
    }

    @Test
    void paymentQueryLocalFailureDoesNotCompleteAndIsRecordedAfterRollback() {
        WxPayOrderService service = wxPaymentService();
        prepareUnpaidOrder(PayType.WXPAY);
        doReturn(wxPaymentJson()).when(service).queryOrder("ORDER-1");
        doThrow(new IllegalStateException("ledger unavailable")).when(payments).createPaymentInfo(anyString());
        assertThrows(IllegalStateException.class, () -> service.checkOrderStatus("ORDER-1"));
        verify(inbox, never()).complete(anyString(), anyString(), anyString());
        verify(inbox).fail(anyString(), eq("wxpay-payment-query"), eq("lease"),
                eq("ledger unavailable"), isNull());
    }

    @ParameterizedTest
    @EnumSource(PayType.class)
    void refundQueryUsesChannelSpecificInboxAndOneLocalTransaction(PayType payType) {
        prepareRefund(payType);
        RefundStatusSyncResult result = refundResult("SUCCESS", RefundStatus.SUCCESS);
        when(wxRefunds.queryRefundStatusForSync("REFUND-1")).thenAnswer(i -> {
            assertFalse(insideTransaction.get());
            return result;
        });
        when(aliRefunds.queryRefundStatusForSync("REFUND-1")).thenAnswer(i -> {
            assertFalse(insideTransaction.get());
            return result;
        });
        doAnswer(i -> {
            assertTrue(insideTransaction.get());
            return true;
        }).when(refunds).syncRefundStatus(result);
        refundService().queryRefundStatus("REFUND-1");
        verify(refunds).repairRefundFromChannel(result);
        verify(refunds).syncRefundStatus(result);
        String consumer = payType == PayType.WXPAY ? "wxpay-refund-query" : "alipay-refund-query";
        verify(inbox).complete(anyString(), eq(consumer), eq("lease"));
    }

    @Test
    void refundQueryBusyDoesNotMutateRefund() {
        prepareRefund(PayType.WXPAY);
        when(wxRefunds.queryRefundStatusForSync("REFUND-1")).thenReturn(refundResult("SUCCESS", RefundStatus.SUCCESS));
        when(inbox.tryStart(anyString(), anyString(), anyString(), anyString())).thenReturn(MessageConsumeClaim.busy());
        assertThrows(ConflictException.class, () -> refundService().queryRefundStatus("REFUND-1"));
        verify(refunds, never()).repairRefundFromChannel(any());
        verify(refunds, never()).syncRefundStatus(any());
    }

    @Test
    void consumedRefundQueryDoesNotRepeatBusinessWrites() {
        prepareRefund(PayType.WXPAY);
        when(wxRefunds.queryRefundStatusForSync("REFUND-1")).thenReturn(refundResult("SUCCESS", RefundStatus.SUCCESS));
        when(inbox.tryStart(anyString(), anyString(), anyString(), anyString())).thenReturn(MessageConsumeClaim.consumed());
        refundService().queryRefundStatus("REFUND-1");
        verify(refunds, never()).repairRefundFromChannel(any());
        verify(refunds, never()).syncRefundStatus(any());
    }

    @Test
    void refundQueryLostLeaseIsFailedAfterRollback() {
        prepareRefund(PayType.WXPAY);
        when(wxRefunds.queryRefundStatusForSync("REFUND-1")).thenReturn(refundResult("SUCCESS", RefundStatus.SUCCESS));
        doThrow(new ConflictException("lease lost")).when(inbox).complete(anyString(), anyString(), anyString());
        assertThrows(ConflictException.class, () -> refundService().queryRefundStatus("REFUND-1"));
        verify(inbox).fail(anyString(), eq("wxpay-refund-query"), eq("lease"), eq("lease lost"), isNull());
    }

    @Test
    void wholeOrderRefundReconciliationUsesSameStableEventAsSingleQuery() {
        prepareRefund(PayType.WXPAY);
        RefundStatusSyncResult result = refundResult("SUCCESS", RefundStatus.SUCCESS);
        when(wxRefunds.queryOrderRefundsForSync("ORDER-1")).thenAnswer(i -> {
            assertFalse(insideTransaction.get());
            return Collections.singletonList(result);
        });
        when(wxRefunds.queryRefundStatusForSync("REFUND-1")).thenReturn(result);
        RefundApplicationServiceImpl service = refundService();
        service.reconcileOrderRefundStatus("ORDER-1");
        service.queryRefundStatus("REFUND-1");
        org.mockito.ArgumentCaptor<String> ids = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(inbox, times(2)).tryStart(ids.capture(), eq("wxpay-refund-query"), eq("REFUND_QUERY_WX"), eq("REFUND-1"));
        assertEquals(ids.getAllValues().get(0), ids.getAllValues().get(1));
    }

    @Test
    void refundQueryStateChangeProducesAnotherEvent() {
        prepareRefund(PayType.WXPAY);
        when(wxRefunds.queryRefundStatusForSync("REFUND-1"))
                .thenReturn(refundResult("PROCESSING", RefundStatus.PROCESSING), refundResult("SUCCESS", RefundStatus.SUCCESS));
        RefundApplicationServiceImpl service = refundService();
        service.queryRefundStatus("REFUND-1");
        service.queryRefundStatus("REFUND-1");
        org.mockito.ArgumentCaptor<String> ids = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(inbox, times(2)).tryStart(ids.capture(), eq("wxpay-refund-query"), eq("REFUND_QUERY_WX"), eq("REFUND-1"));
        assertNotEquals(ids.getAllValues().get(0), ids.getAllValues().get(1));
    }

    private RefundApplicationServiceImpl refundService() {
        return new RefundApplicationServiceImpl(refunds, orders, wxRefunds, aliRefunds,
                mock(OrderRefundStatusService.class), locks, mock(MessageOutboxService.class), inbox, transactions);
    }

    private WxPayOrderService wxPaymentService() {
        PaymentConfigLoader loader = mock(PaymentConfigLoader.class);
        PaymentAppConfig config = new PaymentAppConfig();
        config.setChannelCode(PaymentConfigLoader.CHANNEL_WXPAY);
        config.setAppid("wx-app");
        config.setMchId("wx-mch");
        when(loader.getRequiredAppConfig(9L)).thenReturn(config);
        return spy(new WxPayOrderService(mock(WxPayConfig.class), loader, orders, payments, inventory,
                mock(WxPayHttpClient.class), mock(WxPayNotificationDecoder.class), locks, transactions, inbox));
    }

    private AliPayServiceImpl aliPaymentService() {
        return spy(new AliPayServiceImpl(orders, mock(AlipayClient.class), mock(AlipayProperties.class),
                mock(PaymentConfigLoader.class), payments, inventory, refunds, locks, transactions, inbox));
    }

    private void prepareUnpaidOrder(PayType payType) {
        OrderInfo order = new OrderInfo();
        order.setOrderNo("ORDER-1");
        order.setPaymentType(payType.getType());
        order.setPaymentAppId(9L);
        order.setOrderStatus(OrderStatus.NOTPAY.getType());
        order.setTotalFee(1000);
        when(orders.getOrderByOrderNoForUpdate("ORDER-1")).thenReturn(order);
        when(orders.updateStatusByOrderNoIfStatus("ORDER-1", OrderStatus.NOTPAY, OrderStatus.SUCCESS)).thenReturn(true);
        when(inventory.commitPayment("ORDER-1")).thenReturn(true);
    }

    private void prepareRefund(PayType payType) {
        RefundInfo refund = new RefundInfo();
        refund.setOrderNo("ORDER-1");
        refund.setRefundNo("REFUND-1");
        refund.setApprovalStatus(RefundApprovalStatus.APPROVED.getType());
        when(refunds.getByRefundNo("REFUND-1")).thenReturn(refund);
        when(refunds.listByOrderNo("ORDER-1")).thenReturn(Collections.singletonList(refund));
        OrderInfo order = new OrderInfo();
        order.setOrderNo("ORDER-1");
        order.setPaymentType(payType.getType());
        when(orders.getOrderByOrderNo("ORDER-1")).thenReturn(order);
    }

    private RefundStatusSyncResult refundResult(String channelStatus, RefundStatus status) {
        return RefundStatusSyncResult.of("ORDER-1", "REFUND-1", "channel-refund", channelStatus, status, "{}", 1000, 500);
    }

    private String paymentEvent(String type, String transactionId, String status) {
        return InboxEventIds.resolve(null, type + ":ORDER-1:" + transactionId + ":" + status);
    }

    private String wxPaymentJson() {
        return "{\"out_trade_no\":\"ORDER-1\",\"transaction_id\":\"wx-tx\",\"trade_state\":\"SUCCESS\","
                + "\"appid\":\"wx-app\",\"mchid\":\"wx-mch\",\"amount\":{\"total\":1000}}";
    }

    private String aliPaymentJson() {
        return "{\"alipay_trade_query_response\":{\"out_trade_no\":\"ORDER-1\",\"trade_no\":\"ali-tx\","
                + "\"trade_status\":\"TRADE_SUCCESS\",\"total_amount\":\"10.00\"}}";
    }
}
