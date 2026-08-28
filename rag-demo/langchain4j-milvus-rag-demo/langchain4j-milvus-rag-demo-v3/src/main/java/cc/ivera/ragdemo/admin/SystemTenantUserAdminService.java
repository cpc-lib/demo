package cc.ivera.ragdemo.admin;

import cc.ivera.ragdemo.audit.TenantAuditService;
import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.domain.tenant.SysRole;
import cc.ivera.ragdemo.domain.tenant.SysTenant;
import cc.ivera.ragdemo.domain.tenant.SysUser;
import cc.ivera.ragdemo.domain.tenant.SysUserRole;
import cc.ivera.ragdemo.exception.TenantAccessDeniedException;
import cc.ivera.ragdemo.mapper.SysRoleMapper;
import cc.ivera.ragdemo.mapper.SysTenantMapper;
import cc.ivera.ragdemo.mapper.SysUserMapper;
import cc.ivera.ragdemo.mapper.SysUserRoleMapper;
import cc.ivera.ragdemo.security.LocalAuthService;
import cc.ivera.ragdemo.service.ragops.AdminRolePolicy;
import cc.ivera.ragdemo.tenant.TenantContext;
import cc.ivera.ragdemo.tenant.TenantContextHolder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class SystemTenantUserAdminService {

    private final SysTenantMapper tenantMapper;
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final LocalAuthService authService;
    private final TenantAuditService auditService;
    private final AdminRolePolicy adminRolePolicy;
    private final RagProperties properties;

    public List<SysTenant> listTenants(String keyword, Integer status, Integer limit) {
        assertPlatformAdmin();
        return TenantContextHolder.callWithBypass(() -> tenantMapper.selectList(new LambdaQueryWrapper<SysTenant>()
                .eq(SysTenant::getIsDeleted, 0)
                .eq(status != null, SysTenant::getStatus, status)
                .and(StringUtils.hasText(keyword), wrapper -> wrapper
                        .like(SysTenant::getTenantCode, keyword)
                        .or()
                        .like(SysTenant::getTenantName, keyword)
                        .or()
                        .like(SysTenant::getExternalId, keyword))
                .orderByDesc(SysTenant::getCreatedAt)
                .last("LIMIT " + limit(limit))));
    }

    public SysTenant createTenant(SystemTenantRequest request) {
        assertPlatformAdmin();
        String tenantCode = required(request == null ? null : request.tenantCode(), "tenantCode");
        String tenantName = required(request.tenantName(), "tenantName");
        return TenantContextHolder.callWithBypass(() -> {
            ensureTenantCodeAvailable(null, tenantCode);
            SysTenant tenant = new SysTenant();
            tenant.setTenantCode(tenantCode);
            tenant.setTenantName(tenantName);
            tenant.setExternalId(trim(request.externalId()));
            tenant.setStatus(request.status() == null ? 1 : request.status());
            tenant.setCreatedAt(LocalDateTime.now());
            tenant.setUpdatedAt(LocalDateTime.now());
            tenant.setIsDeleted(0);
            tenantMapper.insert(tenant);
            auditService.record("SYSTEM_TENANT_CREATE", "TENANT", String.valueOf(tenant.getId()), "SUCCESS", "{}");
            return tenant;
        });
    }

    public SysTenant updateTenant(Long tenantId, SystemTenantRequest request) {
        assertPlatformAdmin();
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId is required");
        }
        return TenantContextHolder.callWithBypass(() -> {
            SysTenant tenant = requireTenant(tenantId);
            String tenantCode = trim(request == null ? null : request.tenantCode());
            if (StringUtils.hasText(tenantCode) && !tenantCode.equals(tenant.getTenantCode())) {
                ensureTenantCodeAvailable(tenantId, tenantCode);
                tenant.setTenantCode(tenantCode);
            }
            if (request != null && StringUtils.hasText(request.tenantName())) {
                tenant.setTenantName(request.tenantName().trim());
            }
            if (request != null && request.externalId() != null) {
                tenant.setExternalId(trim(request.externalId()));
            }
            if (request != null && request.status() != null) {
                tenant.setStatus(request.status());
            }
            tenant.setUpdatedAt(LocalDateTime.now());
            tenantMapper.updateById(tenant);
            auditService.record("SYSTEM_TENANT_UPDATE", "TENANT", String.valueOf(tenantId), "SUCCESS", "{}");
            return tenant;
        });
    }

    public SysTenant enableTenant(Long tenantId) {
        return setTenantStatus(tenantId, 1, "SYSTEM_TENANT_ENABLE");
    }

    public SysTenant disableTenant(Long tenantId) {
        return setTenantStatus(tenantId, 0, "SYSTEM_TENANT_DISABLE");
    }

    public List<SysUser> listUsers(Long tenantId, String keyword, Integer status, Integer limit) {
        assertPlatformAdmin();
        return TenantContextHolder.callWithBypass(() -> userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(tenantId != null, SysUser::getTenantId, tenantId)
                .gt(tenantId == null, SysUser::getTenantId, 0)
                .eq(SysUser::getIsDeleted, 0)
                .eq(status != null, SysUser::getStatus, status)
                .and(StringUtils.hasText(keyword), wrapper -> wrapper
                        .like(SysUser::getExternalUserId, keyword)
                        .or()
                        .like(SysUser::getUsername, keyword)
                        .or()
                        .like(SysUser::getDisplayName, keyword)
                        .or()
                        .like(SysUser::getEmail, keyword))
                .orderByDesc(SysUser::getCreatedAt)
                .last("LIMIT " + limit(limit))));
    }

    public SysUser createUser(SystemUserRequest request) {
        assertPlatformAdmin();
        if (request == null || request.tenantId() == null) {
            throw new IllegalArgumentException("tenantId is required");
        }
        String externalUserId = required(request.externalUserId(), "externalUserId");
        String password = required(request.password(), "password");
        return TenantContextHolder.callWithBypass(() -> {
            requireTenant(request.tenantId());
            ensureUserAvailable(request.tenantId(), externalUserId);
            SysUser user = new SysUser();
            user.setTenantId(request.tenantId());
            user.setExternalUserId(externalUserId);
            user.setUsername(defaultText(request.username(), externalUserId));
            user.setDisplayName(defaultText(request.displayName(), externalUserId));
            user.setEmail(trim(request.email()));
            user.setPasswordHash(authService.hashNewPassword(password));
            user.setPasswordUpdatedAt(LocalDateTime.now());
            user.setMustChangePassword(Boolean.FALSE.equals(request.mustChangePassword()) ? 0 : 1);
            user.setStatus(request.status() == null ? 1 : request.status());
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            user.setIsDeleted(0);
            userMapper.insert(user);
            auditService.record("SYSTEM_USER_CREATE", "USER", user.getExternalUserId(), "SUCCESS", "{}");
            return user;
        });
    }

    public SysUser updateUser(Long id, SystemUserRequest request) {
        assertPlatformAdmin();
        return TenantContextHolder.callWithBypass(() -> {
            SysUser user = requireUser(id);
            if (request != null && StringUtils.hasText(request.username())) {
                user.setUsername(request.username().trim());
            }
            if (request != null && request.displayName() != null) {
                user.setDisplayName(trim(request.displayName()));
            }
            if (request != null && request.email() != null) {
                user.setEmail(trim(request.email()));
            }
            if (request != null && request.status() != null) {
                user.setStatus(request.status());
            }
            user.setUpdatedAt(LocalDateTime.now());
            userMapper.updateById(user);
            auditService.record("SYSTEM_USER_UPDATE", "USER", user.getExternalUserId(), "SUCCESS", "{}");
            return user;
        });
    }

    public SysUser enableUser(Long id) {
        return setUserStatus(id, 1, "SYSTEM_USER_ENABLE");
    }

    public SysUser disableUser(Long id) {
        return setUserStatus(id, 0, "SYSTEM_USER_DISABLE");
    }

    public SysUser resetUserPassword(Long id, UserPasswordResetRequest request) {
        assertPlatformAdmin();
        String password = required(request == null ? null : request.password(), "password");
        return TenantContextHolder.callWithBypass(() -> {
            SysUser user = requireUser(id);
            user.setPasswordHash(authService.hashNewPassword(password));
            user.setPasswordUpdatedAt(LocalDateTime.now());
            user.setMustChangePassword(Boolean.FALSE.equals(request.mustChangePassword()) ? 0 : 1);
            user.setUpdatedAt(LocalDateTime.now());
            userMapper.updateById(user);
            auditService.record("SYSTEM_USER_RESET_PASSWORD", "USER", user.getExternalUserId(), "SUCCESS", "{}");
            return user;
        });
    }

    public List<SysRole> listRoles(Long tenantId) {
        assertPlatformAdmin();
        return TenantContextHolder.callWithBypass(() -> roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .eq(tenantId != null, SysRole::getTenantId, tenantId)
                .gt(tenantId == null, SysRole::getTenantId, 0)
                .eq(SysRole::getIsDeleted, 0)
                .orderByAsc(SysRole::getTenantId)
                .orderByAsc(SysRole::getRoleCode)));
    }

    public List<SysUserRole> listUserRoles(Long id) {
        assertPlatformAdmin();
        return TenantContextHolder.callWithBypass(() -> {
            SysUser user = requireUser(id);
            return userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                    .eq(SysUserRole::getTenantId, user.getTenantId())
                    .eq(SysUserRole::getUserId, user.getExternalUserId())
                    .eq(SysUserRole::getIsDeleted, 0)
                    .orderByAsc(SysUserRole::getRoleCode));
        });
    }

    public List<SysUserRole> updateUserRoles(Long id, UserRolesUpdateRequest request) {
        assertPlatformAdmin();
        List<String> roleCodes = normalizeRoles(request == null ? null : request.roleCodes());
        return TenantContextHolder.callWithBypass(() -> {
            SysUser user = requireUser(id);
            List<SysUserRole> oldRoles = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                    .eq(SysUserRole::getTenantId, user.getTenantId())
                    .eq(SysUserRole::getUserId, user.getExternalUserId())
                    .eq(SysUserRole::getIsDeleted, 0));
            for (SysUserRole oldRole : oldRoles) {
                oldRole.setIsDeleted(1);
                userRoleMapper.updateById(oldRole);
            }
            List<SysUserRole> inserted = roleCodes.stream()
                    .map(roleCode -> insertUserRole(user, ensureRole(user.getTenantId(), roleCode)))
                    .toList();
            auditService.record("SYSTEM_USER_ROLES_UPDATE", "USER", user.getExternalUserId(), "SUCCESS", "{}");
            return inserted;
        });
    }

    private SysTenant setTenantStatus(Long tenantId, int status, String operation) {
        assertPlatformAdmin();
        return TenantContextHolder.callWithBypass(() -> {
            SysTenant tenant = requireTenant(tenantId);
            tenant.setStatus(status);
            tenant.setUpdatedAt(LocalDateTime.now());
            tenantMapper.updateById(tenant);
            auditService.record(operation, "TENANT", String.valueOf(tenantId), "SUCCESS", "{}");
            return tenant;
        });
    }

    private SysUser setUserStatus(Long id, int status, String operation) {
        assertPlatformAdmin();
        return TenantContextHolder.callWithBypass(() -> {
            SysUser user = requireUser(id);
            user.setStatus(status);
            user.setUpdatedAt(LocalDateTime.now());
            userMapper.updateById(user);
            auditService.record(operation, "USER", user.getExternalUserId(), "SUCCESS", "{}");
            return user;
        });
    }

    private SysRole ensureRole(Long tenantId, String roleCode) {
        SysRole existing = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getTenantId, tenantId)
                .eq(SysRole::getRoleCode, roleCode)
                .eq(SysRole::getIsDeleted, 0)
                .last("LIMIT 1"));
        if (existing != null) {
            return existing;
        }
        SysRole role = new SysRole();
        role.setTenantId(tenantId);
        role.setRoleCode(roleCode);
        role.setRoleName(roleCode);
        role.setRoleScope("TENANT");
        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedAt(LocalDateTime.now());
        role.setIsDeleted(0);
        roleMapper.insert(role);
        return role;
    }

    private SysUserRole insertUserRole(SysUser user, SysRole role) {
        SysUserRole userRole = new SysUserRole();
        userRole.setTenantId(user.getTenantId());
        userRole.setUserId(user.getExternalUserId());
        userRole.setRoleId(role.getId());
        userRole.setRoleCode(role.getRoleCode());
        userRole.setCreatedAt(LocalDateTime.now());
        userRole.setIsDeleted(0);
        userRoleMapper.insert(userRole);
        return userRole;
    }

    private void assertPlatformAdmin() {
        TenantContext context = TenantContextHolder.require();
        if (context.operatorTenantId() != null
                || !adminRolePolicy.isPlatformAdmin(context.user(), properties.getSecurity().getAdminRoles())) {
            throw new TenantAccessDeniedException("Only platform administrators can manage system tenants and users");
        }
    }

    private SysTenant requireTenant(Long tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId is required");
        }
        SysTenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null || Integer.valueOf(1).equals(tenant.getIsDeleted())) {
            throw new NoSuchElementException("Tenant not found: " + tenantId);
        }
        return tenant;
    }

    private SysUser requireUser(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("user id is required");
        }
        SysUser user = userMapper.selectById(id);
        if (user == null || Integer.valueOf(1).equals(user.getIsDeleted())) {
            throw new NoSuchElementException("User not found: " + id);
        }
        return user;
    }

    private void ensureTenantCodeAvailable(Long currentTenantId, String tenantCode) {
        SysTenant duplicate = tenantMapper.selectOne(new LambdaQueryWrapper<SysTenant>()
                .eq(SysTenant::getTenantCode, tenantCode)
                .eq(SysTenant::getIsDeleted, 0)
                .ne(currentTenantId != null, SysTenant::getId, currentTenantId)
                .last("LIMIT 1"));
        if (duplicate != null) {
            throw new IllegalArgumentException("Tenant code already exists: " + tenantCode);
        }
    }

    private void ensureUserAvailable(Long tenantId, String externalUserId) {
        SysUser duplicate = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getTenantId, tenantId)
                .eq(SysUser::getExternalUserId, externalUserId)
                .eq(SysUser::getIsDeleted, 0)
                .last("LIMIT 1"));
        if (duplicate != null) {
            throw new IllegalArgumentException("User already exists in tenant: " + externalUserId);
        }
    }

    private List<String> normalizeRoles(List<String> roleCodes) {
        if (roleCodes == null) {
            return List.of();
        }
        return roleCodes.stream()
                .filter(StringUtils::hasText)
                .map(role -> role.trim().toUpperCase(Locale.ROOT))
                .peek(role -> {
                    if (platformRole(role)) {
                        throw new IllegalArgumentException("Platform roles cannot be assigned to tenant users: " + role);
                    }
                })
                .distinct()
                .toList();
    }

    private boolean platformRole(String roleCode) {
        return "PLATFORM_ADMIN".equals(roleCode) || "SUPER_ADMIN".equals(roleCode);
    }

    private int limit(Integer limit) {
        return Math.max(1, Math.min(500, limit == null ? 100 : limit));
    }

    private String required(String value, String field) {
        String trimmed = trim(value);
        if (!StringUtils.hasText(trimmed)) {
            throw new IllegalArgumentException(field + " is required");
        }
        return trimmed;
    }

    private String defaultText(String value, String fallback) {
        String trimmed = trim(value);
        return StringUtils.hasText(trimmed) ? trimmed : fallback;
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
