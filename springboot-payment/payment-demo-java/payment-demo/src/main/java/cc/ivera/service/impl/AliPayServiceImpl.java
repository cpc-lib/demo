package cc.ivera.service.impl;

import cc.ivera.config.AlipayProperties;
import cc.ivera.config.PaymentAppConfig;
import cc.ivera.config.PaymentConfigLoader;
import cc.ivera.entity.OrderInfo;
import cc.ivera.entity.RefundInfo;
import cc.ivera.enums.OrderStatus;
import cc.ivera.enums.PayType;
import cc.ivera.enums.RefundStatus;
import cc.ivera.enums.alipay.AliPayTradeState;
import cc.ivera.exception.BizException;
import cc.ivera.exception.ConflictException;
import cc.ivera.lock.DistributedLockTemplate;
import cc.ivera.service.AliPayService;
import cc.ivera.service.InventoryService;
import cc.ivera.service.OrderInfoService;
import cc.ivera.service.PaymentInfoService;
import cc.ivera.service.RefundInfoService;
import cc.ivera.service.refund.RefundStatusSyncResult;
import cc.ivera.util.JsonUtils;
import cc.ivera.util.MoneyUtils;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayConfig;
import com.alipay.api.AlipayConstants;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.*;
import com.alipay.api.response.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class AliPayServiceImpl implements AliPayService {

    private static final String ALIPAY_REFUND_SUCCESS = "REFUND_SUCCESS";

    private static final String ALIPAY_TRADE_FINISHED = "TRADE_FINISHED";

    private static final String ORDER_TRANSITION_LOCK_PREFIX = "payment:order:transition:";

    private final OrderInfoService orderInfoService;

    private final AlipayClient alipayClient;

    private final AlipayProperties alipayProperties;

    private final PaymentConfigLoader paymentConfigLoader;

    private final PaymentInfoService paymentInfoService;

    private final InventoryService inventoryService;

    private final RefundInfoService refundInfoService;

    private final DistributedLockTemplate distributedLockTemplate;

    private final TransactionTemplate transactionTemplate;

    public AliPayServiceImpl(
        OrderInfoService orderInfoService,
        AlipayClient alipayClient,
        AlipayProperties alipayProperties,
        PaymentConfigLoader paymentConfigLoader,
        PaymentInfoService paymentInfoService,
        InventoryService inventoryService,
        RefundInfoService refundInfoService,
        DistributedLockTemplate distributedLockTemplate,
        TransactionTemplate transactionTemplate
    ) {
        this.orderInfoService = orderInfoService;
        this.alipayClient = alipayClient;
        this.alipayProperties = alipayProperties;
        this.paymentConfigLoader = paymentConfigLoader;
        this.paymentInfoService = paymentInfoService;
        this.inventoryService = inventoryService;
        this.refundInfoService = refundInfoService;
        this.distributedLockTemplate = distributedLockTemplate;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public String tradeCreate(Long productId, Long paymentAppId, String idempotencyKey) {
        return distributedLockTemplate.execute(
                "payment:ali:pagepay:" + idempotencyKey,
                3000L,
                -1L,
                () -> doTradeCreate(productId, paymentAppId, idempotencyKey)
        );
    }

    @Override
    public String tradeCreateOrder(String orderNo) {
        return distributedLockTemplate.execute(
                "payment:ali:pagepay:order:" + orderNo,
                3000L,
                15000L,
                () -> {
                    OrderInfo orderInfo = requirePayableAliOrder(orderNo);
                    PaymentAppConfig payConfig = resolveAliPayConfig(orderInfo.getPaymentAppId());
                    return requestTradePage(orderInfo, payConfig);
                }
        );
    }

    private String doTradeCreate(Long productId, Long paymentAppId, String idempotencyKey) {
        log.info("生成支付宝订单");
        PaymentAppConfig payConfig = resolveAliPayConfig(paymentAppId);
        OrderInfo orderInfo = orderInfoService.createOrReuseOrder(
                productId,
                PayType.ALIPAY.getType(),
                payConfig.getAppId(),
                PaymentConfigLoader.CHANNEL_ALIPAY,
                idempotencyKey
        );
        return requestTradePage(orderInfo, payConfig);
    }

    private String requestTradePage(OrderInfo orderInfo, PaymentAppConfig payConfig) {
        try {
            AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
            request.setNotifyUrl(required(payConfig.getAlipayNotifyUrl(), "支付宝notifyUrl未配置"));
            request.setReturnUrl(required(payConfig.getReturnUrl(), "支付宝returnUrl未配置"));

            Map<String, Object> bizContent = new HashMap<>();
            bizContent.put("out_trade_no", orderInfo.getOrderNo());
            bizContent.put("total_amount", MoneyUtils.centsToYuan(orderInfo.getTotalFee()));
            bizContent.put("subject", orderInfo.getTitle());
            bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");
            request.setBizContent(JsonUtils.toJson(bizContent));

            AlipayTradePagePayResponse response = buildAlipayClient(payConfig).pageExecute(request);
            if (response.isSuccess()) {
                log.info("支付宝下单成功，返回结果 ===> {}", response.getBody());
                return response.getBody();
            }

            log.info("支付宝下单失败，返回码 ===> {}, 返回描述 ===> {}", response.getCode(), response.getMsg());
            throw new BizException("创建支付宝支付交易失败：" + response.getSubMsg());
        } catch (AlipayApiException e) {
            log.error("创建支付宝支付交易失败", e);
            throw new BizException("创建支付宝支付交易失败", e);
        }
    }

    @Override
    public void processOrder(Map<String, String> params) {
        log.info("处理支付宝支付通知");

        String orderNo = params.get("out_trade_no");
        if (orderNo == null || orderNo.trim().isEmpty()) {
            throw new BizException("支付宝支付通知缺少商户订单号");
        }

        String notifyId = params.get("notify_id");
        String lockKey = "payment:order:transition:" + orderNo;

        distributedLockTemplate.execute(lockKey, 5000L, 30000L, () ->
                transactionTemplate.execute(status -> {
                    doProcessAliPayNotifyInTransaction(params, orderNo, notifyId);
                    return null;
                })
        );
    }

    private void doProcessAliPayNotifyInTransaction(Map<String, String> params, String orderNo, String notifyId) {
        log.info("支付宝支付通知加锁处理开始，orderNo={}, notifyId={}", orderNo, notifyId);

        // 支付宝异步通知同样可能重复投递，和主动查单、关单并发。
        // Redis 分布式锁 + 数据库行锁 + 状态条件更新共同保证幂等。
        OrderInfo lockedOrder = orderInfoService.getOrderByOrderNoForUpdate(orderNo);
        if (lockedOrder == null) {
            throw new BizException("支付宝支付通知对应订单不存在，orderNo=" + orderNo);
        }
        validateAliPayOrderNotify(lockedOrder, params);

        if (!OrderStatus.NOTPAY.getType().equals(lockedOrder.getOrderStatus())) {
            log.info("支付宝支付通知重复或订单状态已变化，幂等忽略 ===> orderNo={}, notifyId={}, currentStatus={}",
                    orderNo, notifyId, lockedOrder.getOrderStatus());
            return;
        }

        boolean updated = orderInfoService.updateStatusByOrderNoIfStatus(
                orderNo,
                OrderStatus.NOTPAY,
                OrderStatus.SUCCESS);
        if (!updated) {
            log.info("支付宝支付通知状态更新失败，可能已被其他事务处理，orderNo={}, notifyId={}", orderNo, notifyId);
            return;
        }

        requirePaymentInventoryCommitted(orderNo);
        paymentInfoService.createPaymentInfoForAliPay(params);
        log.info("支付宝支付通知处理完成，orderNo={}, notifyId={}", orderNo, notifyId);
    }


    private void validateAliPayOrderNotify(OrderInfo orderInfo, Map<String, String> params) {
        if (!PayType.ALIPAY.getType().equals(orderInfo.getPaymentType())) {
            throw new BizException("支付宝支付通知支付类型不匹配，orderNo=" + orderInfo.getOrderNo());
        }

        String totalAmount = params.get("total_amount");
        if (totalAmount == null || totalAmount.trim().isEmpty()) {
            throw new BizException("支付宝支付通知缺少金额字段，orderNo=" + orderInfo.getOrderNo());
        }

        int notifyTotal = MoneyUtils.yuanToCents(totalAmount);
        if (!Integer.valueOf(notifyTotal).equals(orderInfo.getTotalFee())) {
            throw new BizException("支付宝支付通知金额与订单金额不一致，orderNo=" + orderInfo.getOrderNo());
        }
    }

    @Override
    public void cancelOrder(String orderNo) {
        if (!StringUtils.hasText(orderNo)) {
            throw new BizException("订单号不能为空");
        }
        distributedLockTemplate.execute(ORDER_TRANSITION_LOCK_PREFIX + orderNo, 5000L, -1L, () -> {
            OrderInfo currentOrder = orderInfoService.getOrderByOrderNo(orderNo);
            if (currentOrder == null) {
                throw new BizException("订单不存在，orderNo=" + orderNo);
            }
            if (!OrderStatus.NOTPAY.getType().equals(currentOrder.getOrderStatus())) {
                log.info("订单当前状态不允许取消，orderNo={}, currentStatus={}",
                        orderNo, currentOrder.getOrderStatus());
                return null;
            }
            closeOrder(orderNo);
            return transactionTemplate.execute(status -> {
                transitionToClosedInTransaction(orderNo, OrderStatus.CANCEL);
                return null;
            });
        });
    }

    @Override
    public String queryOrder(String orderNo) {
        try {
            log.info("调用支付宝查单接口 ===> {}", orderNo);

            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            Map<String, Object> bizContent = new HashMap<>();
            bizContent.put("out_trade_no", orderNo);
            request.setBizContent(JsonUtils.toJson(bizContent));

            AlipayTradeQueryResponse response = buildAlipayClient(resolveAliPayConfigByOrderNo(orderNo)).execute(request);
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
        syncOrderStatus(orderNo, false);
    }

    @Override
    public void checkOrderStatusAndCloseIfUnpaid(String orderNo) {
        syncOrderStatus(orderNo, true);
    }

    private void syncOrderStatus(String orderNo, boolean closeUnpaidOrder) {
        log.warn("根据订单号核实支付宝订单状态 ===> {}, closeUnpaidOrder={}", orderNo, closeUnpaidOrder);

        String result = queryOrder(orderNo);
        if (!StringUtils.hasText(result)) {
            throw new BizException("支付宝订单状态不明确，请稍后重试，orderNo=" + orderNo);
        }

        Map<String, Object> resultMap = JsonUtils.toObjectMap(result);
        Map<String, Object> alipayTradeQueryResponse = JsonUtils.toObjectMap(resultMap.get("alipay_trade_query_response"));
        if (alipayTradeQueryResponse == null) {
            throw new BizException("支付宝查单响应缺少交易信息，orderNo=" + orderNo);
        }

        String tradeStatus = (String) alipayTradeQueryResponse.get("trade_status");
        if (AliPayTradeState.NOTPAY.getType().equals(tradeStatus)) {
            if (closeUnpaidOrder) {
                closeUnpaidAliOrder(orderNo);
            }
            return;
        }

        if (isAliPaySuccess(tradeStatus) || AliPayTradeState.CLOSED.getType().equals(tradeStatus)) {
            syncAliOrderStatus(orderNo, tradeStatus, alipayTradeQueryResponse);
            return;
        }
        throw new BizException("支付宝订单状态不明确，请稍后重试，orderNo=" + orderNo);
    }

    private void closeUnpaidAliOrder(String orderNo) {
        distributedLockTemplate.execute(ORDER_TRANSITION_LOCK_PREFIX + orderNo, 5000L, -1L, () -> {
            OrderInfo currentOrder = orderInfoService.getOrderByOrderNo(orderNo);
            if (currentOrder == null) {
                throw new BizException("订单不存在，orderNo=" + orderNo);
            }
            if (!OrderStatus.NOTPAY.getType().equals(currentOrder.getOrderStatus())) {
                return null;
            }
            closeOrder(orderNo);
            return transactionTemplate.execute(status -> {
                transitionToClosedInTransaction(orderNo, OrderStatus.CLOSED);
                return null;
            });
        });
    }

    private void syncAliOrderStatus(
            String orderNo,
            String tradeStatus,
            Map<String, Object> channelOrder
    ) {
        distributedLockTemplate.execute(ORDER_TRANSITION_LOCK_PREFIX + orderNo, 5000L, -1L, () ->
                transactionTemplate.execute(status -> {
                    OrderInfo lockedOrder = orderInfoService.getOrderByOrderNoForUpdate(orderNo);
                    if (lockedOrder == null) {
                        throw new BizException("支付宝查单对应订单不存在，orderNo=" + orderNo);
                    }
                    if (!OrderStatus.NOTPAY.getType().equals(lockedOrder.getOrderStatus())) {
                        return null;
                    }
                    validateAliPayOrderQuery(lockedOrder, channelOrder);
                    if (isAliPaySuccess(tradeStatus)) {
                        boolean updated = orderInfoService.updateStatusByOrderNoIfStatus(
                                orderNo,
                                OrderStatus.NOTPAY,
                                OrderStatus.SUCCESS
                        );
                        if (updated) {
                            requirePaymentInventoryCommitted(orderNo);
                            paymentInfoService.createPaymentInfoForAliPay(channelOrder);
                        }
                        return null;
                    }
                    transitionToClosedInTransaction(orderNo, OrderStatus.CLOSED, lockedOrder);
                    return null;
                })
        );
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

            AlipayTradeRefundResponse response = buildAlipayClient(resolveAliPayConfigByOrderNo(refundInfo.getOrderNo())).execute(request);
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

            AlipayDataDataserviceBillDownloadurlQueryResponse response = buildAlipayClient(resolveAliPayConfig(null)).execute(request);
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

            AlipayTradeCloseResponse response = buildAlipayClient(resolveAliPayConfigByOrderNo(orderNo)).execute(request);
            if (response.isSuccess()) {
                log.info("支付宝关单成功，返回结果 ===> {}", response.getBody());
            } else {
                log.info("支付宝关单失败，返回码 ===> {}, 返回描述 ===> {}", response.getCode(), response.getMsg());
                throw new BizException("支付宝关单失败：" + firstNonBlank(response.getSubMsg(), response.getMsg()));
            }
        } catch (AlipayApiException e) {
            log.error("调用支付宝关单接口失败", e);
            throw new BizException("关单接口调用失败", e);
        }
    }

    private void requirePaymentInventoryCommitted(String orderNo) {
        if (!inventoryService.commitPayment(orderNo)) {
            throw new ConflictException("订单库存状态不一致，支付状态未提交");
        }
    }

    private void transitionToClosedInTransaction(String orderNo, OrderStatus targetStatus) {
        OrderInfo lockedOrder = orderInfoService.getOrderByOrderNoForUpdate(orderNo);
        if (lockedOrder == null) {
            throw new BizException("订单不存在，orderNo=" + orderNo);
        }
        transitionToClosedInTransaction(orderNo, targetStatus, lockedOrder);
    }

    private void transitionToClosedInTransaction(
            String orderNo,
            OrderStatus targetStatus,
            OrderInfo lockedOrder
    ) {
        if (!OrderStatus.NOTPAY.getType().equals(lockedOrder.getOrderStatus())) {
            return;
        }
        boolean updated = orderInfoService.updateStatusByOrderNoIfStatus(
                orderNo,
                OrderStatus.NOTPAY,
                targetStatus
        );
        if (updated && !inventoryService.releaseReservation(orderNo)) {
            throw new ConflictException("订单库存状态不一致，关闭状态未提交");
        }
    }

    private boolean isAliPaySuccess(String tradeStatus) {
        return AliPayTradeState.SUCCESS.getType().equals(tradeStatus)
                || ALIPAY_TRADE_FINISHED.equals(tradeStatus);
    }

    private void validateAliPayOrderQuery(OrderInfo orderInfo, Map<String, Object> channelOrder) {
        if (!PayType.ALIPAY.getType().equals(orderInfo.getPaymentType())
                || (StringUtils.hasText(orderInfo.getPaymentChannelCode())
                && !PaymentConfigLoader.CHANNEL_ALIPAY.equals(orderInfo.getPaymentChannelCode()))) {
            throw new BizException("支付宝查单支付渠道与本地订单不匹配，orderNo=" + orderInfo.getOrderNo());
        }
        Object channelOrderNo = channelOrder.get("out_trade_no");
        if (channelOrderNo == null
                || !orderInfo.getOrderNo().equals(channelOrderNo.toString())) {
            throw new BizException("支付宝查单订单号与本地订单不匹配，orderNo=" + orderInfo.getOrderNo());
        }
        Object totalAmount = channelOrder.get("total_amount");
        if (totalAmount == null
                || !Integer.valueOf(MoneyUtils.yuanToCents(totalAmount.toString()))
                .equals(orderInfo.getTotalFee())) {
            throw new BizException("支付宝查单金额与本地订单不一致，orderNo=" + orderInfo.getOrderNo());
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

            AlipayTradeFastpayRefundQueryResponse response = buildAlipayClient(resolveAliPayConfigByOrderNo(refundInfo.getOrderNo())).execute(request);
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

    private PaymentAppConfig resolveAliPayConfig(Long paymentAppId) {
        PaymentAppConfig config = paymentAppId == null
                ? paymentConfigLoader.getDefaultAppConfigByChannelCode(PaymentConfigLoader.CHANNEL_ALIPAY)
                : paymentConfigLoader.getRequiredAppConfig(paymentAppId);
        if (config == null) {
            return buildDefaultAliPayConfig();
        }
        if (!PaymentConfigLoader.CHANNEL_ALIPAY.equals(config.getChannelCode())) {
            throw new BizException("支付应用不是支付宝渠道");
        }
        return config;
    }

    private OrderInfo requirePayableAliOrder(String orderNo) {
        OrderInfo orderInfo = orderInfoService.getOrderByOrderNo(orderNo);
        if (orderInfo == null) {
            throw new BizException("订单不存在");
        }
        if (!OrderStatus.NOTPAY.getType().equals(orderInfo.getOrderStatus())) {
            throw new BizException("订单状态不允许支付");
        }
        if (!PayType.ALIPAY.getType().equals(orderInfo.getPaymentType())
                || !PaymentConfigLoader.CHANNEL_ALIPAY.equals(orderInfo.getPaymentChannelCode())) {
            throw new BizException("订单支付渠道不是支付宝");
        }
        return orderInfo;
    }

    private PaymentAppConfig resolveAliPayConfigByOrderNo(String orderNo) {
        OrderInfo orderInfo = orderInfoService.getOrderByOrderNo(orderNo);
        return resolveAliPayConfig(orderInfo == null ? null : orderInfo.getPaymentAppId());
    }

    private PaymentAppConfig buildDefaultAliPayConfig() {
        PaymentAppConfig config = new PaymentAppConfig();
        config.setChannelCode(PaymentConfigLoader.CHANNEL_ALIPAY);
        config.setAlipayAppId(alipayProperties.getAppId());
        config.setSellerId(alipayProperties.getSellerId());
        config.setGatewayUrl(alipayProperties.getGatewayUrl());
        config.setMerchantPrivateKey(alipayProperties.getMerchantPrivateKey());
        config.setAlipayPublicKey(alipayProperties.getAlipayPublicKey());
        config.setContentKey(alipayProperties.getContentKey());
        config.setReturnUrl(alipayProperties.getReturnUrl());
        config.setAlipayNotifyUrl(alipayProperties.getNotifyUrl());
        return config;
    }

    private AlipayClient buildAlipayClient(PaymentAppConfig payConfig) throws AlipayApiException {
        AlipayConfig alipayConfig = new AlipayConfig();
        alipayConfig.setServerUrl(required(payConfig.getGatewayUrl(), "支付宝gatewayUrl未配置"));
        alipayConfig.setAppId(required(payConfig.getAlipayAppId(), "支付宝appId未配置"));
        alipayConfig.setPrivateKey(required(payConfig.getMerchantPrivateKey(), "支付宝merchantPrivateKey未配置"));
        alipayConfig.setFormat(AlipayConstants.FORMAT_JSON);
        alipayConfig.setCharset(AlipayConstants.CHARSET_UTF8);
        alipayConfig.setAlipayPublicKey(required(payConfig.getAlipayPublicKey(), "支付宝alipayPublicKey未配置"));
        alipayConfig.setSignType(AlipayConstants.SIGN_TYPE_RSA2);
        return new DefaultAlipayClient(alipayConfig);
    }

    private String required(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(message);
        }
        return value.trim();
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
