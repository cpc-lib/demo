package cc.ivera.ragdemo.service.ragops;

import cc.ivera.ragdemo.config.RagProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductionConfigurationPolicyTest {

    private final ProductionConfigurationPolicy policy = new ProductionConfigurationPolicy();

    @Test
    void devModeAllowsLocalDemoDefaults() {
        RagProperties properties = new RagProperties();
        properties.getSecurity().setMode("dev");
        properties.getTenant().setDevHeaderEnabled(true);
        properties.getTenant().setAllowDemoTenantFallback(true);

        List<String> violations = policy.violations(properties);

        assertThat(violations).isEmpty();
    }

    @Test
    void prodProfileRejectsDevSecurityMode() {
        RagProperties properties = new RagProperties();
        properties.getSecurity().setMode("dev");

        List<String> violations = policy.violations(properties, true);

        assertThat(violations).containsExactly(
                "rag.security.mode must be jwt or gateway when the prod Spring profile is active"
        );
    }

    @Test
    void jwtModeRequiresVerifierAndRejectsDevelopmentIdentitySettings() {
        RagProperties properties = new RagProperties();
        properties.getSecurity().setMode("jwt");
        properties.getTenant().setDevHeaderEnabled(true);
        properties.getTenant().setAllowDemoTenantFallback(true);

        List<String> violations = policy.violations(properties);

        assertThat(violations).containsExactly(
                "rag.security.jwt.jwks-uri or rag.security.jwt.hmac-secret must be configured when rag.security.mode=jwt",
                "rag.tenant.dev-header-enabled must be false when rag.security.mode=jwt",
                "rag.tenant.allow-demo-tenant-fallback must be false when rag.security.mode=jwt",
                "rag.security.api-key-encryption-key must be configured with at least 16 characters when rag.security.mode=jwt",
                "rag.security.public-path-prefixes must not expose /actuator when rag.security.mode=jwt"
        );
    }

    @Test
    void gatewayModeRequiresSharedSecret() {
        RagProperties properties = hardenedGatewayProperties();
        properties.getSecurity().getGateway().setSharedSecret(null);

        List<String> violations = policy.violations(properties);

        assertThat(violations).containsExactly(
                "rag.security.gateway.shared-secret must be configured when rag.security.mode=gateway"
        );
    }

    @Test
    void hardenedJwtModeIsAccepted() {
        RagProperties properties = hardenedJwtProperties();

        List<String> violations = policy.violations(properties);

        assertThat(violations).isEmpty();
    }

    @Test
    void hardenedGatewayModeIsAccepted() {
        RagProperties properties = hardenedGatewayProperties();

        List<String> violations = policy.violations(properties);

        assertThat(violations).isEmpty();
    }

    private RagProperties hardenedJwtProperties() {
        RagProperties properties = baseHardenedProperties("jwt");
        properties.getSecurity().getJwt().setHmacSecret("jwt-hmac-secret-at-least-sixteen-chars");
        return properties;
    }

    private RagProperties hardenedGatewayProperties() {
        RagProperties properties = baseHardenedProperties("gateway");
        properties.getSecurity().getGateway().setSharedSecret("gateway-secret-at-least-sixteen-chars");
        return properties;
    }

    private RagProperties baseHardenedProperties(String mode) {
        RagProperties properties = new RagProperties();
        properties.getSecurity().setMode(mode);
        properties.getSecurity().setApiKeyEncryptionKey("api-key-secret-at-least-sixteen-chars");
        properties.getSecurity().setPublicPathPrefixes(List.of("/error", "/swagger-ui", "/v3/api-docs", "/assets", "/favicon.ico"));
        properties.getTenant().setDevHeaderEnabled(false);
        properties.getTenant().setAllowDemoTenantFallback(false);
        return properties;
    }
}
