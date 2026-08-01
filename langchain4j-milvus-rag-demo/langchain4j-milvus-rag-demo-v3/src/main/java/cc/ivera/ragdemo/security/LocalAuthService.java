package cc.ivera.ragdemo.security;

import cc.ivera.ragdemo.audit.TenantAuditService;
import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.domain.tenant.SysPlatformAdmin;
import cc.ivera.ragdemo.domain.tenant.SysRole;
import cc.ivera.ragdemo.domain.tenant.SysTenant;
import cc.ivera.ragdemo.domain.tenant.SysUser;
import cc.ivera.ragdemo.domain.tenant.SysUserRole;
import cc.ivera.ragdemo.exception.TenantAccessDeniedException;
import cc.ivera.ragdemo.mapper.SysPlatformAdminMapper;
import cc.ivera.ragdemo.mapper.SysRoleMapper;
import cc.ivera.ragdemo.mapper.SysTenantMapper;
import cc.ivera.ragdemo.mapper.SysUserMapper;
import cc.ivera.ragdemo.mapper.SysUserRoleMapper;
import cc.ivera.ragdemo.tenant.AuthenticatedIdentity;
import cc.ivera.ragdemo.tenant.CurrentUserResponse;
import cc.ivera.ragdemo.tenant.IdentityAuthenticationException;
import cc.ivera.ragdemo.tenant.TenantContext;
import cc.ivera.ragdemo.tenant.TenantContextHolder;
import cc.ivera.ragdemo.tenant.UserContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class LocalAuthService {

    private static final String PLATFORM_TENANT_EXTERNAL_ID = "platform";
    private static final List<String> PLATFORM_ADMIN_ROLES = List.of("SUPER_ADMIN");

    private final SysTenantMapper tenantMapper;
    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysPlatformAdminMapper platformAdminMapper;
    @SuppressWarnings("unused")
    private final SysRoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final LocalJwtService jwtService;
    private final TenantAuditService auditService;
    private final RagProperties properties;

    public LoginResponse login(LoginRequest request) {
        LoginInput input = normalizeLogin(request);
        return TenantContextHolder.callWithBypass(() -> {
            if (input.systemLogin()) {
                return systemLogin(input);
            }
            return tenantLogin(input);
        });
    }

    public void changePassword(ChangePasswordRequest request) {
        TenantContext context = TenantContextHolder.require();
        String oldPassword = request == null ? null : request.currentPassword();
        String newPassword = request == null ? null : request.newPassword();
        passwordPolicy.validateNewPassword(newPassword);
        if (context.tenantId() == null) {
            changePlatformAdminPassword(context, oldPassword, newPassword);
            return;
        }
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getTenantId, context.tenantId())
                .eq(SysUser::getExternalUserId, context.operatorUserId())
                .eq(SysUser::getIsDeleted, 0)
                .last("LIMIT 1"));
        if (user == null || !active(user)) {
            throw new TenantAccessDeniedException("Current user is disabled or missing");
        }
        if (!StringUtils.hasText(user.getPasswordHash()) || !passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new IdentityAuthenticationException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordUpdatedAt(LocalDateTime.now());
        user.setMustChangePassword(0);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        auditService.record("AUTH_PASSWORD_CHANGE", "USER", user.getExternalUserId(), "SUCCESS", "{}");
    }

    public void logout() {
        TenantContext context = TenantContextHolder.current().orElse(null);
        auditService.record("AUTH_LOGOUT", "USER", context == null ? null : context.operatorUserId(), "SUCCESS", "{}");
    }

    public String hashNewPassword(String password) {
        passwordPolicy.validateNewPassword(password);
        return passwordEncoder.encode(password);
    }

    private LoginResponse tenantLogin(LoginInput input) {
        SysTenant tenant = resolveTenant(input);
        if (!active(tenant)) {
            throw new TenantAccessDeniedException("Tenant is disabled");
        }
        SysUser user = resolveUser(tenant.getId(), input.account());
        if (!active(user)) {
            throw new TenantAccessDeniedException("User is disabled");
        }
        if (!StringUtils.hasText(user.getPasswordHash())
                || !passwordEncoder.matches(input.password(), user.getPasswordHash())) {
            throw new IdentityAuthenticationException("Invalid username or password");
        }
        AuthenticatedIdentity identity = new AuthenticatedIdentity(
                user.getTenantId(),
                tenant.getExternalId(),
                user.getExternalUserId(),
                StringUtils.hasText(user.getDisplayName()) ? user.getDisplayName() : user.getExternalUserId(),
                activeTenantRoles(user.getTenantId(), user.getExternalUserId()),
                List.of(),
                List.of(),
                List.of(),
                null,
                null,
                null,
                "local"
        );
        LocalJwtService.TokenIssue token = jwtService.issue(identity);
        auditService.record("AUTH_LOGIN", "USER", user.getExternalUserId(), "SUCCESS", "{}");
        return new LoginResponse(token.accessToken(), token.issuedAt(), token.expiresAt(), currentUser(identity));
    }

    private LoginResponse systemLogin(LoginInput input) {
        SysPlatformAdmin admin = resolvePlatformAdmin(input.account());
        if (!active(admin)) {
            throw new TenantAccessDeniedException("Platform administrator is disabled");
        }
        if (!StringUtils.hasText(admin.getPasswordHash())
                || !passwordEncoder.matches(input.password(), admin.getPasswordHash())) {
            throw new IdentityAuthenticationException("Invalid username or password");
        }
        String userId = admin.getAdminUsername();
        AuthenticatedIdentity identity = new AuthenticatedIdentity(
                null,
                PLATFORM_TENANT_EXTERNAL_ID,
                userId,
                StringUtils.hasText(admin.getDisplayName()) ? admin.getDisplayName() : userId,
                PLATFORM_ADMIN_ROLES,
                List.of(),
                List.of(),
                List.of(),
                null,
                null,
                null,
                "local"
        );
        LocalJwtService.TokenIssue token = jwtService.issue(identity);
        auditService.record("AUTH_LOGIN", "PLATFORM_ADMIN", userId, "SUCCESS", "{}");
        return new LoginResponse(token.accessToken(), token.issuedAt(), token.expiresAt(), currentUser(identity));
    }

    private void changePlatformAdminPassword(TenantContext context, String oldPassword, String newPassword) {
        if (!context.platformAdmin()) {
            throw new TenantAccessDeniedException("Only platform administrators can change platform password");
        }
        SysPlatformAdmin admin = platformAdminMapper.selectOne(new LambdaQueryWrapper<SysPlatformAdmin>()
                .eq(SysPlatformAdmin::getAdminUsername, context.operatorUserId())
                .eq(SysPlatformAdmin::getIsDeleted, 0)
                .last("LIMIT 1"));
        if (admin == null || !active(admin)) {
            throw new TenantAccessDeniedException("Current platform administrator is disabled or missing");
        }
        if (!StringUtils.hasText(admin.getPasswordHash()) || !passwordEncoder.matches(oldPassword, admin.getPasswordHash())) {
            throw new IdentityAuthenticationException("Current password is incorrect");
        }
        admin.setPasswordHash(passwordEncoder.encode(newPassword));
        admin.setPasswordUpdatedAt(LocalDateTime.now());
        admin.setMustChangePassword(0);
        admin.setUpdatedAt(LocalDateTime.now());
        platformAdminMapper.updateById(admin);
        auditService.record("AUTH_PASSWORD_CHANGE", "PLATFORM_ADMIN", admin.getAdminUsername(), "SUCCESS", "{}");
    }

    private LoginInput normalizeLogin(LoginRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Login request is required");
        }
        String account = trim(request.account());
        String password = trim(request.password());
        if (!StringUtils.hasText(account)) {
            throw new IllegalArgumentException("Account is required");
        }
        if (!StringUtils.hasText(password)) {
            throw new IllegalArgumentException("Password is required");
        }
        String loginType = normalizeLoginType(request.loginType());
        boolean systemLogin = "SYSTEM".equals(loginType)
                || (request.tenantId() == null
                && !StringUtils.hasText(request.tenantCode())
                && !StringUtils.hasText(request.tenant()));
        String tenant = trim(StringUtils.hasText(request.tenant()) ? request.tenant() : request.tenantCode());
        if (!systemLogin && request.tenantId() == null && !StringUtils.hasText(tenant)) {
            throw new IllegalArgumentException("Tenant is required");
        }
        return new LoginInput(request.tenantId(), tenant, account, password, systemLogin);
    }

    private SysTenant resolveTenant(LoginInput input) {
        Long tenantIdFromText = parseLongOrNull(input.tenant());
        SysTenant tenant = tenantMapper.selectOne(new LambdaQueryWrapper<SysTenant>()
                .eq(input.tenantId() != null, SysTenant::getId, input.tenantId())
                .and(input.tenantId() == null, wrapper -> {
                    if (tenantIdFromText != null) {
                        wrapper.eq(SysTenant::getId, tenantIdFromText).or();
                    }
                    wrapper.eq(SysTenant::getTenantCode, input.tenant())
                            .or()
                            .eq(SysTenant::getTenantName, input.tenant())
                            .or()
                            .eq(SysTenant::getExternalId, input.tenant());
                })
                .eq(SysTenant::getIsDeleted, 0)
                .last("LIMIT 1"));
        if (tenant == null) {
            throw new IdentityAuthenticationException("Invalid username or password");
        }
        return tenant;
    }

    private SysUser resolveUser(Long tenantId, String account) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getTenantId, tenantId)
                .eq(SysUser::getIsDeleted, 0)
                .and(wrapper -> wrapper
                        .eq(SysUser::getExternalUserId, account)
                        .or()
                        .eq(SysUser::getUsername, account)
                        .or()
                        .eq(SysUser::getEmail, account))
                .last("LIMIT 1"));
        if (user == null) {
            throw new IdentityAuthenticationException("Invalid username or password");
        }
        return user;
    }

    private SysPlatformAdmin resolvePlatformAdmin(String account) {
        SysPlatformAdmin admin = platformAdminMapper.selectOne(new LambdaQueryWrapper<SysPlatformAdmin>()
                .eq(SysPlatformAdmin::getIsDeleted, 0)
                .and(wrapper -> wrapper
                        .eq(SysPlatformAdmin::getAdminUsername, account)
                        .or()
                        .eq(SysPlatformAdmin::getEmail, account))
                .last("LIMIT 1"));
        if (admin == null) {
            throw new IdentityAuthenticationException("Invalid username or password");
        }
        return admin;
    }

    private List<String> activeTenantRoles(Long tenantId, String userId) {
        return userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getTenantId, tenantId)
                        .eq(SysUserRole::getUserId, userId)
                        .eq(SysUserRole::getIsDeleted, 0))
                .stream()
                .map(SysUserRole::getRoleCode)
                .filter(StringUtils::hasText)
                .map(role -> role.trim().toUpperCase(Locale.ROOT))
                .filter(role -> !platformRole(role))
                .distinct()
                .toList();
    }

    private CurrentUserResponse currentUser(AuthenticatedIdentity identity) {
        UserContext user = new UserContext(
                identity.userId(),
                identity.userName(),
                identity.roles(),
                identity.workspaceIds(),
                identity.authorizedKnowledgeBaseIds(),
                identity.permissionTags()
        );
        return new CurrentUserResponse(
                identity.tenantId(),
                identity.tenantId(),
                identity.tenantExternalId(),
                identity.userId(),
                identity.userName(),
                user.roles(),
                user.authorizedKnowledgeBaseIds(),
                user.permissionTags(),
                user.platformAdmin(),
                false,
                null,
                null
        );
    }

    private boolean active(SysTenant tenant) {
        return tenant != null && Integer.valueOf(1).equals(tenant.getStatus()) && !Integer.valueOf(1).equals(tenant.getIsDeleted());
    }

    private boolean active(SysUser user) {
        return user != null && Integer.valueOf(1).equals(user.getStatus()) && !Integer.valueOf(1).equals(user.getIsDeleted());
    }

    private boolean active(SysPlatformAdmin admin) {
        return admin != null && Integer.valueOf(1).equals(admin.getStatus()) && !Integer.valueOf(1).equals(admin.getIsDeleted());
    }

    private boolean platformRole(String roleCode) {
        if (!StringUtils.hasText(roleCode)) {
            return false;
        }
        Set<String> adminRoles = properties.getSecurity().getAdminRoles() == null
                ? Set.of()
                : properties.getSecurity().getAdminRoles().stream()
                .filter(StringUtils::hasText)
                .map(role -> role.trim().toUpperCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toSet());
        if (adminRoles.isEmpty()) {
            adminRoles = Set.of("SUPER_ADMIN");
        }
        return adminRoles.contains(roleCode.trim().toUpperCase(Locale.ROOT))
                || "PLATFORM_ADMIN".equals(roleCode.trim().toUpperCase(Locale.ROOT));
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeLoginType(String value) {
        if (!StringUtils.hasText(value)) {
            return "TENANT";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private Long parseLongOrNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private record LoginInput(Long tenantId, String tenant, String account, String password, boolean systemLogin) {
    }
}
