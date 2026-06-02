package cc.ivera.controller.support;

import cc.ivera.util.HttpUtils;
import cc.ivera.util.JsonUtils;
import cc.ivera.util.WechatPay2ValidatorForRequest;
import com.wechat.pay.contrib.apache.httpclient.auth.Verifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Component
@Slf4j
public class WxPayNotifyHandler {

    private static final String NOTIFY_IDEMPOTENT_KEY_PREFIX = "payment:wx:notify:processed:";

    private static final long NOTIFY_IDEMPOTENT_EXPIRE_HOURS = 24L;

    private final Verifier verifier;

    private final StringRedisTemplate stringRedisTemplate;

    public WxPayNotifyHandler(Verifier verifier, StringRedisTemplate stringRedisTemplate) {
        this.verifier = verifier;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public String handle(HttpServletRequest request,
                         HttpServletResponse response,
                         Consumer<Map<String, Object>> processor,
                         String errorMessage) {
        try {
            String body = HttpUtils.readData(request);
            Map<String, Object> bodyMap = JsonUtils.toObjectMap(body);
            if (bodyMap == null) {
                throw new IllegalArgumentException("微信通知报文不能为空");
            }

            String requestId = (String) bodyMap.get("id");
            log.info("微信支付通知id ===> {}", requestId);

            if (!tryAcquireNotifyLock(requestId)) {
                log.info("微信通知已处理或正在处理中，直接返回成功，requestId={}", requestId);
                return successNotifyResponse(response);
            }

            try {
                WechatPay2ValidatorForRequest validator = new WechatPay2ValidatorForRequest(verifier, requestId, body);
                if (!validator.validate(request)) {
                    log.error("通知验签失败");
                    releaseNotifyLock(requestId);
                    return errorNotifyResponse(response, "通知验签失败");
                }
                log.info("通知验签成功");

                processor.accept(bodyMap);
                markNotifyProcessed(requestId);
                return successNotifyResponse(response);
            } catch (Exception e) {
                releaseNotifyLock(requestId);
                throw e;
            }
        } catch (Exception e) {
            log.error(errorMessage, e);
            return errorNotifyResponse(response, "失败");
        }
    }

    private boolean tryAcquireNotifyLock(String requestId) {
        if (requestId == null || requestId.trim().isEmpty()) {
            return true;
        }
        String key = NOTIFY_IDEMPOTENT_KEY_PREFIX + requestId;
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(key, "processing", NOTIFY_IDEMPOTENT_EXPIRE_HOURS, TimeUnit.HOURS);
        return Boolean.TRUE.equals(success);
    }

    private void releaseNotifyLock(String requestId) {
        if (requestId == null || requestId.trim().isEmpty()) {
            return;
        }
        String key = NOTIFY_IDEMPOTENT_KEY_PREFIX + requestId;
        String currentValue = stringRedisTemplate.opsForValue().get(key);
        if ("processing".equals(currentValue)) {
            stringRedisTemplate.delete(key);
        }
    }

    private void markNotifyProcessed(String requestId) {
        if (requestId == null || requestId.trim().isEmpty()) {
            return;
        }
        String key = NOTIFY_IDEMPOTENT_KEY_PREFIX + requestId;
        stringRedisTemplate.opsForValue().set(key, "processed", NOTIFY_IDEMPOTENT_EXPIRE_HOURS, TimeUnit.HOURS);
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
