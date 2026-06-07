package cc.ivera.controller;

import cc.ivera.config.WxPayConfig;
import cc.ivera.service.wxpay.WxPayOrderFacade;
import cc.ivera.util.HttpUtils;
import cc.ivera.vo.R;
import com.github.wxpay.sdk.WXPayUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.Positive;
import java.util.HashMap;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/api/wx-pay-v2")
@Api(tags = "网站微信支付APIv2")
@Slf4j
@Validated
public class WxPayV2Controller {

    private final WxPayOrderFacade wxPayOrderFacade;

    private final WxPayConfig wxPayConfig;

    public WxPayV2Controller(WxPayOrderFacade wxPayOrderFacade,
                             WxPayConfig wxPayConfig) {
        this.wxPayOrderFacade = wxPayOrderFacade;
        this.wxPayConfig = wxPayConfig;
    }

    @ApiOperation("调用统一下单API，生成支付二维码")
    @PostMapping("/native/{productId}")
    public R<Map<String, Object>> createNative(@PathVariable @Positive(message = "商品ID必须大于0") Long productId,
                                               HttpServletRequest request) {
        log.info("发起支付请求 v2");

        String remoteAddr = request.getRemoteAddr();
        Map<String, Object> map = wxPayOrderFacade.nativePayV2(productId, remoteAddr);
        return R.ok().setData(map);
    }

    @ApiOperation("支付通知")
    @PostMapping("/native/notify")
    public String wxNotify(HttpServletRequest request) {
        log.info("微信发送的回调");

        try {
            String body = HttpUtils.readData(request);

            if (!WXPayUtil.isSignatureValid(body, wxPayConfig.getPartnerKey())) {
                log.error("通知验签失败");
                return wxNotifyFail("验签失败");
            }

            Map<String, String> notifyMap = WXPayUtil.xmlToMap(body);
            if (!"SUCCESS".equals(notifyMap.get("return_code")) || !"SUCCESS".equals(notifyMap.get("result_code"))) {
                log.error("微信支付v2通知返回失败");
                return wxNotifyFail("失败");
            }

            String orderNo = notifyMap.get("out_trade_no");
            if (orderNo == null || orderNo.trim().isEmpty()) {
                log.error("微信支付v2通知缺少商户订单号");
                return wxNotifyFail("订单号不能为空");
            }

            wxPayOrderFacade.processOrderV2(notifyMap, body);

            log.info("支付成功，已应答");
            return wxNotifySuccess();
        } catch (Exception e) {
            log.error("处理微信支付v2通知失败", e);
            return wxNotifyFail("失败");
        }
    }

    private String wxNotifySuccess() {
        return wxNotifyResponse("SUCCESS", "OK");
    }

    private String wxNotifyFail(String message) {
        return wxNotifyResponse("FAIL", message);
    }

    private String wxNotifyResponse(String returnCode, String returnMsg) {
        Map<String, String> returnMap = new HashMap<>();
        returnMap.put("return_code", returnCode);
        returnMap.put("return_msg", returnMsg);
        try {
            return WXPayUtil.mapToXml(returnMap);
        } catch (Exception e) {
            log.error("构造微信支付v2通知响应失败", e);
            return "<xml><return_code><![CDATA[" + returnCode + "]]></return_code><return_msg><![CDATA[" + returnMsg + "]]></return_msg></xml>";
        }
    }
}
