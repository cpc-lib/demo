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
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Service
public class RefundApplicationServiceImpl implements RefundApplicationService {

    @Resource
    private RefundInfoService refundInfoService;

    @Resource
    private OrderInfoService orderInfoService;

    @Resource
    private WxPayService wxPayService;

    @Resource
    private AliPayService aliPayService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RefundInfo createApplication(String orderNo, Integer refundAmount, String reason) {
        return refundInfoService.createRefundApplication(orderNo, refundAmount, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(String refundNo, String approveRemark) throws Exception {
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

        refundInfoService.markApprovalPassed(refundNo, approveRemark);
        RefundInfo latestRefundInfo = refundInfoService.getByRefundNo(refundNo);

        if (PayType.WXPAY.getType().equals(orderInfo.getPaymentType())) {
            wxPayService.executeRefund(latestRefundInfo);
            return;
        }
        if (PayType.ALIPAY.getType().equals(orderInfo.getPaymentType())) {
            aliPayService.executeRefund(latestRefundInfo);
            return;
        }
        throw new BizException("不支持的支付方式：" + orderInfo.getPaymentType());
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
}
