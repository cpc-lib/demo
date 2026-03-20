package cc.ivera.controller;

import cc.ivera.config.WxPayConfig;
import cc.ivera.entity.OrderInfo;
import cc.ivera.service.WxPayService;
import cc.ivera.util.HttpUtils;
import cc.ivera.util.WechatPay2ValidatorForRequest;
import cc.ivera.vo.R;
import com.google.gson.Gson;
import com.wechat.pay.contrib.apache.httpclient.auth.Verifier;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
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
public class WxPayController {

    @Resource
    private WxPayService wxPayService;

    @Resource
    private Verifier verifier;


    @Resource
    private WxPayConfig wxPayConfig;

    /**
     * Native下单表用户对接微信呢支付t_payment_info信息表 二维码页面一个接口调用后去调用本地t_order_info的状态
     *
     * @param productId
     * @return
     * @throws Exception
     */
    @ApiOperation("调用统一下单API，生成支付二维码")
    @PostMapping("/native/{productId}")
    public R nativePay(@PathVariable Long productId) throws Exception {
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
        Gson gson = new Gson();
        Map<String, String> map = new HashMap<>();//应答对象

        try {
            //处理通知参数
            String body = HttpUtils.readData(request);
            Map<String, Object> bodyMap = gson.fromJson(body, HashMap.class);
            String requestId = (String) bodyMap.get("id");
            log.info("支付通知的id ===> {}", requestId);
            //log.info("支付通知的完整数据 ===> {}", body);
            //int a = 9 / 0;

            //签名的验证
            WechatPay2ValidatorForRequest wechatPay2ValidatorForRequest
                    = new WechatPay2ValidatorForRequest(verifier, requestId, body);
            if (!wechatPay2ValidatorForRequest.validate(request)) {
                log.error("通知验签失败");
                //失败应答
                response.setStatus(500);
                map.put("code", "ERROR");
                map.put("message", "通知验签失败");
                return gson.toJson(map);
            }
            log.info("通知验签成功");

            //处理订单
            wxPayService.processOrder(bodyMap);

            //应答超时
            //模拟接收微信端的重复通知
//            TimeUnit.SECONDS.sleep(5);

            //成功应答
            response.setStatus(200);
            map.put("code", "SUCCESS");
            map.put("message", "成功");
            return gson.toJson(map);
        } catch (Exception e) {
            e.printStackTrace();
            //失败应答
            response.setStatus(500);
            map.put("code", "ERROR");
            map.put("message", "失败");
            return gson.toJson(map);
        }

    }

    /**
     * TODO 用户取消订单
     * 做了两件时间 -> 主动调用微信支付单
     * 修改了本地之歌订单orderNo地状态为主动关闭
     *
     * @param orderNo
     * @return
     * @throws Exception
     */
    @ApiOperation("用户取消订单")
    @PostMapping("/cancel/{orderNo}")
    public R cancel(@PathVariable String orderNo) throws Exception {
        log.info("取消订单");
        wxPayService.cancelOrder(orderNo);
        return R.ok().setMessage("订单已取消");
    }

