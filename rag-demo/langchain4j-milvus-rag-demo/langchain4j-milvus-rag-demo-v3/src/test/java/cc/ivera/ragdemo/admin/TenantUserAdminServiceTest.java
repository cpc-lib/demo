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

class TenantUserAdminServiceTest {

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void nonTenantAdminCannotCreateUsers() {
        TenantContextHolder.set(context(List.of("KB_READER")));
        TenantUserAdminService service = fixture().service;

        assertThatThrownBy(() -> service.createUser(new SystemUserRequest(null, "bob", "bob", "Bob", null, "Secret123!", 1, false)))
                .isInstanceOf(TenantAccessDeniedException.class)
                .hasMessageContaining("Only tenant administrators");
    }

    @Test
    void tenantAdminCreatesUserInCurrentTenantAndIgnoresRequestTenantId() {
        TenantContextHolder.set(context(List.of("TENANT_ADMIN")));
        TestFixture fixture = fixture();
        when(fixture.userMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(fixture.authService.hashNewPassword("Secret123!")).thenReturn("hashed-password");

        SysUser created = fixture.service.createUser(new SystemUserRequest(99L, "bob", "bob", "Bob", "bob@example.com", "Secret123!", 1, false));

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(fixture.userMapper).insert(captor.capture());
        assertThat(created).isSameAs(captor.getValue());
        assertThat(captor.getValue().getTenantId()).isEqualTo(7L);
        assertThat(captor.getValue().getExternalUserId()).isEqualTo("bob");
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hashed-password");
        assertThat(captor.getValue().getMustChangePassword()).isZero();
    }

    @Test
    void tenantAdminCannotAssignPlatformRolesToTenantUsers() {
        TenantContextHolder.set(context(List.of("TENANT_ADMIN")));
        TestFixture fixture = fixture();
        when(fixture.userMapper.selectOne(any(Wrapper.class))).thenReturn(user());

        assertThatThrownBy(() -> fixture.service.updateUserRoles(11L, new UserRolesUpdateRequest(List.of("SUPER_ADMIN"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Platform roles cannot be assigned to tenant users");
    }

    @Test
    void tenantAdminUpdatesRolesInsideCurrentTenant() {
        TenantContextHolder.set(context(List.of("TENANT_ADMIN")));
        TestFixture fixture = fixture();
        SysUser user = user();
        SysUserRole oldRole = new SysUserRole();
        oldRole.setId(3L);
        oldRole.setTenantId(7L);
        oldRole.setUserId("bob");
        oldRole.setRoleCode("KB_READER");
        oldRole.setIsDeleted(0);
        when(fixture.userMapper.selectOne(any(Wrapper.class))).thenReturn(user);
        when(fixture.userRoleMapper.selectList(any(Wrapper.class))).thenReturn(List.of(oldRole));
        when(fixture.roleMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        List<SysUserRole> roles = fixture.service.updateUserRoles(11L, new UserRolesUpdateRequest(List.of("tenant_admin", "KB_OWNER")));

        verify(fixture.userRoleMapper).updateById(oldRole);
        assertThat(oldRole.getIsDeleted()).isEqualTo(1);
        ArgumentCaptor<SysUserRole> captor = ArgumentCaptor.forClass(SysUserRole.class);
        verify(fixture.userRoleMapper, org.mockito.Mockito.times(2)).insert(captor.capture());
        assertThat(captor.getAllValues()).extracting(SysUserRole::getTenantId).containsOnly(7L);
        assertThat(roles).extracting(SysUserRole::getRoleCode).containsExactly("TENANT_ADMIN", "KB_OWNER");
    }

    private TestFixture fixture() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
        LocalAuthService authService = mock(LocalAuthService.class);
        TenantUserAdminService service = new TenantUserAdminService(
                userMapper,
                roleMapper,
                userRoleMapper,
                authService,
                mock(TenantAuditService.class)
        );
        return new TestFixture(userMapper, roleMapper, userRoleMapper, authService, service);
    }

    private TenantContext context(List<String> roles) {
        return new TenantContext(
                7L,
                "demo",
                new UserContext("tenant-admin", "Tenant Admin", roles, List.of(), List.of(), List.of()),
                "req-1",
                "127.0.0.1",
                false,
                false,
                7L,
                null,
                Instant.now()
        );
    }

    private SysUser user() {
        SysUser user = new SysUser();
        user.setId(11L);
        user.setTenantId(7L);
        user.setExternalUserId("bob");
        user.setUsername("bob");
        user.setDisplayName("Bob");
        user.setStatus(1);
        user.setIsDeleted(0);
        return user;
    }

    private record TestFixture(
            SysUserMapper userMapper,
            SysRoleMapper roleMapper,
            SysUserRoleMapper userRoleMapper,
            LocalAuthService authService,
            TenantUserAdminService service
    ) {
    }
}
