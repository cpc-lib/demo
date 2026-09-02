package cc.ivera.controller;

import cc.ivera.config.WxPayConfig;
import cc.ivera.config.PaymentConfigLoader;
import cc.ivera.entity.OrderInfo;
import cc.ivera.enums.OrderStatus;
import cc.ivera.enums.PayType;
import cc.ivera.exception.BizException;
import cc.ivera.exception.ConflictException;
import cc.ivera.lock.DistributedLockTemplate;
import cc.ivera.security.AuthContext;
import cc.ivera.service.OrderInfoService;
import cc.ivera.service.PaymentInfoService;
import cc.ivera.service.InventoryService;
import cc.ivera.service.wxpay.WxPayOrderFacade;
import cc.ivera.util.HttpUtils;
import cc.ivera.vo.R;
import com.github.wxpay.sdk.WXPayUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@CrossOrigin //跨域
@RestController
@RequestMapping("/api/wx-pay-v2")
@Api(tags = "网站微信支付APIv2")
@Slf4j
@Validated
public class WxPayV2Controller {

    private static final String NOTIFY_IDEMPOTENT_KEY_PREFIX = "payment:wx:v2:notify:processed:";

    private static final long NOTIFY_IDEMPOTENT_EXPIRE_HOURS = 24L;

    private final WxPayOrderFacade wxPayOrderFacade;

    private final WxPayConfig wxPayConfig;

    private final OrderInfoService orderInfoService;

    private final PaymentInfoService paymentInfoService;

    private final InventoryService inventoryService;

    private final DistributedLockTemplate distributedLockTemplate;

    private final TransactionTemplate transactionTemplate;

    private final StringRedisTemplate stringRedisTemplate;

