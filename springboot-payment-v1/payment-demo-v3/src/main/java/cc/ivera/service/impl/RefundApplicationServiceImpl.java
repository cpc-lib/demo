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
import cc.ivera.service.WxPayService;
import cc.ivera.util.JsonUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class RefundApplicationServiceImpl implements RefundApplicationService {

    private final RefundInfoService refundInfoService;

    private final OrderInfoService orderInfoService;

    private final WxPayService wxPayService;

    private final AliPayService aliPayService;

    public RefundApplicationServiceImpl(
        RefundInfoService refundInfoService,
        OrderInfoService orderInfoService,
        WxPayService wxPayService,
        AliPayService aliPayService
    ) {
        this.refundInfoService = refundInfoService;
        this.orderInfoService = orderInfoService;
        this.wxPayService = wxPayService;
        this.aliPayService = aliPayService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RefundInfo createApplication(String orderNo, Integer refundAmount, String reason) {
        return refundInfoService.createRefundApplication(orderNo, refundAmount, reason);
    }

    @Override
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
            wxPayService.executeRefund(refundInfo);
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
}
