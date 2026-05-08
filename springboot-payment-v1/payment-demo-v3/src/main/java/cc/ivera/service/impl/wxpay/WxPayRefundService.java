package cc.ivera.service.impl.wxpay;

import cc.ivera.config.WxPayConfig;
import cc.ivera.entity.RefundInfo;
import cc.ivera.enums.RefundStatus;
import cc.ivera.enums.wxpay.WxApiType;
import cc.ivera.enums.wxpay.WxNotifyType;
import cc.ivera.enums.wxpay.WxRefundStatus;
import cc.ivera.exception.BizException;
import cc.ivera.service.RefundInfoService;
import cc.ivera.service.refund.RefundStatusSyncResult;
import cc.ivera.util.HttpClientUtils;
import cc.ivera.util.JsonUtils;
import com.github.wxpay.sdk.WXPayUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class WxPayRefundService {

    private final WxPayConfig wxPayConfig;

    private final RefundInfoService refundsInfoService;

    private final WxPayHttpClient wxPayHttpClient;

    private final WxPayNotificationDecoder wxPayNotificationDecoder;

    public WxPayRefundService(
        WxPayConfig wxPayConfig,
        RefundInfoService refundsInfoService,
        WxPayHttpClient wxPayHttpClient,
        WxPayNotificationDecoder wxPayNotificationDecoder
    ) {
        this.wxPayConfig = wxPayConfig;
        this.refundsInfoService = refundsInfoService;
        this.wxPayHttpClient = wxPayHttpClient;
        this.wxPayNotificationDecoder = wxPayNotificationDecoder;
    }

    public void executeRefund(RefundInfo refundInfo) {
        if (refundInfo == null) {
            throw new BizException("退款申请单不能为空");
        }

        log.info("调用微信退款API, refundNo = {}", refundInfo.getRefundNo());

        Map<String, Object> paramsMap = new HashMap<>();
        paramsMap.put("out_trade_no", refundInfo.getOrderNo());
        paramsMap.put("out_refund_no", refundInfo.getRefundNo());
        paramsMap.put("reason", refundInfo.getReason());
        paramsMap.put("notify_url", wxPayConfig.getNotifyDomain().concat(WxNotifyType.REFUND_NOTIFY.getType()));

        Map<String, Object> amountMap = new HashMap<>();
        amountMap.put("refund", refundInfo.getRefund());
        amountMap.put("total", refundInfo.getTotalFee());
        amountMap.put("currency", "CNY");
        paramsMap.put("amount", amountMap);

        String jsonParams = JsonUtils.toJson(paramsMap);
        log.info("微信退款请求参数 ===> {}", jsonParams);

        String url = wxPayConfig.getDomain().concat(WxApiType.DOMESTIC_REFUNDS.getType());
        WxPayHttpClient.WxPayHttpResponse response;
        try {
            response = wxPayHttpClient.postJsonForResponse(url, jsonParams);
        } catch (IOException e) {
            refundsInfoService.updateRefundToFailed(refundInfo.getRefundNo(), e.getMessage());
            throw new BizException("微信退款异常", e);
        }
        if (response.isSuccessful()) {
            log.info("微信退款申请已受理，返回结果 = {}", response.getBody());
            refundsInfoService.updateRefundToProcessing(refundInfo.getRefundNo(), response.getBody());
            return;
        }

        refundsInfoService.updateRefundToFailed(refundInfo.getRefundNo(), response.getBody());
        throw new BizException("微信退款异常，响应码 = " + response.getStatusCode() + ", 返回结果 = " + response.getBody());
    }

    public String queryRefund(String refundNo) {
        log.info("调用微信退款查询接口 ===> {}", refundNo);

        String url = String.format(WxApiType.DOMESTIC_REFUNDS_QUERY.getType(), refundNo);
        url = wxPayConfig.getDomain().concat(url);

        try {
            return wxPayHttpClient.get(url, "查询微信退款异常");
        } catch (IOException e) {
            throw new BizException("查询微信退款异常", e);
        }
    }

    public RefundStatusSyncResult queryRefundStatusForSync(String refundNo) {
        String result = queryRefund(refundNo);
        log.info("微信退款查询结果 ===> {}", result);

        Map<String, Object> resultMap = JsonUtils.toObjectMap(result);
        String channelStatus = getString(resultMap, "status");
        if (channelStatus == null || channelStatus.trim().isEmpty()) {
            log.warn("微信退款查询结果缺少status，refundNo={}", refundNo);
        }
        return buildRefundStatusSyncResult(resultMap, channelStatus, result);
    }

    public List<RefundStatusSyncResult> queryOrderRefundsForSync(String orderNo) {
        log.info("调用微信订单退款查询接口 ===> {}", orderNo);

        List<RefundStatusSyncResult> resultList = new ArrayList<>();
        int offset = 0;
        while (true) {
            Map<String, String> resultMap = queryOrderRefundsByV2(orderNo, offset);
            if (resultMap.isEmpty()) {
                return resultList;
            }

            int pageRefundCount = parseInteger(resultMap.get("refund_count"), 0);
            int totalRefundCount = parseInteger(resultMap.get("total_refund_count"), pageRefundCount);
            if (pageRefundCount <= 0) {
                return resultList;
            }

            int pageSize = 0;
            for (int i = 0; i < pageRefundCount; i++) {
                String refundNo = resultMap.get("out_refund_no_" + i);
                if (refundNo == null || refundNo.trim().isEmpty()) {
                    continue;
                }

                pageSize++;
                String channelStatus = resultMap.get("refund_status_" + i);
                Map<String, Object> item = new HashMap<>();
                item.put("out_trade_no", resultMap.get("out_trade_no"));
                item.put("out_refund_no", refundNo);
                item.put("refund_id", resultMap.get("refund_id_" + i));
                item.put("refund_status", channelStatus);
                item.put("total_fee", resultMap.get("total_fee"));
                item.put("refund_fee", resultMap.get("refund_fee_" + i));
                item.put("offset", offset);

                resultList.add(RefundStatusSyncResult.of(
                        resultMap.get("out_trade_no"),
                        refundNo,
                        resultMap.get("refund_id_" + i),
                        channelStatus,
                        mapWxRefundStatus(channelStatus),
                        JsonUtils.toJson(item),
                        parseInteger(resultMap.get("total_fee"), null),
                        parseInteger(resultMap.get("refund_fee_" + i), null)));
            }

            if (pageSize == 0 || resultList.size() >= totalRefundCount) {
                return resultList;
            }
            offset += pageSize;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void processRefund(Map<String, Object> bodyMap) {
        log.info("处理微信退款结果通知");

        String plainText;
        try {
            plainText = wxPayNotificationDecoder.decryptResource(bodyMap);
        } catch (GeneralSecurityException e) {
            throw new BizException("微信退款通知解密失败", e);
        }

        Map<String, Object> plainTextMap = JsonUtils.toObjectMap(plainText);
        String channelStatus = getString(plainTextMap, "refund_status");
        RefundStatusSyncResult syncResult = buildRefundStatusSyncResult(plainTextMap, channelStatus, plainText);

        boolean updated = refundsInfoService.syncRefundStatus(syncResult);
        logRefundSyncIgnoredIfNeeded(updated, syncResult);
    }

    private RefundStatusSyncResult buildRefundStatusSyncResult(Map<String, Object> refundMap,
                                                               String channelStatus,
                                                               String content) {
        return RefundStatusSyncResult.of(
                getString(refundMap, "out_trade_no"),
                getString(refundMap, "out_refund_no"),
                getString(refundMap, "refund_id"),
                channelStatus,
                mapWxRefundStatus(channelStatus),
                content,
                getAmountInteger(refundMap, "total"),
                getAmountInteger(refundMap, "refund"));
    }

    private RefundStatus mapWxRefundStatus(String channelStatus) {
        if (WxRefundStatus.SUCCESS.getType().equals(channelStatus)) {
            return RefundStatus.SUCCESS;
        }
        if (WxRefundStatus.PROCESSING.getType().equals(channelStatus)) {
            return RefundStatus.PROCESSING;
        }
        if (WxRefundStatus.ABNORMAL.getType().equals(channelStatus)) {
            return RefundStatus.ABNORMAL;
        }
        if (WxRefundStatus.CLOSED.getType().equals(channelStatus)) {
            return RefundStatus.CLOSED;
        }
        if ("REFUNDCLOSE".equals(channelStatus)) {
            return RefundStatus.CLOSED;
        }
        if ("CHANGE".equals(channelStatus)) {
            return RefundStatus.ABNORMAL;
        }
        return null;
    }

    private Map<String, String> queryOrderRefundsByV2(String orderNo, int offset) {
        HttpClientUtils client = new HttpClientUtils(wxPayConfig.getDomain().concat(WxApiType.REFUND_QUERY_V2.getType()));
        Map<String, String> params = new HashMap<>();
        params.put("appid", wxPayConfig.getAppid());
        params.put("mch_id", wxPayConfig.getMchId());
        params.put("nonce_str", WXPayUtil.generateNonceStr());
        params.put("out_trade_no", orderNo);
        params.put("offset", String.valueOf(offset));

        try {
            String xmlParams = WXPayUtil.generateSignedXml(params, wxPayConfig.getPartnerKey());
            client.setXmlParam(xmlParams);
            client.setHttps(true);
            client.post();

            String resultXml = client.getContent();
            log.info("微信订单退款查询结果 ===> {}", resultXml);
            Map<String, String> resultMap = WXPayUtil.xmlToMap(resultXml);
            if (!"SUCCESS".equals(resultMap.get("return_code"))) {
                throw new BizException("微信订单退款查询通信失败：" + resultMap.get("return_msg"));
            }
            if (!"SUCCESS".equals(resultMap.get("result_code"))) {
                if ("REFUNDNOTEXIST".equals(resultMap.get("err_code"))) {
                    return Collections.emptyMap();
                }
                throw new BizException("微信订单退款查询失败：" + resultMap.get("err_code_des"));
            }
            return resultMap;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("微信订单退款查询异常", e);
        }
    }

    private void logRefundSyncIgnoredIfNeeded(boolean updated, RefundStatusSyncResult syncResult) {
        if (!updated && syncResult != null) {
            log.info("微信退款状态同步无需更新，refundNo={}, channelStatus={}",
                    syncResult.getRefundNo(),
                    syncResult.getChannelStatus());
        }
    }

    private String getString(Map<String, Object> map, String key) {
        if (map == null) {
            return null;
        }
        Object value = map.get(key);
        return value == null ? null : value.toString();
    }

    private Integer getAmountInteger(Map<String, Object> map, String key) {
        Map<String, Object> amountMap = JsonUtils.toObjectMap(map == null ? null : map.get("amount"));
        return parseInteger(getString(amountMap, key), null);
    }

    private Integer parseInteger(String value, Integer defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
