package cc.ivera.ragdemo.admin;

import cc.ivera.ragdemo.audit.TenantAuditService;
import cc.ivera.ragdemo.domain.tenant.SysRole;
import cc.ivera.ragdemo.domain.tenant.SysUser;
import cc.ivera.ragdemo.domain.tenant.SysUserRole;
import cc.ivera.ragdemo.exception.TenantAccessDeniedException;
import cc.ivera.ragdemo.mapper.SysRoleMapper;
import cc.ivera.ragdemo.mapper.SysUserMapper;
import cc.ivera.ragdemo.mapper.SysUserRoleMapper;
import cc.ivera.ragdemo.security.LocalAuthService;
import cc.ivera.ragdemo.tenant.TenantContext;
import cc.ivera.ragdemo.tenant.TenantContextHolder;
import cc.ivera.ragdemo.tenant.UserContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class TenantUserAdminService {

    private static final Set<String> PLATFORM_ROLES = Set.of("SUPER_ADMIN", "PLATFORM_ADMIN");

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final LocalAuthService authService;
    private final TenantAuditService auditService;

    public List<SysUser> listUsers(String keyword, Integer status, Integer limit) {
        Long tenantId = requireManagedTenantId();
        return userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getTenantId, tenantId)
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
                .last("LIMIT " + limit(limit)));
    }

    public SysUser createUser(SystemUserRequest request) {
        Long tenantId = requireManagedTenantId();
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        String externalUserId = required(request.externalUserId(), "externalUserId");
        String password = required(request.password(), "password");
        ensureUserAvailable(tenantId, externalUserId);

        SysUser user = new SysUser();
        user.setTenantId(tenantId);
        user.setExternalUserId(externalUserId);
        user.setUsername(defaultText(request.username(), externalUserId));
        user.setDisplayName(defaultText(request.displayName(), externalUserId));
        user.setEmail(trim(request.email()));
        user.setPasswordHash(authService.hashNewPassword(password));
        user.setPasswordUpdatedAt(LocalDateTime.now());
        user.setMustChangePassword(Boolean.TRUE.equals(request.mustChangePassword()) ? 1 : 0);
        user.setStatus(request.status() == null ? 1 : request.status());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setIsDeleted(0);
        userMapper.insert(user);
        auditService.record("TENANT_USER_CREATE", "USER", user.getExternalUserId(), "SUCCESS", "{}");
        return user;
    }

    public SysUser updateUser(Long id, SystemUserRequest request) {
        requireManagedTenantId();
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
        auditService.record("TENANT_USER_UPDATE", "USER", user.getExternalUserId(), "SUCCESS", "{}");
        return user;
    }

    public SysUser enableUser(Long id) {
        return setUserStatus(id, 1, "TENANT_USER_ENABLE");
    }

    public SysUser disableUser(Long id) {
        return setUserStatus(id, 0, "TENANT_USER_DISABLE");
    }

    public SysUser resetUserPassword(Long id, UserPasswordResetRequest request) {
        requireManagedTenantId();
        String password = required(request == null ? null : request.password(), "password");
        SysUser user = requireUser(id);
        user.setPasswordHash(authService.hashNewPassword(password));
        user.setPasswordUpdatedAt(LocalDateTime.now());
        user.setMustChangePassword(Boolean.FALSE.equals(request.mustChangePassword()) ? 0 : 1);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        auditService.record("TENANT_USER_RESET_PASSWORD", "USER", user.getExternalUserId(), "SUCCESS", "{}");
        return user;
    }

    public List<SysRole> listRoles() {
        Long tenantId = requireManagedTenantId();
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getTenantId, tenantId)
                .eq(SysRole::getIsDeleted, 0)
                .orderByAsc(SysRole::getRoleCode));
    }

    public List<SysUserRole> listUserRoles(Long id) {
        requireManagedTenantId();
        SysUser user = requireUser(id);
        return userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getTenantId, user.getTenantId())
                .eq(SysUserRole::getUserId, user.getExternalUserId())
                .eq(SysUserRole::getIsDeleted, 0)
                .orderByAsc(SysUserRole::getRoleCode));
    }

    public List<SysUserRole> updateUserRoles(Long id, UserRolesUpdateRequest request) {
        requireManagedTenantId();
        List<String> roleCodes = normalizeTenantRoles(request == null ? null : request.roleCodes());
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
        auditService.record("TENANT_USER_ROLES_UPDATE", "USER", user.getExternalUserId(), "SUCCESS", "{}");
        return inserted;
    }

    private SysUser setUserStatus(Long id, int status, String operation) {
        requireManagedTenantId();
        SysUser user = requireUser(id);
        user.setStatus(status);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        auditService.record(operation, "USER", user.getExternalUserId(), "SUCCESS", "{}");
        return user;
    }

    private Long requireManagedTenantId() {
        TenantContext context = TenantContextHolder.require();
        Long tenantId = context.tenantId();
        if (tenantId == null) {
            throw new TenantAccessDeniedException("Tenant context is required for tenant user management");
        }
        UserContext user = context.user();
        if (user == null || (!user.hasRole("TENANT_ADMIN") && !user.platformAdmin())) {
            throw new TenantAccessDeniedException("Only tenant administrators can manage tenant users");
        }
        return tenantId;
    }

    private SysUser requireUser(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("user id is required");
        }
        Long tenantId = TenantContextHolder.requireTenantId();
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getId, id)
                .eq(SysUser::getTenantId, tenantId)
                .eq(SysUser::getIsDeleted, 0)
                .last("LIMIT 1"));
        if (user == null) {
            throw new NoSuchElementException("User not found in current tenant: " + id);
        }
        return user;
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

    private List<String> normalizeTenantRoles(List<String> roleCodes) {
        if (roleCodes == null) {
            return List.of();
        }
        return roleCodes.stream()
                .filter(StringUtils::hasText)
                .map(role -> role.trim().toUpperCase(Locale.ROOT))
                .peek(role -> {
                    if (PLATFORM_ROLES.contains(role)) {
                        throw new IllegalArgumentException("Platform roles cannot be assigned to tenant users: " + role);
                    }
                })
                .distinct()
                .toList();
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
