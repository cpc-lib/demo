package cc.ivera.controller;

import cc.ivera.enums.UserRole;
import cc.ivera.exception.ForbiddenException;
import cc.ivera.security.AuthContext;
import cc.ivera.security.AuthUser;
import cc.ivera.service.MessageOutboxService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class AdminOutboxControllerTest {

    private final MessageOutboxService service = mock(MessageOutboxService.class);
    private final AdminOutboxController controller = new AdminOutboxController(service);

    @AfterEach
    void clearContext() {
        AuthContext.clear();
    }

    @Test
    void adminCanRetryAFailedOutboxEvent() {
        AuthContext.setUser(user(UserRole.ADMIN));

        controller.retry("event-1");

        verify(service).retryFailed("event-1");
    }

    @Test
    void ordinaryUserCannotRetryOutboxEvents() {
        AuthContext.setUser(user(UserRole.USER));

        assertThatThrownBy(() -> controller.retry("event-1"))
                .isInstanceOf(ForbiddenException.class);

        verify(service, never()).retryFailed("event-1");
    }

    private AuthUser user(UserRole role) {
        return new AuthUser(7L, role.name().toLowerCase(), role);
    }
}
