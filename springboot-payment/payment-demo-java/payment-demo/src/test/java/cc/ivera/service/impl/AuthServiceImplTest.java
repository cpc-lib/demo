package cc.ivera.service.impl;

import cc.ivera.config.AuthProperties;
import cc.ivera.dto.LoginRequest;
import cc.ivera.dto.PasswordChangeRequest;
import cc.ivera.dto.RegisterRequest;
import cc.ivera.entity.RefreshToken;
import cc.ivera.entity.User;
import cc.ivera.enums.UserRole;
import cc.ivera.exception.BizException;
import cc.ivera.exception.UnauthorizedException;
import cc.ivera.mapper.RefreshTokenMapper;
import cc.ivera.mapper.UserMapper;
import cc.ivera.security.AuthUser;
import cc.ivera.security.JwtTokenService;
import cc.ivera.vo.AuthSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceImplTest {

    private UserMapper userMapper;
    private RefreshTokenMapper refreshTokenMapper;
    private BCryptPasswordEncoder passwordEncoder;
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        refreshTokenMapper = mock(RefreshTokenMapper.class);
        passwordEncoder = new BCryptPasswordEncoder();
        AuthProperties properties = new AuthProperties();
        properties.setJwtSecret("0123456789abcdef0123456789abcdef");
        properties.setAccessTokenSeconds(900);
        properties.setRefreshTokenSeconds(604800);
        JwtTokenService tokenService = new JwtTokenService(properties);
        authService = new AuthServiceImpl(
                userMapper,
                refreshTokenMapper,
                passwordEncoder,
                tokenService,
                properties
        );
    }

    @Test
    void registrationCreatesUserRoleAndStoresOnlyPasswordHash() {
        when(userMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(21L);
            return 1;
        }).when(userMapper).insert(any(User.class));

        AuthSession session = authService.register(new RegisterRequest("alice", "StrongPass1!"));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertEquals(UserRole.USER, saved.getRole());
        assertNotEquals("StrongPass1!", saved.getPasswordHash());
        assertTrue(passwordEncoder.matches("StrongPass1!", saved.getPasswordHash()));
        assertNotNull(session.getAccessToken());
        assertNotNull(session.getRefreshToken());

        ArgumentCaptor<RefreshToken> refreshCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenMapper).insert(refreshCaptor.capture());
        assertEquals(sha256(session.getRefreshToken()), refreshCaptor.getValue().getTokenHash());
        assertFalse(refreshCaptor.getValue().getTokenHash().contains(session.getRefreshToken()));
    }

    @Test
    void duplicateUsernameIsRejectedBeforePasswordOrTokenPersistence() {
        when(userMapper.selectOne(any())).thenReturn(user(1L, "alice", "hash", UserRole.USER));

        assertThrows(BizException.class,
                () -> authService.register(new RegisterRequest("alice", "StrongPass1!")));

        verify(userMapper, never()).insert(any(User.class));
        verify(refreshTokenMapper, never()).insert(any(RefreshToken.class));
    }

    @Test
    void loginRejectsWrongPassword() {
        User user = user(3L, "alice", passwordEncoder.encode("CorrectPass1!"), UserRole.USER);
        when(userMapper.selectOne(any())).thenReturn(user);

        assertThrows(UnauthorizedException.class,
                () -> authService.login(new LoginRequest("alice", "WrongPass1!")));

        verify(refreshTokenMapper, never()).insert(any(RefreshToken.class));
    }

    @Test
    void refreshRotatesTokenWithinSameFamilyAndLinksReplacementHash() {
        String currentRawToken = "current-refresh-token";
        RefreshToken current = refreshToken(5L, 8L, sha256(currentRawToken), "family-1", null);
        when(refreshTokenMapper.selectByHashForUpdate(sha256(currentRawToken))).thenReturn(current);
        when(userMapper.selectById(8L)).thenReturn(user(8L, "alice", "hash", UserRole.USER));

        AuthSession session = authService.refresh(currentRawToken);

        assertNotEquals(currentRawToken, session.getRefreshToken());
        assertNotNull(current.getRevokedAt());
        assertEquals(sha256(session.getRefreshToken()), current.getReplacedByHash());
        verify(refreshTokenMapper).selectByHashForUpdate(sha256(currentRawToken));
        verify(refreshTokenMapper, never()).selectOne(any());
        verify(refreshTokenMapper).updateById(current);

        ArgumentCaptor<RefreshToken> replacementCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenMapper).insert(replacementCaptor.capture());
        assertEquals("family-1", replacementCaptor.getValue().getTokenFamily());
        assertEquals(sha256(session.getRefreshToken()), replacementCaptor.getValue().getTokenHash());
    }

    @Test
    void replayOfRotatedTokenRevokesItsWholeFamily() {
        String rawToken = "already-used-token";
        RefreshToken rotated = refreshToken(6L, 9L, sha256(rawToken), "family-replay", new Date());
        when(refreshTokenMapper.selectByHashForUpdate(sha256(rawToken))).thenReturn(rotated);

        assertThrows(UnauthorizedException.class, () -> authService.refresh(rawToken));

        verify(refreshTokenMapper).revokeFamily(anyString(), any(Date.class));
        verify(refreshTokenMapper, never()).insert(any(RefreshToken.class));
    }

    @Test
    void logoutRevokesOnlyCurrentTokenFamily() {
        String rawToken = "logout-token";
        RefreshToken current = refreshToken(7L, 10L, sha256(rawToken), "family-logout", null);
        when(refreshTokenMapper.selectByHashForUpdate(sha256(rawToken))).thenReturn(current);

        authService.logout(rawToken);

        verify(refreshTokenMapper).revokeFamily(anyString(), any(Date.class));
        verify(refreshTokenMapper, never()).revokeAllByUser(any(Long.class), any(Date.class));
    }

    @Test
    void passwordChangeUsesBcryptAndRevokesAllUserSessions() {
        User user = user(12L, "alice", passwordEncoder.encode("OldPass1!"), UserRole.USER);
        when(userMapper.selectById(12L)).thenReturn(user);

        authService.changePassword(
                new AuthUser(12L, "alice", UserRole.USER),
                new PasswordChangeRequest("OldPass1!", "NewPass2!")
        );

        assertTrue(passwordEncoder.matches("NewPass2!", user.getPasswordHash()));
        assertFalse(passwordEncoder.matches("OldPass1!", user.getPasswordHash()));
        verify(userMapper).updateById(user);
        verify(refreshTokenMapper).revokeAllByUser(any(Long.class), any(Date.class));
    }

    private User user(Long id, String username, String passwordHash, UserRole role) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPasswordHash(passwordHash);
        user.setRole(role);
        return user;
    }

    private RefreshToken refreshToken(
            Long id,
            Long userId,
            String tokenHash,
            String family,
            Date revokedAt
    ) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(id);
        refreshToken.setUserId(userId);
        refreshToken.setTokenHash(tokenHash);
        refreshToken.setTokenFamily(family);
        refreshToken.setExpiresAt(new Date(System.currentTimeMillis() + 60000));
        refreshToken.setRevokedAt(revokedAt);
        return refreshToken;
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte item : digest) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
