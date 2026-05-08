package cc.ivera.controller.support;

import cc.ivera.util.HttpUtils;
import cc.ivera.util.JsonUtils;
import cc.ivera.util.WechatPay2ValidatorForRequest;
import com.wechat.pay.contrib.apache.httpclient.auth.Verifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Component
@Slf4j
public class WxPayNotifyHandler {

    private final Verifier verifier;

    public WxPayNotifyHandler(Verifier verifier) {
        this.verifier = verifier;
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

            WechatPay2ValidatorForRequest validator = new WechatPay2ValidatorForRequest(verifier, requestId, body);
            if (!validator.validate(request)) {
                log.error("通知验签失败");
                return errorNotifyResponse(response, "通知验签失败");
            }
            log.info("通知验签成功");

            processor.accept(bodyMap);
            return successNotifyResponse(response);
        } catch (Exception e) {
            log.error(errorMessage, e);
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
