package cc.ivera.security;

import cc.ivera.enums.UserRole;
import cc.ivera.exception.ForbiddenException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class AdminInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (AuthContext.requireUser().getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("无权执行该操作");
        }
        return true;
    }
}
