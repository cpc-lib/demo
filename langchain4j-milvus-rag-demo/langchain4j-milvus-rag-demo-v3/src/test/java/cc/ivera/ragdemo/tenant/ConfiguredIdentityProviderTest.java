package cc.ivera.ragdemo.tenant;

import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.security.LocalJwtService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfiguredIdentityProviderTest {

    @Test
    void devModeAcceptsLocalBearerTokenWithoutDemoHeaderFallback() {
        RagProperties properties = devPropertiesWithoutDemoFallback();
        LocalJwtService.TokenIssue token = new LocalJwtService(properties).issue(new AuthenticatedIdentity(
                7L,
                "tenant-a",
                "alice",
                "Alice",
                List.of("TENANT_ADMIN"),
                List.of(),
                List.of(),
                List.of(),
                "login-test",
                null,
                null,
                "local"
        ));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token.accessToken());

        AuthenticatedIdentity identity = new ConfiguredIdentityProvider(properties).authenticate(request);

        assertThat(identity.tenantId()).isEqualTo(7L);
        assertThat(identity.tenantExternalId()).isEqualTo("tenant-a");
        assertThat(identity.userId()).isEqualTo("alice");
        assertThat(identity.roles()).containsExactly("TENANT_ADMIN");
        assertThat(identity.source()).isEqualTo("jwt");
    }

    @Test
    void devModeAcceptsPlatformAdminBearerTokenWithoutTenantClaim() {
        RagProperties properties = devPropertiesWithoutDemoFallback();
        LocalJwtService.TokenIssue token = new LocalJwtService(properties).issue(new AuthenticatedIdentity(
                null,
                "platform",
                "admin",
                "Super Administrator",
                List.of("SUPER_ADMIN"),
                List.of(),
                List.of(),
                List.of(),
                "login-platform",
                null,
                null,
                "local"
        ));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token.accessToken());

        AuthenticatedIdentity identity = new ConfiguredIdentityProvider(properties).authenticate(request);

        assertThat(identity.tenantId()).isNull();
        assertThat(identity.tenantExternalId()).isEqualTo("platform");
        assertThat(identity.userId()).isEqualTo("admin");
        assertThat(identity.roles()).containsExactly("SUPER_ADMIN");
    }

    @Test
    void devModeRejectsTenantUserBearerTokenWithoutTenantClaim() {
        RagProperties properties = devPropertiesWithoutDemoFallback();
        LocalJwtService.TokenIssue token = new LocalJwtService(properties).issue(new AuthenticatedIdentity(
                null,
                null,
                "alice",
                "Alice",
                List.of("TENANT_ADMIN"),
                List.of(),
                List.of(),
                List.of(),
                "login-bad",
                null,
                null,
                "local"
        ));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token.accessToken());

        assertThatThrownBy(() -> new ConfiguredIdentityProvider(properties).authenticate(request))
                .isInstanceOf(IdentityAuthenticationException.class)
                .hasMessageContaining("Missing JWT tenant claim");
    }

    @Test
    void devModeRejectsMissingIdentityWhenDemoHeaderFallbackIsDisabled() {
        RagProperties properties = devPropertiesWithoutDemoFallback();

        assertThatThrownBy(() -> new ConfiguredIdentityProvider(properties).authenticate(new MockHttpServletRequest()))
                .isInstanceOf(IdentityAuthenticationException.class)
                .hasMessageContaining("Missing tenant authentication headers");
    }

    private RagProperties devPropertiesWithoutDemoFallback() {
        RagProperties properties = new RagProperties();
        properties.getSecurity().setMode("dev");
        properties.getTenant().setDevHeaderEnabled(true);
        properties.getTenant().setAllowDemoTenantFallback(false);
        return properties;
    }
}
