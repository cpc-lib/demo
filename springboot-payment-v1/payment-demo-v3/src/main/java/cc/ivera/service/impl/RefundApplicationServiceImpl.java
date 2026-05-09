package cc.ivera.service.impl;

import cc.ivera.entity.OrderInfo;
import cc.ivera.entity.RefundInfo;
import cc.ivera.enums.PayType;
import cc.ivera.enums.RefundApprovalStatus;
import cc.ivera.enums.RefundStatus;
import cc.ivera.exception.BizException;
import cc.ivera.service.AliPayService;
import cc.ivera.service.OrderInfoService;
import cc.ivera.service.RefundApplicationService;
import cc.ivera.service.RefundInfoService;
import cc.ivera.service.refund.OrderRefundStatusService;
import cc.ivera.service.refund.RefundStatusSyncResult;
import cc.ivera.service.refund.RefundStatusSyncService;
import cc.ivera.service.wxpay.WxPayRefundFacade;
import cc.ivera.support.DistributedLockExecutor;
import cc.ivera.support.PaymentLockKeys;
import cc.ivera.util.JsonUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

@Service
public class RefundApplicationServiceImpl implements RefundApplicationService {

    private final RefundInfoService refundInfoService;

    private final OrderInfoService orderInfoService;

    private final WxPayRefundFacade wxPayRefundFacade;

    private final AliPayService aliPayService;

    private final OrderRefundStatusService orderRefundStatusService;

    private final RefundStatusSyncService refundStatusSyncService;

    private final DistributedLockExecutor distributedLockExecutor;

