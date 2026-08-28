package cc.ivera.ragdemo.config;


import cc.ivera.ragdemo.exception.ApiErrorCode;
import cc.ivera.ragdemo.service.ragops.AdminRolePolicy;
import cc.ivera.ragdemo.tenant.TenantContextFilter;
import cc.ivera.ragdemo.util.ApiKeyEncryptor;
import cc.ivera.ragdemo.util.TraceUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

import java.io.IOException;
import java.util.Arrays;

@Configuration
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class SecurityConfig {

    private final RagProperties properties;
    private final TenantContextFilter tenantContextFilter;
    private final AdminRolePolicy adminRolePolicy;

    @Bean
    public SecurityFilterChain ragSecurityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .addFilterBefore(tenantContextFilter, AuthorizationFilter.class)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) ->
                                writeError(response, HttpServletResponse.SC_UNAUTHORIZED, ApiErrorCode.UNAUTHORIZED, "Authentication is required"))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeError(response, HttpServletResponse.SC_FORBIDDEN, ApiErrorCode.FORBIDDEN, "Access denied")))
                .authorizeHttpRequests(auth -> {
                    for (String prefix : properties.getSecurity().getPublicPathPrefixes()) {
                        auth.requestMatchers(prefix + "/**").permitAll();
                        auth.requestMatchers(prefix).permitAll();
                    }
                    auth.requestMatchers("/api/auth/login").permitAll();
                    auth.requestMatchers("/api/admin/model-configs/invalidate-models/all")
                            .hasAnyAuthority(adminRolePolicy.authorityNames(properties.getSecurity().getAdminRoles()));
                    auth.requestMatchers(tenantScopedAdminRequestMatchers())
                            .hasAnyAuthority(tenantScopedAdminAuthorityNames());
                    auth.requestMatchers("/api/admin/**")
                            .hasAnyAuthority(adminRolePolicy.authorityNames(properties.getSecurity().getAdminRoles()));
                    auth.anyRequest().authenticated();
                });
        return http.build();
    }

    @Bean
    public FilterRegistrationBean<TenantContextFilter> tenantContextFilterRegistration(TenantContextFilter filter) {
        FilterRegistrationBean<TenantContextFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public ApiKeyEncryptor apiKeyEncryptor() {
        RagProperties.Security security = properties.getSecurity();
        String encryptionKey = security.getApiKeyEncryptionKey();
        if (encryptionKey == null || encryptionKey.length() < 16) {
            // Default key for development; production should set rag.security.api-key-encryption-key
            encryptionKey = "rag-demo-default-encryption-key";
        }
        return new ApiKeyEncryptor(encryptionKey);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService localLoginOnlyUserDetailsService() {
        return username -> {
            throw new UsernameNotFoundException("Use /api/auth/login for local password authentication");
        };
    }

    String[] tenantScopedAdminAuthorityNames() {
        return Arrays.stream(new String[][]{
                        adminRolePolicy.authorityNames(properties.getSecurity().getAdminRoles()),
                        new String[]{"TENANT_ADMIN"}
                })
                .flatMap(Arrays::stream)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toArray(String[]::new);
    }

    String[] tenantScopedAdminRequestMatchers() {
        return new String[]{
                "/api/admin/model-configs",
                "/api/admin/model-configs/**",
                "/api/admin/tenant",
                "/api/admin/tenant/**",
                "/api/admin/agent-prompts",
                "/api/admin/agent-prompts/**"
        };
    }

    private void writeError(HttpServletResponse response,
                            int status,
                            ApiErrorCode errorCode,
                            String message) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.getWriter().write("""
                {"ok":false,"traceId":"%s","error":{"code":"%s","message":"%s"}}
                """.formatted(TraceUtils.currentTraceId(), errorCode.code(), jsonEscape(message)));
    }

    private String jsonEscape(String value) {
        return value == null ? "" : value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }
}
