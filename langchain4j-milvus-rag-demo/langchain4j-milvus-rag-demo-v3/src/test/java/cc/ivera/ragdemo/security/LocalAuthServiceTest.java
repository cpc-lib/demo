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
import cc.ivera.ragdemo.tenant.IdentityAuthenticationException;
import cc.ivera.ragdemo.tenant.TenantContext;
import cc.ivera.ragdemo.tenant.TenantContextHolder;
import cc.ivera.ragdemo.tenant.UserContext;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LocalAuthServiceTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void loginReturnsTokenAndCurrentUserWhenPasswordMatches() {
        TestFixture fixture = fixture();
        when(fixture.tenantMapper.selectOne(any(Wrapper.class))).thenReturn(tenant(7L, "tenant-a", 1));
        when(fixture.userMapper.selectOne(any(Wrapper.class))).thenReturn(user(7L, "alice", "alice@example.com", passwordEncoder.encode("Secret123!"), 1));
        when(fixture.userRoleMapper.selectList(any(Wrapper.class))).thenReturn(List.of(userRole(7L, "alice", "TENANT_ADMIN")));

        LoginResponse response = fixture.service.login(new LoginRequest(null, "tenant-a", "alice", "Secret123!"));

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.currentUser().tenantId()).isEqualTo(7L);
        assertThat(response.currentUser().userId()).isEqualTo("alice");
        assertThat(response.currentUser().roles()).containsExactly("TENANT_ADMIN");
    }

    @Test
    void tenantLoginAcceptsTenantFieldInsteadOfTenantCodeOrTenantId() {
        TestFixture fixture = fixture();
        when(fixture.tenantMapper.selectOne(any(Wrapper.class))).thenReturn(tenant(7L, "tenant-a", 1));
        when(fixture.userMapper.selectOne(any(Wrapper.class))).thenReturn(user(7L, "alice", "alice@example.com", passwordEncoder.encode("Secret123!"), 1));
        when(fixture.userRoleMapper.selectList(any(Wrapper.class))).thenReturn(List.of(userRole(7L, "alice", "TENANT_ADMIN")));

        LoginResponse response = fixture.service.login(new LoginRequest(null, null, "Tenant tenant-a", "alice", "Secret123!", "TENANT"));

        assertThat(response.currentUser().tenantId()).isEqualTo(7L);
        assertThat(response.currentUser().userId()).isEqualTo("alice");
    }

    @Test
    void tenantLoginDoesNotAcceptSyntheticSystemTenantAlias() {
        TestFixture fixture = fixture();
        when(fixture.tenantMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        assertThatThrownBy(() -> fixture.service.login(new LoginRequest(null, null, "0", "demo-user", "a605288582", "TENANT")))
                .isInstanceOf(IdentityAuthenticationException.class)
                .hasMessageContaining("Invalid username or password");
    }

    @Test
    void systemLoginAllowsPlatformAdminWithoutTenant() {
        TestFixture fixture = fixture();
        when(fixture.platformAdminMapper.selectOne(any(Wrapper.class))).thenReturn(platformAdmin("admin", "admin@example.com", passwordEncoder.encode("Secret123!"), 1));

        LoginResponse response = fixture.service.login(new LoginRequest(null, null, null, "admin", "Secret123!", "SYSTEM"));

        assertThat(response.currentUser().tenantId()).isNull();
        assertThat(response.currentUser().operatorTenantId()).isNull();
        assertThat(response.currentUser().tenantExternalId()).isEqualTo("platform");
        assertThat(response.currentUser().userId()).isEqualTo("admin");
        assertThat(response.currentUser().platformAdmin()).isTrue();
        assertThat(response.currentUser().roles()).containsExactly("SUPER_ADMIN");
    }

    @Test
    void systemLoginDoesNotUseTenantUsers() {
        TestFixture fixture = fixture();
        when(fixture.platformAdminMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        assertThatThrownBy(() -> fixture.service.login(new LoginRequest(null, null, null, "alice", "Secret123!", "SYSTEM")))
                .isInstanceOf(IdentityAuthenticationException.class)
                .hasMessageContaining("Invalid username or password");
    }

    @Test
    void loginRejectsWrongPassword() {
        TestFixture fixture = fixture();
        when(fixture.tenantMapper.selectOne(any(Wrapper.class))).thenReturn(tenant(7L, "tenant-a", 1));
        when(fixture.userMapper.selectOne(any(Wrapper.class))).thenReturn(user(7L, "alice", "alice@example.com", passwordEncoder.encode("Secret123!"), 1));

        assertThatThrownBy(() -> fixture.service.login(new LoginRequest(null, "tenant-a", "alice", "bad-password")))
                .isInstanceOf(IdentityAuthenticationException.class)
                .hasMessageContaining("Invalid username or password");
    }

    @Test
    void loginRejectsDisabledTenant() {
        TestFixture fixture = fixture();
        when(fixture.tenantMapper.selectOne(any(Wrapper.class))).thenReturn(tenant(7L, "tenant-a", 0));

        assertThatThrownBy(() -> fixture.service.login(new LoginRequest(null, "tenant-a", "alice", "Secret123!")))
                .isInstanceOf(TenantAccessDeniedException.class)
                .hasMessageContaining("Tenant is disabled");
    }

    @Test
    void changePasswordRequiresCurrentPasswordAndStoresNewHash() {
        TestFixture fixture = fixture();
        SysUser existing = user(7L, "alice", "alice@example.com", passwordEncoder.encode("Secret123!"), 1);
        when(fixture.userMapper.selectOne(any(Wrapper.class))).thenReturn(existing);
        TenantContextHolder.set(TenantContextHolder.systemContext(7L, "test"));

        fixture.service.changePassword(new ChangePasswordRequest("Secret123!", "NextSecret123!"));

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(fixture.userMapper).updateById(captor.capture());
        assertThat(passwordEncoder.matches("NextSecret123!", captor.getValue().getPasswordHash())).isTrue();
        assertThat(captor.getValue().getMustChangePassword()).isZero();
        assertThat(captor.getValue().getPasswordUpdatedAt()).isNotNull();
    }

    @Test
    void changePasswordUpdatesPlatformAdminWhenIdentityHasNoTenant() {
        TestFixture fixture = fixture();
        SysPlatformAdmin existing = platformAdmin("admin", "admin@example.local", passwordEncoder.encode("Secret123!"), 1);
        when(fixture.platformAdminMapper.selectOne(any(Wrapper.class))).thenReturn(existing);
        TenantContextHolder.set(platformContext());

        fixture.service.changePassword(new ChangePasswordRequest("Secret123!", "NextSecret123!"));

        ArgumentCaptor<SysPlatformAdmin> captor = ArgumentCaptor.forClass(SysPlatformAdmin.class);
        verify(fixture.platformAdminMapper).updateById(captor.capture());
        assertThat(passwordEncoder.matches("NextSecret123!", captor.getValue().getPasswordHash())).isTrue();
        assertThat(captor.getValue().getMustChangePassword()).isZero();
        assertThat(captor.getValue().getPasswordUpdatedAt()).isNotNull();
    }

    private TestFixture fixture() {
        SysTenantMapper tenantMapper = mock(SysTenantMapper.class);
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
        SysPlatformAdminMapper platformAdminMapper = mock(SysPlatformAdminMapper.class);
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        RagProperties properties = new RagProperties();
        properties.getSecurity().getJwt().setHmacSecret("12345678901234567890123456789012");
        LocalJwtService jwtService = new LocalJwtService(properties);
        LocalAuthService service = new LocalAuthService(
                tenantMapper,
                userMapper,
                userRoleMapper,
                platformAdminMapper,
                roleMapper,
                passwordEncoder,
                new PasswordPolicy(),
                jwtService,
                mock(TenantAuditService.class),
                properties
        );
        return new TestFixture(tenantMapper, userMapper, userRoleMapper, platformAdminMapper, service);
    }

    private SysTenant tenant(Long id, String code, int status) {
        SysTenant tenant = new SysTenant();
        tenant.setId(id);
        tenant.setTenantCode(code);
        tenant.setTenantName("Tenant " + code);
        tenant.setExternalId(code);
        tenant.setStatus(status);
        tenant.setIsDeleted(0);
        return tenant;
    }

    private SysUser user(Long tenantId, String externalUserId, String email, String passwordHash, int status) {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setTenantId(tenantId);
        user.setExternalUserId(externalUserId);
        user.setUsername(externalUserId);
        user.setDisplayName("Alice");
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setStatus(status);
        user.setIsDeleted(0);
        return user;
    }

    private SysUserRole userRole(Long tenantId, String userId, String roleCode) {
        SysUserRole role = new SysUserRole();
        role.setTenantId(tenantId);
        role.setUserId(userId);
        role.setRoleCode(roleCode);
        role.setIsDeleted(0);
        return role;
    }

    private SysPlatformAdmin platformAdmin(String username, String email, String passwordHash, int status) {
        SysPlatformAdmin admin = new SysPlatformAdmin();
        admin.setId(1L);
        admin.setSingletonKey(1);
        admin.setAdminUsername(username);
        admin.setDisplayName("Super Administrator");
        admin.setEmail(email);
        admin.setPasswordHash(passwordHash);
        admin.setStatus(status);
        admin.setIsDeleted(0);
        return admin;
    }

    private TenantContext platformContext() {
        return new TenantContext(
                null,
                "platform",
                new UserContext("admin", "Super Administrator", List.of("SUPER_ADMIN"), List.of(), List.of(), List.of()),
                "req-platform",
                "127.0.0.1",
                false,
                false,
                null,
                null,
                Instant.now()
        );
    }

    private record TestFixture(
            SysTenantMapper tenantMapper,
            SysUserMapper userMapper,
            SysUserRoleMapper userRoleMapper,
            SysPlatformAdminMapper platformAdminMapper,
            LocalAuthService service
    ) {
    }
}
