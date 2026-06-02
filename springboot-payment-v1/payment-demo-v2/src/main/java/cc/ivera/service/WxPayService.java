package cc.ivera.service;

import cc.ivera.entity.OrderInfo;
import cc.ivera.entity.RefundInfo;

import java.util.Map;

public interface WxPayService {
    Map<String, Object> nativePay(Long productId);

    void processOrder(Map<String, Object> bodyMap);

    void cancelOrder(String orderNo);

    String queryOrder(String orderNo);

    void checkOrderStatus(String orderNo);

    void executeRefund(RefundInfo refundInfo);

    String queryRefund(String refundNo);

    void processRefund(Map<String, Object> bodyMap);

    String queryBill(String billDate, String type);

    String downloadBill(String billDate, String type);

    Map<String, Object> nativePayV2(Long productId, String remoteAddr);

    Map<String, Object> jsapiPay(OrderInfo orderInfo, String openid);
}