    public WxPayV2Controller(
        WxPayOrderFacade wxPayOrderFacade,
        WxPayConfig wxPayConfig,
        OrderInfoService orderInfoService,
        PaymentInfoService paymentInfoService,
        InventoryService inventoryService,
        DistributedLockTemplate distributedLockTemplate,
        TransactionTemplate transactionTemplate,
        StringRedisTemplate stringRedisTemplate
    ) {
        this.wxPayOrderFacade = wxPayOrderFacade;
        this.wxPayConfig = wxPayConfig;
        this.orderInfoService = orderInfoService;
        this.paymentInfoService = paymentInfoService;
        this.inventoryService = inventoryService;
        this.distributedLockTemplate = distributedLockTemplate;
        this.transactionTemplate = transactionTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * Native下单
     *
     * @param productId
     * @return
     */
    @ApiOperation("调用统一下单API，生成支付二维码")
    @PostMapping("/native/{productId}")
    public R<Map<String, Object>> createNative(@PathVariable @Positive(message = "商品ID必须大于0") Long productId,
                                               @RequestParam(required = false) Long paymentAppId,
                                               @RequestHeader("Idempotency-Key")
                                               @NotBlank(message = "订单幂等键不能为空")
                                               @Size(max = 64, message = "订单幂等键不能超过64个字符") String idempotencyKey,
                                               HttpServletRequest request) {
        AuthContext.requireShoppingUser();
        log.info("发起支付请求 v2，productId={}, paymentAppId={}", productId, paymentAppId);

        String remoteAddr = request.getRemoteAddr();
        //修改code_url可以重新获取到新的地址信息
        Map<String, Object> map = wxPayOrderFacade.nativePayV2(
                productId,
                remoteAddr,
                paymentAppId,
                idempotencyKey
        );
        return R.ok().setData(map);
    }

    @ApiOperation("为已有订单生成APIv2支付二维码")
    @PostMapping("/native/order/{orderNo}")
    public R<Map<String, Object>> createNativeOrder(
            @PathVariable String orderNo,
            HttpServletRequest request
    ) {
        orderInfoService.getOrderForUser(orderNo, AuthContext.requireShoppingUser());
        Map<String, Object> map = wxPayOrderFacade.nativePayV2Order(orderNo, request.getRemoteAddr());
        return R.ok(map);
    }

    /**
     * 支付通知
     * 微信支付通过支付通知接口将用户支付成功消息通知给商户
     */
    @PostMapping("/native/notify")
    public String wxNotify(HttpServletRequest request) {
        log.info("微信支付v2发送的回调");

        try {
            //处理通知参数
            String body = HttpUtils.readData(request);

            //解析xml数据
            Map<String, String> notifyMap = WXPayUtil.xmlToMap(body);

            //获取微信支付流水号作为通知ID
            String transactionId = notifyMap.get("transaction_id");
            log.info("微信支付v2通知transactionId ===> {}", transactionId);

            //幂等检查
            if (!tryAcquireNotifyLock(transactionId)) {
                log.info("微信支付v2通知已处理或正在处理中，直接返回成功，transactionId={}", transactionId);
                return wxNotifySuccess();
            }

            try {
                //验签
                if (!WXPayUtil.isSignatureValid(body,
                        wxPayConfig.getPartnerKey())) {
                    log.error("通知验签失败");
                    releaseNotifyLock(transactionId);
                    return wxNotifyFail("验签失败");
                }

                //判断通信和业务是否成功
                if (!"SUCCESS".equals(notifyMap.get("return_code")) || !"SUCCESS".equals(notifyMap.get("result_code"))) {
                    log.error("微信支付v2通知结果失败");
                    releaseNotifyLock(transactionId);
                    return wxNotifyFail("失败");
                }

                //获取商户订单号
                String orderNo = notifyMap.get("out_trade_no");
                if (orderNo == null || orderNo.trim().isEmpty()) {
                    log.error("微信支付v2通知缺少商户订单号");
                    releaseNotifyLock(transactionId);
                    return wxNotifyFail("订单号不能为空");
                }

                // 金额格式先做基础校验，和本地订单金额的一致性校验放到加锁事务内完成。
                Long notifyTotalFee = parseTotalFee(notifyMap.get("total_fee"));
                if (notifyTotalFee == null) {
                    log.error("微信支付v2通知金额格式错误");
                    releaseNotifyLock(transactionId);
                    return wxNotifyFail("金额校验失败");
                }

                String lockKey = "payment:order:transition:" + orderNo;

                // 使用 Redisson 分布式锁（leaseTime=-1 开启看门狗自动续期）
                distributedLockTemplate.execute(lockKey, 5000L, -1L, () ->
                        transactionTemplate.execute(status -> {
                            doProcessWxPayV2NotifyInTransaction(orderNo, transactionId, notifyTotalFee, notifyMap, body);
                            return null;
                        })
                );

                markNotifyProcessed(transactionId);
                log.info("微信支付v2通知处理成功，已应答");
                return wxNotifySuccess();
            } catch (Exception e) {
                releaseNotifyLock(transactionId);
                throw e;
            }
        } catch (Exception e) {
            log.error("处理微信支付v2通知失败", e);
            return wxNotifyFail("失败");
        }
    }

    private boolean tryAcquireNotifyLock(String transactionId) {
        if (transactionId == null || transactionId.trim().isEmpty()) {
            return true;
        }
        String key = NOTIFY_IDEMPOTENT_KEY_PREFIX + transactionId;
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(key, "processing", NOTIFY_IDEMPOTENT_EXPIRE_HOURS, TimeUnit.HOURS);
        return Boolean.TRUE.equals(success);
    }

    private void releaseNotifyLock(String transactionId) {
        if (transactionId == null || transactionId.trim().isEmpty()) {
            return;
        }
        String key = NOTIFY_IDEMPOTENT_KEY_PREFIX + transactionId;
        String currentValue = stringRedisTemplate.opsForValue().get(key);
        if ("processing".equals(currentValue)) {
            stringRedisTemplate.delete(key);
        }
    }

    private void markNotifyProcessed(String transactionId) {
        if (transactionId == null || transactionId.trim().isEmpty()) {
            return;
        }
        String key = NOTIFY_IDEMPOTENT_KEY_PREFIX + transactionId;
        stringRedisTemplate.opsForValue().set(key, "processed", NOTIFY_IDEMPOTENT_EXPIRE_HOURS, TimeUnit.HOURS);
    }

    private void doProcessWxPayV2NotifyInTransaction(String orderNo,
                                                     String transactionId,
                                                     Long notifyTotalFee,
                                                     Map<String, String> notifyMap,
                                                     String body) {
        log.info("微信支付v2通知加锁处理开始，orderNo={}, transactionId={}", orderNo, transactionId);

        // 支付成功通知可能重复投递，也可能和延迟关单、主动查单并发。
        // Redis 分布式锁控制多实例并发，select ... for update 控制数据库行级并发。
        OrderInfo lockedOrder = orderInfoService.getOrderByOrderNoForUpdate(orderNo);
        if (lockedOrder == null) {
            throw new BizException("微信支付v2通知订单不存在，orderNo=" + orderNo);
        }
        if (notifyTotalFee == null || !notifyTotalFee.equals(lockedOrder.getTotalFee().longValue())) {
            throw new BizException("微信支付v2通知金额与本地订单金额不一致，orderNo=" + orderNo);
        }
        if (!PayType.WXPAY.getType().equals(lockedOrder.getPaymentType())
                || (org.springframework.util.StringUtils.hasText(lockedOrder.getPaymentChannelCode())
                && !PaymentConfigLoader.CHANNEL_WXPAY.equals(lockedOrder.getPaymentChannelCode()))) {
            throw new BizException("微信支付v2通知支付渠道与本地订单不匹配，orderNo=" + orderNo);
        }
        validateWxPayV2Merchant(notifyMap, orderNo);
        if (!OrderStatus.NOTPAY.getType().equals(lockedOrder.getOrderStatus())) {
            log.info("微信支付v2通知重复或订单状态已变化，幂等忽略 ===> orderNo={}, transactionId={}, currentStatus={}",
                    orderNo, transactionId, lockedOrder.getOrderStatus());
            return;
        }

        // 只有数据库条件更新成功的请求才写支付流水。
        boolean updated = orderInfoService.updateStatusByOrderNoIfStatus(
                orderNo,
                OrderStatus.NOTPAY,
                OrderStatus.SUCCESS);
        if (updated) {
            if (!inventoryService.commitPayment(orderNo)) {
                throw new ConflictException("订单库存状态不一致，支付状态未提交");
            }
            paymentInfoService.createPaymentInfoForWxPayV2(notifyMap, body);
            log.info("微信支付v2通知处理完成，orderNo={}, transactionId={}", orderNo, transactionId);
        } else {
            log.info("微信支付v2通知状态更新失败，可能已被其他事务处理，orderNo={}, transactionId={}", orderNo, transactionId);
        }
    }

    private String wxNotifySuccess() {
        return wxNotifyResponse("SUCCESS", "OK");
    }

    private void validateWxPayV2Merchant(Map<String, String> notifyMap, String orderNo) {
        String notifyAppId = notifyMap.get("appid");
        if (org.springframework.util.StringUtils.hasText(notifyAppId)
                && org.springframework.util.StringUtils.hasText(wxPayConfig.getAppid())
                && !notifyAppId.equals(wxPayConfig.getAppid())) {
            throw new BizException("微信支付v2通知appid不匹配，orderNo=" + orderNo);
        }
        String notifyMchId = notifyMap.get("mch_id");
        if (org.springframework.util.StringUtils.hasText(notifyMchId)
                && org.springframework.util.StringUtils.hasText(wxPayConfig.getMchId())
                && !notifyMchId.equals(wxPayConfig.getMchId())) {
            throw new BizException("微信支付v2通知商户号不匹配，orderNo=" + orderNo);
        }
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
