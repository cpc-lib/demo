package cc.ivera.service.impl.wxpay;

import cc.ivera.config.PaymentAppConfig;
import cc.ivera.config.PaymentConfigLoader;
import cc.ivera.config.WxPayConfig;
import cc.ivera.entity.OrderInfo;
import cc.ivera.enums.OrderStatus;
import cc.ivera.enums.PayType;
import cc.ivera.enums.wxpay.WxApiType;
import cc.ivera.enums.wxpay.WxNotifyType;
import cc.ivera.enums.wxpay.WxTradeState;
import cc.ivera.exception.BizException;
import cc.ivera.exception.ConflictException;
import cc.ivera.lock.DistributedLockTemplate;
import cc.ivera.service.OrderInfoService;
import cc.ivera.service.PaymentInfoService;
import cc.ivera.service.InventoryService;
import cc.ivera.service.wxpay.WxPayOrderFacade;
import cc.ivera.util.HttpClientUtils;
import cc.ivera.util.JsonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.wxpay.sdk.WXPayUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class WxPayOrderService implements WxPayOrderFacade {

    private static final long PAY_NOTIFY_LOCK_WAIT_MS = 5000L;

    private static final String ORDER_TRANSITION_LOCK_PREFIX = "payment:order:transition:";

    private final WxPayConfig wxPayConfig;
    private final PaymentConfigLoader paymentConfigLoader;
    private final OrderInfoService orderInfoService;
    private final PaymentInfoService paymentInfoService;
    private final InventoryService inventoryService;
    private final WxPayHttpClient wxPayHttpClient;
    private final WxPayNotificationDecoder wxPayNotificationDecoder;
    private final DistributedLockTemplate distributedLockTemplate;
    private final TransactionTemplate transactionTemplate;

    public WxPayOrderService(WxPayConfig wxPayConfig,
                             PaymentConfigLoader paymentConfigLoader,
                             OrderInfoService orderInfoService,
                             PaymentInfoService paymentInfoService,
                             InventoryService inventoryService,
                             WxPayHttpClient wxPayHttpClient,
                             WxPayNotificationDecoder wxPayNotificationDecoder,
                             DistributedLockTemplate distributedLockTemplate,
                             TransactionTemplate transactionTemplate) {
        this.wxPayConfig = wxPayConfig;
        this.paymentConfigLoader = paymentConfigLoader;
        this.orderInfoService = orderInfoService;
        this.paymentInfoService = paymentInfoService;
        this.inventoryService = inventoryService;
        this.wxPayHttpClient = wxPayHttpClient;
        this.wxPayNotificationDecoder = wxPayNotificationDecoder;
        this.distributedLockTemplate = distributedLockTemplate;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public Map<String, Object> nativePay(
            Long productId,
            Long paymentAppId,
            String idempotencyKey
    ) {
        return distributedLockTemplate.execute(
                "payment:wx:native:v3:" + idempotencyKey,
                3000L,
                -1L,
                () -> doNativePay(productId, paymentAppId, idempotencyKey)
        );
    }

    @Override
    public Map<String, Object> nativePayOrder(String orderNo) {
        return distributedLockTemplate.execute(
                "payment:wx:native:v3:order:" + orderNo,
                3000L,
                -1L,
                () -> {
                    OrderInfo orderInfo = requirePayableWxOrder(orderNo);
                    PaymentAppConfig payConfig = resolveWxPayConfig(orderInfo.getPaymentAppId());
                    return requestNativePay(orderInfo, payConfig);
                }
        );
    }

    private Map<String, Object> doNativePay(
            Long productId,
            Long paymentAppId,
            String idempotencyKey
    ) {
        PaymentAppConfig payConfig = resolveWxPayConfig(paymentAppId);
        OrderInfo orderInfo = orderInfoService.createOrReuseOrder(
                productId,
                PayType.WXPAY.getType(),
                payConfig.getAppId(),
                PaymentConfigLoader.CHANNEL_WXPAY,
                idempotencyKey
        );
        if (orderInfo == null) {
            throw new BizException("订单创建失败");
        }
        return requestNativePay(orderInfo, payConfig);
    }

    private Map<String, Object> requestNativePay(OrderInfo orderInfo, PaymentAppConfig payConfig) {
        if (StringUtils.hasText(orderInfo.getCodeUrl())) {
            log.info("订单已存在，复用二维码，orderNo={}", orderInfo.getOrderNo());
            return buildNativePayResult(orderInfo.getOrderNo(), orderInfo.getCodeUrl());
        }

        Map<String, Object> paramsMap = new HashMap<>();
        paramsMap.put("appid", required(payConfig.getAppid(), "微信appid未配置"));
        paramsMap.put("mchid", required(payConfig.getMchId(), "微信商户号未配置"));
        paramsMap.put("description", orderInfo.getTitle());
        paramsMap.put("out_trade_no", orderInfo.getOrderNo());
        paramsMap.put("notify_url", buildNotifyUrl(payConfig, WxNotifyType.NATIVE_NOTIFY));

        Map<String, Object> amountMap = new HashMap<>();
        amountMap.put("total", orderInfo.getTotalFee());
        amountMap.put("currency", "CNY");
        paramsMap.put("amount", amountMap);

        String jsonParams = JsonUtils.toJson(paramsMap);
        log.info("微信V3 Native下单请求参数 ===> {}", jsonParams);

        String url = required(payConfig.getDomain(), "微信支付网关domain未配置").concat(WxApiType.NATIVE_PAY.getType());
        try {
            String bodyAsString = wxPayHttpClient.postJson(payConfig, url, jsonParams, "Native下单失败");
            Map<String, Object> resultMap = JsonUtils.toObjectMap(bodyAsString);
            String codeUrl = getString(resultMap, "code_url");
            if (!StringUtils.hasText(codeUrl)) {
                throw new BizException("微信Native下单响应缺少code_url");
            }
            orderInfoService.saveCodeUrl(orderInfo.getOrderNo(), codeUrl);
            return buildNativePayResult(orderInfo.getOrderNo(), codeUrl);
        } catch (IOException e) {
            throw new BizException("Native下单失败", e);
        }
    }

    @Override
    public void processOrder(Map<String, Object> bodyMap) {
        log.info("处理微信支付结果通知");

        String plainText;
        try {
            plainText = wxPayNotificationDecoder.decryptResource(bodyMap);
        } catch (GeneralSecurityException e) {
            throw new BizException("微信支付通知解密失败", e);
        }

        Map<String, Object> plainTextMap = JsonUtils.toObjectMap(plainText);
        String orderNo = getString(plainTextMap, "out_trade_no");
        if (!StringUtils.hasText(orderNo)) {
            throw new BizException("微信支付通知缺少商户订单号");
        }

        String notifyId = getString(bodyMap, "id");
        distributedLockTemplate.execute(ORDER_TRANSITION_LOCK_PREFIX + orderNo, PAY_NOTIFY_LOCK_WAIT_MS, -1L, () ->
                transactionTemplate.execute(status -> {
                    doProcessOrderNotifyInTransaction(orderNo, plainTextMap, plainText, notifyId);
                    return null;
                })
        );
    }

    private void doProcessOrderNotifyInTransaction(String orderNo,
                                                   Map<String, Object> plainTextMap,
                                                   String plainText,
                                                   String notifyId) {
        OrderInfo lockedOrder = orderInfoService.getOrderByOrderNoForUpdate(orderNo);
        if (lockedOrder == null) {
            throw new BizException("微信支付通知对应订单不存在，orderNo=" + orderNo);
        }

        validateWxPayOrderNotify(lockedOrder, plainTextMap);

        if (!OrderStatus.NOTPAY.getType().equals(lockedOrder.getOrderStatus())) {
            log.info("微信支付通知重复或订单状态已变化，幂等忽略，orderNo={}, notifyId={}, currentStatus={}",
                    orderNo, notifyId, lockedOrder.getOrderStatus());
            return;
        }

        boolean updated = orderInfoService.updateStatusByOrderNoIfStatus(orderNo, OrderStatus.NOTPAY, OrderStatus.SUCCESS);
        if (!updated) {
            log.info("微信支付通知状态更新失败，可能已被其他事务处理，orderNo={}, notifyId={}", orderNo, notifyId);
            return;
        }

        requirePaymentInventoryCommitted(orderNo);
        paymentInfoService.createPaymentInfo(plainText);
        log.info("微信支付通知处理完成，orderNo={}, notifyId={}", orderNo, notifyId);
    }

    @Override
    public void cancelOrder(String orderNo) {
        if (!StringUtils.hasText(orderNo)) {
            throw new BizException("订单号不能为空");
        }
        distributedLockTemplate.execute(ORDER_TRANSITION_LOCK_PREFIX + orderNo, 3000L, -1L, () -> {
            OrderInfo currentOrder = orderInfoService.getOrderByOrderNo(orderNo);
            if (currentOrder == null) {
                throw new BizException("订单不存在，orderNo=" + orderNo);
            }
            if (!OrderStatus.NOTPAY.getType().equals(currentOrder.getOrderStatus())) {
                log.info("订单当前状态不允许取消，orderNo={}, currentStatus={}", orderNo, currentOrder.getOrderStatus());
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
        if (!StringUtils.hasText(orderNo)) {
            throw new BizException("订单号不能为空");
        }
        PaymentAppConfig payConfig = resolveWxPayConfigByOrderNo(orderNo);
        String url = String.format(WxApiType.ORDER_QUERY_BY_NO.getType(), orderNo);
        url = required(payConfig.getDomain(), "微信支付网关domain未配置")
                .concat(url)
                .concat("?mchid=")
                .concat(required(payConfig.getMchId(), "微信商户号未配置"));
        try {
            return wxPayHttpClient.get(payConfig, url, "查单接口调用异常");
        } catch (IOException e) {
            throw new BizException("查单接口调用异常", e);
        }
    }

    @Override
    public Map<String, Object> queryPaymentStatus(String orderNo) {
        OrderInfo orderInfo = orderInfoService.getOrderByOrderNo(orderNo);
        if (orderInfo == null) {
            throw new BizException("订单不存在，orderNo=" + orderNo);
        }
        if (!PayType.WXPAY.getType().equals(orderInfo.getPaymentType())) {
            throw new BizException("订单不是微信支付订单，orderNo=" + orderNo);
        }

        String result = queryOrder(orderNo);
        Map<String, Object> resultMap = JsonUtils.toObjectMap(result);
        String tradeState = getString(resultMap, "trade_state");
        String tradeStateDesc = getString(resultMap, "trade_state_desc");
        String localStatusBefore = orderInfo.getOrderStatus();

        if (WxTradeState.SUCCESS.getType().equals(tradeState)
                || WxTradeState.CLOSED.getType().equals(tradeState)) {
            syncWxOrderStatus(orderNo, resultMap, result, tradeState);
        }

        String localStatusAfter = orderInfoService.getOrderStatus(orderNo);
        return buildPaymentStatusResult(orderNo, tradeState, tradeStateDesc, localStatusBefore, localStatusAfter, resultMap);
    }

    @Override
    public void checkOrderStatus(String orderNo) {
        String result = queryOrder(orderNo);
        Map<String, Object> resultMap = JsonUtils.toObjectMap(result);
        String tradeState = getString(resultMap, "trade_state");
        if (WxTradeState.SUCCESS.getType().equals(tradeState)
                || WxTradeState.CLOSED.getType().equals(tradeState)) {
            syncWxOrderStatus(orderNo, resultMap, result, tradeState);
            return;
        }
        if (WxTradeState.NOTPAY.getType().equals(tradeState)) {
            distributedLockTemplate.execute(ORDER_TRANSITION_LOCK_PREFIX + orderNo, 5000L, -1L, () -> {
                OrderInfo currentOrder = orderInfoService.getOrderByOrderNo(orderNo);
                if (currentOrder == null) {
                    throw new BizException("查单同步对应订单不存在，orderNo=" + orderNo);
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
            return;
        }
        throw new BizException("微信订单状态不明确，请稍后重试，orderNo=" + orderNo);
    }

    private void syncWxOrderStatus(
            String orderNo,
            Map<String, Object> resultMap,
            String result,
            String tradeState
    ) {
        distributedLockTemplate.execute(ORDER_TRANSITION_LOCK_PREFIX + orderNo, 5000L, -1L, () ->
                transactionTemplate.execute(status -> {
                    doSyncOrderStatusFromWxQuery(orderNo, resultMap, result, tradeState);
                    return null;
                })
        );
    }

    private void doSyncOrderStatusFromWxQuery(String orderNo,
                                              Map<String, Object> resultMap,
                                              String result,
                                              String tradeState) {
        OrderInfo lockedOrder = orderInfoService.getOrderByOrderNoForUpdate(orderNo);
        if (lockedOrder == null) {
            throw new BizException("查单同步对应订单不存在，orderNo=" + orderNo);
        }
        if (!OrderStatus.NOTPAY.getType().equals(lockedOrder.getOrderStatus())) {
            log.info("微信查单同步发现订单已处理，orderNo={}, currentStatus={}", orderNo, lockedOrder.getOrderStatus());
            return;
        }

        if (WxTradeState.SUCCESS.getType().equals(tradeState)) {
            validateWxPayOrderNotify(lockedOrder, resultMap);
            if (orderInfoService.updateStatusByOrderNoIfStatus(orderNo, OrderStatus.NOTPAY, OrderStatus.SUCCESS)) {
                requirePaymentInventoryCommitted(orderNo);
                paymentInfoService.createPaymentInfo(result);
            }
            return;
        }
        if (WxTradeState.CLOSED.getType().equals(tradeState)) {
            validateWxPayOrderNotify(lockedOrder, resultMap);
            transitionToClosedInTransaction(orderNo, OrderStatus.CLOSED, lockedOrder);
        }
    }

    @Override
    public Map<String, Object> nativePayV2(
            Long productId,
            String remoteAddr,
            Long paymentAppId,
            String idempotencyKey
    ) {
        return distributedLockTemplate.execute(
                "payment:wx:native:v2:" + idempotencyKey,
                3000L,
                -1L,
                () -> doNativePayV2(productId, remoteAddr, paymentAppId, idempotencyKey)
        );
    }

    @Override
    public Map<String, Object> nativePayV2Order(String orderNo, String remoteAddr) {
        return distributedLockTemplate.execute(
                "payment:wx:native:v2:order:" + orderNo,
                3000L,
                -1L,
                () -> {
                    OrderInfo orderInfo = requirePayableWxOrder(orderNo);
                    PaymentAppConfig payConfig = resolveWxPayConfig(orderInfo.getPaymentAppId());
                    return requestNativePayV2(orderInfo, remoteAddr, payConfig);
                }
        );
    }

    private Map<String, Object> doNativePayV2(
            Long productId,
            String remoteAddr,
            Long paymentAppId,
            String idempotencyKey
    ) {
        PaymentAppConfig payConfig = resolveWxPayConfig(paymentAppId);
        OrderInfo orderInfo = orderInfoService.createOrReuseOrder(
                productId,
                PayType.WXPAY.getType(),
                payConfig.getAppId(),
                PaymentConfigLoader.CHANNEL_WXPAY,
                idempotencyKey
        );
        if (orderInfo == null) {
            throw new BizException("订单创建失败");
        }
        return requestNativePayV2(orderInfo, remoteAddr, payConfig);
    }

    private Map<String, Object> requestNativePayV2(
            OrderInfo orderInfo,
            String remoteAddr,
            PaymentAppConfig payConfig
    ) {
        if (StringUtils.hasText(orderInfo.getCodeUrl())) {
            return buildNativePayResult(orderInfo.getOrderNo(), orderInfo.getCodeUrl());
        }

        HttpClientUtils client = new HttpClientUtils(required(payConfig.getDomain(), "微信支付网关domain未配置").concat(WxApiType.NATIVE_PAY_V2.getType()));
        Map<String, String> params = new HashMap<>();
        params.put("appid", required(payConfig.getAppid(), "微信appid未配置"));
        params.put("mch_id", required(payConfig.getMchId(), "微信商户号未配置"));
        params.put("nonce_str", WXPayUtil.generateNonceStr());
        params.put("body", orderInfo.getTitle());
        params.put("out_trade_no", orderInfo.getOrderNo());
        params.put("total_fee", String.valueOf(orderInfo.getTotalFee()));
        params.put("spbill_create_ip", StringUtils.hasText(remoteAddr) ? remoteAddr : "127.0.0.1");
        params.put("notify_url", buildNotifyUrl(payConfig, WxNotifyType.NATIVE_NOTIFY_V2));
        params.put("trade_type", "NATIVE");

        try {
            String xmlParams = WXPayUtil.generateSignedXml(params, required(payConfig.getPartnerKey(), "微信APIv2密钥partnerKey未配置"));
            client.setXmlParam(xmlParams);
            client.setHttps(true);
            client.post();
            String resultXml = client.getContent();
            Map<String, String> resultMap = WXPayUtil.xmlToMap(resultXml);
            if ("FAIL".equals(resultMap.get("return_code")) || "FAIL".equals(resultMap.get("result_code"))) {
                throw new BizException("微信支付V2统一下单错误：" + resultXml);
            }
            String codeUrl = resultMap.get("code_url");
            if (!StringUtils.hasText(codeUrl)) {
                throw new BizException("微信支付V2统一下单响应缺少code_url");
            }
            orderInfoService.saveCodeUrl(orderInfo.getOrderNo(), codeUrl);
            return buildNativePayResult(orderInfo.getOrderNo(), codeUrl);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("微信支付V2统一下单失败", e);
        }
    }

    @Override
    public Map<String, Object> jsapiPay(OrderInfo orderInfo, String openid) {
        PaymentAppConfig payConfig = resolveWxPayConfig(orderInfo == null ? null : orderInfo.getPaymentAppId());
        String lockKey = "payment:wx:jsapi:" + (orderInfo == null ? "unknown" : orderInfo.getOrderNo());
        return distributedLockTemplate.execute(lockKey, 5000L, -1L, () -> {
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectMapper objectMapper = new ObjectMapper();
                ObjectNode rootNode = objectMapper.createObjectNode();
                rootNode.put("mchid", required(payConfig.getMchId(), "微信商户号未配置"))
                        .put("appid", required(payConfig.getAppid(), "微信appid未配置"))
                        .put("description", orderInfo.getTitle())
                        .put("notify_url", buildNotifyUrl(payConfig, WxNotifyType.NATIVE_NOTIFY))
                        .put("out_trade_no", orderInfo.getOrderNo());
                rootNode.putObject("amount").put("total", orderInfo.getTotalFee());
                rootNode.putObject("payer").put("openid", openid);
                objectMapper.writeValue(bos, rootNode);

                String url = required(payConfig.getDomain(), "微信支付网关domain未配置").concat(WxApiType.JSAPI_PAY.getType());
                String bodyAsString = wxPayHttpClient.postJson(payConfig, url, bos.toString("UTF-8"), "JSAPI下单失败");
                Map<String, Object> responseMap = JsonUtils.toObjectMap(bodyAsString);
                String prepayId = (String) responseMap.get("prepay_id");

                return getPayment("prepay_id=" + prepayId,
                        required(payConfig.getAppid(), "微信appid未配置"),
                        wxPayConfig.getPrivateKey(required(payConfig.getPrivateKeyPath(), "微信私钥文件路径未配置")));
            } catch (Exception e) {
                throw new BizException("支付失败" + e.getMessage(), e);
            }
        });
    }

    private void closeOrder(String orderNo) {
        PaymentAppConfig payConfig = resolveWxPayConfigByOrderNo(orderNo);
        String url = String.format(WxApiType.CLOSE_ORDER_BY_NO.getType(), orderNo);
        url = required(payConfig.getDomain(), "微信支付网关domain未配置").concat(url);

        Map<String, String> paramsMap = new HashMap<>();
        paramsMap.put("mchid", required(payConfig.getMchId(), "微信商户号未配置"));
        try {
            wxPayHttpClient.postJson(payConfig, url, JsonUtils.toJson(paramsMap), "Native关单失败");
        } catch (IOException e) {
            throw new BizException("Native关单失败", e);
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
            log.info("订单关闭状态更新被忽略，orderNo={}, currentStatus={}", orderNo, lockedOrder.getOrderStatus());
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

    private void validateWxPayOrderNotify(OrderInfo orderInfo, Map<String, Object> notifyMap) {
        if (!PayType.WXPAY.getType().equals(orderInfo.getPaymentType())) {
            throw new BizException("支付通知支付类型不匹配，orderNo=" + orderInfo.getOrderNo());
        }
        PaymentAppConfig payConfig = resolveWxPayConfig(orderInfo.getPaymentAppId());
        String notifyOrderNo = getString(notifyMap, "out_trade_no");
        if (!orderInfo.getOrderNo().equals(notifyOrderNo)) {
            throw new BizException("支付通知订单号与本地订单不匹配，orderNo=" + orderInfo.getOrderNo());
        }
        String notifyMchId = getString(notifyMap, "mchid");
        if (!StringUtils.hasText(notifyMchId) || !notifyMchId.equals(payConfig.getMchId())) {
            throw new BizException("支付通知商户号不匹配，orderNo=" + orderInfo.getOrderNo());
        }
        String notifyAppid = getString(notifyMap, "appid");
        if (!StringUtils.hasText(notifyAppid) || !notifyAppid.equals(payConfig.getAppid())) {
            throw new BizException("支付通知appid不匹配，orderNo=" + orderInfo.getOrderNo());
        }

        Integer notifyTotal = getWxPayTotalAmount(notifyMap);
        if (notifyTotal == null) {
            throw new BizException("支付通知缺少金额字段，orderNo=" + orderInfo.getOrderNo());
        }
        if (!notifyTotal.equals(orderInfo.getTotalFee())) {
            throw new BizException("支付通知金额与订单金额不一致，orderNo=" + orderInfo.getOrderNo());
        }
    }

    private Integer getWxPayTotalAmount(Map<String, Object> notifyMap) {
        Map<String, Object> amountMap = JsonUtils.toObjectMap(notifyMap == null ? null : notifyMap.get("amount"));
        if (amountMap == null) {
            return null;
        }
        Object total = amountMap.get("total");
        if (total == null) {
            total = amountMap.get("payer_total");
        }
        return toInteger(total);
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.valueOf(value.toString());
    }

    private Map<String, Object> buildNativePayResult(String orderNo, String codeUrl) {
        Map<String, Object> map = new HashMap<>();
        map.put("codeUrl", codeUrl);
        map.put("orderNo", orderNo);
        return map;
    }

    private Map<String, Object> buildPaymentStatusResult(String orderNo,
                                                         String tradeState,
                                                         String tradeStateDesc,
                                                         String localStatusBefore,
                                                         String localStatusAfter,
                                                         Map<String, Object> wxPayResult) {
        Map<String, Object> map = new HashMap<>();
        map.put("orderNo", orderNo);
        map.put("tradeState", tradeState);
        map.put("tradeStateDesc", tradeStateDesc);
        map.put("localStatusBefore", localStatusBefore);
        map.put("localStatus", localStatusAfter);
        map.put("wxPayResult", wxPayResult);
        return map;
    }

    private Map<String, Object> getPayment(String prepayId, String appId, PrivateKey privateKey) {
        Map<String, Object> map = new HashMap<>();
        String nonceStr = UUID.randomUUID().toString().toUpperCase();
        long timeStamp = System.currentTimeMillis() / 1000;
        String source = appId + "\n" + timeStamp + "\n" + nonceStr + "\n" + prepayId + "\n";
        String sign = getSign(source.getBytes(StandardCharsets.UTF_8), privateKey);
        map.put("appId", appId);
        map.put("timeStamp", timeStamp);
        map.put("nonceStr", nonceStr);
        map.put("package", prepayId);
        map.put("signType", "RSA");
        map.put("paySign", sign);
        return map;
    }

    private String getSign(byte[] message, PrivateKey privateKey) {
        try {
            Signature sign = Signature.getInstance("SHA256withRSA");
            sign.initSign(privateKey);
            sign.update(message);
            return Base64.getEncoder().encodeToString(sign.sign());
        } catch (Exception e) {
            throw new BizException("获取微信支付签名失败", e);
        }
    }

    private PaymentAppConfig resolveWxPayConfig(Long paymentAppId) {
        PaymentAppConfig config = paymentAppId == null
                ? paymentConfigLoader.getDefaultAppConfigByChannelCode(PaymentConfigLoader.CHANNEL_WXPAY)
                : paymentConfigLoader.getRequiredAppConfig(paymentAppId);
        if (config == null) {
            return buildDefaultWxPayConfig();
        }
        if (!PaymentConfigLoader.CHANNEL_WXPAY.equals(config.getChannelCode())) {
            throw new BizException("支付应用不是微信支付渠道");
        }
        return config;
    }

    private OrderInfo requirePayableWxOrder(String orderNo) {
        OrderInfo orderInfo = orderInfoService.getOrderByOrderNo(orderNo);
        if (orderInfo == null) {
            throw new BizException("订单不存在");
        }
        if (!OrderStatus.NOTPAY.getType().equals(orderInfo.getOrderStatus())) {
            throw new BizException("订单状态不允许支付");
        }
        if (!PayType.WXPAY.getType().equals(orderInfo.getPaymentType())
                || !PaymentConfigLoader.CHANNEL_WXPAY.equals(orderInfo.getPaymentChannelCode())) {
            throw new BizException("订单支付渠道不是微信支付");
        }
        return orderInfo;
    }

    private PaymentAppConfig resolveWxPayConfigByOrderNo(String orderNo) {
        OrderInfo orderInfo = orderInfoService.getOrderByOrderNo(orderNo);
        return resolveWxPayConfig(orderInfo == null ? null : orderInfo.getPaymentAppId());
    }

    private PaymentAppConfig buildDefaultWxPayConfig() {
        PaymentAppConfig config = new PaymentAppConfig();
        config.setChannelCode(PaymentConfigLoader.CHANNEL_WXPAY);
        config.setAppid(wxPayConfig.getAppid());
        config.setMchId(wxPayConfig.getMchId());
        config.setMchSerialNo(wxPayConfig.getMchSerialNo());
        config.setPrivateKeyPath(wxPayConfig.getPrivateKeyPath());
        config.setApiV3Key(wxPayConfig.getApiV3Key());
        config.setPartnerKey(wxPayConfig.getPartnerKey());
        config.setDomain(wxPayConfig.getDomain());
        config.setNotifyUrl(wxPayConfig.getNotifyDomain());
        return config;
    }

    private String buildNotifyUrl(PaymentAppConfig config, WxNotifyType notifyType) {
        return required(config.getNotifyUrl(), "微信支付通知域名notifyUrl未配置").concat(notifyType.getType());
    }

    private String required(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(message);
        }
        return value.trim();
    }

    private String getString(Map<String, Object> map, String key) {
        if (map == null) {
            return null;
        }
        Object value = map.get(key);
        return value == null ? null : value.toString();
    }
}
