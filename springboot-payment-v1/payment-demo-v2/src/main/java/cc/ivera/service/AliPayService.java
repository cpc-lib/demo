package cc.ivera.service;

import cc.ivera.entity.RefundInfo;

import java.util.Map;

public interface AliPayService {
    String tradeCreate(Long productId);

    void processOrder(Map<String, String> params);

    void cancelOrder(String orderNo);

    String queryOrder(String orderNo);

    void checkOrderStatus(String orderNo);

    void executeRefund(RefundInfo refundInfo);

    String queryRefund(String refundNo);

    String queryBill(String billDate, String type);

}
