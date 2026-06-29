package cc.ivera.service.provider;

import cc.ivera.entity.RefundInfo;
import cc.ivera.enums.PayType;
import cc.ivera.service.AliPayService;
import cc.ivera.service.refund.RefundStatusSyncResult;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 支付宝支付提供商适配器 — 将 AliPayService 适配为 PaymentProvider 接口。
 */
@Component
public class AliPayProviderAdapter implements PaymentProvider {

    private final AliPayService aliPayService;

    public AliPayProviderAdapter(AliPayService aliPayService) {
        this.aliPayService = aliPayService;
    }

    @Override
    public Object createPayment(Long productId, Long paymentAppId) {
        return aliPayService.tradeCreate(productId, paymentAppId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void processPaymentNotification(Map<String, ?> params) {
        aliPayService.processOrder((Map<String, String>) params);
    }

    @Override
    public void cancelOrder(String orderNo) {
        aliPayService.cancelOrder(orderNo);
    }

    @Override
    public String queryOrder(String orderNo) {
        return aliPayService.queryOrder(orderNo);
    }

    @Override
    public void checkOrderStatus(String orderNo) {
        aliPayService.checkOrderStatus(orderNo);
    }

    @Override
    public void executeRefund(RefundInfo refundInfo) {
        aliPayService.executeRefund(refundInfo);
    }

    @Override
    public String queryRefund(String refundNo) {
        return aliPayService.queryRefund(refundNo);
    }

    @Override
    public RefundStatusSyncResult queryRefundStatusForSync(String refundNo) {
        return aliPayService.queryRefundStatusForSync(refundNo);
    }

    @Override
    public String getPaymentType() {
        return PayType.ALIPAY.getType();
    }
}
