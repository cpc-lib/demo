package cc.ivera.service.impl;

import cc.ivera.entity.OrderInfo;
import cc.ivera.entity.RefundInfo;
import cc.ivera.enums.PayType;
import cc.ivera.enums.RefundApprovalStatus;
import cc.ivera.enums.RefundStatus;
import cc.ivera.exception.ConflictException;
import cc.ivera.lock.DistributedLockTemplate;
import cc.ivera.mq.OutboxEventTypes;
import cc.ivera.service.AliPayService;
import cc.ivera.service.MessageOutboxService;
import cc.ivera.service.OrderInfoService;
import cc.ivera.service.RefundInfoService;
import cc.ivera.service.refund.OrderRefundStatusService;
import cc.ivera.service.wxpay.WxPayRefundFacade;
import cc.ivera.util.JsonUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RefundApplicationRabbitSyncTest {

    @Test
    void approvalOnlyMarksApprovedAndPersistsARefundSubmitOutboxEvent() {
        Fixture fixture = new Fixture();
        RefundApplicationServiceImpl service = fixture.createService();

        service.approve("REFUND-1", "ok");

        verify(fixture.refundInfoService).markApprovalPassed("REFUND-1", "ok");
        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(fixture.messageOutboxService).insertOnce(
                eq("REFUND_SUBMIT_REQUESTED:REFUND-1"),
                eq("REFUND"),
                eq("REFUND-1"),
                eq(OutboxEventTypes.REFUND_SUBMIT_REQUESTED),
                payload.capture()
        );
        assertThat(JsonUtils.toObjectMap(payload.getValue()))
                .containsEntry("refundNo", "REFUND-1");
        verifyNoInteractions(fixture.wxPayRefundFacade, fixture.aliPayService);
        verify(fixture.refundInfoService, never()).updateRefundIfStatusIn(
                anyString(), any(), any(), any(), any(), any());
    }

    @Test
    void outboxFailurePropagatesWithoutCallingARefundChannel() {
        Fixture fixture = new Fixture();
        RefundApplicationServiceImpl service = fixture.createService();
        doThrow(new ConflictException("outbox failed"))
                .when(fixture.messageOutboxService).insertOnce(
                        anyString(), anyString(), anyString(), anyString(), anyString());

        assertThatThrownBy(() -> service.approve("REFUND-1", "ok"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("outbox failed");

        verifyNoInteractions(fixture.wxPayRefundFacade, fixture.aliPayService);
    }

    private static class Fixture {

        private final RefundInfoService refundInfoService = mock(RefundInfoService.class);
        private final OrderInfoService orderInfoService = mock(OrderInfoService.class);
        private final WxPayRefundFacade wxPayRefundFacade = mock(WxPayRefundFacade.class);
        private final AliPayService aliPayService = mock(AliPayService.class);
        private final OrderRefundStatusService orderRefundStatusService = mock(OrderRefundStatusService.class);
        private final DistributedLockTemplate distributedLockTemplate = mock(DistributedLockTemplate.class);
        private final MessageOutboxService messageOutboxService = mock(MessageOutboxService.class);
        private final RefundInfo refundInfo = new RefundInfo();

        private Fixture() {
            refundInfo.setRefundNo("REFUND-1");
            refundInfo.setOrderNo("ORDER-1");
            refundInfo.setApprovalStatus(RefundApprovalStatus.PENDING.getType());
            refundInfo.setRefundStatus(RefundStatus.CREATED.getType());

            OrderInfo orderInfo = new OrderInfo();
            orderInfo.setOrderNo("ORDER-1");
            orderInfo.setPaymentType(PayType.WXPAY.getType());

            when(distributedLockTemplate.execute(
                    anyString(),
                    anyLong(),
                    anyLong(),
                    org.mockito.ArgumentMatchers.<Supplier<Object>>any()))
                    .thenAnswer(invocation -> {
                        Supplier<?> supplier = invocation.getArgument(3);
                        return supplier.get();
                    });
            when(refundInfoService.getByRefundNo("REFUND-1")).thenReturn(refundInfo);
            when(orderInfoService.getOrderByOrderNo("ORDER-1")).thenReturn(orderInfo);
        }

        private RefundApplicationServiceImpl createService() {
            return new RefundApplicationServiceImpl(
                    refundInfoService,
                    orderInfoService,
                    wxPayRefundFacade,
                    aliPayService,
                    orderRefundStatusService,
                    distributedLockTemplate,
                    messageOutboxService);
        }
    }
}
