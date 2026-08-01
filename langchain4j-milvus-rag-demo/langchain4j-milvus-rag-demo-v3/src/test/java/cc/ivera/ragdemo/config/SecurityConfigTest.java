package cc.ivera.ragdemo.config;

import cc.ivera.ragdemo.service.ragops.AdminRolePolicy;
import cc.ivera.ragdemo.tenant.TenantContextFilter;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class SecurityConfigTest {

    @Test
    void tenantScopedAdminAuthoritiesIncludeTenantAndPlatformAdmins() {
        RagProperties properties = new RagProperties();
        properties.getSecurity().setAdminRoles(java.util.List.of("PLATFORM_ADMIN", "SUPER_ADMIN"));
        SecurityConfig securityConfig = new SecurityConfig(properties, mock(TenantContextFilter.class), new AdminRolePolicy());

        assertThat(securityConfig.tenantScopedAdminAuthorityNames())
                .containsExactlyInAnyOrder("PLATFORM_ADMIN", "SUPER_ADMIN", "TENANT_ADMIN");
    }

    @Test
    void tenantScopedAdminPathsIncludePromptManagement() {
        SecurityConfig securityConfig = new SecurityConfig(new RagProperties(), mock(TenantContextFilter.class), new AdminRolePolicy());

        assertThat(securityConfig.tenantScopedAdminRequestMatchers())
                .contains("/api/admin/agent-prompts", "/api/admin/agent-prompts/**");
    }

    @Test
    void customUserDetailsServicePreventsSpringBootGeneratedPassword() {
        SecurityConfig securityConfig = new SecurityConfig(new RagProperties(), mock(TenantContextFilter.class), new AdminRolePolicy());

        assertThatThrownBy(() -> securityConfig.localLoginOnlyUserDetailsService().loadUserByUsername("admin"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("/api/auth/login");
    }
}
