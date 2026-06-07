package cc.ivera.service;

import cc.ivera.entity.RefundInfo;

import java.util.Map;

public interface AliPayService {
    String tradeCreate(Long productId);

    void processOrder(Map<String, String> params);

    void cancelOrder(String orderNo);

    String queryOrder(String orderNo);

    void checkOrderStatus(String orderNo);

    void refund(String orderNo, Integer refundAmount, String reason);

    void executeRefund(RefundInfo refundInfo);

    default void refund(String orderNo, String reason) {
        refund(orderNo, null, reason);
    }

    String queryRefund(String refundNo);

    String queryBill(String billDate, String type);

}
