package cc.ivera.service;

import cc.ivera.entity.OrderInfo;
import cc.ivera.entity.RefundInfo;
import cc.ivera.service.refund.RefundStatusSyncResult;

import java.util.List;
import java.util.Map;

public interface WxPayService {
    Map<String, Object> nativePay(Long productId);

    void processOrder(Map<String, Object> bodyMap);

    void cancelOrder(String orderNo);

    String queryOrder(String orderNo);

    void checkOrderStatus(String orderNo);

    void executeRefund(RefundInfo refundInfo);

    String queryRefund(String refundNo);

    RefundStatusSyncResult queryRefundStatusForSync(String refundNo);

    List<RefundStatusSyncResult> queryOrderRefundsForSync(String orderNo);

    void processRefund(Map<String, Object> bodyMap);

    String queryBill(String billDate, String type);

    String queryBill(String billDate, String type, String billType, String accountType, String tarType);

    String downloadBill(String billDate, String type);

    String downloadBill(String billDate, String type, String billType, String accountType, String tarType);

    Map<String, Object> nativePayV2(Long productId, String remoteAddr);

    Map<String, Object> jsapiPay(OrderInfo orderInfo, String openid);
}
