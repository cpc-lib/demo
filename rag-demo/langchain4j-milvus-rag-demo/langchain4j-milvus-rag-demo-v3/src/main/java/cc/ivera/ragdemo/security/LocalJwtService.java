package cc.ivera.ragdemo.security;

import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.tenant.AuthenticatedIdentity;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class LocalJwtService {

    private static final String DEV_HMAC_SECRET = "rag-demo-local-login-development-secret";
    private static final long DEFAULT_TTL_SECONDS = 12 * 60 * 60;

    private final RagProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TokenIssue issue(AuthenticatedIdentity identity) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(DEFAULT_TTL_SECONDS);
        Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
        Map<String, Object> claims = new LinkedHashMap<>();
        RagProperties.Security.Jwt jwt = properties.getSecurity().getJwt();
        putIfText(claims, "iss", jwt.getIssuer());
        claims.put(jwt.getUserIdClaim(), identity.userId());
        claims.put(jwt.getUserNameClaim(), identity.userName());
        if (identity.tenantId() != null) {
            claims.put(jwt.getTenantIdClaim(), identity.tenantId());
        }
        putIfText(claims, jwt.getTenantExternalIdClaim(), identity.tenantExternalId());
        claims.put(jwt.getRolesClaim(), identity.roles() == null ? List.of() : identity.roles());
        claims.put(jwt.getKnowledgeBaseIdsClaim(), stringValues(identity.authorizedKnowledgeBaseIds()));
        claims.put(jwt.getPermissionTagsClaim(), identity.permissionTags() == null ? List.of() : identity.permissionTags());
        claims.put(jwt.getRequestIdClaim(), StringUtils.hasText(identity.requestId()) ? identity.requestId() : "login-" + UUID.randomUUID().toString().replace("-", ""));
        claims.put("iat", issuedAt.getEpochSecond());
        claims.put("exp", expiresAt.getEpochSecond());

        String unsignedToken = base64Json(header) + "." + base64Json(claims);
        return new TokenIssue(unsignedToken + "." + sign(unsignedToken), issuedAt, expiresAt);
    }

    private List<String> stringValues(List<Long> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream().map(String::valueOf).toList();
    }

    private void putIfText(Map<String, Object> claims, String key, String value) {
        if (StringUtils.hasText(value)) {
            claims.put(key, value.trim());
        }
    }

    private String base64Json(Map<String, Object> value) {
        try {
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize JWT value", ex);
        }
    }

    private String sign(String unsignedToken) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hmacKey(), "HmacSHA256"));
            byte[] signature = mac.doFinal(unsignedToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to sign local JWT", ex);
        }
    }

    private byte[] hmacKey() {
        String secret = properties.getSecurity().getJwt().getHmacSecret();
        String effectiveSecret = StringUtils.hasText(secret) ? secret.trim() : DEV_HMAC_SECRET;
        byte[] raw = effectiveSecret.getBytes(StandardCharsets.UTF_8);
        if (raw.length >= 32) {
            return raw;
        }
        try {
            return MessageDigest.getInstance("SHA-256").digest(raw);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    public record TokenIssue(String accessToken, Instant issuedAt, Instant expiresAt) {
    }
}
