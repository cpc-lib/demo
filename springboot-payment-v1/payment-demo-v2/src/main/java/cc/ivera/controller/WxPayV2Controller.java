package cc.ivera.controller;

import cc.ivera.config.WxPayConfig;
import cc.ivera.entity.OrderInfo;
import cc.ivera.enums.OrderStatus;
import cc.ivera.service.OrderInfoService;
import cc.ivera.service.PaymentInfoService;
import cc.ivera.service.WxPayService;
import cc.ivera.util.HttpUtils;
import cc.ivera.vo.R;
import com.github.wxpay.sdk.WXPayUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.Positive;
import java.util.HashMap;
import java.util.Map;

@CrossOrigin //跨域
@RestController
@RequestMapping("/api/wx-pay-v2")
@Api(tags = "网站微信支付APIv2")
@Slf4j
@Validated
public class WxPayV2Controller {

    @Resource
    private WxPayService wxPayService;

    @Resource
    private WxPayConfig wxPayConfig;

    @Resource
    private OrderInfoService orderInfoService;

    @Resource
    private PaymentInfoService paymentInfoService;

    /**
     * Native下单
     *
     * @param productId
     * @return
     */
    @ApiOperation("调用统一下单API，生成支付二维码")
    @PostMapping("/native/{productId}")
    public R<Map<String, Object>> createNative(@PathVariable @Positive(message = "商品ID必须大于0") Long productId, HttpServletRequest request) {
        log.info("发起支付请求 v2");

        String remoteAddr = request.getRemoteAddr();
        //修改code_url可以重新获取到新的地址信息
        Map<String, Object> map = wxPayService.nativePayV2(productId, remoteAddr);
        return R.ok().setData(map);
    }

    /**
     * 支付通知
     * 微信支付通过支付通知接口将用户支付成功消息通知给商户
     */
    @PostMapping("/native/notify")
    @Transactional(rollbackFor = Exception.class)
    public String wxNotify(HttpServletRequest request) {
        log.info("微信发送的回调");

        try {
            //处理通知参数
            String body = HttpUtils.readData(request);

            //验签
            if (!WXPayUtil.isSignatureValid(body,
                    wxPayConfig.getPartnerKey())) {
                log.error("通知验签失败");
                return wxNotifyFail("验签失败");
            }

            //解析xml数据
            Map<String, String> notifyMap = WXPayUtil.xmlToMap(body);
            //判断通信和业务是否成功
            if (!"SUCCESS".equals(notifyMap.get("return_code")) || !"SUCCESS".equals(notifyMap.get("result_code"))) {
                log.error("失败");
                return wxNotifyFail("失败");
            }

            //获取商户订单号
            String orderNo = notifyMap.get("out_trade_no");
            if (orderNo == null || orderNo.trim().isEmpty()) {
                log.error("微信支付v2通知缺少商户订单号");
                return wxNotifyFail("订单号不能为空");
            }

            OrderInfo orderInfo = orderInfoService.getOrderByOrderNo(orderNo);
            if (orderInfo == null) {
                log.error("微信支付v2通知订单不存在 ===> {}", orderNo);
                return wxNotifyFail("订单不存在");
            }

            //并校验返回的订单金额是否与商户侧的订单金额一致
            Long totalFee = parseTotalFee(notifyMap.get("total_fee"));
            if (totalFee == null || !totalFee.equals(orderInfo.getTotalFee())) {
                log.error("金额校验失败");
                return wxNotifyFail("金额校验失败");
            }

            //处理订单：只有数据库条件更新成功的请求才写支付流水
            boolean updated = orderInfoService.updateStatusByOrderNoIfStatus(
                    orderNo,
                    OrderStatus.NOTPAY,
                    OrderStatus.SUCCESS);
            if (updated) {
                paymentInfoService.createPaymentInfoForWxPayV2(notifyMap, body);
            } else {
                log.info("微信支付v2通知重复或订单状态已变化，忽略处理 ===> {}", orderNo);
            }

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

    private Long parseTotalFee(String totalFee) {
        try {
            return Long.parseLong(totalFee);
        } catch (NumberFormatException e) {
            log.error("微信支付v2通知金额格式错误 ===> {}", totalFee, e);
            return null;
        }
    }
}