    /**
     * 查询订单 查询订单详情信息
     *
     * @param orderNo
     * @return
     * @throws Exception
     */
    @ApiOperation("查询订单：测试订单状态用")
    @GetMapping("/query/{orderNo}")
    public R queryOrder(@PathVariable String orderNo) throws Exception {
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
     * @throws Exception
     */
    @ApiOperation("申请退款")
    @PostMapping("/refunds/{orderNo}/{reason}")
    public R refunds(@PathVariable String orderNo, @PathVariable String reason) throws Exception {
        log.info("申请退款");
        wxPayService.refund(orderNo, reason);
        return R.ok();
    }

    /**
     * 查询退款
     *
     * @param refundNo
     * @return
     * @throws Exception
     */
    @ApiOperation("查询退款：测试用")
    @GetMapping("/query-refund/{refundNo}")
    public R queryRefund(@PathVariable String refundNo) throws Exception {
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

        Gson gson = new Gson();
        Map<String, String> map = new HashMap<>();//应答对象

        try {
            //处理通知参数
            String body = HttpUtils.readData(request);
            Map<String, Object> bodyMap = gson.fromJson(body, HashMap.class);
            String requestId = (String) bodyMap.get("id");
            log.info("支付通知的id ===> {}", requestId);

            //签名的验证
            WechatPay2ValidatorForRequest wechatPay2ValidatorForRequest
                    = new WechatPay2ValidatorForRequest(verifier, requestId, body);
            if (!wechatPay2ValidatorForRequest.validate(request)) {
                log.error("通知验签失败");
                //失败应答
                response.setStatus(500);
                map.put("code", "ERROR");
                map.put("message", "通知验签失败");
                return gson.toJson(map);
            }
            log.info("通知验签成功");

            //处理退款单
            wxPayService.processRefund(bodyMap);

            //成功应答
            response.setStatus(200);
            map.put("code", "SUCCESS");
            map.put("message", "成功");
            return gson.toJson(map);
        } catch (Exception e) {
            e.printStackTrace();
            //失败应答
            response.setStatus(500);
            map.put("code", "ERROR");
            map.put("message", "失败");
            return gson.toJson(map);
        }
    }

    @ApiOperation("获取账单url：测试用")
    @GetMapping("/querybill/{billDate}/{type}")
    public R queryTradeBill(
            @PathVariable String billDate,
            @PathVariable String type) throws Exception {
        log.info("获取账单url");
        String downloadUrl = wxPayService.queryBill(billDate, type);
        return R.ok().setMessage("获取账单url成功").data("downloadUrl", downloadUrl);
    }

    @ApiOperation("下载账单")
    @GetMapping("/downloadbill/{billDate}/{type}")
    public R downloadBill(
            @PathVariable String billDate,
            @PathVariable String type) throws Exception {
        log.info("下载账单");
        String result = wxPayService.downloadBill(billDate, type);

        return R.ok().data("result", result);
    }


    @ApiOperation("调用统一下单API，生成预支付交易会话标识")
    @PostMapping("/jsapi")
    public R jsapiPay(OrderInfo orderInfo, String openid) throws Exception {
        log.info("发起支付请求");
        Map<String, Object> map = wxPayService.jsapiPay(orderInfo, openid);
        return R.ok().setData(map);
    }

//    @ApiOperation("微信支付回调")
//    @PostMapping("/jsapi/notify")
//    public R jsapiNotify(HttpServletRequest req, HttpServletResponse res) {
//        try {
//            String body = readData(req);
//            // 构建request，传入必要参数
//            NotificationRequest request = new NotificationRequest.Builder().withSerialNumber(req.getHeader("Wechatpay-Serial"))
//                    .withNonce(req.getHeader("Wechatpay-Nonce"))
//                    .withTimestamp(req.getHeader("Wechatpay-Timestamp"))
//                    .withSignature(req.getHeader("Wechatpay-Signature"))
//                    .withBody(body)
//                    .build();
//            NotificationHandler handler = new NotificationHandler(verifier, wxPayConfig.getApiV3Key().getBytes(StandardCharsets.UTF_8));
//            // 验签和解析请求体
//            Notification notification = handler.parse(request);
//            // 从notification中获取解密报文
//            String decryptData = notification.getDecryptData();
//            //开始你的业务逻辑、
//            System.out.println(decryptData);
//
//            res.setStatus(200);
//            return R.ok();
//        } catch (Exception e) {
//            throw new RuntimeException("解密失败===>" + e.getMessage());
//        }
//    }


    @ApiOperation("微信支付回调")
    @PostMapping("/jsapi/notify/v1")
    public String jsapiNotifyV1(HttpServletRequest request, HttpServletResponse response) {

        Gson gson = new Gson();
        Map<String, String> map = new HashMap<>();//应答对象
        try {
            String body = readData(request);
            Map<String, Object> bodyMap = gson.fromJson(body, HashMap.class);
            String requestId = (String) bodyMap.get("id");
            log.info("支付通知的id ===> {}", requestId);
            // 构建request，传入必要参数
            WechatPay2ValidatorForRequest wechatPay2ValidatorForRequest
                    = new WechatPay2ValidatorForRequest(verifier, requestId, body);
            if (!wechatPay2ValidatorForRequest.validate(request)) {
                log.error("通知验签失败");
                //失败应答
                response.setStatus(500);
                map.put("code", "ERROR");
                map.put("message", "通知验签失败");
                return gson.toJson(map);
            }
            log.info("通知验签成功");
            wxPayService.processOrder(bodyMap);

            response.setStatus(200);
            map.put("code", "SUCCESS");
            map.put("message", "成功");
            return gson.toJson(map);
        } catch (Exception e) {
            e.printStackTrace();
            //失败应答
            response.setStatus(500);
            map.put("code", "ERROR");
            map.put("message", "失败");
            return gson.toJson(map);
        }
    }


    /**
     * 读取微信支付信息v
     *
     * @param request 信息
     * @return 结果
     */
    private static String readData(HttpServletRequest request) {
        BufferedReader br = null;
        try {
            StringBuilder result = new StringBuilder();
            br = request.getReader();
            for (String line; (line = br.readLine()) != null; ) {
                if (result.length() > 0) {
                    result.append("\n");
                }
                result.append(line);
            }
            return result.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
