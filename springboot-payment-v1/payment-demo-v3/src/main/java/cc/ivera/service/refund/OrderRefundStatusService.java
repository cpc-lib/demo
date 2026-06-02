package cc.ivera.service.refund;

import cc.ivera.entity.OrderInfo;
import cc.ivera.entity.RefundInfo;
import cc.ivera.enums.OrderStatus;
import cc.ivera.enums.RefundStatus;
import cc.ivera.mapper.RefundInfoMapper;
import cc.ivera.service.OrderInfoService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class OrderRefundStatusService {

    private final OrderInfoService orderInfoService;

    private final RefundInfoMapper refundInfoMapper;

    public OrderRefundStatusService(
        OrderInfoService orderInfoService,
        RefundInfoMapper refundInfoMapper
    ) {
        this.orderInfoService = orderInfoService;
        this.refundInfoMapper = refundInfoMapper;
    }

    public void refreshOrderRefundStatus(String orderNo) {
        OrderInfo orderInfo = orderInfoService.getOrderByOrderNo(orderNo);
        if (orderInfo == null) {
            return;
        }

        int successRefundAmount = getRefundAmount(orderNo, RefundStatus.SUCCESS);
        int processingRefundAmount = getRefundAmount(orderNo, RefundStatus.PROCESSING);
        int abnormalRefundAmount = getRefundAmount(orderNo, RefundStatus.ABNORMAL);

        if (successRefundAmount >= orderInfo.getTotalFee()) {
            orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.REFUND_SUCCESS);
            return;
        }

        if (successRefundAmount > 0) {
            orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.PARTIAL_REFUND);
            return;
        }

        if (processingRefundAmount > 0) {
            orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.REFUND_PROCESSING);
            return;
        }

        if (abnormalRefundAmount > 0) {
            orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.REFUND_ABNORMAL);
            return;
        }

        orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.SUCCESS);
    }

    public void refreshOrderRefundStatusByRefundNo(String refundNo) {
        QueryWrapper<RefundInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("refund_no", refundNo);
        RefundInfo refundInfo = refundInfoMapper.selectOne(queryWrapper);
        if (refundInfo != null) {
            refreshOrderRefundStatus(refundInfo.getOrderNo());
        }
    }

    private int getRefundAmount(String orderNo, RefundStatus refundStatus) {
        Integer amount = refundInfoMapper.sumRefundAmountByOrderNoAndStatuses(
                orderNo,
                Collections.singletonList(refundStatus.getType())
        );
        return amount == null ? 0 : amount;
    }
}
