package cc.ivera.service.impl;

import cc.ivera.config.AlipayProperties;
import cc.ivera.entity.OrderInfo;
import cc.ivera.entity.RefundInfo;
import cc.ivera.enums.OrderStatus;
import cc.ivera.enums.PayType;
import cc.ivera.enums.RefundStatus;
import cc.ivera.enums.alipay.AliPayTradeState;
import cc.ivera.exception.BizException;
import cc.ivera.service.AliPayService;
import cc.ivera.service.OrderInfoService;
import cc.ivera.service.PaymentInfoService;
import cc.ivera.service.RefundInfoService;
import cc.ivera.service.refund.RefundStatusSyncResult;
import cc.ivera.util.JsonUtils;
import cc.ivera.util.MoneyUtils;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.*;
import com.alipay.api.response.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class AliPayServiceImpl implements AliPayService {

    private static final String ALIPAY_REFUND_SUCCESS = "REFUND_SUCCESS";

    private final OrderInfoService orderInfoService;

    private final AlipayClient alipayClient;

    private final AlipayProperties alipayProperties;

    private final PaymentInfoService paymentInfoService;

    private final RefundInfoService refundInfoService;

    public AliPayServiceImpl(
        OrderInfoService orderInfoService,
        AlipayClient alipayClient,
        AlipayProperties alipayProperties,
        PaymentInfoService paymentInfoService,
        RefundInfoService refundInfoService
    ) {
        this.orderInfoService = orderInfoService;
        this.alipayClient = alipayClient;
        this.alipayProperties = alipayProperties;
        this.paymentInfoService = paymentInfoService;
        this.refundInfoService = refundInfoService;
    }

    @Override
    public String tradeCreate(Long productId) {
        try {
            log.info("生成支付宝订单");
            OrderInfo orderInfo = orderInfoService.createOrReuseOrder(productId, PayType.ALIPAY.getType());

            AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
            request.setNotifyUrl(alipayProperties.getNotifyUrl());
            request.setReturnUrl(alipayProperties.getReturnUrl());

            Map<String, Object> bizContent = new HashMap<>();
            bizContent.put("out_trade_no", orderInfo.getOrderNo());
            bizContent.put("total_amount", MoneyUtils.centsToYuan(orderInfo.getTotalFee()));
            bizContent.put("subject", orderInfo.getTitle());
            bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");
            request.setBizContent(JsonUtils.toJson(bizContent));

            AlipayTradePagePayResponse response = alipayClient.pageExecute(request);
            if (response.isSuccess()) {
                log.info("支付宝下单成功，返回结果 ===> {}", response.getBody());
                return response.getBody();
            }

            log.info("支付宝下单失败，返回码 ===> {}, 返回描述 ===> {}", response.getCode(), response.getMsg());
            throw new BizException("创建支付宝支付交易失败");
        } catch (AlipayApiException e) {
            log.error("创建支付宝支付交易失败", e);
            throw new BizException("创建支付宝支付交易失败", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processOrder(Map<String, String> params) {
        log.info("处理支付宝支付通知");

        String orderNo = params.get("out_trade_no");
        boolean updated = orderInfoService.updateStatusByOrderNoIfStatus(
                orderNo,
                OrderStatus.NOTPAY,
                OrderStatus.SUCCESS);
        if (!updated) {
            log.info("支付宝支付通知重复或订单状态已变化，忽略处理 ===> {}", orderNo);
            return;
        }

        paymentInfoService.createPaymentInfoForAliPay(params);
    }

    @Override
    public void cancelOrder(String orderNo) {
        if (!OrderStatus.NOTPAY.getType().equals(orderInfoService.getOrderStatus(orderNo))) {
            log.info("订单当前状态不允许取消，orderNo={}", orderNo);
            return;
        }

        closeOrder(orderNo);
        boolean updated = orderInfoService.updateStatusByOrderNoIfStatus(orderNo, OrderStatus.NOTPAY, OrderStatus.CANCEL);
        if (!updated) {
            log.info("订单取消状态更新被忽略，orderNo={}", orderNo);
        }
    }

    @Override
    public String queryOrder(String orderNo) {
        try {
            log.info("调用支付宝查单接口 ===> {}", orderNo);

            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            Map<String, Object> bizContent = new HashMap<>();
            bizContent.put("out_trade_no", orderNo);
            request.setBizContent(JsonUtils.toJson(bizContent));

            AlipayTradeQueryResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                log.info("支付宝查单成功，返回结果 ===> {}", response.getBody());
                return response.getBody();
            }

            log.info("支付宝查单失败，返回码 ===> {}, 返回描述 ===> {}", response.getCode(), response.getMsg());
            return null;
        } catch (AlipayApiException e) {
            log.error("调用支付宝查单接口失败", e);
            throw new BizException("查单接口调用失败", e);
        }
    }

    @Override
    public void checkOrderStatus(String orderNo) {
        log.warn("根据订单号核实支付宝订单状态 ===> {}", orderNo);

        String result = queryOrder(orderNo);
        if (result == null) {
            log.warn("支付宝侧订单未创建 ===> {}", orderNo);
            orderInfoService.updateStatusByOrderNoIfStatus(orderNo, OrderStatus.NOTPAY, OrderStatus.CLOSED);
            return;
        }

        Map<String, Object> resultMap = JsonUtils.toObjectMap(result);
        Map<String, Object> alipayTradeQueryResponse = JsonUtils.toObjectMap(resultMap.get("alipay_trade_query_response"));
        if (alipayTradeQueryResponse == null) {
            log.warn("支付宝查单响应缺少交易信息 ===> {}", orderNo);
            return;
        }

        String tradeStatus = (String) alipayTradeQueryResponse.get("trade_status");
        if (AliPayTradeState.NOTPAY.getType().equals(tradeStatus)) {
            log.warn("支付宝核实订单未支付 ===> {}", orderNo);
            closeOrder(orderNo);
            orderInfoService.updateStatusByOrderNoIfStatus(orderNo, OrderStatus.NOTPAY, OrderStatus.CLOSED);
            return;
        }

        if (AliPayTradeState.SUCCESS.getType().equals(tradeStatus)) {
            log.warn("支付宝核实订单已支付 ===> {}", orderNo);
            boolean updated = orderInfoService.updateStatusByOrderNoIfStatus(
                    orderNo,
                    OrderStatus.NOTPAY,
                    OrderStatus.SUCCESS);
            if (!updated) {
                log.info("支付宝查单确认支付成功，但订单已处理，忽略支付日志 ===> {}", orderNo);
                return;
            }
            paymentInfoService.createPaymentInfoForAliPay(alipayTradeQueryResponse);
        }
    }

    @Override
    public void executeRefund(RefundInfo refundInfo) {
        try {
            if (refundInfo == null) {
                throw new BizException("退款申请单不能为空");
            }

            log.info("调用支付宝退款API, refundNo = {}", refundInfo.getRefundNo());

            AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
            Map<String, Object> bizContent = new HashMap<>();
            bizContent.put("out_trade_no", refundInfo.getOrderNo());
            bizContent.put("out_request_no", refundInfo.getRefundNo());
            bizContent.put("refund_amount", MoneyUtils.centsToYuan(refundInfo.getRefund()));
            bizContent.put("refund_reason", refundInfo.getReason());
            request.setBizContent(JsonUtils.toJson(bizContent));

            AlipayTradeRefundResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                log.info("支付宝退款成功，返回结果 ===> {}", response.getBody());
                refundInfoService.updateRefundToSuccess(refundInfo.getRefundNo(), response.getTradeNo(), response.getBody());
                return;
            }

            log.info("支付宝退款失败，返回码 ===> {}, 返回描述 ===> {}", response.getCode(), response.getMsg());
            refundInfoService.updateRefundToFailed(refundInfo.getRefundNo(), response.getBody());
            throw new BizException("创建支付宝退款申请失败：" + response.getSubMsg());
        } catch (AlipayApiException e) {
            log.error("调用支付宝退款接口失败", e);
            throw new BizException("创建支付宝退款申请失败", e);
        }
    }

    @Override
    public String queryRefund(String refundNo) {
        AlipayTradeFastpayRefundQueryResponse response = executeRefundQuery(getRefundInfoOrThrow(refundNo));
        return response == null ? null : response.getBody();
    }

    @Override
    public RefundStatusSyncResult queryRefundStatusForSync(String refundNo) {
        RefundInfo refundInfo = getRefundInfoOrThrow(refundNo);
        AlipayTradeFastpayRefundQueryResponse response = executeRefundQuery(refundInfo);
        if (response == null) {
            throw new BizException("支付宝退款查询无结果");
        }

        String channelStatus = response.getRefundStatus();
        if (channelStatus == null || channelStatus.trim().isEmpty()) {
            log.info("支付宝退款查询暂未返回可同步状态，refundNo={}", refundNo);
        }

        return RefundStatusSyncResult.of(
                firstNonBlank(response.getOutTradeNo(), refundInfo.getOrderNo()),
                firstNonBlank(response.getOutRequestNo(), refundNo),
                response.getTradeNo(),
                channelStatus,
                mapAliPayRefundStatus(channelStatus),
                response.getBody(),
                parseYuanToCents(response.getTotalAmount()),
                parseYuanToCents(response.getRefundAmount()));
    }

    @Override
    public String queryBill(String billDate, String type) {
        try {
            AlipayDataDataserviceBillDownloadurlQueryRequest request = new AlipayDataDataserviceBillDownloadurlQueryRequest();
            Map<String, Object> bizContent = new HashMap<>();
            bizContent.put("bill_type", type);
            bizContent.put("bill_date", billDate);
            request.setBizContent(JsonUtils.toJson(bizContent));

            AlipayDataDataserviceBillDownloadurlQueryResponse response = alipayClient.execute(request);
            if (!response.isSuccess()) {
                log.info("支付宝申请账单失败，返回码 ===> {}, 返回描述 ===> {}", response.getCode(), response.getMsg());
                throw new BizException("申请支付宝账单失败");
            }

            log.info("支付宝申请账单成功，返回结果 ===> {}", response.getBody());
            Map<String, Object> resultMap = JsonUtils.toObjectMap(response.getBody());
            Map<String, Object> billDownloadurlResponse = JsonUtils.toObjectMap(
                    resultMap.get("alipay_data_dataservice_bill_downloadurl_query_response"));
            if (billDownloadurlResponse == null) {
                throw new BizException("申请支付宝账单失败");
            }

            String billDownloadUrl = (String) billDownloadurlResponse.get("bill_download_url");
            if (billDownloadUrl == null || billDownloadUrl.trim().isEmpty()) {
                throw new BizException("申请支付宝账单失败");
            }
            return billDownloadUrl;
        } catch (AlipayApiException e) {
            log.error("调用支付宝账单接口失败", e);
            throw new BizException("申请支付宝账单失败", e);
        }
    }

    private void closeOrder(String orderNo) {
        try {
            log.info("调用支付宝关单接口，订单号 ===> {}", orderNo);

            AlipayTradeCloseRequest request = new AlipayTradeCloseRequest();
            Map<String, Object> bizContent = new HashMap<>();
            bizContent.put("out_trade_no", orderNo);
            request.setBizContent(JsonUtils.toJson(bizContent));

            AlipayTradeCloseResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                log.info("支付宝关单成功，返回结果 ===> {}", response.getBody());
            } else {
                log.info("支付宝关单失败，返回码 ===> {}, 返回描述 ===> {}", response.getCode(), response.getMsg());
            }
        } catch (AlipayApiException e) {
            log.error("调用支付宝关单接口失败", e);
            throw new BizException("关单接口调用失败", e);
        }
    }

    private RefundInfo getRefundInfoOrThrow(String refundNo) {
        RefundInfo refundInfo = refundInfoService.getByRefundNo(refundNo);
        if (refundInfo == null) {
            throw new BizException("退款单不存在");
        }
        return refundInfo;
    }

    private AlipayTradeFastpayRefundQueryResponse executeRefundQuery(RefundInfo refundInfo) {
        try {
            log.info("调用支付宝退款查询接口 ===> {}", refundInfo.getRefundNo());

            AlipayTradeFastpayRefundQueryRequest request = new AlipayTradeFastpayRefundQueryRequest();
            Map<String, Object> bizContent = new HashMap<>();
            bizContent.put("out_trade_no", refundInfo.getOrderNo());
            bizContent.put("out_request_no", refundInfo.getRefundNo());
            request.setBizContent(JsonUtils.toJson(bizContent));

            AlipayTradeFastpayRefundQueryResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                log.info("支付宝退款查询成功，返回结果 ===> {}", response.getBody());
                return response;
            }

            log.info("支付宝退款查询失败，返回码 ===> {}, 返回描述 ===> {}", response.getCode(), response.getMsg());
            return null;
        } catch (AlipayApiException e) {
            log.error("调用支付宝退款查询接口失败", e);
            throw new BizException("退款查询接口调用失败", e);
        }
    }

    private RefundStatus mapAliPayRefundStatus(String channelStatus) {
        if (ALIPAY_REFUND_SUCCESS.equals(channelStatus)) {
            return RefundStatus.SUCCESS;
        }
        return null;
    }

    private String firstNonBlank(String primary, String fallback) {
        return primary == null || primary.trim().isEmpty() ? fallback : primary;
    }

    private Integer parseYuanToCents(String amount) {
        if (amount == null || amount.trim().isEmpty()) {
            return null;
        }
        return MoneyUtils.yuanToCents(amount);
    }
}
