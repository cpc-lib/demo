package cc.ivera.controller;

import cc.ivera.dto.CartItemRequest;
import cc.ivera.dto.CartQuantityRequest;
import cc.ivera.enums.UserRole;
import cc.ivera.exception.ForbiddenException;
import cc.ivera.security.AuthContext;
import cc.ivera.security.AuthUser;
import cc.ivera.service.CartService;
import cc.ivera.vo.CartView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CartControllerTest {

    private final CartService cartService = mock(CartService.class);
    private final CartController controller = new CartController(cartService);

    @AfterEach
    void clearContext() {
        AuthContext.clear();
    }

    @Test
    void allOperationsDeriveUserIdFromAuthenticatedContext() {
        AuthContext.setUser(new AuthUser(77L, "alice", UserRole.USER));
        CartView view = new CartView(Collections.emptyList(), 0, 0, 0);
        when(cartService.getCart(77L)).thenReturn(view);

        assertEquals(view, controller.getCart().getData());
        controller.addItem(new CartItemRequest(101L, 2));
        controller.updateItem(101L, new CartQuantityRequest(4));
        controller.removeItem(101L);
        controller.clear();

        verify(cartService).getCart(77L);
        verify(cartService).addItem(77L, 101L, 2);
        verify(cartService).updateItem(77L, 101L, 4);
        verify(cartService).removeItem(77L, 101L);
        verify(cartService).clear(77L);
    }

    @Test
    void adminCannotReadCart() {
        AuthContext.setUser(new AuthUser(1L, "admin", UserRole.ADMIN));

        assertThrows(ForbiddenException.class, controller::getCart);
    }

    @Test
    void adminCannotMutateCart() {
        AuthContext.setUser(new AuthUser(1L, "admin", UserRole.ADMIN));

        assertThrows(ForbiddenException.class,
                () -> controller.addItem(new CartItemRequest(101L, 1)));
        assertThrows(ForbiddenException.class,
                () -> controller.updateItem(101L, new CartQuantityRequest(1)));
        assertThrows(ForbiddenException.class, () -> controller.removeItem(101L));
        assertThrows(ForbiddenException.class, controller::clear);
    }
}
