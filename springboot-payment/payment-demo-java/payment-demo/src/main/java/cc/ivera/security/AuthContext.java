package cc.ivera.security;

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

    public static void clear() {
        CURRENT_USER.remove();
    }
}
