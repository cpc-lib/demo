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
import cc.ivera.ragdemo.tenant.UserContext;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SystemTenantUserAdminServiceTest {

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void nonPlatformAdminCannotListTenants() {
        TenantContextHolder.set(tenantContext(List.of("TENANT_ADMIN")));
        SystemTenantUserAdminService service = fixture().service;

        assertThatThrownBy(() -> service.listTenants(null, null, 20))
                .isInstanceOf(TenantAccessDeniedException.class)
                .hasMessageContaining("Only platform administrators");
    }

    @Test
    void tenantScopedSuperAdminRoleCannotListTenants() {
        TenantContextHolder.set(tenantContext(List.of("SUPER_ADMIN")));
        SystemTenantUserAdminService service = fixture().service;

        assertThatThrownBy(() -> service.listTenants(null, null, 20))
                .isInstanceOf(TenantAccessDeniedException.class)
                .hasMessageContaining("Only platform administrators");
    }

    @Test
    void platformAdminCanCreateTenant() {
        TenantContextHolder.set(platformContext());
        TestFixture fixture = fixture();
        when(fixture.tenantMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        SysTenant created = fixture.service.createTenant(new SystemTenantRequest("tenant-a", "Tenant A", "tenant-a", 1));

        ArgumentCaptor<SysTenant> captor = ArgumentCaptor.forClass(SysTenant.class);
        verify(fixture.tenantMapper).insert(captor.capture());
        assertThat(created).isSameAs(captor.getValue());
        assertThat(captor.getValue().getTenantCode()).isEqualTo("tenant-a");
        assertThat(captor.getValue().getTenantName()).isEqualTo("Tenant A");
        assertThat(captor.getValue().getStatus()).isEqualTo(1);
        assertThat(captor.getValue().getIsDeleted()).isZero();
    }

    @Test
    void resetPasswordStoresHashAndRequiresChangeByDefault() {
        TenantContextHolder.set(platformContext());
        TestFixture fixture = fixture();
        SysUser user = user();
        when(fixture.userMapper.selectById(11L)).thenReturn(user);
        when(fixture.authService.hashNewPassword("NextSecret123!")).thenReturn("hashed-password");

        SysUser updated = fixture.service.resetUserPassword(11L, new UserPasswordResetRequest("NextSecret123!", null));

        verify(fixture.userMapper).updateById(user);
        assertThat(updated.getPasswordHash()).isEqualTo("hashed-password");
        assertThat(updated.getMustChangePassword()).isEqualTo(1);
        assertThat(updated.getPasswordUpdatedAt()).isNotNull();
    }

    @Test
    void updateUserRolesSoftDeletesOldRolesAndInsertsNormalizedNewRoles() {
        TenantContextHolder.set(platformContext());
        TestFixture fixture = fixture();
        SysUser user = user();
        SysUserRole oldRole = new SysUserRole();
        oldRole.setId(3L);
        oldRole.setTenantId(7L);
        oldRole.setUserId("alice");
        oldRole.setRoleCode("KB_OWNER");
        oldRole.setIsDeleted(0);
        when(fixture.userMapper.selectById(11L)).thenReturn(user);
        when(fixture.userRoleMapper.selectList(any(Wrapper.class))).thenReturn(List.of(oldRole));
        when(fixture.roleMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        List<SysUserRole> roles = fixture.service.updateUserRoles(11L, new UserRolesUpdateRequest(List.of("tenant_admin", "KB_OWNER")));

        verify(fixture.userRoleMapper).updateById(oldRole);
        assertThat(oldRole.getIsDeleted()).isEqualTo(1);
        ArgumentCaptor<SysUserRole> captor = ArgumentCaptor.forClass(SysUserRole.class);
        verify(fixture.userRoleMapper, org.mockito.Mockito.times(2)).insert(captor.capture());
        assertThat(captor.getAllValues()).extracting(SysUserRole::getRoleCode)
                .containsExactly("TENANT_ADMIN", "KB_OWNER");
        assertThat(roles).extracting(SysUserRole::getRoleCode)
                .containsExactly("TENANT_ADMIN", "KB_OWNER");
    }

    @Test
    void updateUserRolesRejectsPlatformRolesForTenantUsers() {
        TenantContextHolder.set(platformContext());
        TestFixture fixture = fixture();
        when(fixture.userMapper.selectById(11L)).thenReturn(user());

        assertThatThrownBy(() -> fixture.service.updateUserRoles(11L, new UserRolesUpdateRequest(List.of("SUPER_ADMIN"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Platform roles cannot be assigned to tenant users");
    }

    private TestFixture fixture() {
        SysTenantMapper tenantMapper = mock(SysTenantMapper.class);
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        LocalAuthService authService = mock(LocalAuthService.class);
        SystemTenantUserAdminService service = new SystemTenantUserAdminService(
                tenantMapper,
                userMapper,
                roleMapper,
                userRoleMapper,
                authService,
                mock(TenantAuditService.class),
                new AdminRolePolicy(),
                new RagProperties()
        );
        return new TestFixture(tenantMapper, userMapper, roleMapper, userRoleMapper, authService, service);
    }

    private TenantContext tenantContext(List<String> roles) {
        return new TenantContext(
                7L,
                null,
                new UserContext("admin", "Admin", roles, List.of(), List.of(), List.of()),
                "req-1",
                "127.0.0.1",
                false,
                false,
                7L,
                null,
                Instant.now()
        );
    }

    private TenantContext platformContext() {
        return new TenantContext(
                null,
                "platform",
                new UserContext("admin", "Admin", List.of("SUPER_ADMIN"), List.of(), List.of(), List.of()),
                "req-1",
                "127.0.0.1",
                false,
                false,
                null,
                null,
                Instant.now()
        );
    }

    private SysUser user() {
        SysUser user = new SysUser();
        user.setId(11L);
        user.setTenantId(7L);
        user.setExternalUserId("alice");
        user.setUsername("alice");
        user.setDisplayName("Alice");
        user.setEmail("alice@example.com");
        user.setStatus(1);
        user.setIsDeleted(0);
        return user;
    }

    private record TestFixture(
            SysTenantMapper tenantMapper,
            SysUserMapper userMapper,
            SysRoleMapper roleMapper,
            SysUserRoleMapper userRoleMapper,
            LocalAuthService authService,
            SystemTenantUserAdminService service
    ) {
    }
}
