package cc.ivera.controller;

import cc.ivera.dto.RefundRequest;
import cc.ivera.service.RefundApplicationService;
import cc.ivera.entity.OrderInfo;
import cc.ivera.service.WxPayService;
import cc.ivera.util.HttpUtils;
import cc.ivera.util.JsonUtils;
import cc.ivera.util.WechatPay2ValidatorForRequest;
import cc.ivera.vo.R;
import com.wechat.pay.contrib.apache.httpclient.auth.Verifier;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;
import java.util.HashMap;
import java.util.Map;

/**
 * 缺陷没有主动查询机制 都是采用微信通知来修改订单的状态 退款单的状态
 */
@CrossOrigin //跨域
@RestController
@RequestMapping("/api/wx-pay")
@Api(tags = "网站微信支付APIv3")
@Slf4j
@Validated
public class WxPayController {

    private final WxPayService wxPayService;

    private final RefundApplicationService refundApplicationService;

    private final Verifier verifier;

    public WxPayController(
        WxPayService wxPayService,
        RefundApplicationService refundApplicationService,
        Verifier verifier
    ) {
        this.wxPayService = wxPayService;
        this.refundApplicationService = refundApplicationService;
        this.verifier = verifier;
    }

    /**
     * Native下单表用户对接微信呢支付t_payment_info信息表 二维码页面一个接口调用后去调用本地t_order_info的状态
     *
     * @param productId
     * @return
     */
    @ApiOperation("调用统一下单API，生成支付二维码")
    @PostMapping("/native/{productId}")
    public R<Map<String, Object>> nativePay(@PathVariable @Positive(message = "商品ID必须大于0") Long productId) {
        log.info("发起支付请求 v3");

        //返回支付二维码连接和订单号
        Map<String, Object> map = wxPayService.nativePay(productId);

        return R.ok().setData(map);
    }

    /**
     * 支付通知->需要在微信账号内配置
     * 微信支付通过支付通知接口将用户支付成功消息通知给商户
     * 特别注意测试需要开启花生壳类似工具
     * 修改订订单状态，记录支付记录
     */
    @ApiOperation("支付通知")
    @PostMapping("/native/notify")
    public String nativeNotify(HttpServletRequest request, HttpServletResponse response) {
        try {
            //处理通知参数
            String body = HttpUtils.readData(request);
            Map<String, Object> bodyMap = JsonUtils.toObjectMap(body);
            String requestId = (String) bodyMap.get("id");
            log.info("支付通知的id ===> {}", requestId);
            //log.info("支付通知的完整数据 ===> {}", body);
            //int a = 9 / 0;

            //签名的验证
            WechatPay2ValidatorForRequest wechatPay2ValidatorForRequest
                    = new WechatPay2ValidatorForRequest(verifier, requestId, body);
            if (!wechatPay2ValidatorForRequest.validate(request)) {
                log.error("通知验签失败");
                return errorNotifyResponse(response, "通知验签失败");
            }
            log.info("通知验签成功");

            //处理订单
            wxPayService.processOrder(bodyMap);

            //应答超时
            //模拟接收微信端的重复通知
//            TimeUnit.SECONDS.sleep(5);

            return successNotifyResponse(response);
        } catch (Exception e) {
            log.error("处理微信支付通知失败", e);
            return errorNotifyResponse(response, "失败");
        }

    }

    /**
     * TODO 用户取消订单
     * 做了两件时间 -> 主动调用微信支付单
     * 修改了本地之歌订单orderNo地状态为主动关闭
     *
     * @param orderNo
     * @return
     */
    @ApiOperation("用户取消订单")
    @PostMapping("/cancel/{orderNo}")
    public R<Map<String, Object>> cancel(@PathVariable @NotBlank(message = "订单号不能为空") @Size(max = 50, message = "订单号长度不能超过50个字符") String orderNo) {
        log.info("取消订单");
        wxPayService.cancelOrder(orderNo);
        return R.ok().setMessage("订单已取消");
    }

    /**
     * 查询订单 查询订单详情信息
     *
     * @param orderNo
     * @return
     */
    @ApiOperation("查询订单：测试订单状态用")
    @GetMapping("/query/{orderNo}")
    public R<Map<String, Object>> queryOrder(@PathVariable @NotBlank(message = "订单号不能为空") @Size(max = 50, message = "订单号长度不能超过50个字符") String orderNo) {
        log.info("查询订单");

        String result = wxPayService.queryOrder(orderNo);
        return R.ok().setMessage("查询成功").data("result", result);
    }

    /**
     * 生成一条t_refund_info记录,申请后钱退回,写退单表，更新退单信息
     *
     * @param orderNo
     * @param reason
     * @return
     */
    @ApiOperation("申请退款")
    @PostMapping("/refunds")
    public R<Map<String, Object>> refunds(@Valid @RequestBody RefundRequest request) {
        log.info("申请退款");
        refundApplicationService.createApplication(request.getOrderNo(), request.getRefundAmount(), request.getReason());
        return R.ok().setMessage("退款申请单创建成功，待审核");
    }

