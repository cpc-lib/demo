package cc.ivera.security;

import cc.ivera.enums.UserRole;
import cc.ivera.exception.ForbiddenException;
import cc.ivera.exception.UnauthorizedException;

public final class AuthContext {

    private static final ThreadLocal<AuthUser> CURRENT_USER = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void setUser(AuthUser user) {
        CURRENT_USER.set(user);
    }

    public static AuthUser getUser() {
        return CURRENT_USER.get();
    }

    public static AuthUser requireUser() {
        AuthUser user = CURRENT_USER.get();
        if (user == null) {
            throw new UnauthorizedException("请先登录");
        }
        return user;
    }

    public static AuthUser requireShoppingUser() {
        AuthUser user = requireUser();
        if (user.getRole() == UserRole.ADMIN) {
            throw new ForbiddenException("管理员账号不参与购物");
        }
        return user;
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
