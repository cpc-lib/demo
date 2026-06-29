package cc.ivera.service.provider;

import cc.ivera.entity.OrderInfo;
import cc.ivera.entity.RefundInfo;
import cc.ivera.enums.PayType;
import cc.ivera.service.refund.RefundStatusSyncResult;
import cc.ivera.service.wxpay.WxPayOrderFacade;
import cc.ivera.service.wxpay.WxPayRefundFacade;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 微信支付 V3 提供商适配器 — 将 WxPayOrderFacade + WxPayRefundFacade 适配为 PaymentProvider 接口。
 */
@Component
public class WxPayProviderAdapter implements PaymentProvider {

    private final WxPayOrderFacade wxPayOrderFacade;
    private final WxPayRefundFacade wxPayRefundFacade;

    public WxPayProviderAdapter(WxPayOrderFacade wxPayOrderFacade,
                                WxPayRefundFacade wxPayRefundFacade) {
        this.wxPayOrderFacade = wxPayOrderFacade;
        this.wxPayRefundFacade = wxPayRefundFacade;
    }

    @Override
    public Object createPayment(Long productId, Long paymentAppId) {
        return wxPayOrderFacade.nativePay(productId, paymentAppId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void processPaymentNotification(Map<String, ?> params) {
        wxPayOrderFacade.processOrder((Map<String, Object>) params);
    }

    @Override
    public void cancelOrder(String orderNo) {
        wxPayOrderFacade.cancelOrder(orderNo);
    }

    @Override
    public String queryOrder(String orderNo) {
        return wxPayOrderFacade.queryOrder(orderNo);
    }

    @Override
    public void checkOrderStatus(String orderNo) {
        wxPayOrderFacade.checkOrderStatus(orderNo);
    }

    @Override
    public void executeRefund(RefundInfo refundInfo) {
        wxPayRefundFacade.executeRefund(refundInfo);
    }

    @Override
    public String queryRefund(String refundNo) {
        return wxPayRefundFacade.queryRefund(refundNo);
    }

    @Override
    public RefundStatusSyncResult queryRefundStatusForSync(String refundNo) {
        return wxPayRefundFacade.queryRefundStatusForSync(refundNo);
    }

    @Override
    public String getPaymentType() {
        return PayType.WXPAY.getType();
    }
}
