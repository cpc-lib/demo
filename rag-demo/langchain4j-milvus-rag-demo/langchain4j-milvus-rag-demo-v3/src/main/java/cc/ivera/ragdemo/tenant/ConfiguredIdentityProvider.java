package cc.ivera.ragdemo.tenant;


import cc.ivera.ragdemo.config.RagProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class ConfiguredIdentityProvider implements IdentityProvider {

    private static final String DEV_HMAC_SECRET = "rag-demo-local-login-development-secret";
    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USER_NAME = "X-User-Name";
    private static final String HEADER_TENANT_ID = "X-Tenant-Id";
    private static final String HEADER_ROLES = "X-Roles";
    private static final String HEADER_KB_IDS = "X-Knowledge-Base-Ids";
    private static final String HEADER_PERMISSION_TAGS = "X-Permission-Tags";
    private static final String HEADER_REQUEST_ID = "X-Request-Id";
    private static final String HEADER_IMPERSONATE_TENANT_ID = "X-Impersonate-Tenant-Id";
    private static final String HEADER_IMPERSONATION_REASON = "X-Impersonation-Reason";
    private static final String COOKIE_USER_ID = "ragUserId";
    private static final String COOKIE_USER_NAME = "ragUserName";
    private static final String COOKIE_TENANT_ID = "ragTenantId";
    private static final String COOKIE_ROLES = "ragRoles";
    private static final String COOKIE_KB_IDS = "ragKnowledgeBaseIds";
    private static final String COOKIE_PERMISSION_TAGS = "ragPermissionTags";
    private static final String COOKIE_IMPERSONATE_TENANT_ID = "ragImpersonateTenantId";
    private static final String COOKIE_IMPERSONATION_REASON = "ragImpersonationReason";

    private final RagProperties properties;
    private volatile JwtDecoder jwtDecoder;

    @Override
    public AuthenticatedIdentity authenticate(HttpServletRequest request) {
        return switch (securityMode()) {
            case "jwt" -> jwtIdentity(request);
            case "gateway" -> gatewayIdentity(request);
            default -> devIdentity(request);
        };
    }

    private AuthenticatedIdentity jwtIdentity(HttpServletRequest request) {
        String authorization = header(request, "Authorization");
        if (!StringUtils.hasText(authorization) || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            throw new IdentityAuthenticationException("Missing bearer token");
        }
        try {
            Jwt jwt = decoder().decode(authorization.substring(7).trim());
            validateJwt(jwt);
            RagProperties.Security.Jwt config = properties.getSecurity().getJwt();
            Long tenantId = longClaim(jwt.getClaim(config.getTenantIdClaim()));
            String userId = stringClaim(jwt.getClaim(config.getUserIdClaim()));
            List<String> roles = stringList(jwt.getClaim(config.getRolesClaim()), List.of());
            if (!StringUtils.hasText(userId)) {
                throw new IdentityAuthenticationException("Missing JWT user claim: " + config.getUserIdClaim());
            }
            if (tenantId == null && !platformAdmin(roles)) {
                throw new IdentityAuthenticationException("Missing JWT tenant claim: " + config.getTenantIdClaim());
            }
            return withImpersonation(request, new AuthenticatedIdentity(
                    tenantId,
                    stringClaim(jwt.getClaim(config.getTenantExternalIdClaim())),
                    userId,
                    valueOrDefault(stringClaim(jwt.getClaim(config.getUserNameClaim())), userId),
                    roles,
                    List.of(),
                    longList(jwt.getClaim(config.getKnowledgeBaseIdsClaim())),
                    stringList(jwt.getClaim(config.getPermissionTagsClaim()), List.of()),
                    valueOrDefault(stringClaim(jwt.getClaim(config.getRequestIdClaim())), requestId(request)),
                    null,
                    null,
                    "jwt"
            ));
        } catch (JwtException ex) {
            throw new IdentityAuthenticationException("Invalid bearer token", ex);
        }
    }

    private AuthenticatedIdentity gatewayIdentity(HttpServletRequest request) {
        RagProperties.Security.Gateway config = properties.getSecurity().getGateway();
        String tenantIdRaw = header(request, config.getTenantIdHeader());
        String userId = header(request, config.getUserIdHeader());
        if (!StringUtils.hasText(tenantIdRaw) || !StringUtils.hasText(userId)) {
            throw new IdentityAuthenticationException("Missing trusted gateway identity headers");
        }
        verifyGatewaySignature(request, config, tenantIdRaw, userId);
        return withImpersonation(request, new AuthenticatedIdentity(
                parseLong(tenantIdRaw, "gateway tenant id"),
                header(request, config.getTenantExternalIdHeader()),
                userId,
                valueOrDefault(header(request, config.getUserNameHeader()), userId),
                values(header(request, config.getRolesHeader()), List.of()),
                List.of(),
                longValues(header(request, config.getKnowledgeBaseIdsHeader())),
                values(header(request, config.getPermissionTagsHeader()), List.of()),
                valueOrDefault(header(request, config.getRequestIdHeader()), requestId(request)),
                null,
                null,
                "gateway"
        ));
    }

    private AuthenticatedIdentity devIdentity(HttpServletRequest request) {
        String authorization = header(request, "Authorization");
        if (StringUtils.hasText(authorization) && authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return jwtIdentity(request);
        }
        if (!properties.getTenant().isDevHeaderEnabled()) {
            throw new IdentityAuthenticationException("Development header identity is disabled");
        }
        Long tenantId = parseLongOrNull(headerOrCookie(request, HEADER_TENANT_ID, COOKIE_TENANT_ID));
        String userId = headerOrCookie(request, HEADER_USER_ID, COOKIE_USER_ID);
        if (tenantId == null && !StringUtils.hasText(userId)) {
            if (!properties.getTenant().isAllowDemoTenantFallback()) {
                throw new IdentityAuthenticationException("Missing tenant authentication headers");
            }
            tenantId = properties.getIngestion().getDefaultTenantId();
            userId = properties.getTenant().getDemoUserId();
        }
        if (tenantId == null) {
            throw new IdentityAuthenticationException("Missing X-Tenant-Id");
        }
        if (!StringUtils.hasText(userId)) {
            throw new IdentityAuthenticationException("Missing X-User-Id");
        }
        return withImpersonation(request, new AuthenticatedIdentity(
                tenantId,
                header(request, "X-Tenant-External-Id"),
                userId.trim(),
                valueOrDefault(headerOrCookie(request, HEADER_USER_NAME, COOKIE_USER_NAME), userId.trim()),
                values(headerOrCookie(request, HEADER_ROLES, COOKIE_ROLES), properties.getTenant().getDemoRoles()),
                List.of(),
                longValues(headerOrCookie(request, HEADER_KB_IDS, COOKIE_KB_IDS)),
                values(headerOrCookie(request, HEADER_PERMISSION_TAGS, COOKIE_PERMISSION_TAGS), List.of()),
                requestId(request),
                null,
                null,
                "dev"
        ));
    }

    private AuthenticatedIdentity withImpersonation(HttpServletRequest request, AuthenticatedIdentity identity) {
        Long impersonatedTenantId = impersonatedTenantId(request);
        if (impersonatedTenantId == null || impersonatedTenantId.equals(identity.tenantId())) {
            return identity;
        }
        UserContext user = new UserContext(
                identity.userId(),
                identity.userName(),
                identity.roles(),
                identity.workspaceIds(),
                identity.authorizedKnowledgeBaseIds(),
                identity.permissionTags()
        );
        if (!user.platformAdmin(properties.getSecurity().getAdminRoles())) {
            throw new IdentityAuthenticationException("Only platform administrators can impersonate another tenant");
        }
        return new AuthenticatedIdentity(
                identity.tenantId(),
                identity.tenantExternalId(),
                identity.userId(),
                identity.userName(),
                identity.roles(),
                identity.workspaceIds(),
                identity.authorizedKnowledgeBaseIds(),
                identity.permissionTags(),
                identity.requestId(),
                impersonatedTenantId,
                impersonationReason(request),
                identity.source()
        );
    }

    private JwtDecoder decoder() {
        JwtDecoder local = jwtDecoder;
        if (local != null) {
            return local;
        }
        RagProperties.Security.Jwt config = properties.getSecurity().getJwt();
        if (StringUtils.hasText(config.getJwksUri())) {
            local = NimbusJwtDecoder.withJwkSetUri(config.getJwksUri().trim()).build();
        } else if (StringUtils.hasText(config.getHmacSecret()) || "dev".equals(securityMode())) {
            String secret = StringUtils.hasText(config.getHmacSecret()) ? config.getHmacSecret() : DEV_HMAC_SECRET;
            SecretKeySpec key = new SecretKeySpec(hmacKey(secret), "HmacSHA256");
            local = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        } else {
            throw new IdentityAuthenticationException("JWT identity mode requires jwks-uri or hmac-secret");
        }
        jwtDecoder = local;
        return local;
    }

    private byte[] hmacKey(String secret) {
        byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
        if (raw.length >= 32) {
            return raw;
        }
        try {
            return MessageDigest.getInstance("SHA-256").digest(raw);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private void validateJwt(Jwt jwt) {
        RagProperties.Security.Jwt config = properties.getSecurity().getJwt();
        String issuer = stringClaim(jwt.getClaim("iss"));
        if (StringUtils.hasText(config.getIssuer())
                && !config.getIssuer().trim().equals(issuer)) {
            throw new IdentityAuthenticationException("Invalid JWT issuer");
        }
        if (StringUtils.hasText(config.getAudience())
                && (jwt.getAudience() == null || !jwt.getAudience().contains(config.getAudience().trim()))) {
            throw new IdentityAuthenticationException("Invalid JWT audience");
        }
    }

    private void verifyGatewaySignature(HttpServletRequest request,
                                        RagProperties.Security.Gateway config,
                                        String tenantId,
                                        String userId) {
        if (!StringUtils.hasText(config.getSharedSecret())) {
            throw new IdentityAuthenticationException("Gateway identity mode requires shared-secret");
        }
        String timestamp = header(request, config.getTimestampHeader());
        String signature = header(request, config.getSignatureHeader());
        if (!StringUtils.hasText(timestamp) || !StringUtils.hasText(signature)) {
            throw new IdentityAuthenticationException("Missing trusted gateway signature");
        }
        Instant signedAt = parseTimestamp(timestamp);
        long skew = Math.abs(Instant.now().getEpochSecond() - signedAt.getEpochSecond());
        if (skew > Math.max(1, config.getMaxClockSkewSeconds())) {
            throw new IdentityAuthenticationException("Trusted gateway signature timestamp is outside the allowed skew");
        }
        String payload = tenantId.trim() + "\n" + userId.trim() + "\n" + timestamp.trim();
        String expected = hmacHex(config.getSharedSecret(), payload);
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signature.trim().getBytes(StandardCharsets.UTF_8))) {
            throw new IdentityAuthenticationException("Invalid trusted gateway signature");
        }
    }

    private String hmacHex(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder(bytes.length * 2);
            for (byte item : bytes) {
                out.append(String.format("%02x", item));
            }
            return out.toString();
        } catch (Exception ex) {
            throw new IdentityAuthenticationException("Failed to verify trusted gateway signature", ex);
        }
    }

    private Instant parseTimestamp(String value) {
        try {
            long number = Long.parseLong(value.trim());
            return number > 1_000_000_000_000L ? Instant.ofEpochMilli(number) : Instant.ofEpochSecond(number);
        } catch (NumberFormatException e) {
            log.debug("Timestamp is not a numeric format, trying ISO-8601 parsing: {}", value);
            try {
                return Instant.parse(value.trim());
            } catch (DateTimeParseException ex) {
                throw new IdentityAuthenticationException("Invalid trusted gateway signature timestamp", ex);
            }
        }
    }

    private String securityMode() {
        String mode = properties.getSecurity().getMode();
        return StringUtils.hasText(mode) ? mode.trim().toLowerCase() : "dev";
    }

    private String headerOrCookie(HttpServletRequest request, String headerName, String cookieName) {
        String headerValue = header(request, headerName);
        return StringUtils.hasText(headerValue) ? headerValue : cookie(request, cookieName);
    }

    private String header(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String cookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                return cookie.getValue().trim();
            }
        }
        return null;
    }

    private String valueOrDefault(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String requestId(HttpServletRequest request) {
        String value = header(request, HEADER_REQUEST_ID);
        return StringUtils.hasText(value) ? value : "req-" + UUID.randomUUID().toString().replace("-", "");
    }

    private Long impersonatedTenantId(HttpServletRequest request) {
        String raw = "dev".equals(securityMode())
                ? headerOrCookie(request, HEADER_IMPERSONATE_TENANT_ID, COOKIE_IMPERSONATE_TENANT_ID)
                : header(request, HEADER_IMPERSONATE_TENANT_ID);
        return parseLongOrNull(raw);
    }

    private String impersonationReason(HttpServletRequest request) {
        return "dev".equals(securityMode())
                ? headerOrCookie(request, HEADER_IMPERSONATION_REASON, COOKIE_IMPERSONATION_REASON)
                : header(request, HEADER_IMPERSONATION_REASON);
    }

    private Long parseLongOrNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return parseLong(value, "numeric identity value");
    }

    private Long parseLong(String value, String label) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            throw new IdentityAuthenticationException("Invalid " + label + ": " + value);
        }
    }

    private String stringClaim(Object value) {
        return value == null || !StringUtils.hasText(String.valueOf(value)) ? null : String.valueOf(value).trim();
    }

    private Long longClaim(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return parseLongOrNull(stringClaim(value));
    }

    private List<String> stringList(Object raw, List<String> fallback) {
        if (raw == null) {
            return fallback == null ? List.of() : fallback;
        }
        if (raw instanceof String string) {
            return values(string, fallback);
        }
        if (raw instanceof Collection<?> collection) {
            return collection.stream()
                    .map(this::stringClaim)
                    .filter(StringUtils::hasText)
                    .toList();
        }
        return fallback == null ? List.of() : fallback;
    }

    private List<Long> longList(Object raw) {
        if (raw == null) {
            return List.of();
        }
        if (raw instanceof String string) {
            return longValues(string);
        }
        if (raw instanceof Collection<?> collection) {
            return collection.stream()
                    .map(item -> item instanceof Number number ? number.longValue() : parseLongOrNull(stringClaim(item)))
                    .filter(value -> value != null)
                    .toList();
        }
        Long value = longClaim(raw);
        return value == null ? List.of() : List.of(value);
    }

    private List<Long> longValues(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .map(item -> parseLong(item, "numeric list item"))
                .toList();
    }

    private List<String> values(String raw, List<String> fallback) {
        if (!StringUtils.hasText(raw)) {
            return fallback == null ? List.of() : fallback;
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }

    private boolean platformAdmin(List<String> roles) {
        java.util.Set<String> adminRoles = properties.getSecurity().getAdminRoles() == null
                ? java.util.Set.of()
                : properties.getSecurity().getAdminRoles().stream()
                .filter(StringUtils::hasText)
                .map(role -> role.trim().toUpperCase(java.util.Locale.ROOT))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        if (adminRoles.isEmpty()) {
            adminRoles = java.util.Set.of("SUPER_ADMIN");
        }
        return roles != null && roles.stream()
                .filter(StringUtils::hasText)
                .map(role -> role.trim().toUpperCase(java.util.Locale.ROOT))
                .anyMatch(adminRoles::contains);
    }
}
