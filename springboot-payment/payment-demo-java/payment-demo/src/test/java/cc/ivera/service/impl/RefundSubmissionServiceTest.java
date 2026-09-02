package cc.ivera.service.impl;

import cc.ivera.entity.OrderInfo;
import cc.ivera.entity.RefundInfo;
import cc.ivera.enums.PayType;
import cc.ivera.enums.RefundApprovalStatus;
import cc.ivera.enums.RefundStatus;
import cc.ivera.exception.BizException;
import cc.ivera.lock.DistributedLockTemplate;
import cc.ivera.service.AliPayService;
import cc.ivera.service.MessageOutboxService;
import cc.ivera.service.OrderInfoService;
import cc.ivera.service.RefundInfoService;
import cc.ivera.service.refund.OrderRefundStatusService;
import cc.ivera.service.wxpay.WxPayRefundFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RefundSubmissionServiceTest {

    private RefundInfoService refundInfoService;
    private WxPayRefundFacade wxPayRefundFacade;
    private RefundInfo refundInfo;
    private RefundApplicationServiceImpl service;

    @BeforeEach
    void setUp() {
        refundInfoService = mock(RefundInfoService.class);
        OrderInfoService orderInfoService = mock(OrderInfoService.class);
        wxPayRefundFacade = mock(WxPayRefundFacade.class);
        DistributedLockTemplate lockTemplate = mock(DistributedLockTemplate.class);
        when(lockTemplate.execute(
                anyString(), anyLong(), anyLong(),
                org.mockito.ArgumentMatchers.<Supplier<Object>>any()))
                .thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(3)).get());

        refundInfo = new RefundInfo();
        refundInfo.setRefundNo("REFUND-1");
        refundInfo.setOrderNo("ORDER-1");
        refundInfo.setApprovalStatus(RefundApprovalStatus.APPROVED.getType());
        refundInfo.setRefundStatus(RefundStatus.CREATED.getType());
        when(refundInfoService.getByRefundNo("REFUND-1")).thenReturn(refundInfo);

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderNo("ORDER-1");
        orderInfo.setPaymentType(PayType.WXPAY.getType());
        when(orderInfoService.getOrderByOrderNo("ORDER-1")).thenReturn(orderInfo);

        service = new RefundApplicationServiceImpl(
                refundInfoService,
                orderInfoService,
                wxPayRefundFacade,
                mock(AliPayService.class),
                mock(OrderRefundStatusService.class),
                lockTemplate,
                mock(MessageOutboxService.class)
        );
    }

    @Test
    void approvedCreatedRefundIsClaimedAndSubmittedWithTheStableRefundNumber() {
        when(refundInfoService.updateRefundIfStatusIn(
                anyString(), any(), any(), any(), any(), any())).thenReturn(true);

        service.submitApprovedRefund("REFUND-1");

        verify(wxPayRefundFacade).executeRefund(refundInfo);
    }

    @Test
    void processingRefundIsSafelyResubmittedBecauseTheChannelUsesRefundNoIdempotency() {
        refundInfo.setRefundStatus(RefundStatus.PROCESSING.getType());

        service.submitApprovedRefund("REFUND-1");

        verify(wxPayRefundFacade).executeRefund(refundInfo);
        verify(refundInfoService, never()).updateRefundIfStatusIn(
                anyString(), any(), org.mockito.ArgumentMatchers.eq(RefundStatus.PROCESSING),
                any(), any(), any());
    }

    @Test
    void uncertainChannelFailureMarksTheLocalSubmissionFailedAndRemainsRetryable() {
        when(refundInfoService.updateRefundIfStatusIn(
                anyString(), any(), any(), any(), any(), any())).thenReturn(true);
        doThrow(new BizException("channel timeout"))
                .when(wxPayRefundFacade).executeRefund(refundInfo);

        assertThatThrownBy(() -> service.submitApprovedRefund("REFUND-1"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("channel timeout");

        verify(refundInfoService).updateRefundIfStatusIn(
                org.mockito.ArgumentMatchers.eq("REFUND-1"),
                any(),
                org.mockito.ArgumentMatchers.eq(RefundStatus.FAILED),
                org.mockito.ArgumentMatchers.contains("channel timeout"),
                any(),
                any()
        );
    }
}
