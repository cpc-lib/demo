package cc.ivera.service.impl;

import cc.ivera.entity.OrderInfo;
import cc.ivera.entity.RefundInfo;
import cc.ivera.enums.OrderStatus;
import cc.ivera.enums.RefundApprovalStatus;
import cc.ivera.enums.RefundStatus;
import cc.ivera.exception.BizException;
import cc.ivera.mapper.RefundInfoMapper;
import cc.ivera.service.OrderInfoService;
import cc.ivera.service.refund.OrderRefundStatusService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RefundAmountReservationTest {
    @ParameterizedTest
    @EnumSource(value = RefundStatus.class, names = {"FAILED", "ABNORMAL", "PROCESSING", "SUCCESS"})
    void approvedRefundStillReservesAmountWhileOutcomeIsUncertainOrSuccessful(RefundStatus status) {
        OrderInfoService orders = mock(OrderInfoService.class);
        RefundInfoMapper mapper = mock(RefundInfoMapper.class);
        RefundInfoServiceImpl service = new RefundInfoServiceImpl(orders, mock(OrderRefundStatusService.class));
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        OrderInfo order = new OrderInfo();
        order.setOrderNo("ORDER-1");
        order.setOrderStatus(OrderStatus.SUCCESS.getType());
        order.setTotalFee(1000);
        when(orders.getOrderByOrderNoForUpdate("ORDER-1")).thenReturn(order);
        RefundInfo existing = new RefundInfo();
        existing.setApprovalStatus(RefundApprovalStatus.APPROVED.getType());
        existing.setRefundStatus(status.getType());
        existing.setRefund(800);
        when(mapper.selectList(any())).thenReturn(Collections.singletonList(existing));

        assertThrows(BizException.class, () -> service.createRefundApplication("ORDER-1", 300, "partial"));
        verify(mapper, never()).insert(any());
    }
}
