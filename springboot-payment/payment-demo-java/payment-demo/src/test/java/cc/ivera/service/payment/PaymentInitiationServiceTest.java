package cc.ivera.service.payment;

import cc.ivera.entity.OrderInfo;
import cc.ivera.entity.PaymentOrder;
import cc.ivera.enums.PaymentOrderStatus;
import cc.ivera.enums.UserRole;
import cc.ivera.exception.BizException;
import cc.ivera.mapper.OrderInfoMapper;
import cc.ivera.security.AuthUser;
import cc.ivera.util.JsonUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PaymentInitiationServiceTest {

    private PaymentAttemptService paymentAttemptService;
    private PaymentChannelGateway paymentChannelGateway;
    private OrderInfoMapper orderInfoMapper;
    private PaymentInitiationService service;
    private AuthUser user;

    @BeforeEach
    void setUp() {
        paymentAttemptService = mock(PaymentAttemptService.class);
        paymentChannelGateway = mock(PaymentChannelGateway.class);
        orderInfoMapper = mock(OrderInfoMapper.class);
        service = new PaymentInitiationServiceImpl(
                paymentAttemptService,
                paymentChannelGateway,
                orderInfoMapper
        );
        user = new AuthUser(7L, "alice", UserRole.USER);
    }

    @Test
    void claimedAttemptCallsTheChannelOutsideTheLocalCreationStepAndStoresParameters() {
        PaymentOrder created = payment("PAY-1", "ORDER-1", PaymentOrderStatus.CREATED);
        PaymentOrder pending = payment("PAY-1", "ORDER-1", PaymentOrderStatus.PENDING);
        OrderInfo order = order("ORDER-1");
        Map<String, Object> channelParams = Collections.singletonMap("codeUrl", "wx://code");
        String serialized = JsonUtils.toJson(channelParams);
        when(paymentAttemptService.createOrReplay("ORDER-1", 9L, "V3", "request-1", user))
                .thenReturn(created);
        when(paymentAttemptService.claimForCreation("PAY-1")).thenReturn(true);
        when(orderInfoMapper.selectByOrderNo("ORDER-1")).thenReturn(order);
        when(paymentChannelGateway.create(created, order, "127.0.0.1")).thenReturn(channelParams);
        when(paymentAttemptService.markPending("PAY-1", serialized)).thenReturn(pending);

        PaymentOrder result = service.initiate(
                "ORDER-1", 9L, "V3", "request-1", "127.0.0.1", user);

        assertSame(pending, result);
        org.mockito.InOrder ordered = inOrder(paymentAttemptService, orderInfoMapper, paymentChannelGateway);
        ordered.verify(paymentAttemptService)
                .createOrReplay("ORDER-1", 9L, "V3", "request-1", user);
        ordered.verify(paymentAttemptService).claimForCreation("PAY-1");
        ordered.verify(orderInfoMapper).selectByOrderNo("ORDER-1");
        ordered.verify(paymentChannelGateway).create(created, order, "127.0.0.1");
        ordered.verify(paymentAttemptService).markPending("PAY-1", serialized);
    }

    @Test
    void exactReplayOfPendingAttemptDoesNotSendAnotherChannelRequest() {
        PaymentOrder pending = payment("PAY-1", "ORDER-1", PaymentOrderStatus.PENDING);
        when(paymentAttemptService.createOrReplay("ORDER-1", 9L, "V3", "request-1", user))
                .thenReturn(pending);

        assertSame(pending, service.initiate(
                "ORDER-1", 9L, "V3", "request-1", "127.0.0.1", user));

        verify(paymentAttemptService, never()).claimForCreation("PAY-1");
        verifyNoInteractions(paymentChannelGateway, orderInfoMapper);
    }

    @Test
    void ambiguousChannelFailureLeavesTheSamePaymentNumberUnknown() {
        PaymentOrder created = payment("PAY-1", "ORDER-1", PaymentOrderStatus.CREATED);
        OrderInfo order = order("ORDER-1");
        BizException channelFailure = new BizException("channel timeout");
        when(paymentAttemptService.createOrReplay("ORDER-1", 9L, "V3", "request-1", user))
                .thenReturn(created);
        when(paymentAttemptService.claimForCreation("PAY-1")).thenReturn(true);
        when(orderInfoMapper.selectByOrderNo("ORDER-1")).thenReturn(order);
        when(paymentChannelGateway.create(created, order, "127.0.0.1"))
                .thenThrow(channelFailure);

        BizException thrown = assertThrows(BizException.class, () -> service.initiate(
                "ORDER-1", 9L, "V3", "request-1", "127.0.0.1", user));

        assertSame(channelFailure, thrown);
        verify(paymentAttemptService).markUnknown("PAY-1");
        verify(paymentAttemptService, never()).markFailed("PAY-1", "channel timeout");
    }

    @Test
    void definitiveChannelRejectionMarksTheSamePaymentNumberFailed() {
        PaymentOrder created = payment("PAY-1", "ORDER-1", PaymentOrderStatus.CREATED);
        OrderInfo order = order("ORDER-1");
        PaymentChannelRejectedException rejection =
                new PaymentChannelRejectedException("merchant configuration invalid");
        when(paymentAttemptService.createOrReplay("ORDER-1", 9L, "V3", "request-1", user))
                .thenReturn(created);
        when(paymentAttemptService.claimForCreation("PAY-1")).thenReturn(true);
        when(orderInfoMapper.selectByOrderNo("ORDER-1")).thenReturn(order);
        when(paymentChannelGateway.create(created, order, "127.0.0.1"))
                .thenThrow(rejection);

        assertSame(rejection, assertThrows(PaymentChannelRejectedException.class,
                () -> service.initiate(
                        "ORDER-1", 9L, "V3", "request-1", "127.0.0.1", user)));

        verify(paymentAttemptService).markFailed("PAY-1", "merchant configuration invalid");
        verify(paymentAttemptService, never()).markUnknown("PAY-1");
    }

    @Test
    void aLostCreationClaimReloadsCurrentStateWithoutCallingTheChannel() {
        PaymentOrder created = payment("PAY-1", "ORDER-1", PaymentOrderStatus.CREATED);
        PaymentOrder current = payment("PAY-1", "ORDER-1", PaymentOrderStatus.SUCCESS);
        when(paymentAttemptService.createOrReplay("ORDER-1", 9L, "V3", "request-1", user))
                .thenReturn(created);
        when(paymentAttemptService.claimForCreation("PAY-1")).thenReturn(false);
        when(paymentAttemptService.getByPaymentNo("PAY-1")).thenReturn(current);

        PaymentOrder result = service.initiate(
                "ORDER-1", 9L, "V3", "request-1", "127.0.0.1", user);

        assertSame(current, result);
        assertEquals(PaymentOrderStatus.SUCCESS, result.getStatus());
        verifyNoInteractions(paymentChannelGateway, orderInfoMapper);
    }

    private PaymentOrder payment(String paymentNo, String orderNo, PaymentOrderStatus status) {
        PaymentOrder paymentOrder = new PaymentOrder();
        paymentOrder.setPaymentNo(paymentNo);
        paymentOrder.setOrderNo(orderNo);
        paymentOrder.setPaymentAppId(9L);
        paymentOrder.setChannel("WXPAY");
        paymentOrder.setProtocol("V3");
        paymentOrder.setStatus(status);
        return paymentOrder;
    }

    private OrderInfo order(String orderNo) {
        OrderInfo order = new OrderInfo();
        order.setOrderNo(orderNo);
        order.setUserId(7L);
        order.setTitle("Java");
        order.setPayableAmount(1000L);
        order.setTotalFee(1000);
        return order;
    }
}
