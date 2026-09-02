package cc.ivera.controller;

import cc.ivera.dto.OrderIdempotencyKeyView;
import cc.ivera.enums.UserRole;
import cc.ivera.exception.ForbiddenException;
import cc.ivera.security.AuthContext;
import cc.ivera.security.AuthUser;
import cc.ivera.service.OrderIdempotencyService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OrderIdempotencyControllerTest {

    @AfterEach
    void clearAuthContext() {
        AuthContext.clear();
    }

    @Test
    void currentUserCanRequestABackendOwnedKey() {
        AuthUser user = new AuthUser(7L, "alice", UserRole.USER);
        AuthContext.setUser(user);
        OrderIdempotencyService service = mock(OrderIdempotencyService.class);
        OrderIdempotencyKeyView view = new OrderIdempotencyKeyView("key-1", new Date());
        when(service.issue(user)).thenReturn(view);
        OrderIdempotencyController controller = new OrderIdempotencyController(service);

        assertEquals(view, controller.issue().getData());
        verify(service).issue(user);
    }

    @Test
    void adminCannotRequestAnOrderKey() {
        AuthContext.setUser(new AuthUser(1L, "admin", UserRole.ADMIN));
        OrderIdempotencyService service = mock(OrderIdempotencyService.class);
        OrderIdempotencyController controller = new OrderIdempotencyController(service);

        assertThrows(ForbiddenException.class, controller::issue);
        verifyNoInteractions(service);
    }
}
