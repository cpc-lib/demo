package cc.ivera.service.payment;

import cc.ivera.config.PaymentAppConfig;
import cc.ivera.config.PaymentConfigLoader;
import cc.ivera.entity.OrderInfo;
import cc.ivera.entity.PaymentOrder;
import cc.ivera.enums.PaymentDisposition;
import cc.ivera.enums.PaymentOrderStatus;
import cc.ivera.enums.UserRole;
import cc.ivera.exception.ConflictException;
import cc.ivera.exception.ForbiddenException;
import cc.ivera.mapper.OrderInfoMapper;
import cc.ivera.mapper.PaymentOrderMapper;
import cc.ivera.security.AuthUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PaymentAttemptServiceTest {

    private OrderInfoMapper orderInfoMapper;
    private PaymentOrderMapper paymentOrderMapper;
    private PaymentConfigLoader paymentConfigLoader;
    private PaymentAttemptService service;
    private AuthUser user;

    @BeforeEach
    void setUp() {
        orderInfoMapper = mock(OrderInfoMapper.class);
        paymentOrderMapper = mock(PaymentOrderMapper.class);
        paymentConfigLoader = mock(PaymentConfigLoader.class);
        service = new PaymentAttemptServiceImpl(
                orderInfoMapper,
                paymentOrderMapper,
                paymentConfigLoader
        );
        user = new AuthUser(7L, "alice", UserRole.USER);
    }

    @Test
    void sameRequestIdWithSameFingerprintReplaysTheSamePaymentNumber() {
        OrderInfo order = payableOrder("ORDER-1", 7L, 1000L, 180_000L);
        PaymentOrder existing = payment(
                "PAY-EXISTING", order.getOrderNo(), 9L, "WXPAY", "V3", "request-1",
                PaymentOrderStatus.UNKNOWN);
        when(orderInfoMapper.selectByOrderNoForUpdate("ORDER-1")).thenReturn(order);
        when(paymentConfigLoader.getRequiredAppConfig(9L)).thenReturn(app(9L, "WXPAY"));
        when(paymentOrderMapper.selectByOrderNoAndRequestIdForUpdate("ORDER-1", "request-1"))
                .thenReturn(existing);

        PaymentOrder replayed = service.createOrReplay(
                "ORDER-1", 9L, "V3", "request-1", user);

        assertSame(existing, replayed);
        assertEquals("PAY-EXISTING", replayed.getPaymentNo());
        assertEquals(PaymentOrderStatus.UNKNOWN, replayed.getStatus());
        verify(paymentOrderMapper, never()).insert(any(PaymentOrder.class));
    }

    @Test
    void sameRequestIdWithDifferentApplicationOrProtocolIsRejected() {
        OrderInfo order = payableOrder("ORDER-1", 7L, 1000L, 180_000L);
        PaymentOrder existing = payment(
                "PAY-EXISTING", order.getOrderNo(), 9L, "WXPAY", "V3", "request-1",
                PaymentOrderStatus.CREATED);
        when(orderInfoMapper.selectByOrderNoForUpdate("ORDER-1")).thenReturn(order);
        when(paymentConfigLoader.getRequiredAppConfig(10L)).thenReturn(app(10L, "WXPAY"));
        when(paymentOrderMapper.selectByOrderNoAndRequestIdForUpdate("ORDER-1", "request-1"))
                .thenReturn(existing);

        assertThrows(ConflictException.class,
                () -> service.createOrReplay("ORDER-1", 10L, "V2", "request-1", user));

        verify(paymentOrderMapper, never()).insert(any(PaymentOrder.class));
    }

    @Test
    void aNewAttemptFreezesOrderApplicationProtocolAmountAndMerchantNumber() {
        OrderInfo order = payableOrder("ORDER-2", 7L, 2500L, 180_000L);
        when(orderInfoMapper.selectByOrderNoForUpdate("ORDER-2")).thenReturn(order);
        when(paymentConfigLoader.getRequiredAppConfig(11L)).thenReturn(app(11L, "ALIPAY"));
        when(paymentOrderMapper.insert(any(PaymentOrder.class))).thenAnswer(invocation -> {
            ((PaymentOrder) invocation.getArgument(0)).setId(31L);
            return 1;
        });

        PaymentOrder created = service.createOrReplay(
                "ORDER-2", 11L, "ALIPAY", "request-2", user);

        ArgumentCaptor<PaymentOrder> captor = ArgumentCaptor.forClass(PaymentOrder.class);
        verify(paymentOrderMapper).insert(captor.capture());
        PaymentOrder inserted = captor.getValue();
        assertSame(inserted, created);
        assertNotNull(inserted.getPaymentNo());
        assertTrue(inserted.getPaymentNo().startsWith("PAY"));
        assertEquals(inserted.getPaymentNo(), inserted.getChannelMerchantOrderNo());
        assertEquals("ORDER-2", inserted.getOrderNo());
        assertEquals(11L, inserted.getPaymentAppId());
        assertEquals("ALIPAY", inserted.getChannel());
        assertEquals("ALIPAY", inserted.getProtocol());
        assertEquals(2500L, inserted.getRequestAmount());
        assertEquals("CNY", inserted.getCurrency());
        assertEquals(PaymentOrderStatus.CREATED, inserted.getStatus());
        assertEquals(PaymentDisposition.NONE, inserted.getDisposition());
        assertEquals(0L, inserted.getPaidAmount());
        assertEquals(0L, inserted.getRefundFrozenAmount());
        assertEquals(0L, inserted.getRefundedAmount());
        assertNotNull(inserted.getRequestFingerprint());
        assertEquals(order.getExpiresAt(), inserted.getExpiresAt());
    }

    @Test
    void anotherRequestMaySelectAnotherApplicationWithoutTouchingInventory() {
        OrderInfo order = payableOrder("ORDER-3", 7L, 5000L, 180_000L);
        when(orderInfoMapper.selectByOrderNoForUpdate("ORDER-3")).thenReturn(order);
        when(paymentConfigLoader.getRequiredAppConfig(9L)).thenReturn(app(9L, "WXPAY"));
        when(paymentConfigLoader.getRequiredAppConfig(11L)).thenReturn(app(11L, "ALIPAY"));
        when(paymentOrderMapper.insert(any(PaymentOrder.class))).thenReturn(1);

        PaymentOrder wx = service.createOrReplay("ORDER-3", 9L, "V3", "request-wx", user);
        PaymentOrder ali = service.createOrReplay("ORDER-3", 11L, "ALIPAY", "request-ali", user);

        assertFalse(wx.getPaymentNo().equals(ali.getPaymentNo()));
        assertEquals("WXPAY", wx.getChannel());
        assertEquals("ALIPAY", ali.getChannel());
        verify(paymentOrderMapper, org.mockito.Mockito.times(2)).insert(any(PaymentOrder.class));
    }

    @Test
    void aNewRequestReusesTheSameApplicationProtocolPendingAttempt() {
        OrderInfo order = payableOrder("ORDER-PENDING", 7L, 1000L, 180_000L);
        PaymentOrder pending = payment(
                "PAY-PENDING", order.getOrderNo(), 9L, "WXPAY", "V3", "request-old",
                PaymentOrderStatus.PENDING);
        when(orderInfoMapper.selectByOrderNoForUpdate("ORDER-PENDING")).thenReturn(order);
        when(paymentConfigLoader.getRequiredAppConfig(9L)).thenReturn(app(9L, "WXPAY"));
        when(paymentOrderMapper.selectReusablePendingForUpdate("ORDER-PENDING", 9L, "V3"))
                .thenReturn(pending);

        PaymentOrder reused = service.createOrReplay(
                "ORDER-PENDING", 9L, "V3", "request-new", user);

        assertSame(pending, reused);
        verify(paymentOrderMapper, never()).insert(any(PaymentOrder.class));
    }

    @Test
    void aNewRequestCannotBypassAnUncertainCreationAttempt() {
        OrderInfo order = payableOrder("ORDER-UNCERTAIN", 7L, 1000L, 180_000L);
        PaymentOrder uncertain = payment(
                "PAY-UNCERTAIN", order.getOrderNo(), 9L, "WXPAY", "V3", "request-old",
                PaymentOrderStatus.UNKNOWN);
        when(orderInfoMapper.selectByOrderNoForUpdate("ORDER-UNCERTAIN")).thenReturn(order);
        when(paymentOrderMapper.selectUncertainByOrderNoForUpdate("ORDER-UNCERTAIN"))
                .thenReturn(uncertain);

        assertThrows(ConflictException.class, () -> service.createOrReplay(
                "ORDER-UNCERTAIN", 11L, "ALIPAY", "request-new", user));

        verify(paymentConfigLoader, never()).getRequiredAppConfig(any(Long.class));
        verify(paymentOrderMapper, never()).insert(any(PaymentOrder.class));
    }

    @Test
    void closingOrNearlyExpiredOrderCannotCreateAnAttempt() {
        OrderInfo closing = payableOrder("ORDER-CLOSING", 7L, 1000L, 180_000L);
        closing.setCloseRequestedAt(new Date());
        when(orderInfoMapper.selectByOrderNoForUpdate("ORDER-CLOSING")).thenReturn(closing);

        assertThrows(ConflictException.class, () -> service.createOrReplay(
                "ORDER-CLOSING", 9L, "V3", "request-1", user));

        OrderInfo expiring = payableOrder("ORDER-EXPIRING", 7L, 1000L, 59_000L);
        when(orderInfoMapper.selectByOrderNoForUpdate("ORDER-EXPIRING")).thenReturn(expiring);
        when(paymentConfigLoader.getRequiredAppConfig(9L)).thenReturn(app(9L, "WXPAY"));

        assertThrows(ConflictException.class, () -> service.createOrReplay(
                "ORDER-EXPIRING", 9L, "V3", "request-2", user));

        verify(paymentOrderMapper, never()).insert(any(PaymentOrder.class));
    }

    @Test
    void adminCannotCreatePaymentAttempt() {
        AuthUser admin = new AuthUser(1L, "admin", UserRole.ADMIN);

        assertThrows(ForbiddenException.class, () -> service.createOrReplay(
                "ORDER-1", 9L, "V3", "request-1", admin));

        verifyNoInteractions(orderInfoMapper, paymentOrderMapper, paymentConfigLoader);
    }

    @Test
    void creationStateCasDoesNotResendUnknownOrOverwriteSuccess() {
        PaymentOrder created = payment(
                "PAY-1", "ORDER-1", 9L, "WXPAY", "V3", "r", PaymentOrderStatus.CREATED);
        OrderInfo order = payableOrder("ORDER-1", 7L, 1000L, 180_000L);
        when(paymentOrderMapper.selectByPaymentNo("PAY-1")).thenReturn(created);
        when(orderInfoMapper.selectByOrderNoForUpdate("ORDER-1")).thenReturn(order);
        when(paymentOrderMapper.selectByPaymentNoForUpdate("PAY-1")).thenReturn(created);
        when(paymentOrderMapper.updateStatus(
                "PAY-1", PaymentOrderStatus.CREATED, PaymentOrderStatus.CREATING)).thenReturn(1);
        assertTrue(service.claimForCreation("PAY-1"));

        when(paymentOrderMapper.updateStatus(
                "PAY-UNKNOWN", PaymentOrderStatus.CREATED, PaymentOrderStatus.CREATING)).thenReturn(0);
        when(paymentOrderMapper.selectByPaymentNo("PAY-UNKNOWN")).thenReturn(payment(
                "PAY-UNKNOWN", "ORDER-1", 9L, "WXPAY", "V3", "r", PaymentOrderStatus.UNKNOWN));
        assertFalse(service.claimForCreation("PAY-UNKNOWN"));

        when(paymentOrderMapper.updateCreationResult(
                "PAY-SUCCESS", PaymentOrderStatus.CREATING, PaymentOrderStatus.PENDING, "params"))
                .thenReturn(0);
        PaymentOrder success = payment(
                "PAY-SUCCESS", "ORDER-1", 9L, "WXPAY", "V3", "r", PaymentOrderStatus.SUCCESS);
        when(paymentOrderMapper.selectByPaymentNo("PAY-SUCCESS")).thenReturn(success);

        assertSame(success, service.markPending("PAY-SUCCESS", "params"));
    }

    @Test
    void creationClaimStopsWhenAnotherAttemptMayAlreadyBeInFlight() {
        PaymentOrder created = payment(
                "PAY-NEW", "ORDER-1", 9L, "WXPAY", "V3", "new", PaymentOrderStatus.CREATED);
        PaymentOrder uncertain = payment(
                "PAY-OLD", "ORDER-1", 9L, "WXPAY", "V3", "old", PaymentOrderStatus.UNKNOWN);
        when(paymentOrderMapper.selectByPaymentNo("PAY-NEW")).thenReturn(created);
        when(orderInfoMapper.selectByOrderNoForUpdate("ORDER-1"))
                .thenReturn(payableOrder("ORDER-1", 7L, 1000L, 180_000L));
        when(paymentOrderMapper.selectUncertainByOrderNoForUpdate("ORDER-1"))
                .thenReturn(uncertain);

        assertFalse(service.claimForCreation("PAY-NEW"));

        verify(paymentOrderMapper, never()).updateStatus(
                "PAY-NEW", PaymentOrderStatus.CREATED, PaymentOrderStatus.CREATING);
    }

    @Test
    void definitiveCreationFailureMayResolveAnEarlierUnknownResult() {
        PaymentOrder failed = payment(
                "PAY-UNKNOWN", "ORDER-1", 9L, "WXPAY", "V3", "r", PaymentOrderStatus.FAILED);
        when(paymentOrderMapper.updateCreationResult(
                "PAY-UNKNOWN", PaymentOrderStatus.CREATING, PaymentOrderStatus.FAILED, "not-found"))
                .thenReturn(0);
        when(paymentOrderMapper.updateCreationResult(
                "PAY-UNKNOWN", PaymentOrderStatus.UNKNOWN, PaymentOrderStatus.FAILED, "not-found"))
                .thenReturn(1);
        when(paymentOrderMapper.selectByPaymentNo("PAY-UNKNOWN")).thenReturn(failed);

        assertSame(failed, service.markFailed("PAY-UNKNOWN", "not-found"));
    }

    private OrderInfo payableOrder(String orderNo, Long userId, Long amount, long remainingMillis) {
        OrderInfo order = new OrderInfo();
        order.setId(21L);
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setOrderStatus("WAIT_PAY");
        order.setPayStatus("UNPAID");
        order.setPayableAmount(amount);
        order.setTotalFee(amount.intValue());
        order.setExpiresAt(new Date(System.currentTimeMillis() + remainingMillis));
        return order;
    }

    private PaymentAppConfig app(Long appId, String channel) {
        PaymentAppConfig config = new PaymentAppConfig();
        config.setAppId(appId);
        config.setChannelCode(channel);
        return config;
    }

    private PaymentOrder payment(
            String paymentNo,
            String orderNo,
            Long paymentAppId,
            String channel,
            String protocol,
            String requestId,
            PaymentOrderStatus status
    ) {
        PaymentOrder payment = new PaymentOrder();
        payment.setId(31L);
        payment.setPaymentNo(paymentNo);
        payment.setOrderNo(orderNo);
        payment.setPaymentAppId(paymentAppId);
        payment.setChannel(channel);
        payment.setProtocol(protocol);
        payment.setChannelMerchantOrderNo(paymentNo);
        payment.setRequestAmount(1000L);
        payment.setCurrency("CNY");
        payment.setStatus(status);
        payment.setDisposition(PaymentDisposition.NONE);
        payment.setRequestId(requestId);
        payment.setRequestFingerprint(PaymentRequestFingerprint.sha256(
                orderNo, paymentAppId, channel, protocol, 1000L));
        payment.setPaidAmount(0L);
        payment.setRefundFrozenAmount(0L);
        payment.setRefundedAmount(0L);
        return payment;
    }
}
