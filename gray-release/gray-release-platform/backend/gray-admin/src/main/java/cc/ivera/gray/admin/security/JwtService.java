package cc.ivera.gray.admin.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    private final ObjectMapper objectMapper;
    private final String secret;
    private final long expireSeconds;

    public JwtService(ObjectMapper objectMapper,
                      @Value("${gray.security.jwt-secret}") String secret,
                      @Value("${gray.security.expire-seconds:86400}") long expireSeconds) {
        this.objectMapper = objectMapper;
        this.secret = secret;
        this.expireSeconds = expireSeconds;
    }

    public String issue(String username, List<String> roles) {
        long expiresAt = Instant.now().plusSeconds(expireSeconds).getEpochSecond();
        Map<String, Object> payload = Map.of(
                "sub", username,
                "roles", roles,
                "exp", expiresAt
        );
        try {
            String body = URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(payload));
            return body + "." + sign(body);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("生成 token 失败", ex);
        }
    }

    public TokenPrincipal verify(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 2 || !constantTimeEquals(sign(parts[0]), parts[1])) {
            throw new IllegalArgumentException("token 签名无效");
        }
        try {
            Map<String, Object> payload = objectMapper.readValue(URL_DECODER.decode(parts[0]), new TypeReference<>() {
            });
            long exp = ((Number) payload.get("exp")).longValue();
            if (Instant.now().getEpochSecond() >= exp) {
                throw new IllegalArgumentException("token 已过期");
            }
            String username = String.valueOf(payload.get("sub"));
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) payload.get("roles");
            return new TokenPrincipal(username, roles);
        } catch (Exception ex) {
            throw new IllegalArgumentException("token 解析失败", ex);
        }
    }

    private String sign(String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return URL_ENCODER.encodeToString(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("token 签名失败", ex);
        }
    }

    private boolean constantTimeEquals(String left, String right) {
        if (left.length() != right.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < left.length(); i++) {
            result |= left.charAt(i) ^ right.charAt(i);
        }
        return result == 0;
    }
}

