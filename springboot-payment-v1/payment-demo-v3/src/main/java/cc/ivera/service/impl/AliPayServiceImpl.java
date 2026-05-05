package cc.ivera.service.impl;

import cc.ivera.config.AlipayProperties;
import cc.ivera.entity.OrderInfo;
import cc.ivera.entity.RefundInfo;
import cc.ivera.exception.BizException;
import cc.ivera.enums.OrderStatus;
import cc.ivera.enums.PayType;
import cc.ivera.enums.alipay.AliPayTradeState;
import cc.ivera.service.AliPayService;
import cc.ivera.service.OrderInfoService;
import cc.ivera.service.PaymentInfoService;
import cc.ivera.service.RefundInfoService;
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

    private final OrderInfoService orderInfoService;

    private final AlipayClient alipayClient;

    private final AlipayProperties alipayProperties;

    private final PaymentInfoService paymentInfoService;

    private final RefundInfoService refundsInfoService;

    public AliPayServiceImpl(
        OrderInfoService orderInfoService,
        AlipayClient alipayClient,
        AlipayProperties alipayProperties,
        PaymentInfoService paymentInfoService,
        RefundInfoService refundsInfoService
    ) {
        this.orderInfoService = orderInfoService;
        this.alipayClient = alipayClient;
        this.alipayProperties = alipayProperties;
        this.paymentInfoService = paymentInfoService;
        this.refundsInfoService = refundsInfoService;
    }

    @Override
    public String tradeCreate(Long productId) {
        try {
            //生成订单
            log.info("生成订单");
            OrderInfo orderInfo = orderInfoService.createOrReuseOrder(productId, PayType.ALIPAY.getType());

            //调用支付宝接口
            AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
            //配置需要的公共请求参数
            //支付完成后，支付宝向谷粒学院发起异步通知的地址
            request.setNotifyUrl(alipayProperties.getNotifyUrl());
            //支付完成后，我们想让页面跳转回谷粒学院的页面，配置returnUrl
            request.setReturnUrl(alipayProperties.getReturnUrl());

            //组装当前业务方法的请求参数
            Map<String, Object> bizContent = new HashMap<>();
            //订单信息
            bizContent.put("out_trade_no", orderInfo.getOrderNo());
            //支付宝支付单位为元
            bizContent.put("total_amount", MoneyUtils.centsToYuan(orderInfo.getTotalFee()));
            bizContent.put("subject", orderInfo.getTitle());
            bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");

            request.setBizContent(JsonUtils.toJson(bizContent));

            //执行请求，调用支付宝接口
            AlipayTradePagePayResponse response = alipayClient.pageExecute(request);

            if (response.isSuccess()) {
                log.info("调用成功，返回结果 ===> {}", response.getBody());
                return response.getBody();
            } else {
                log.info("调用失败，返回码 ===> {}, 返回描述 ===> {}", response.getCode(), response.getMsg());
                throw new BizException("创建支付交易失败");
            }
        } catch (AlipayApiException e) {
            log.error("创建支付宝支付交易失败", e);
            throw new BizException("创建支付交易失败", e);
        }
    }

    /**
     * 处理订单
     *
     * @param params
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void processOrder(Map<String, String> params) {
        log.info("处理订单");

        //获取订单号
        String orderNo = params.get("out_trade_no");

        boolean updated = orderInfoService.updateStatusByOrderNoIfStatus(
                orderNo,
                OrderStatus.NOTPAY,
                OrderStatus.SUCCESS);
        if (!updated) {
            log.info("支付宝支付通知重复或订单状态已变化，忽略处理 ===> {}", orderNo);
            return;
        }

        //记录支付日志
        paymentInfoService.createPaymentInfoForAliPay(params);
    }

    /**
     * 用户取消订单
     *
     * @param orderNo
     */
    @Override
    public void cancelOrder(String orderNo) {
        //调用支付宝提供的统一收单交易关闭接口
        this.closeOrder(orderNo);

        //更新用户订单状态
        orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.CANCEL);
    }

    /**
     * 查询订单
     *
     * @param orderNo
     * @return 返回订单查询结果，如果返回null则表示支付宝端尚未创建订单
     */
    @Override
    public String queryOrder(String orderNo) {
        try {
            log.info("查单接口调用 ===> {}", orderNo);

            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            Map<String, Object> bizContent = new HashMap<>();
            bizContent.put("out_trade_no", orderNo);
            request.setBizContent(JsonUtils.toJson(bizContent));

            AlipayTradeQueryResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                log.info("调用成功，返回结果 ===> {}", response.getBody());
                return response.getBody();
            } else {
                log.info("调用失败，返回码 ===> {}, 返回描述 ===> {}", response.getCode(), response.getMsg());
                return null;//订单不存在
            }
        } catch (AlipayApiException e) {
            log.error("调用支付宝查单接口失败", e);
            throw new BizException("查单接口的调用失败", e);
        }
    }

    /**
     * 根据订单号调用支付宝查单接口，核实订单状态
     * 如果订单未创建，则更新商户端订单状态
     * 如果订单未支付，则调用关单接口关闭订单，并更新商户端订单状态
     * 如果订单已支付，则更新商户端订单状态，并记录支付日志
     *
     * @param orderNo
     */
    @Override
    public void checkOrderStatus(String orderNo) {
        log.warn("根据订单号核实订单状态 ===> {}", orderNo);

        String result = this.queryOrder(orderNo);

        //订单未创建
        if (result == null) {
            log.warn("核实订单未创建 ===> {}", orderNo);
            //更新本地订单状态
            orderInfoService.updateStatusByOrderNoIfStatus(orderNo, OrderStatus.NOTPAY, OrderStatus.CLOSED);
            return;
        }

        //解析查单响应结果
        Map<String, Object> resultMap = JsonUtils.toObjectMap(result);
        Map<String, Object> alipayTradeQueryResponse = JsonUtils.toObjectMap(resultMap.get("alipay_trade_query_response"));
        if (alipayTradeQueryResponse == null) {
            log.warn("支付宝查单响应缺少交易信息 ===> {}", orderNo);
            return;
        }

        String tradeStatus = (String) alipayTradeQueryResponse.get("trade_status");
        if (AliPayTradeState.NOTPAY.getType().equals(tradeStatus)) {
            log.warn("核实订单未支付 ===> {}", orderNo);

            //如果订单未支付，则调用关单接口关闭订单
            this.closeOrder(orderNo);

            // 并更新商户端订单状态
            orderInfoService.updateStatusByOrderNoIfStatus(orderNo, OrderStatus.NOTPAY, OrderStatus.CLOSED);
        } else if (AliPayTradeState.SUCCESS.getType().equals(tradeStatus)) {
            log.warn("核实订单已支付 ===> {}", orderNo);

            //如果订单已支付，则更新商户端订单状态
            boolean updated = orderInfoService.updateStatusByOrderNoIfStatus(
                    orderNo,
                    OrderStatus.NOTPAY,
                    OrderStatus.SUCCESS);
            if (!updated) {
                log.info("支付宝查单确认支付成功，但订单已处理，忽略支付日志 ===> {}", orderNo);
                return;
            }

            //并记录支付日志
            paymentInfoService.createPaymentInfoForAliPay(alipayTradeQueryResponse);
        }
    }

    /**
     * 关单
     *
     * @param orderNo 订单号
     */
    private void closeOrder(String orderNo) {
        try {
            log.info("关单接口的调用，订单号 ===> {}", orderNo);

            //出现了二维码时，支付宝平台还没有创建订单。
            //只有用户扫码了(不需要确认支付)或在支付页面登录了，才会创建订单。
            //如果用户没有扫码，此时会出现订单不存在
            AlipayTradeCloseRequest request = new AlipayTradeCloseRequest();
            Map<String, Object> bizContent = new HashMap<>();
            bizContent.put("out_trade_no", orderNo);
            request.setBizContent(JsonUtils.toJson(bizContent));
            AlipayTradeCloseResponse response = alipayClient.execute(request);

            if (response.isSuccess()) {
                log.info("调用成功，返回结果 ===> {}", response.getBody());
            } else {
                log.info("调用失败，返回码 ===> {}, 返回描述 ===> {}", response.getCode(), response.getMsg());
            }
        } catch (AlipayApiException e) {
            log.error("调用支付宝关单接口失败", e);
            throw new BizException("关单接口的调用失败", e);
        }
    }

    @Override
    public void executeRefund(RefundInfo refundInfo) {
        try {
            if (refundInfo == null) {
                throw new BizException("退款申请单不能为空");
            }

            log.info("调用退款API, refundNo = {}", refundInfo.getRefundNo());

            AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
            Map<String, Object> bizContent = new HashMap<>();
            bizContent.put("out_trade_no", refundInfo.getOrderNo());
            bizContent.put("out_request_no", refundInfo.getRefundNo());
            bizContent.put("refund_amount", MoneyUtils.centsToYuan(refundInfo.getRefund()));
            bizContent.put("refund_reason", refundInfo.getReason());
            request.setBizContent(JsonUtils.toJson(bizContent));

            AlipayTradeRefundResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                log.info("调用成功，返回结果 ===> {}", response.getBody());
                refundsInfoService.updateRefundToSuccess(refundInfo.getRefundNo(), response.getTradeNo(), response.getBody());
            } else {
                log.info("调用失败，返回码 ===> {}, 返回描述 ===> {}", response.getCode(), response.getMsg());
                refundsInfoService.updateRefundToFailed(refundInfo.getRefundNo(), response.getBody());
                throw new BizException("创建退款申请失败：" + response.getSubMsg());
            }
        } catch (AlipayApiException e) {
            log.error("调用支付宝退款接口失败", e);
            throw new BizException("创建退款申请失败", e);
        }
    }

    /**
     * 查询退款
     *
     * @param orderNo
     * @return
     */
    @Override
    public String queryRefund(String refundNo) {
        try {
            log.info("查询退款接口调用 ===> {}", refundNo);

            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<RefundInfo> queryWrapper =
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            queryWrapper.eq("refund_no", refundNo);
            RefundInfo refundInfo = refundsInfoService.getOne(queryWrapper);
            if (refundInfo == null) {
                throw new BizException("退款单不存在");
            }

            AlipayTradeFastpayRefundQueryRequest request = new AlipayTradeFastpayRefundQueryRequest();
            Map<String, Object> bizContent = new HashMap<>();
            bizContent.put("out_trade_no", refundInfo.getOrderNo());
            bizContent.put("out_request_no", refundNo);
            request.setBizContent(JsonUtils.toJson(bizContent));

            AlipayTradeFastpayRefundQueryResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                log.info("调用成功，返回结果 ===> {}", response.getBody());
                return response.getBody();
            } else {
                log.info("调用失败，返回码 ===> {}, 返回描述 ===> {}", response.getCode(), response.getMsg());
                return null;
            }
        } catch (AlipayApiException e) {
            log.error("调用支付宝退款查询接口失败", e);
            throw new BizException("查单接口的调用失败", e);
        }
    }

    /**
     * 申请账单
     *
     * @param billDate
     * @param type
     * @return
     */
    @Override
    public String queryBill(String billDate, String type) {
        try {
            AlipayDataDataserviceBillDownloadurlQueryRequest request = new AlipayDataDataserviceBillDownloadurlQueryRequest();
            Map<String, Object> bizContent = new HashMap<>();
            bizContent.put("bill_type", type);
            bizContent.put("bill_date", billDate);
            request.setBizContent(JsonUtils.toJson(bizContent));
            AlipayDataDataserviceBillDownloadurlQueryResponse response = alipayClient.execute(request);

            if (response.isSuccess()) {
                log.info("调用成功，返回结果 ===> {}", response.getBody());

                //获取账单下载地址
                Map<String, Object> resultMap = JsonUtils.toObjectMap(response.getBody());
                Map<String, Object> billDownloadurlResponse = JsonUtils.toObjectMap(
                        resultMap.get("alipay_data_dataservice_bill_downloadurl_query_response"));
                if (billDownloadurlResponse == null) {
                    throw new BizException("申请账单失败");
                }
                String billDownloadUrl = (String) billDownloadurlResponse.get("bill_download_url");
                if (billDownloadUrl == null || billDownloadUrl.trim().isEmpty()) {
                    throw new BizException("申请账单失败");
                }

                return billDownloadUrl;
            } else {
                log.info("调用失败，返回码 ===> {}, 返回描述 ===> {}", response.getCode(), response.getMsg());
                throw new BizException("申请账单失败");
            }
        } catch (AlipayApiException e) {
            log.error("调用支付宝账单接口失败", e);
            throw new BizException("申请账单失败", e);
        }
    }
}
