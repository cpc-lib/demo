package com.example.scanlogin.application;

import com.example.scanlogin.infrastructure.UserRepository;
import com.example.scanlogin.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;

@Service
public class UserAccountService {
    private static final Logger log = LoggerFactory.getLogger(UserAccountService.class);
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final JwtService jwt;
    private final RefreshTokenService refreshTokens;

    public UserAccountService(UserRepository users, PasswordEncoder passwords, JwtService jwt,
                              RefreshTokenService refreshTokens) {
        this.users = users;
        this.passwords = passwords;
        this.jwt = jwt;
        this.refreshTokens = refreshTokens;
    }

    public UserInfo register(String username, String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password exceeds BCrypt limit");
        }
        try {
            long id = users.create(username, passwords.encode(password));
            return new UserInfo(id, username);
        } catch (DuplicateKeyException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }
    }

    public MobileLogin loginMobile(String username, String password, HttpServletRequest request) {
        UserRepository.User user = requireUsableUser(username, password);
        audit(user.id(), "MOBILE_LOGIN", request);
        return new MobileLogin(jwt.issue(user.id(), "mobile", tokenVersionOf(user.id())),
                persistRefresh(user.id(), RefreshTokenService.MOBILE, "mobile-refresh"),
                user.username(), user.id());
    }

    public MobileLogin loginPc(String username, String password, HttpServletRequest request) {
        UserRepository.User user = requireUsableUser(username, password);
        audit(user.id(), "PC_PASSWORD_LOGIN", request);
        return new MobileLogin(jwt.issue(user.id(), "pc", tokenVersionOf(user.id())),
                persistRefresh(user.id(), RefreshTokenService.PC, "pc-refresh"),
                user.username(), user.id());
    }

    /** Exchanges a valid mobile refresh token for a fresh pair; rotates the stored token. */
    public MobileLogin refreshMobile(String refreshToken) {
        JwtService.Identity identity =
                refreshTokens.requireActive(refreshToken, "mobile-refresh", RefreshTokenService.MOBILE);
        UserInfo info = requireActive(identity.userId());
        refreshTokens.rotate(refreshToken);
        return new MobileLogin(jwt.issue(identity.userId(), "mobile", tokenVersionOf(identity.userId())),
                persistRefresh(identity.userId(), RefreshTokenService.MOBILE, "mobile-refresh"),
                info.username(), info.id());
    }

    /** PC refresh: validates the cookie token against the store and rotates it. */
    public MobileLogin refreshPc(String refreshToken) {
        JwtService.Identity identity =
                refreshTokens.requireActive(refreshToken, "pc-refresh", RefreshTokenService.PC);
        UserInfo info = requireActive(identity.userId());
        refreshTokens.rotate(refreshToken);
        return new MobileLogin(jwt.issue(identity.userId(), "pc", tokenVersionOf(identity.userId())),
                persistRefresh(identity.userId(), RefreshTokenService.PC, "pc-refresh"),
                info.username(), info.id());
    }

    private String persistRefresh(long userId, String client, String kind) {
        String refreshToken = jwt.issue(userId, kind, tokenVersionOf(userId));
        refreshTokens.persist(userId, client, refreshToken);
        return refreshToken;
    }

    /** Current session version; baked into every issued token. */
    public long tokenVersionOf(long userId) {
        return users.tokenVersion(userId);
    }

    /**
     * Kick primitive for the upcoming admin feature: bumps the session version so
     * every issued access token fails at once, and revokes all refresh tokens
     * (both clients) so no session can renew itself.
     */
    public void kickUser(long userId) {
        users.bumpTokenVersion(userId);
        refreshTokens.revokeAllForUser(userId);
    }

    private UserRepository.User requireUsableUser(String username, String password) {
        UserRepository.User user = users.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (!user.enabled() || !passwords.matches(password, user.passwordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        return user;
    }

    public UserInfo requireActive(long id) {
        UserRepository.User user = users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        if (!user.enabled()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User disabled");
        return new UserInfo(user.id(), user.username());
    }

    public void audit(long id, String type, HttpServletRequest request) {
        try {
            users.audit(id, type, request.getRemoteAddr(), request.getHeader("User-Agent"));
        } catch (RuntimeException error) {
            log.warn("Login audit failed, user={}, event={}", id, type, error);
        }
    }

    public record UserInfo(long id, String username) {}
    /** Access token plus its long-lived refresh companion; shared by mobile and PC password flows. */
    public record MobileLogin(String accessToken, String refreshToken, String username, long userId) {}
}
