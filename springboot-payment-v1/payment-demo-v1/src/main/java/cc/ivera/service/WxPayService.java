package cc.ivera.service;

import cc.ivera.entity.OrderInfo;
import cc.ivera.entity.RefundInfo;

import java.security.GeneralSecurityException;
import java.util.Map;

public interface WxPayService {
    Map<String, Object> nativePay(Long productId) throws Exception;

    void processOrder(Map<String, Object> bodyMap) throws GeneralSecurityException;

    void cancelOrder(String orderNo) throws Exception;

    String queryOrder(String orderNo) throws Exception;

    void checkOrderStatus(String orderNo) throws Exception;

    void refund(String orderNo, Integer refundAmount, String reason) throws Exception;

    void executeRefund(RefundInfo refundInfo) throws Exception;

    default void refund(String orderNo, String reason) throws Exception {
        refund(orderNo, null, reason);
    }

    String queryRefund(String refundNo) throws Exception;

    void checkRefundStatus(String refundNo) throws Exception;

    void processRefund(Map<String, Object> bodyMap) throws Exception;

    String queryBill(String billDate, String type) throws Exception;

    String downloadBill(String billDate, String type) throws Exception;

    Map<String, Object> nativePayV2(Long productId, String remoteAddr) throws Exception;

    Map<String, Object> jsapiPay(OrderInfo orderInfo, String openid);
}
