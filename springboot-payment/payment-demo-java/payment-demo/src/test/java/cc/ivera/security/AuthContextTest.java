package cc.ivera.security;

import cc.ivera.enums.UserRole;
import cc.ivera.exception.ForbiddenException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthContextTest {

    @AfterEach
    void clearContext() {
        AuthContext.clear();
    }

    @Test
    void requireShoppingUserAllowsOnlyUserRole() {
        AuthUser user = new AuthUser(2L, "alice", UserRole.USER);
        AuthContext.setUser(user);

        assertSame(user, AuthContext.requireShoppingUser());
    }

    @Test
    void requireShoppingUserRejectsMissingRole() {
        AuthContext.setUser(new AuthUser(3L, "unknown", null));

        assertThrows(ForbiddenException.class, AuthContext::requireShoppingUser);
    }

    @Test
    void requireAdminAllowsOnlyAdminRole() {
        AuthUser admin = new AuthUser(4L, "operator", UserRole.ADMIN);
        AuthContext.setUser(admin);

        assertSame(admin, AuthContext.requireAdmin());
    }

    @Test
    void requireAdminRejectsUserRole() {
        AuthContext.setUser(new AuthUser(5L, "buyer", UserRole.USER));

        assertThrows(ForbiddenException.class, AuthContext::requireAdmin);
    }
}
