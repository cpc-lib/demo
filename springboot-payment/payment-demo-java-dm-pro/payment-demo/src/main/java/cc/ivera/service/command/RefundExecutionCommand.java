package cc.ivera.service.command;

import cc.ivera.entity.RefundInfo;
import cc.ivera.enums.PayType;
import cc.ivera.exception.BizException;
import cc.ivera.service.AliPayService;
import cc.ivera.service.wxpay.WxPayRefundFacade;
import lombok.extern.slf4j.Slf4j;

/**
 * 退款执行命令 — Command 模式。
 *
 * 将退款执行逻辑封装为命令对象，根据支付类型分发到不同 Provider。
 */
@Slf4j
public class RefundExecutionCommand implements PaymentCommand<Void> {

    private final String paymentType;
    private final RefundInfo refundInfo;
    private final AliPayService aliPayService;
    private final WxPayRefundFacade wxPayRefundFacade;

    public RefundExecutionCommand(String paymentType,
                                  RefundInfo refundInfo,
                                  AliPayService aliPayService,
                                  WxPayRefundFacade wxPayRefundFacade) {
        this.paymentType = paymentType;
        this.refundInfo = refundInfo;
        this.aliPayService = aliPayService;
        this.wxPayRefundFacade = wxPayRefundFacade;
    }

    @Override
    public Void execute() {
        if (PayType.WXPAY.getType().equals(paymentType)) {
            wxPayRefundFacade.executeRefund(refundInfo);
        } else if (PayType.ALIPAY.getType().equals(paymentType)) {
            aliPayService.executeRefund(refundInfo);
        } else {
            throw new BizException("不支持的支付类型: " + paymentType);
        }
        return null;
    }

    @Override
    public String getCommandName() {
        return "RefundExecution(" + refundInfo.getRefundNo() + ")";
    }

    @Override
    public String getLockKey() {
        return "payment:refund:execute:" + refundInfo.getRefundNo();
    }
}
