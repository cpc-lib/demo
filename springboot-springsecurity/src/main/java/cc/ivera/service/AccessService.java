package cc.ivera.service;

import org.springframework.security.core.Authentication;

import javax.servlet.http.HttpServletRequest;


public interface AccessService {
    boolean hasPermission(HttpServletRequest request, Authentication authentication);
}