    @ApiOperation("申请退款-兼容旧接口")
    @PostMapping("/refunds/{orderNo}/{reason}")
    public R<Map<String, Object>> refundsLegacy(@PathVariable @NotBlank(message = "订单号不能为空") @Size(max = 50, message = "订单号长度不能超过50个字符") String orderNo,
                                                @PathVariable @NotBlank(message = "退款原因不能为空") @Size(max = 50, message = "退款原因长度不能超过50个字符") String reason) {
        log.info("申请退款(旧接口)");
        refundApplicationService.createApplication(orderNo, null, reason);
        return R.ok().setMessage("退款申请单创建成功，待审核");
    }

    /**
     * 查询退款
     *
     * @param refundNo
     * @return
     */
    @ApiOperation("查询退款：测试用")
    @GetMapping("/query-refund/{refundNo}")
    public R<Map<String, Object>> queryRefund(@PathVariable @NotBlank(message = "退款单号不能为空") @Size(max = 50, message = "退款单号长度不能超过50个字符") String refundNo) {
        log.info("查询退款");
        String result = wxPayService.queryRefund(refundNo);
        return R.ok().setMessage("查询成功").data("result", result);
    }

    /**
     * 退款结果通知 -需要在微信账号内配置
     * 退款状态改变后，微信会把相关退款结果发送给商户。
     */
    @ApiOperation("退款结果通知")
    @PostMapping("/refunds/notify")
    public String refundsNotify(HttpServletRequest request, HttpServletResponse response) {
        log.info("退款通知执行");

        try {
            //处理通知参数
            String body = HttpUtils.readData(request);
            Map<String, Object> bodyMap = JsonUtils.toObjectMap(body);
            String requestId = (String) bodyMap.get("id");
            log.info("支付通知的id ===> {}", requestId);

            //签名的验证
            WechatPay2ValidatorForRequest wechatPay2ValidatorForRequest
                    = new WechatPay2ValidatorForRequest(verifier, requestId, body);
            if (!wechatPay2ValidatorForRequest.validate(request)) {
                log.error("通知验签失败");
                return errorNotifyResponse(response, "通知验签失败");
            }
            log.info("通知验签成功");

            //处理退款单
            wxPayService.processRefund(bodyMap);

            return successNotifyResponse(response);
        } catch (Exception e) {
            log.error("处理微信退款通知失败", e);
            return errorNotifyResponse(response, "失败");
        }
    }

    @ApiOperation("获取账单url：测试用")
    @GetMapping("/querybill/{billDate}/{type}")
    public R<Map<String, Object>> queryTradeBill(
            @PathVariable @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "账单日期格式必须为yyyy-MM-dd") String billDate,
            @PathVariable @Pattern(regexp = "tradebill|fundflowbill", message = "微信账单类型只支持tradebill或fundflowbill") String type) {
        log.info("获取账单url");
        String downloadUrl = wxPayService.queryBill(billDate, type);
        return R.ok().setMessage("获取账单url成功").data("downloadUrl", downloadUrl);
    }

    @ApiOperation("下载账单")
    @GetMapping("/downloadbill/{billDate}/{type}")
    public R<Map<String, Object>> downloadBill(
            @PathVariable @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "账单日期格式必须为yyyy-MM-dd") String billDate,
            @PathVariable @Pattern(regexp = "tradebill|fundflowbill", message = "微信账单类型只支持tradebill或fundflowbill") String type) {
        log.info("下载账单");
        String result = wxPayService.downloadBill(billDate, type);

        return R.ok().data("result", result);
    }

    @ApiOperation("调用统一下单API，生成预支付交易会话标识")
    @PostMapping("/jsapi")
    public R<Map<String, Object>> jsapiPay(OrderInfo orderInfo,
                                           @NotBlank(message = "openid不能为空") @Size(max = 128, message = "openid长度不能超过128个字符") String openid) {
        log.info("发起支付请求");
        Map<String, Object> map = wxPayService.jsapiPay(orderInfo, openid);
        return R.ok().setData(map);
    }

    @ApiOperation("微信支付回调")
    @PostMapping("/jsapi/notify/v1")
    public String jsapiNotifyV1(HttpServletRequest request, HttpServletResponse response) {

        try {
            String body = HttpUtils.readData(request);
            Map<String, Object> bodyMap = JsonUtils.toObjectMap(body);
            String requestId = (String) bodyMap.get("id");
            log.info("支付通知的id ===> {}", requestId);
            // 构建request，传入必要参数
            WechatPay2ValidatorForRequest wechatPay2ValidatorForRequest
                    = new WechatPay2ValidatorForRequest(verifier, requestId, body);
            if (!wechatPay2ValidatorForRequest.validate(request)) {
                log.error("通知验签失败");
                return errorNotifyResponse(response, "通知验签失败");
            }
            log.info("通知验签成功");
            wxPayService.processOrder(bodyMap);

            return successNotifyResponse(response);
        } catch (Exception e) {
            log.error("处理微信 JSAPI 支付通知失败", e);
            return errorNotifyResponse(response, "失败");
        }
    }

    private String successNotifyResponse(HttpServletResponse response) {
        return notifyResponse(response, 200, "SUCCESS", "成功");
    }

    private String errorNotifyResponse(HttpServletResponse response, String message) {
        return notifyResponse(response, 500, "ERROR", message);
    }

    private String notifyResponse(HttpServletResponse response, int status, String code, String message) {
        response.setStatus(status);
        Map<String, String> map = new HashMap<>();
        map.put("code", code);
        map.put("message", message);
        return JsonUtils.toJson(map);
    }

}
