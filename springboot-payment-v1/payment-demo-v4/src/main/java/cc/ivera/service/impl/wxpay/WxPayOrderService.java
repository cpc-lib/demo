package cc.ivera.service.impl.wxpay;

import cc.ivera.config.WxPayConfig;
import cc.ivera.entity.OrderInfo;
import cc.ivera.enums.OrderStatus;
import cc.ivera.enums.PayType;
import cc.ivera.enums.wxpay.WxApiType;
import cc.ivera.enums.wxpay.WxNotifyType;
import cc.ivera.enums.wxpay.WxTradeState;
import cc.ivera.exception.BizException;
import cc.ivera.lock.DistributedLockTemplate;
import cc.ivera.service.OrderInfoService;
import cc.ivera.service.PaymentInfoService;
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

    private static final long PAY_NOTIFY_LOCK_LEASE_MS = 30000L;

    private final WxPayConfig wxPayConfig;

    private final OrderInfoService orderInfoService;

    private final PaymentInfoService paymentInfoService;

    private final WxPayHttpClient wxPayHttpClient;

    private final WxPayNotificationDecoder wxPayNotificationDecoder;

    private final DistributedLockTemplate distributedLockTemplate;

    private final TransactionTemplate transactionTemplate;

    public WxPayOrderService(
        WxPayConfig wxPayConfig,
        OrderInfoService orderInfoService,
        PaymentInfoService paymentInfoService,
        WxPayHttpClient wxPayHttpClient,
        WxPayNotificationDecoder wxPayNotificationDecoder,
        DistributedLockTemplate distributedLockTemplate,
        TransactionTemplate transactionTemplate
    ) {
        this.wxPayConfig = wxPayConfig;
        this.orderInfoService = orderInfoService;
        this.paymentInfoService = paymentInfoService;
        this.wxPayHttpClient = wxPayHttpClient;
        this.wxPayNotificationDecoder = wxPayNotificationDecoder;
        this.distributedLockTemplate = distributedLockTemplate;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public Map<String, Object> nativePay(Long productId) {
        return distributedLockTemplate.execute(
                "payment:wx:native:v3:" + productId,
                3000L,
                -1L,
                () -> doNativePay(productId)
        );
    }

    private Map<String, Object> doNativePay(Long productId) {
        log.info("生成订单");

        OrderInfo orderInfo = orderInfoService.createOrReuseOrder(productId, PayType.WXPAY.getType());
        if (orderInfo == null) {
            throw new BizException("订单创建失败");
        }
        String codeUrl = orderInfo.getCodeUrl();
        if (StringUtils.hasText(codeUrl)) {
            log.info("订单已存在，二维码已保存");
            return buildNativePayResult(orderInfo.getOrderNo(), codeUrl);
        }

        log.info("调用统一下单API");

        Map<String, Object> paramsMap = new HashMap<>();
        paramsMap.put("appid", wxPayConfig.getAppid());
        paramsMap.put("mchid", wxPayConfig.getMchId());
        paramsMap.put("description", orderInfo.getTitle());
        paramsMap.put("out_trade_no", orderInfo.getOrderNo());
        paramsMap.put("notify_url", wxPayConfig.getNotifyDomain().concat(WxNotifyType.NATIVE_NOTIFY.getType()));

        Map<String, Object> amountMap = new HashMap<>();
        amountMap.put("total", orderInfo.getTotalFee());
        amountMap.put("currency", "CNY");
        paramsMap.put("amount", amountMap);

        String jsonParams = JsonUtils.toJson(paramsMap);
        log.info("请求参数 ===> {}", jsonParams);

        String url = wxPayConfig.getDomain().concat(WxApiType.NATIVE_PAY.getType());
        String bodyAsString;
        try {
            bodyAsString = wxPayHttpClient.postJson(url, jsonParams, "Native下单失败");
        } catch (IOException e) {
            throw new BizException("Native下单失败", e);
        }
        Map<String, Object> resultMap = JsonUtils.toObjectMap(bodyAsString);

        codeUrl = getString(resultMap, "code_url");
        orderInfoService.saveCodeUrl(orderInfo.getOrderNo(), codeUrl);

        return buildNativePayResult(orderInfo.getOrderNo(), codeUrl);
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
        String lockKey = "payment:wx:notify:pay:" + orderNo;

        // 使用 Redisson 分布式锁，leaseTime=-1 开启看门狗自动续期
        distributedLockTemplate.execute(lockKey, PAY_NOTIFY_LOCK_WAIT_MS, -1L, () ->
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
        log.info("微信支付通知加锁处理开始，orderNo={}, notifyId={}", orderNo, notifyId);

        // 付款成功通知可能重复投递，也可能和主动查单、延迟关单并发。
        // 这里在 Redis 分布式锁内再加数据库行锁，保证同一订单本地串行处理。
        OrderInfo lockedOrder = orderInfoService.getOrderByOrderNoForUpdate(orderNo);
        if (lockedOrder == null) {
            throw new BizException("微信支付通知对应订单不存在，orderNo=" + orderNo);
        }

        validateWxPayOrderNotify(lockedOrder, plainTextMap);

        if (!OrderStatus.NOTPAY.getType().equals(lockedOrder.getOrderStatus())) {
            log.info("微信支付通知重复或订单状态已变化，幂等忽略 ===> orderNo={}, notifyId={}, currentStatus={}",
                    orderNo, notifyId, lockedOrder.getOrderStatus());
            return;
        }

        boolean updated = orderInfoService.updateStatusByOrderNoIfStatus(
                orderNo,
                OrderStatus.NOTPAY,
                OrderStatus.SUCCESS);
        if (!updated) {
            log.info("微信支付通知状态更新失败，可能已被其他事务处理，orderNo={}, notifyId={}", orderNo, notifyId);
            return;
        }

        paymentInfoService.createPaymentInfo(plainText);
        log.info("微信支付通知处理完成，orderNo={}, notifyId={}", orderNo, notifyId);
    }

    @Override
    public void cancelOrder(String orderNo) {
        if (!StringUtils.hasText(orderNo)) {
            throw new BizException("订单号不能为空");
        }

        distributedLockTemplate.execute("payment:order:cancel:" + orderNo, 3000L, -1L, () ->
                transactionTemplate.execute(status -> {
                    OrderInfo lockedOrder = orderInfoService.getOrderByOrderNoForUpdate(orderNo);
                    if (lockedOrder == null) {
                        throw new BizException("订单不存在，orderNo=" + orderNo);
                    }
                    if (!OrderStatus.NOTPAY.getType().equals(lockedOrder.getOrderStatus())) {
                        log.info("订单当前状态不允许取消，orderNo={}, currentStatus={}",
                                orderNo, lockedOrder.getOrderStatus());
                        return null;
                    }

                    closeOrder(orderNo);
                    boolean updated = orderInfoService.updateStatusByOrderNoIfStatus(orderNo, OrderStatus.NOTPAY, OrderStatus.CANCEL);
                    if (!updated) {
                        log.info("订单取消状态更新被忽略，orderNo={}", orderNo);
                    }
                    return null;
                })
        );
    }

    @Override
    public String queryOrder(String orderNo) {
        log.info("查单接口调用 ===> {}", orderNo);

        String url = String.format(WxApiType.ORDER_QUERY_BY_NO.getType(), orderNo);
        url = wxPayConfig.getDomain().concat(url).concat("?mchid=").concat(wxPayConfig.getMchId());

        try {
            return wxPayHttpClient.get(url, "查单接口调用异常");
        } catch (IOException e) {
            throw new BizException("查单接口调用异常", e);
        }
    }

    @Override
    public Map<String, Object> queryPaymentStatus(String orderNo) {
        if (!StringUtils.hasText(orderNo)) {
            throw new BizException("订单号不能为空");
        }

        OrderInfo orderInfo = orderInfoService.getOrderByOrderNo(orderNo);
        if (orderInfo == null) {
            throw new BizException("订单不存在，orderNo=" + orderNo);
        }
        if (!PayType.WXPAY.getType().equals(orderInfo.getPaymentType())) {
            throw new BizException("订单不是微信支付订单，orderNo=" + orderNo);
        }

        log.info("主动查询微信支付状态 ===> {}", orderNo);

        String result = queryOrder(orderNo);
        Map<String, Object> resultMap = JsonUtils.toObjectMap(result);
        String tradeState = getString(resultMap, "trade_state");
        String tradeStateDesc = getString(resultMap, "trade_state_desc");
        String localStatusBefore = orderInfo.getOrderStatus();

        distributedLockTemplate.execute("payment:wx:query:order:" + orderNo, 5000L, -1L, () ->
                transactionTemplate.execute(status -> {
                    doSyncOrderStatusFromWxQuery(orderNo, resultMap, result, tradeState, false);
                    return null;
                })
        );

        String localStatusAfter = orderInfoService.getOrderStatus(orderNo);
        return buildPaymentStatusResult(orderNo, tradeState, tradeStateDesc, localStatusBefore, localStatusAfter, resultMap);
    }

    @Override
    public void checkOrderStatus(String orderNo) {
        log.warn("根据订单号核实订单状态 ===> {}", orderNo);

        String result = queryOrder(orderNo);
        Map<String, Object> resultMap = JsonUtils.toObjectMap(result);
        String tradeState = getString(resultMap, "trade_state");

        distributedLockTemplate.execute("payment:wx:check:order:" + orderNo, 5000L, -1L, () ->
                transactionTemplate.execute(status -> {
                    doSyncOrderStatusFromWxQuery(orderNo, resultMap, result, tradeState, true);
                    return null;
                })
        );
    }

    private void doSyncOrderStatusFromWxQuery(String orderNo,
                                              Map<String, Object> resultMap,
                                              String result,
                                              String tradeState,
                                              boolean closeUnpaidOrder) {
        OrderInfo lockedOrder = orderInfoService.getOrderByOrderNoForUpdate(orderNo);
        if (lockedOrder == null) {
            throw new BizException("查单同步对应订单不存在，orderNo=" + orderNo);
        }
        if (!OrderStatus.NOTPAY.getType().equals(lockedOrder.getOrderStatus())) {
            log.info("微信查单同步发现订单已处理，orderNo={}, currentStatus={}", orderNo, lockedOrder.getOrderStatus());
            return;
        }

        if (WxTradeState.SUCCESS.getType().equals(tradeState)) {
            log.warn("核实订单已支付 ===> {}", orderNo);
            validateWxPayOrderNotify(lockedOrder, resultMap);

            boolean updated = orderInfoService.updateStatusByOrderNoIfStatus(
                    orderNo,
                    OrderStatus.NOTPAY,
                    OrderStatus.SUCCESS);
            if (!updated) {
                log.info("微信查单确认支付成功，但订单已处理，忽略支付日志 ===> {}", orderNo);
                return;
            }

            paymentInfoService.createPaymentInfo(result);
        } else if (WxTradeState.NOTPAY.getType().equals(tradeState)) {
            log.warn("核实订单未支付 ===> {}", orderNo);
            if (closeUnpaidOrder) {
                closeOrder(orderNo);
                orderInfoService.updateStatusByOrderNoIfStatus(orderNo, OrderStatus.NOTPAY, OrderStatus.CLOSED);
            }
        } else if (WxTradeState.CLOSED.getType().equals(tradeState)) {
            log.warn("核实订单已关闭 ===> {}", orderNo);
            orderInfoService.updateStatusByOrderNoIfStatus(orderNo, OrderStatus.NOTPAY, OrderStatus.CLOSED);
        }
    }

    @Override
    public Map<String, Object> nativePayV2(Long productId, String remoteAddr) {
        return distributedLockTemplate.execute(
                "payment:wx:native:v2:" + productId,
                3000L,
                -1L,
                () -> doNativePayV2(productId, remoteAddr)
        );
    }

    private Map<String, Object> doNativePayV2(Long productId, String remoteAddr) {
        log.info("生成订单");

        OrderInfo orderInfo = orderInfoService.createOrReuseOrder(productId, PayType.WXPAY.getType());
        if (orderInfo == null) {
            throw new BizException("订单创建失败");
        }
        String codeUrl = orderInfo.getCodeUrl();
        if (StringUtils.hasText(codeUrl)) {
            log.info("订单已存在，二维码已保存");
            return buildNativePayResult(orderInfo.getOrderNo(), codeUrl);
        }

        log.info("调用统一下单API");

        HttpClientUtils client = new HttpClientUtils(wxPayConfig.getDomain().concat(WxApiType.NATIVE_PAY_V2.getType()));

        Map<String, String> params = new HashMap<>();
        params.put("appid", wxPayConfig.getAppid());
        params.put("mch_id", wxPayConfig.getMchId());
        params.put("nonce_str", WXPayUtil.generateNonceStr());
        params.put("body", orderInfo.getTitle());
        params.put("out_trade_no", orderInfo.getOrderNo());
        params.put("total_fee", orderInfo.getTotalFee() + "");
        params.put("spbill_create_ip", remoteAddr);
        params.put("notify_url", wxPayConfig.getNotifyDomain().concat(WxNotifyType.NATIVE_NOTIFY_V2.getType()));
        params.put("trade_type", "NATIVE");

        String resultXml;
        Map<String, String> resultMap;
        try {
            String xmlParams = WXPayUtil.generateSignedXml(params, wxPayConfig.getPartnerKey());
            log.info("\n xmlParams：\n{}", xmlParams);

            client.setXmlParam(xmlParams);
            client.setHttps(true);
            client.post();

            resultXml = client.getContent();
            log.info("\n resultXml：\n{}", resultXml);
            resultMap = WXPayUtil.xmlToMap(resultXml);
        } catch (Exception e) {
            throw new BizException("微信支付v2统一下单失败", e);
        }

        if ("FAIL".equals(resultMap.get("return_code")) || "FAIL".equals(resultMap.get("result_code"))) {
            log.error("微信支付统一下单错误 ===> {} ", resultXml);
            throw new BizException("微信支付统一下单错误");
        }

        codeUrl = resultMap.get("code_url");
        orderInfoService.saveCodeUrl(orderInfo.getOrderNo(), codeUrl);

        return buildNativePayResult(orderInfo.getOrderNo(), codeUrl);
    }

    @Override
    public Map<String, Object> jsapiPay(OrderInfo orderInfo, String openid) {
        String lockKey = "payment:wx:jsapi:" + orderInfo.getOrderNo();
        return distributedLockTemplate.execute(lockKey, 5000L, -1L, () -> {
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectMapper objectMapper = new ObjectMapper();
                ObjectNode rootNode = objectMapper.createObjectNode();
                rootNode.put("mchid", wxPayConfig.getMchId())
                        .put("appid", wxPayConfig.getAppid())
                        .put("description", orderInfo.getTitle())
                        .put("notify_url", "回调链接自己写")
                        .put("out_trade_no", orderInfo.getOrderNo());
                rootNode.putObject("amount")
                        .put("total", orderInfo.getTotalFee());
                rootNode.putObject("payer")
                        .put("openid", openid);
                objectMapper.writeValue(bos, rootNode);

                String url = wxPayConfig.getDomain().concat(WxApiType.JSAPI_PAY.getType());
                String bodyAsString = wxPayHttpClient.postJson(url, bos.toString("UTF-8"), "JSAPI下单失败");
                Map<String, Object> responseMap = JsonUtils.toObjectMap(bodyAsString);
                String prepayId = (String) responseMap.get("prepay_id");

                return getPayment("prepay_id=" + prepayId,
                        wxPayConfig.getAppid(),
                        wxPayConfig.getPrivateKey(wxPayConfig.getPrivateKeyPath()));
            } catch (Exception e) {
                throw new BizException("支付失败" + e.getMessage(), e);
            }
        });
    }

    private void closeOrder(String orderNo) {
        log.info("关单接口的调用，订单号 ===> {}", orderNo);

        String url = String.format(WxApiType.CLOSE_ORDER_BY_NO.getType(), orderNo);
        url = wxPayConfig.getDomain().concat(url);

        Map<String, String> paramsMap = new HashMap<>();
        paramsMap.put("mchid", wxPayConfig.getMchId());
        String jsonParams = JsonUtils.toJson(paramsMap);
        log.info("请求参数 ===> {}", jsonParams);

        try {
            wxPayHttpClient.postJson(url, jsonParams, "Native关单失败");
        } catch (IOException e) {
            throw new BizException("Native关单失败", e);
        }
    }


    private void validateWxPayOrderNotify(OrderInfo orderInfo, Map<String, Object> notifyMap) {
        if (!PayType.WXPAY.getType().equals(orderInfo.getPaymentType())) {
            throw new BizException("支付通知支付类型不匹配，orderNo=" + orderInfo.getOrderNo());
        }

        Integer notifyTotal = getWxPayTotalAmount(notifyMap);
        if (notifyTotal == null) {
            log.warn("微信支付通知缺少金额字段，跳过金额校验，orderNo={}", orderInfo.getOrderNo());
            return;
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

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? null : value.toString();
    }
}
