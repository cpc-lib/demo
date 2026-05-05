package cc.ivera.service.impl.wxpay;

import cc.ivera.config.WxPayConfig;
import cc.ivera.entity.RefundInfo;
import cc.ivera.enums.RefundStatus;
import cc.ivera.enums.wxpay.WxApiType;
import cc.ivera.enums.wxpay.WxNotifyType;
import cc.ivera.enums.wxpay.WxRefundStatus;
import cc.ivera.exception.BizException;
import cc.ivera.service.RefundInfoService;
import cc.ivera.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.HashMap;
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

        log.info("调用退款API, refundNo = {}", refundInfo.getRefundNo());

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
        log.info("请求参数 ===> {}", jsonParams);

        String url = wxPayConfig.getDomain().concat(WxApiType.DOMESTIC_REFUNDS.getType());
        WxPayHttpClient.WxPayHttpResponse response;
        try {
            response = wxPayHttpClient.postJsonForResponse(url, jsonParams);
        } catch (IOException e) {
            refundsInfoService.updateRefundToFailed(refundInfo.getRefundNo(), e.getMessage());
            throw new BizException("退款异常", e);
        }
        if (response.isSuccessful()) {
            log.info("退款申请已受理，返回结果 = {}", response.getBody());
            refundsInfoService.updateRefundToProcessing(refundInfo.getRefundNo(), response.getBody());
            return;
        }

        refundsInfoService.updateRefundToFailed(refundInfo.getRefundNo(), response.getBody());
        throw new BizException("退款异常, 响应码 = " + response.getStatusCode() + ", 退款返回结果 = " + response.getBody());
    }

    public String queryRefund(String refundNo) {
        log.info("查询退款接口调用 ===> {}", refundNo);

        String url = String.format(WxApiType.DOMESTIC_REFUNDS_QUERY.getType(), refundNo);
        url = wxPayConfig.getDomain().concat(url);

        try {
            return wxPayHttpClient.get(url, "查询退款异常");
        } catch (IOException e) {
            throw new BizException("查询退款异常", e);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void processRefund(Map<String, Object> bodyMap) {
        log.info("退款通知");

        String plainText;
        try {
            plainText = wxPayNotificationDecoder.decryptResource(bodyMap);
        } catch (GeneralSecurityException e) {
            throw new BizException("微信退款通知解密失败", e);
        }
        Map<String, Object> plainTextMap = JsonUtils.toObjectMap(plainText);
        String refundNo = (String) plainTextMap.get("out_refund_no");
        String refundId = (String) plainTextMap.get("refund_id");
        String refundStatus = (String) plainTextMap.get("refund_status");

        if (WxRefundStatus.SUCCESS.getType().equals(refundStatus)) {
            boolean updated = refundsInfoService.updateRefundIfStatusIn(
                    refundNo,
                    refundId,
                    RefundStatus.SUCCESS,
                    null,
                    plainText,
                    Arrays.asList(RefundStatus.CREATED, RefundStatus.PROCESSING, RefundStatus.ABNORMAL));
            logRefundNotifyIgnoredIfDuplicate(updated, refundNo, refundStatus);
            return;
        }

        if (WxRefundStatus.ABNORMAL.getType().equals(refundStatus)) {
            boolean updated = refundsInfoService.updateRefundIfStatusIn(
                    refundNo,
                    refundId,
                    RefundStatus.ABNORMAL,
                    null,
                    plainText,
                    Arrays.asList(RefundStatus.CREATED, RefundStatus.PROCESSING));
            logRefundNotifyIgnoredIfDuplicate(updated, refundNo, refundStatus);
            return;
        }

        if (WxRefundStatus.CLOSED.getType().equals(refundStatus)) {
            boolean updated = refundsInfoService.updateRefundIfStatusIn(
                    refundNo,
                    refundId,
                    RefundStatus.CLOSED,
                    null,
                    plainText,
                    Arrays.asList(RefundStatus.CREATED, RefundStatus.PROCESSING, RefundStatus.ABNORMAL));
            logRefundNotifyIgnoredIfDuplicate(updated, refundNo, refundStatus);
            return;
        }

        log.info("微信退款通知状态无需处理，refundNo={}, refundStatus={}", refundNo, refundStatus);
    }

    private void logRefundNotifyIgnoredIfDuplicate(boolean updated, String refundNo, String refundStatus) {
        if (!updated) {
            log.info("微信退款通知重复或退款状态已变化，忽略处理，refundNo={}, refundStatus={}", refundNo, refundStatus);
        }
    }

}
