package cc.ivera.ragdemo.controller;

import cc.ivera.ragdemo.security.ChangePasswordRequest;
import cc.ivera.ragdemo.security.LocalAuthService;
import cc.ivera.ragdemo.security.LoginRequest;
import cc.ivera.ragdemo.security.LoginResponse;
import cc.ivera.ragdemo.tenant.CurrentUserResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    @Test
    void loginReturnsAuthResponse() {
        LocalAuthService service = mock(LocalAuthService.class);
        LoginRequest request = new LoginRequest(null, "tenant-a", "alice", "Secret123!");
        LoginResponse loginResponse = new LoginResponse(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(60),
                new CurrentUserResponse(7L, 7L, "tenant-a", "alice", "Alice", List.of("TENANT_ADMIN"), List.of(), List.of(), false, false, null, null)
        );
        when(service.login(request)).thenReturn(loginResponse);

        AuthController controller = new AuthController(service);

        assertThat(controller.login(request).data()).isSameAs(loginResponse);
    }

    @Test
    void changePasswordDelegatesToService() {
        LocalAuthService service = mock(LocalAuthService.class);
        AuthController controller = new AuthController(service);
        ChangePasswordRequest request = new ChangePasswordRequest("Secret123!", "NextSecret123!");

        assertThat(controller.changePassword(request).data()).containsEntry("changed", true);
        verify(service).changePassword(request);
    }
}
