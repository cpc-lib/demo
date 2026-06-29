package cc.ivera.service.impl;

import cc.ivera.entity.OrderInfo;
import cc.ivera.entity.RefundInfo;
import cc.ivera.enums.PayType;
import cc.ivera.enums.RefundStatus;
import cc.ivera.exception.BizException;
import cc.ivera.lock.DistributedLockTemplate;
import cc.ivera.service.AliPayService;
import cc.ivera.service.OrderInfoService;
import cc.ivera.service.RefundInfoService;
import cc.ivera.service.RefundStatusSyncMessageService;
import cc.ivera.service.refund.OrderRefundStatusService;
import cc.ivera.service.wxpay.WxPayRefundFacade;
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

class RefundApplicationRabbitSyncTest {

    @Test
    void approve_sends_refund_status_sync_message_after_refund_submission() {
        Fixture fixture = new Fixture();
        RefundApplicationServiceImpl service = fixture.createService();

        service.approve("REFUND-1", "ok");

        verify(fixture.wxPayRefundFacade).executeRefund(fixture.refundInfo);
        verify(fixture.refundStatusSyncMessageService).sendRefundStatusSyncMessage("REFUND-1");
    }

    @Test
    void approve_does_not_send_refund_status_sync_message_when_refund_submission_fails() {
        Fixture fixture = new Fixture();
        RefundApplicationServiceImpl service = fixture.createService();
        doThrow(new BizException("submit failed")).when(fixture.wxPayRefundFacade).executeRefund(fixture.refundInfo);

        assertThatThrownBy(() -> service.approve("REFUND-1", "ok"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("submit failed");

        verify(fixture.refundStatusSyncMessageService, never()).sendRefundStatusSyncMessage(anyString());
    }

    private static class Fixture {

        private final RefundInfoService refundInfoService = mock(RefundInfoService.class);
        private final OrderInfoService orderInfoService = mock(OrderInfoService.class);
        private final WxPayRefundFacade wxPayRefundFacade = mock(WxPayRefundFacade.class);
        private final AliPayService aliPayService = mock(AliPayService.class);
        private final OrderRefundStatusService orderRefundStatusService = mock(OrderRefundStatusService.class);
        private final DistributedLockTemplate distributedLockTemplate = mock(DistributedLockTemplate.class);
        private final RefundStatusSyncMessageService refundStatusSyncMessageService = mock(RefundStatusSyncMessageService.class);
        private final RefundInfo refundInfo = new RefundInfo();

        private Fixture() {
            refundInfo.setRefundNo("REFUND-1");
            refundInfo.setOrderNo("ORDER-1");
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
            when(refundInfoService.updateRefundIfStatusIn(
                    anyString(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any())).thenReturn(true);
        }

        private RefundApplicationServiceImpl createService() {
            return new RefundApplicationServiceImpl(
                    refundInfoService,
                    orderInfoService,
                    wxPayRefundFacade,
                    aliPayService,
                    orderRefundStatusService,
                    distributedLockTemplate,
                    refundStatusSyncMessageService);
        }
    }
}
