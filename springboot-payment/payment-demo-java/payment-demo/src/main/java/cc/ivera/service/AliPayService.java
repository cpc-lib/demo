package cc.ivera.service;

import cc.ivera.entity.RefundInfo;
import cc.ivera.entity.OrderInfo;
import cc.ivera.entity.PaymentOrder;
import cc.ivera.service.refund.RefundStatusSyncResult;

import java.util.Map;

public interface AliPayService {
    String tradeCreate(Long productId, Long paymentAppId, String idempotencyKey);

    String tradeCreateOrder(String orderNo);

    String createPayment(PaymentOrder paymentOrder, OrderInfo orderInfo);

    void processOrder(Map<String, String> params);

    void cancelOrder(String orderNo);

    String queryOrder(String orderNo);

    void checkOrderStatus(String orderNo);

    void checkOrderStatusAndCloseIfUnpaid(String orderNo);

    void executeRefund(RefundInfo refundInfo);

    String queryRefund(String refundNo);

    RefundStatusSyncResult queryRefundStatusForSync(String refundNo);

    String queryBill(String billDate, String type);

}
