package cc.ivera.ragdemo.service.ragops;

import cc.ivera.ragdemo.config.RagProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

@Component
public class ProductionConfigurationPolicy {

    private static final String DEV_MODE = "dev";
    private static final String JWT_MODE = "jwt";
    private static final String GATEWAY_MODE = "gateway";
    private static final String DEFAULT_API_KEY_ENCRYPTION_KEY = "rag-demo-default-encryption-key";

    public List<String> violations(RagProperties properties) {
        return violations(properties, false);
    }

    public List<String> violations(RagProperties properties, boolean productionProfileActive) {
        if (properties == null || DEV_MODE.equals(securityMode(properties))) {
            if (productionProfileActive) {
                return List.of("rag.security.mode must be jwt or gateway when the prod Spring profile is active");
            }
            return List.of();
        }

        String mode = securityMode(properties);
        List<String> violations = new ArrayList<>();
        RagProperties.Security security = properties.getSecurity();
        RagProperties.Tenant tenant = properties.getTenant();

        if (!JWT_MODE.equals(mode) && !GATEWAY_MODE.equals(mode)) {
            violations.add("rag.security.mode must be one of dev, jwt, gateway");
        }
        if (JWT_MODE.equals(mode) && !hasJwtVerifier(security.getJwt())) {
            violations.add("rag.security.jwt.jwks-uri or rag.security.jwt.hmac-secret must be configured when rag.security.mode=jwt");
        }
        if (GATEWAY_MODE.equals(mode) && !hasText(security.getGateway().getSharedSecret())) {
            violations.add("rag.security.gateway.shared-secret must be configured when rag.security.mode=gateway");
        }
        if (tenant.isDevHeaderEnabled()) {
            violations.add("rag.tenant.dev-header-enabled must be false when rag.security.mode=" + mode);
        }
        if (tenant.isAllowDemoTenantFallback()) {
            violations.add("rag.tenant.allow-demo-tenant-fallback must be false when rag.security.mode=" + mode);
        }
        if (!security.isApiKeyEncryptionEnabled()) {
            violations.add("rag.security.api-key-encryption-enabled must be true when rag.security.mode=" + mode);
        } else if (!hasProductionApiKeyEncryptionKey(security.getApiKeyEncryptionKey())) {
            violations.add("rag.security.api-key-encryption-key must be configured with at least 16 characters when rag.security.mode=" + mode);
        }
        if (exposesActuator(security.getPublicPathPrefixes())) {
            violations.add("rag.security.public-path-prefixes must not expose /actuator when rag.security.mode=" + mode);
        }
        return List.copyOf(violations);
    }

    private boolean hasJwtVerifier(RagProperties.Security.Jwt jwt) {
        return jwt != null && (hasText(jwt.getJwksUri()) || hasText(jwt.getHmacSecret()));
    }

    private boolean hasProductionApiKeyEncryptionKey(String apiKeyEncryptionKey) {
        return hasText(apiKeyEncryptionKey)
                && apiKeyEncryptionKey.length() >= 16
                && !DEFAULT_API_KEY_ENCRYPTION_KEY.equals(apiKeyEncryptionKey);
    }

    private boolean exposesActuator(Collection<String> publicPathPrefixes) {
        if (publicPathPrefixes == null) {
            return false;
        }
        return publicPathPrefixes.stream()
                .filter(this::hasText)
                .map(String::trim)
                .map(this::normalizePath)
                .anyMatch(prefix -> "/actuator".equals(prefix) || prefix.startsWith("/actuator/"));
    }

    private String normalizePath(String path) {
        String normalized = path.startsWith("/") ? path : "/" + path;
        while (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String securityMode(RagProperties properties) {
        RagProperties.Security security = properties.getSecurity();
        String mode = security == null ? null : security.getMode();
        return hasText(mode) ? mode.trim().toLowerCase(Locale.ROOT) : DEV_MODE;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
