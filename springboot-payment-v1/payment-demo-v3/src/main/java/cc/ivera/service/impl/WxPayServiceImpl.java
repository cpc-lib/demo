package cc.ivera.service.impl;

import cc.ivera.entity.OrderInfo;
import cc.ivera.entity.RefundInfo;
import cc.ivera.service.WxPayService;
import cc.ivera.service.impl.wxpay.WxPayBillService;
import cc.ivera.service.impl.wxpay.WxPayOrderService;
import cc.ivera.service.impl.wxpay.WxPayRefundService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class WxPayServiceImpl implements WxPayService {

    private final WxPayOrderService wxPayOrderService;

    private final WxPayRefundService wxPayRefundService;

    private final WxPayBillService wxPayBillService;

    public WxPayServiceImpl(
        WxPayOrderService wxPayOrderService,
        WxPayRefundService wxPayRefundService,
        WxPayBillService wxPayBillService
    ) {
        this.wxPayOrderService = wxPayOrderService;
        this.wxPayRefundService = wxPayRefundService;
        this.wxPayBillService = wxPayBillService;
    }

    @Override
    public Map<String, Object> nativePay(Long productId) {
        return wxPayOrderService.nativePay(productId);
    }

    @Override
    public void processOrder(Map<String, Object> bodyMap) {
        wxPayOrderService.processOrder(bodyMap);
    }

    @Override
    public void cancelOrder(String orderNo) {
        wxPayOrderService.cancelOrder(orderNo);
    }

    @Override
    public String queryOrder(String orderNo) {
        return wxPayOrderService.queryOrder(orderNo);
    }

    @Override
    public void checkOrderStatus(String orderNo) {
        wxPayOrderService.checkOrderStatus(orderNo);
    }

    @Override
    public void executeRefund(RefundInfo refundInfo) {
        wxPayRefundService.executeRefund(refundInfo);
    }

    @Override
    public String queryRefund(String refundNo) {
        return wxPayRefundService.queryRefund(refundNo);
    }

    @Override
    public void processRefund(Map<String, Object> bodyMap) {
        wxPayRefundService.processRefund(bodyMap);
    }

    @Override
    public String queryBill(String billDate, String type) {
        return wxPayBillService.queryBill(billDate, type);
    }

    @Override
    public String downloadBill(String billDate, String type) {
        return wxPayBillService.downloadBill(billDate, type);
    }

    @Override
    public Map<String, Object> nativePayV2(Long productId, String remoteAddr) {
        return wxPayOrderService.nativePayV2(productId, remoteAddr);
    }

    @Override
    public Map<String, Object> jsapiPay(OrderInfo orderInfo, String openid) {
        return wxPayOrderService.jsapiPay(orderInfo, openid);
    }
}
