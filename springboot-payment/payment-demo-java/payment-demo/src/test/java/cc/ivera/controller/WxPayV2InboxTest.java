package cc.ivera.controller;

import cc.ivera.config.WxPayConfig;
import cc.ivera.entity.OrderInfo;
import cc.ivera.enums.OrderStatus;
import cc.ivera.enums.PayType;
import cc.ivera.exception.ConflictException;
import cc.ivera.lock.DistributedLockTemplate;
import cc.ivera.service.InventoryService;
import cc.ivera.service.MessageConsumeClaim;
import cc.ivera.service.MessageConsumeLogService;
import cc.ivera.service.OrderInfoService;
import cc.ivera.service.PaymentInfoService;
import cc.ivera.service.wxpay.WxPayOrderFacade;
import com.github.wxpay.sdk.WXPayUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class WxPayV2InboxTest {

    private static final String PARTNER_KEY = "test-partner-key";
    private static final String CONSUMER = "wxpay-v2-payment-notify";

    private OrderInfoService orderInfoService;
    private PaymentInfoService paymentInfoService;
    private InventoryService inventoryService;
    private MessageConsumeLogService consumeLogService;
    private DistributedLockTemplate lockTemplate;
    private PlatformTransactionManager transactionManager;
    private WxPayV2Controller controller;
    private boolean insideTransaction;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        orderInfoService = mock(OrderInfoService.class);
        paymentInfoService = mock(PaymentInfoService.class);
        inventoryService = mock(InventoryService.class);
        consumeLogService = mock(MessageConsumeLogService.class);
        lockTemplate = mock(DistributedLockTemplate.class);
        transactionManager = mock(PlatformTransactionManager.class);
        when(lockTemplate.execute(anyString(), anyLong(), anyLong(), any(Supplier.class)))
                .thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(3)).get());
        when(transactionManager.getTransaction(any())).thenAnswer(invocation -> {
            insideTransaction = true;
            return new SimpleTransactionStatus();
        });
        doAnswer(invocation -> {
            insideTransaction = false;
            return null;
        }).when(transactionManager).commit(any());
        doAnswer(invocation -> {
            insideTransaction = false;
            return null;
        }).when(transactionManager).rollback(any());
        WxPayConfig config = mock(WxPayConfig.class);
        when(config.getPartnerKey()).thenReturn(PARTNER_KEY);
        controller = new WxPayV2Controller(mock(WxPayOrderFacade.class), config,
                orderInfoService, paymentInfoService, inventoryService, lockTemplate,
                new TransactionTemplate(transactionManager), consumeLogService);
    }

    @Test
    void invalidSignatureNeverClaimsInbox() throws Exception {
        assertEquals("FAIL", responseCode(notifyBody(), "wrong-key"));

        verifyNoInteractions(consumeLogService, orderInfoService, paymentInfoService, inventoryService, lockTemplate);
    }

    @Test
    void invalidAmountNeverClaimsInbox() throws Exception {
        Map<String, String> notify = notifyBody();
        notify.put("total_fee", "invalid");

        assertEquals("FAIL", responseCode(notify, PARTNER_KEY));

        verifyNoInteractions(consumeLogService, orderInfoService, paymentInfoService, inventoryService, lockTemplate);
    }

    @Test
    void missingOrderNumberNeverClaimsInbox() throws Exception {
        Map<String, String> notify = notifyBody();
        notify.remove("out_trade_no");

        assertEquals("FAIL", responseCode(notify, PARTNER_KEY));

        verifyNoInteractions(consumeLogService, orderInfoService, paymentInfoService, inventoryService, lockTemplate);
    }

    @Test
    void consumedNotificationAcknowledgesWithoutRepeatingBusiness() throws Exception {
        stubClaim(MessageConsumeClaim.consumed());

        assertEquals("SUCCESS", responseCode(notifyBody(), PARTNER_KEY));

        verifyNoInteractions(orderInfoService, paymentInfoService, inventoryService, lockTemplate, transactionManager);
        verify(consumeLogService, never()).complete(anyString(), anyString(), anyString());
    }

    @Test
    void busyNotificationFailsWithoutRepeatingBusiness() throws Exception {
        stubClaim(MessageConsumeClaim.busy());

        assertEquals("FAIL", responseCode(notifyBody(), PARTNER_KEY));

        verifyNoInteractions(orderInfoService, paymentInfoService, inventoryService, lockTemplate, transactionManager);
        verify(consumeLogService, never()).fail(anyString(), anyString(), anyString(), anyString(), any());
    }

    @Test
    void claimedPaymentAndInboxCompleteShareBusinessTransaction() throws Exception {
        stubClaim(MessageConsumeClaim.claimed("lease-v2"));
        stubUnpaidOrder();
        when(inventoryService.commitPayment("ORDER-V2")).thenAnswer(invocation -> {
            assertTrue(insideTransaction);
            return true;
        });
        doAnswer(invocation -> {
            assertTrue(insideTransaction);
            return null;
        }).when(paymentInfoService).createPaymentInfoForWxPayV2(any(), anyString());
        doAnswer(invocation -> {
            assertTrue(insideTransaction);
            verify(paymentInfoService).createPaymentInfoForWxPayV2(any(), anyString());
            return null;
        }).when(consumeLogService).complete("wx-v2-tx", CONSUMER, "lease-v2");

        assertEquals("SUCCESS", responseCode(notifyBody(), PARTNER_KEY));

        verify(consumeLogService).complete("wx-v2-tx", CONSUMER, "lease-v2");
        verify(transactionManager).commit(any());
        verify(transactionManager, never()).rollback(any());
        assertFalse(insideTransaction);
    }

    @Test
    void businessFailureRollsBackBeforeFailingInbox() throws Exception {
        stubClaim(MessageConsumeClaim.claimed("lease-v2"));
        stubUnpaidOrder();
        assertFailureRecordedOutsideTransaction();

        assertEquals("FAIL", responseCode(notifyBody(), PARTNER_KEY));

        verify(transactionManager).rollback(any());
        verify(transactionManager, never()).commit(any());
        verify(consumeLogService).fail(eq("wx-v2-tx"), eq(CONSUMER), eq("lease-v2"), anyString(), eq(null));
        verify(consumeLogService, never()).complete(anyString(), anyString(), anyString());
        verifyNoInteractions(paymentInfoService);
    }

    @Test
    void inboxCompletionFailureRollsBackAndReturnsFail() throws Exception {
        stubClaim(MessageConsumeClaim.claimed("lease-v2"));
        stubUnpaidOrder();
        when(inventoryService.commitPayment("ORDER-V2")).thenReturn(true);
        doAnswer(invocation -> {
            assertTrue(insideTransaction);
            throw new ConflictException("租约已过期");
        }).when(consumeLogService).complete("wx-v2-tx", CONSUMER, "lease-v2");
        assertFailureRecordedOutsideTransaction();

        assertEquals("FAIL", responseCode(notifyBody(), PARTNER_KEY));

        verify(transactionManager).rollback(any());
        verify(transactionManager, never()).commit(any());
        verify(consumeLogService).fail(eq("wx-v2-tx"), eq(CONSUMER), eq("lease-v2"), anyString(), eq(null));
    }

    private void assertFailureRecordedOutsideTransaction() {
        doAnswer(invocation -> {
            assertFalse(insideTransaction);
            return null;
        }).when(consumeLogService).fail(eq("wx-v2-tx"), eq(CONSUMER), eq("lease-v2"), anyString(), eq(null));
    }

    private void stubClaim(MessageConsumeClaim claim) {
        when(consumeLogService.tryStart("wx-v2-tx", CONSUMER, "PAYMENT_NOTIFY_WX_V2", "ORDER-V2"))
                .thenAnswer(invocation -> {
                    assertFalse(insideTransaction);
                    return claim;
                });
    }

    private void stubUnpaidOrder() {
        OrderInfo order = new OrderInfo();
        order.setOrderNo("ORDER-V2");
        order.setOrderStatus(OrderStatus.NOTPAY.getType());
        order.setPaymentType(PayType.WXPAY.getType());
        order.setTotalFee(1000);
        when(orderInfoService.getOrderByOrderNoForUpdate("ORDER-V2")).thenReturn(order);
        when(orderInfoService.updateStatusByOrderNoIfStatus("ORDER-V2", OrderStatus.NOTPAY, OrderStatus.SUCCESS))
                .thenReturn(true);
    }

    private String responseCode(Map<String, String> notify, String signingKey) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent(WXPayUtil.generateSignedXml(notify, signingKey).getBytes(StandardCharsets.UTF_8));
        return WXPayUtil.xmlToMap(controller.wxNotify(request)).get("return_code");
    }

    private Map<String, String> notifyBody() {
        Map<String, String> notify = new HashMap<>();
        notify.put("return_code", "SUCCESS");
        notify.put("result_code", "SUCCESS");
        notify.put("out_trade_no", "ORDER-V2");
        notify.put("transaction_id", "wx-v2-tx");
        notify.put("total_fee", "1000");
        notify.put("nonce_str", "nonce-v2");
        return notify;
    }
}
