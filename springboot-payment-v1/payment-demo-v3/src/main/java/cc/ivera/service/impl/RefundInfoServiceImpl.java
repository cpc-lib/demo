package cc.ivera.service.impl;

import cc.ivera.entity.OrderInfo;
import cc.ivera.entity.RefundInfo;
import cc.ivera.enums.OrderStatus;
import cc.ivera.enums.RefundApprovalStatus;
import cc.ivera.enums.RefundStatus;
import cc.ivera.exception.BizException;
import cc.ivera.mapper.RefundInfoMapper;
import cc.ivera.service.OrderInfoService;
import cc.ivera.service.RefundInfoService;
import cc.ivera.util.OrderNoUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RefundInfoServiceImpl extends ServiceImpl<RefundInfoMapper, RefundInfo> implements RefundInfoService {

    private final OrderInfoService orderInfoService;

    public RefundInfoServiceImpl(
        OrderInfoService orderInfoService
    ) {
        this.orderInfoService = orderInfoService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RefundInfo createRefundApplication(String orderNo, Integer refundAmount, String reason) {
        if (orderNo == null || orderNo.trim().isEmpty()) {
            throw new BizException("订单号不能为空");
        }

        OrderInfo orderInfo = orderInfoService.getOrderByOrderNoForUpdate(orderNo);
        if (orderInfo == null) {
            throw new BizException("订单不存在");
        }
        if (orderInfo.getTotalFee() == null || orderInfo.getTotalFee() <= 0) {
            throw new BizException("订单金额非法");
        }

        String orderStatus = orderInfo.getOrderStatus();
        boolean refundable = OrderStatus.SUCCESS.getType().equals(orderStatus)
                || OrderStatus.PARTIAL_REFUND.getType().equals(orderStatus)
                || OrderStatus.REFUND_PROCESSING.getType().equals(orderStatus);
        if (!refundable) {
            throw new BizException("当前订单状态不允许申请退款：" + orderStatus);
        }

        int reservedRefundAmount = getReservedRefundAmount(orderNo);
        int remainRefundAmount = orderInfo.getTotalFee() - reservedRefundAmount;
        if (remainRefundAmount <= 0) {
            throw new BizException("金额已经全部退还处理");
        }

        int actualRefundAmount = refundAmount == null ? remainRefundAmount : refundAmount;
        if (actualRefundAmount <= 0) {
            throw new BizException("退款金额必须大于0");
        }
        if (actualRefundAmount > remainRefundAmount) {
            throw new BizException("退款申请金额超过可退余额，可退金额为：" + remainRefundAmount + "分");
        }

        RefundInfo refundInfo = new RefundInfo();
        refundInfo.setOrderNo(orderNo);
        refundInfo.setRefundNo(OrderNoUtils.getRefundNo());
        refundInfo.setTotalFee(orderInfo.getTotalFee());
        refundInfo.setRefund(actualRefundAmount);
        refundInfo.setReason((reason == null || reason.trim().isEmpty()) ? "正常退款" : reason.trim());
        refundInfo.setApprovalStatus(RefundApprovalStatus.PENDING.getType());
        refundInfo.setRefundStatus(RefundStatus.CREATED.getType());
        refundInfo.setApproveRemark(null);
        refundInfo.setApprovedTime(null);
        baseMapper.insert(refundInfo);

        return refundInfo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRefundToProcessing(String refundNo, String contentReturn) {
        updateRefund(refundNo, null, RefundStatus.PROCESSING.getType(), contentReturn, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRefundToSuccess(String refundNo, String refundId, String content) {
        updateRefund(refundNo, refundId, RefundStatus.SUCCESS.getType(), content, content);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRefundToFailed(String refundNo, String content) {
        updateRefund(refundNo, null, RefundStatus.FAILED.getType(), content, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRefundToAbnormal(String refundNo, String content) {
        updateRefund(refundNo, null, RefundStatus.ABNORMAL.getType(), content, content);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRefundToClosed(String refundNo, String content) {
        updateRefund(refundNo, null, RefundStatus.CLOSED.getType(), content, content);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateRefundIfStatusIn(String refundNo,
                                          String refundId,
                                          RefundStatus targetStatus,
                                          String contentReturn,
                                          String contentNotify,
                                          Collection<RefundStatus> currentStatuses) {
        if (refundNo == null || refundNo.trim().isEmpty()) {
            throw new BizException("退款单号不能为空");
        }
        if (targetStatus == null) {
            throw new BizException("目标退款状态不能为空");
        }
        if (currentStatuses == null || currentStatuses.isEmpty()) {
            throw new BizException("当前退款状态不能为空");
        }

        List<String> currentStatusTypes = currentStatuses.stream()
                .map(RefundStatus::getType)
                .collect(Collectors.toList());

        QueryWrapper<RefundInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("refund_no", refundNo)
                .in("refund_status", currentStatusTypes);

        RefundInfo refundInfo = new RefundInfo();
        refundInfo.setRefundId(refundId);
        refundInfo.setRefundStatus(targetStatus.getType());
        if (contentReturn != null) {
            refundInfo.setContentReturn(contentReturn);
        }
        if (contentNotify != null) {
            refundInfo.setContentNotify(contentNotify);
        }

        boolean updated = baseMapper.update(refundInfo, queryWrapper) > 0;
        if (updated) {
            refreshOrderRefundStatusByRefundNo(refundNo);
        }
        return updated;
    }

    private int getSuccessRefundAmount(String orderNo) {
        Integer amount = baseMapper.sumRefundAmountByOrderNoAndStatuses(
                orderNo,
                Collections.singletonList(RefundStatus.SUCCESS.getType())
        );
        return amount == null ? 0 : amount;
    }

    private int getOccupiedRefundAmount(String orderNo) {
        Integer amount = baseMapper.sumRefundAmountByOrderNoAndStatuses(
                orderNo,
                Arrays.asList(RefundStatus.PROCESSING.getType(), RefundStatus.SUCCESS.getType())
        );
        return amount == null ? 0 : amount;
    }

    private int getReservedRefundAmount(String orderNo) {
        List<RefundInfo> refundInfoList = listByOrderNo(orderNo);
        int total = 0;
        for (RefundInfo refundInfo : refundInfoList) {
            if (refundInfo == null || refundInfo.getRefund() == null) {
                continue;
            }
            boolean reserved = RefundApprovalStatus.PENDING.getType().equals(refundInfo.getApprovalStatus())
                    || (RefundApprovalStatus.APPROVED.getType().equals(refundInfo.getApprovalStatus())
                    && !RefundStatus.FAILED.getType().equals(refundInfo.getRefundStatus())
                    && !RefundStatus.CLOSED.getType().equals(refundInfo.getRefundStatus()));
            if (reserved) {
                total += refundInfo.getRefund();
            }
        }
        return total;
    }

    @Override
    public RefundInfo getByRefundNo(String refundNo) {
        QueryWrapper<RefundInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("refund_no", refundNo);
        return baseMapper.selectOne(queryWrapper);
    }

    @Override
    public List<RefundInfo> listByOrderNo(String orderNo) {
        QueryWrapper<RefundInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_no", orderNo).orderByDesc("create_time");
        return baseMapper.selectList(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markApprovalPassed(String refundNo, String approveRemark) {
        RefundInfo refundInfo = getByRefundNo(refundNo);
        if (refundInfo == null) {
            throw new BizException("退款申请单不存在");
        }
        if (RefundApprovalStatus.REJECTED.getType().equals(refundInfo.getApprovalStatus())) {
            throw new BizException("退款申请单已拒绝，不能再通过");
        }
        if (RefundApprovalStatus.APPROVED.getType().equals(refundInfo.getApprovalStatus())) {
            return;
        }
        if (RefundStatus.SUCCESS.getType().equals(refundInfo.getRefundStatus())) {
            throw new BizException("该退款申请单已退款成功，请勿重复处理");
        }
        if (RefundStatus.PROCESSING.getType().equals(refundInfo.getRefundStatus())) {
            throw new BizException("该退款申请单已在退款处理中，请勿重复处理");
        }

        QueryWrapper<RefundInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("refund_no", refundNo);

        RefundInfo update = new RefundInfo();
        update.setApprovalStatus(RefundApprovalStatus.APPROVED.getType());
        update.setApproveRemark((approveRemark == null || approveRemark.trim().isEmpty()) ? "审核通过" : approveRemark.trim());
        update.setApprovedTime(new Date());
        baseMapper.update(update, queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markApprovalRejected(String refundNo, String approveRemark) {
        RefundInfo refundInfo = getByRefundNo(refundNo);
        if (refundInfo == null) {
            throw new BizException("退款申请单不存在");
        }
        if (RefundApprovalStatus.APPROVED.getType().equals(refundInfo.getApprovalStatus())) {
            throw new BizException("退款申请单已审核通过，不能再拒绝");
        }
        if (RefundApprovalStatus.REJECTED.getType().equals(refundInfo.getApprovalStatus())) {
            return;
        }

        QueryWrapper<RefundInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("refund_no", refundNo);

        RefundInfo update = new RefundInfo();
        update.setApprovalStatus(RefundApprovalStatus.REJECTED.getType());
        update.setApproveRemark((approveRemark == null || approveRemark.trim().isEmpty()) ? "审核拒绝" : approveRemark.trim());
        update.setApprovedTime(new Date());
        update.setRefundStatus(RefundStatus.CLOSED.getType());
        baseMapper.update(update, queryWrapper);
    }

    @Override
    public void refreshOrderRefundStatus(String orderNo) {
        OrderInfo orderInfo = orderInfoService.getOrderByOrderNo(orderNo);
        if (orderInfo == null) {
            return;
        }

        int successRefundAmount = getSuccessRefundAmount(orderNo);
        int occupiedRefundAmount = getOccupiedRefundAmount(orderNo);

        if (successRefundAmount >= orderInfo.getTotalFee()) {
            orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.REFUND_SUCCESS);
            return;
        }

        if (successRefundAmount > 0) {
            orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.PARTIAL_REFUND);
            return;
        }

        if (occupiedRefundAmount > 0) {
            orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.REFUND_PROCESSING);
            return;
        }

        orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.SUCCESS);
    }

    private void updateRefund(String refundNo,
                              String refundId,
                              String refundStatus,
                              String contentReturn,
                              String contentNotify) {
        QueryWrapper<RefundInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("refund_no", refundNo);

        RefundInfo refundInfo = new RefundInfo();
        refundInfo.setRefundId(refundId);
        refundInfo.setRefundStatus(refundStatus);
        if (contentReturn != null) {
            refundInfo.setContentReturn(contentReturn);
        }
        if (contentNotify != null) {
            refundInfo.setContentNotify(contentNotify);
        }
        baseMapper.update(refundInfo, queryWrapper);

        RefundInfo latestRefundInfo = baseMapper.selectOne(queryWrapper);
        if (latestRefundInfo != null) {
            refreshOrderRefundStatus(latestRefundInfo.getOrderNo());
        }
    }

    private void refreshOrderRefundStatusByRefundNo(String refundNo) {
        QueryWrapper<RefundInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("refund_no", refundNo);
        RefundInfo latestRefundInfo = baseMapper.selectOne(queryWrapper);
        if (latestRefundInfo != null) {
            refreshOrderRefundStatus(latestRefundInfo.getOrderNo());
        }
    }
}
