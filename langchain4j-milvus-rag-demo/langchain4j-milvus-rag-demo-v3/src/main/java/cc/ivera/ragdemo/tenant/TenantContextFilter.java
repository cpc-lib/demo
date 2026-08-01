package cc.ivera.ragdemo.tenant;

import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.exception.ApiErrorCode;
import cc.ivera.ragdemo.exception.MissingTenantContextException;
import cc.ivera.ragdemo.util.TraceUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TenantContextFilter extends OncePerRequestFilter {

    private static final List<String> DEFAULT_PUBLIC_PATH_PREFIXES = List.of(
            "/error",
            "/swagger-ui",
            "/v3/api-docs",
            "/actuator",
            "/api/auth/login",
            "/assets",
            "/favicon.ico"
    );

    private final IdentityProvider identityProvider;
    private final List<String> publicPathPrefixes;

    @Autowired
    public TenantContextFilter(IdentityProvider identityProvider, RagProperties properties) {
        this.identityProvider = identityProvider;
        this.publicPathPrefixes = publicPathPrefixes(properties);
    }

    public TenantContextFilter(IdentityProvider identityProvider) {
        this.identityProvider = identityProvider;
        this.publicPathPrefixes = DEFAULT_PUBLIC_PATH_PREFIXES;
    }

    public TenantContextFilter(RagProperties properties) {
        this(new ConfiguredIdentityProvider(properties), properties);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path == null || publicPathPrefixes.stream().anyMatch(prefix -> matchesPrefix(path, prefix));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            TenantContextHolder.set(resolveContext(request));
            AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(TenantContextHolder.requireUser());
            request.setAttribute(AuthenticatedUserPrincipal.class.getName(), principal);
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    authorities(TenantContextHolder.requireUser().roles())
            ));
            SecurityContextHolder.setContext(securityContext);
            filterChain.doFilter(request, response);
        } catch (MissingTenantContextException ex) {
            writeUnauthorized(response, ex.getMessage());
        } finally {
            SecurityContextHolder.clearContext();
            TenantContextHolder.clear();
        }
    }

    private List<SimpleGrantedAuthority> authorities(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return List.of();
        }
        return roles.stream()
                .flatMap(role -> Stream.of(role, "ROLE_" + role))
                .distinct()
                .map(SimpleGrantedAuthority::new)
                .toList();
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        String traceId = TraceUtils.currentTraceId();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.getWriter().write("""
                {"ok":false,"traceId":"%s","error":{"code":"%s","message":"%s"}}
                """.formatted(traceId, ApiErrorCode.UNAUTHORIZED.code(), jsonEscape(message)));
    }

    private String jsonEscape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    private TenantContext resolveContext(HttpServletRequest request) {
        AuthenticatedIdentity identity = identityProvider.authenticate(request);
        UserContext user = new UserContext(
                identity.userId(),
                identity.userName(),
                identity.roles(),
                identity.workspaceIds(),
                identity.authorizedKnowledgeBaseIds(),
                identity.permissionTags()
        );
        boolean impersonating = identity.impersonatedTenantId() != null && !identity.impersonatedTenantId().equals(identity.tenantId());
        Long effectiveTenantId = impersonating ? identity.impersonatedTenantId() : identity.tenantId();
        return new TenantContext(
                effectiveTenantId,
                identity.tenantExternalId(),
                user,
                identity.requestId(),
                sourceIp(request),
                false,
                impersonating,
                identity.tenantId(),
                identity.impersonationReason(),
                Instant.now()
        );
    }

    private String header(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        return value == null || value.isBlank() ? null : value.trim();
    }

    private List<String> publicPathPrefixes(RagProperties properties) {
        if (properties == null || properties.getSecurity().getPublicPathPrefixes() == null
                || properties.getSecurity().getPublicPathPrefixes().isEmpty()) {
            return DEFAULT_PUBLIC_PATH_PREFIXES;
        }
        return properties.getSecurity().getPublicPathPrefixes();
    }

    private boolean matchesPrefix(String path, String prefix) {
        if (!StringUtils.hasText(prefix)) {
            return false;
        }
        String cleaned = prefix.trim();
        return path.equals(cleaned) || path.startsWith(cleaned.endsWith("/") ? cleaned : cleaned + "/");
    }

    private String sourceIp(HttpServletRequest request) {
        String forwarded = header(request, "X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
