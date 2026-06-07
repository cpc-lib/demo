package cc.ivera.service.impl.wxpay;

import cc.ivera.config.WxPayConfig;
import cc.ivera.util.JsonUtils;
import com.wechat.pay.contrib.apache.httpclient.util.AesUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Map;

@Component
@Slf4j
public class WxPayNotificationDecoder {

    @Resource
    private WxPayConfig wxPayConfig;

    public String decryptResource(Map<String, Object> bodyMap) throws GeneralSecurityException {
        log.info("密文解密");

        Map<String, Object> resourceMap = JsonUtils.toObjectMap(bodyMap.get("resource"));
        if (resourceMap == null) {
            throw new IllegalArgumentException("微信通知resource不能为空");
        }

        String ciphertext = getRequiredString(resourceMap, "ciphertext");
        String nonce = getRequiredString(resourceMap, "nonce");
        String associatedData = getRequiredString(resourceMap, "associated_data");

        log.info("密文 ===> {}", ciphertext);

        AesUtil aesUtil = new AesUtil(wxPayConfig.getApiV3Key().getBytes(StandardCharsets.UTF_8));
        String plainText = aesUtil.decryptToString(associatedData.getBytes(StandardCharsets.UTF_8),
                nonce.getBytes(StandardCharsets.UTF_8),
                ciphertext);

        log.info("明文 ===> {}", plainText);
        return plainText;
    }

    private String getRequiredString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (!(value instanceof String) || ((String) value).trim().isEmpty()) {
            throw new IllegalArgumentException("微信通知resource." + key + "不能为空");
        }
        return (String) value;
    }
}
