package cc.ivera.service;

import cc.ivera.dto.LoginRequest;
import cc.ivera.dto.PasswordChangeRequest;
import cc.ivera.dto.RegisterRequest;
import cc.ivera.security.AuthUser;
import cc.ivera.vo.AuthSession;

public interface AuthService {

    AuthSession register(RegisterRequest request);

    AuthSession login(LoginRequest request);

    AuthSession refresh(String rawRefreshToken);

    void logout(String rawRefreshToken);

    void changePassword(AuthUser authUser, PasswordChangeRequest request);
}
