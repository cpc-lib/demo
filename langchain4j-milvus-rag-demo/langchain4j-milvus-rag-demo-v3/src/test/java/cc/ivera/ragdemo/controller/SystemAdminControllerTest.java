package cc.ivera.ragdemo.controller;

import cc.ivera.ragdemo.admin.SystemTenantRequest;
import cc.ivera.ragdemo.admin.SystemTenantUserAdminService;
import cc.ivera.ragdemo.admin.UserPasswordResetRequest;
import cc.ivera.ragdemo.domain.tenant.SysTenant;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SystemAdminControllerTest {

    @Test
    void createTenantDelegatesToService() {
        SystemTenantUserAdminService service = mock(SystemTenantUserAdminService.class);
        SystemTenantRequest request = new SystemTenantRequest("tenant-a", "Tenant A", "tenant-a", 1);
        SysTenant tenant = new SysTenant();
        tenant.setId(7L);
        when(service.createTenant(request)).thenReturn(tenant);

        SystemAdminController controller = new SystemAdminController(service);

        assertThat(controller.createTenant(request).data()).isSameAs(tenant);
    }

    @Test
    void resetPasswordDelegatesToService() {
        SystemTenantUserAdminService service = mock(SystemTenantUserAdminService.class);
        UserPasswordResetRequest request = new UserPasswordResetRequest("NextSecret123!", true);
        SystemAdminController controller = new SystemAdminController(service);

        controller.resetUserPassword(11L, request);

        verify(service).resetUserPassword(11L, request);
    }
}