    public RefundApplicationServiceImpl(
        RefundInfoService refundInfoService,
        OrderInfoService orderInfoService,
        WxPayRefundFacade wxPayRefundFacade,
        AliPayService aliPayService,
        OrderRefundStatusService orderRefundStatusService,
        RefundStatusSyncService refundStatusSyncService,
        DistributedLockExecutor distributedLockExecutor
    ) {
        this.refundInfoService = refundInfoService;
        this.orderInfoService = orderInfoService;
        this.wxPayRefundFacade = wxPayRefundFacade;
        this.aliPayService = aliPayService;
        this.orderRefundStatusService = orderRefundStatusService;
        this.refundStatusSyncService = refundStatusSyncService;
        this.distributedLockExecutor = distributedLockExecutor;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RefundInfo createApplication(String orderNo, Integer refundAmount, String reason) {
        return refundInfoService.createRefundApplication(orderNo, refundAmount, reason);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void approve(String refundNo, String approveRemark) {
        RefundInfo refundInfo = refundInfoService.getByRefundNo(refundNo);
        if (refundInfo == null) {
            throw new BizException("退款申请单不存在");
        }
        if (RefundApprovalStatus.REJECTED.getType().equals(refundInfo.getApprovalStatus())) {
            throw new BizException("退款申请单已拒绝，不能审核通过");
        }
        if (RefundStatus.SUCCESS.getType().equals(refundInfo.getRefundStatus())) {
            throw new BizException("该退款申请单已退款成功，请勿重复处理");
        }
        if (RefundStatus.PROCESSING.getType().equals(refundInfo.getRefundStatus())) {
            throw new BizException("该退款申请单已在退款处理中，请勿重复处理");
        }

        OrderInfo orderInfo = orderInfoService.getOrderByOrderNo(refundInfo.getOrderNo());
        if (orderInfo == null) {
            throw new BizException("订单不存在");
        }
        validateSupportedPayType(orderInfo.getPaymentType());

        refundInfoService.markApprovalPassed(refundNo, approveRemark);
        claimRefundForExecution(refundNo);
        RefundInfo latestRefundInfo = refundInfoService.getByRefundNo(refundNo);

        try {
            executeRefund(orderInfo.getPaymentType(), latestRefundInfo);
        } catch (RuntimeException e) {
            markRefundSubmitFailed(refundNo, e);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(String refundNo, String approveRemark) {
        refundInfoService.markApprovalRejected(refundNo, approveRemark);
    }

    @Override
    public List<RefundInfo> listAll() {
        QueryWrapper<RefundInfo> queryWrapper = new QueryWrapper<RefundInfo>().orderByDesc("create_time");
        return refundInfoService.list(queryWrapper);
    }

    @Override
    public List<RefundInfo> listByOrderNo(String orderNo) {
        return refundInfoService.listByOrderNo(orderNo);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public RefundInfo queryRefundStatus(String refundNo) {
        return executeWithRefundLock(refundNo, () -> queryRefundStatusInLock(refundNo));
    }

    private RefundInfo queryRefundStatusInLock(String refundNo) {
        RefundInfo refundInfo = refundInfoService.getByRefundNo(refundNo);
        if (refundInfo == null) {
            throw new BizException("退款申请单不存在");
        }
        if (!RefundApprovalStatus.APPROVED.getType().equals(refundInfo.getApprovalStatus())) {
            throw new BizException("退款申请尚未提交支付渠道，不能主动查询渠道状态");
        }

        OrderInfo orderInfo = orderInfoService.getOrderByOrderNo(refundInfo.getOrderNo());
        if (orderInfo == null) {
            throw new BizException("订单不存在");
        }
        validateSupportedPayType(orderInfo.getPaymentType());

        syncRefundByChannel(orderInfo.getPaymentType(), refundInfo);

        return refundInfoService.getByRefundNo(refundNo);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<RefundInfo> reconcileOrderRefundStatus(String orderNo) {
        OrderInfo orderInfo = orderInfoService.getOrderByOrderNo(orderNo);
        if (orderInfo == null) {
            throw new BizException("订单不存在");
        }
        validateSupportedPayType(orderInfo.getPaymentType());

        Set<String> channelRefundNos = reconcileWxOrderRefundsIfNeeded(orderInfo);
        syncLocalRefundsForOrder(orderInfo, channelRefundNos);
        orderRefundStatusService.refreshOrderRefundStatus(orderNo);
        return refundInfoService.listByOrderNo(orderNo);
    }

    private void claimRefundForExecution(String refundNo) {
        boolean updated = refundInfoService.updateRefundIfStatusIn(
                refundNo,
                null,
                RefundStatus.PROCESSING,
                null,
                null,
                Arrays.asList(RefundStatus.CREATED, RefundStatus.FAILED, RefundStatus.ABNORMAL));
        if (!updated) {
            throw new BizException("该退款申请单状态已变化，请勿重复处理");
        }
    }

    private void executeRefund(String paymentType, RefundInfo refundInfo) {
        if (PayType.WXPAY.getType().equals(paymentType)) {
            wxPayRefundFacade.executeRefund(refundInfo);
            return;
        }
        if (PayType.ALIPAY.getType().equals(paymentType)) {
            aliPayService.executeRefund(refundInfo);
            return;
        }
        throw new BizException("不支持的支付方式：" + paymentType);
    }

    private void validateSupportedPayType(String paymentType) {
        if (!PayType.WXPAY.getType().equals(paymentType)
                && !PayType.ALIPAY.getType().equals(paymentType)) {
            throw new BizException("不支持的支付方式：" + paymentType);
        }
    }

    private void markRefundSubmitFailed(String refundNo, Exception exception) {
        String message = exception.getMessage() == null ? "退款提交失败" : exception.getMessage();
        refundInfoService.updateRefundIfStatusIn(
                refundNo,
                null,
                RefundStatus.FAILED,
                JsonUtils.toJson(Collections.singletonMap("message", message)),
                null,
                Arrays.asList(RefundStatus.CREATED, RefundStatus.PROCESSING));
    }

    private RefundStatusSyncResult queryRefundStatusFromChannel(String paymentType, String refundNo) {
        if (PayType.WXPAY.getType().equals(paymentType)) {
            return wxPayRefundFacade.queryRefundStatusForSync(refundNo);
        }
        if (PayType.ALIPAY.getType().equals(paymentType)) {
            return aliPayService.queryRefundStatusForSync(refundNo);
        }
        throw new BizException("不支持的支付方式：" + paymentType);
    }

    private Set<String> reconcileWxOrderRefundsIfNeeded(OrderInfo orderInfo) {
        Set<String> channelRefundNos = new HashSet<>();
        if (!PayType.WXPAY.getType().equals(orderInfo.getPaymentType())) {
            return channelRefundNos;
        }

        List<RefundStatusSyncResult> syncResults = wxPayRefundFacade.queryOrderRefundsForSync(orderInfo.getOrderNo());
        for (RefundStatusSyncResult syncResult : syncResults) {
            validateChannelOrderResult(orderInfo, syncResult);
            repairAndSyncRefundInLock(syncResult);
            if (syncResult.getRefundNo() != null) {
                channelRefundNos.add(syncResult.getRefundNo());
            }
        }
        return channelRefundNos;
    }

    private void syncLocalRefundsForOrder(OrderInfo orderInfo, Set<String> channelRefundNos) {
        List<RefundInfo> refundInfoList = refundInfoService.listByOrderNo(orderInfo.getOrderNo());
        for (RefundInfo refundInfo : refundInfoList) {
            if (!shouldSyncLocalRefund(refundInfo)) {
                continue;
            }
            if (channelRefundNos.contains(refundInfo.getRefundNo())) {
                continue;
            }
            syncRefundByChannelInLock(orderInfo.getPaymentType(), refundInfo);
        }
    }

    private void syncRefundByChannelInLock(String paymentType, RefundInfo refundInfo) {
        executeWithRefundLock(refundInfo.getRefundNo(), () -> syncRefundByChannel(paymentType, refundInfo));
    }

    private void syncRefundByChannel(String paymentType, RefundInfo refundInfo) {
        RefundStatusSyncResult syncResult = queryRefundStatusFromChannel(paymentType, refundInfo.getRefundNo());
        validateChannelResult(refundInfo, syncResult);
        refundStatusSyncService.repairAndSync(syncResult);
    }

    private void repairAndSyncRefundInLock(RefundStatusSyncResult syncResult) {
        executeWithRefundLock(syncResult.getRefundNo(), () -> refundStatusSyncService.repairAndSync(syncResult));
    }

    private void executeWithRefundLock(String refundNo, Runnable action) {
        executeWithRefundLock(refundNo, () -> {
            action.run();
            return null;
        });
    }

    private <T> T executeWithRefundLock(String refundNo, Supplier<T> action) {
        if (!StringUtils.hasText(refundNo)) {
            throw new BizException("退款单号不能为空");
        }
        return distributedLockExecutor.execute(PaymentLockKeys.refund(refundNo), action);
    }

    private boolean shouldSyncLocalRefund(RefundInfo refundInfo) {
        return refundInfo != null
                && refundInfo.getRefundNo() != null
                && !refundInfo.getRefundNo().trim().isEmpty()
                && RefundApprovalStatus.APPROVED.getType().equals(refundInfo.getApprovalStatus());
    }

    private void validateChannelOrderResult(OrderInfo orderInfo, RefundStatusSyncResult syncResult) {
        if (syncResult == null) {
            throw new BizException("退款查询结果不能为空");
        }
        if (syncResult.getOrderNo() != null && !orderInfo.getOrderNo().equals(syncResult.getOrderNo())) {
            throw new BizException("渠道退款查询结果与本地订单不一致");
        }
    }

    private void validateChannelResult(RefundInfo refundInfo, RefundStatusSyncResult syncResult) {
        if (syncResult == null) {
            throw new BizException("退款查询结果不能为空");
        }
        if (syncResult.getRefundNo() != null && !refundInfo.getRefundNo().equals(syncResult.getRefundNo())) {
            throw new BizException("渠道退款查询结果与本地退款单不一致");
        }
        if (syncResult.getOrderNo() != null && !refundInfo.getOrderNo().equals(syncResult.getOrderNo())) {
            throw new BizException("渠道退款查询结果与本地订单不一致");
        }
    }
}
