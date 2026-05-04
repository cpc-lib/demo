package cc.ivera.service.impl.wxpay;

import cc.ivera.config.WxPayConfig;
import cc.ivera.entity.OrderInfo;
import cc.ivera.enums.OrderStatus;
import cc.ivera.enums.PayType;
import cc.ivera.enums.wxpay.WxApiType;
import cc.ivera.enums.wxpay.WxNotifyType;
import cc.ivera.enums.wxpay.WxTradeState;
import cc.ivera.exception.BizException;
import cc.ivera.service.OrderInfoService;
import cc.ivera.service.PaymentInfoService;
import cc.ivera.util.HttpClientUtils;
import cc.ivera.util.JsonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.wxpay.sdk.WXPayUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
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
public class WxPayOrderService {

    @Resource
    private WxPayConfig wxPayConfig;

    @Resource
    private OrderInfoService orderInfoService;

    @Resource
    private PaymentInfoService paymentInfoService;

    @Resource
    private WxPayHttpClient wxPayHttpClient;

    @Resource
    private WxPayNotificationDecoder wxPayNotificationDecoder;

    public Map<String, Object> nativePay(Long productId) {
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

    @Transactional(rollbackFor = Exception.class)
    public void processOrder(Map<String, Object> bodyMap) {
        log.info("处理订单");

        String plainText;
        try {
            plainText = wxPayNotificationDecoder.decryptResource(bodyMap);
        } catch (GeneralSecurityException e) {
            throw new BizException("微信支付通知解密失败", e);
        }
        Map<String, Object> plainTextMap = JsonUtils.toObjectMap(plainText);
        String orderNo = (String) plainTextMap.get("out_trade_no");

        boolean updated = orderInfoService.updateStatusByOrderNoIfStatus(
                orderNo,
                OrderStatus.NOTPAY,
                OrderStatus.SUCCESS);
        if (!updated) {
            log.info("微信支付通知重复或订单状态已变化，忽略处理 ===> {}", orderNo);
            return;
        }

        paymentInfoService.createPaymentInfo(plainText);
    }

    public void cancelOrder(String orderNo) {
        closeOrder(orderNo);
        orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.CANCEL);
    }

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

    public void checkOrderStatus(String orderNo) {
        log.warn("根据订单号核实订单状态 ===> {}", orderNo);

        String result = queryOrder(orderNo);
        Map<String, Object> resultMap = JsonUtils.toObjectMap(result);
        String tradeState = getString(resultMap, "trade_state");

        if (WxTradeState.SUCCESS.getType().equals(tradeState)) {
            log.warn("核实订单已支付 ===> {}", orderNo);

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
            closeOrder(orderNo);
            orderInfoService.updateStatusByOrderNoIfStatus(orderNo, OrderStatus.NOTPAY, OrderStatus.CLOSED);
        }
    }

    public Map<String, Object> nativePayV2(Long productId, String remoteAddr) {
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

    public Map<String, Object> jsapiPay(OrderInfo orderInfo, String openid) {
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

    private Map<String, Object> buildNativePayResult(String orderNo, String codeUrl) {
        Map<String, Object> map = new HashMap<>();
        map.put("codeUrl", codeUrl);
        map.put("orderNo", orderNo);
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
