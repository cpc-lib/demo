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
import cc.ivera.service.AuthService;
import cc.ivera.vo.AuthSession;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;

    private final RefreshTokenMapper refreshTokenMapper;

    private final BCryptPasswordEncoder passwordEncoder;

    private final JwtTokenService tokenService;

    private final AuthProperties properties;

    private final SecureRandom secureRandom = new SecureRandom();

    public AuthServiceImpl(
            UserMapper userMapper,
            RefreshTokenMapper refreshTokenMapper,
            BCryptPasswordEncoder passwordEncoder,
            JwtTokenService tokenService,
            AuthProperties properties
    ) {
        this.userMapper = userMapper;
        this.refreshTokenMapper = refreshTokenMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.properties = properties;
    }

    @Override
    @Transactional
    public AuthSession register(RegisterRequest request) {
        String username = request.getUsername().trim();
        if (findUserByUsername(username) != null) {
            throw new BizException("用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.USER);
        userMapper.insert(user);
        return createSession(user, UUID.randomUUID().toString());
    }

    @Override
    @Transactional
    public AuthSession login(LoginRequest request) {
        User user = findUserByUsername(request.getUsername().trim());
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("用户名或密码错误");
        }
        return createSession(user, UUID.randomUUID().toString());
    }

    @Override
    @Transactional
    public AuthSession refresh(String rawRefreshToken) {
        RefreshToken current = findRefreshToken(rawRefreshToken);
        Date now = new Date();
        if (current == null) {
            throw new UnauthorizedException("刷新令牌无效");
        }
        if (current.getRevokedAt() != null) {
            refreshTokenMapper.revokeFamily(current.getTokenFamily(), now);
            throw new UnauthorizedException("检测到刷新令牌重复使用，请重新登录");
        }
        if (current.getExpiresAt() == null || !current.getExpiresAt().after(now)) {
            current.setRevokedAt(now);
            refreshTokenMapper.updateById(current);
            throw new UnauthorizedException("刷新令牌已过期，请重新登录");
        }

        User user = userMapper.selectById(current.getUserId());
        if (user == null) {
            refreshTokenMapper.revokeFamily(current.getTokenFamily(), now);
            throw new UnauthorizedException("用户不存在");
        }

        String replacementRawToken = generateRefreshToken();
        String replacementHash = sha256(replacementRawToken);
        current.setRevokedAt(now);
        current.setReplacedByHash(replacementHash);
        refreshTokenMapper.updateById(current);
        persistRefreshToken(user.getId(), current.getTokenFamily(), replacementHash, now);
        return buildSession(user, replacementRawToken);
    }

    @Override
    @Transactional
    public void logout(String rawRefreshToken) {
        RefreshToken current = findRefreshToken(rawRefreshToken);
        if (current != null) {
            refreshTokenMapper.revokeFamily(current.getTokenFamily(), new Date());
        }
    }

    @Override
    @Transactional
    public void changePassword(AuthUser authUser, PasswordChangeRequest request) {
        User user = userMapper.selectById(authUser.getUserId());
        if (user == null || !passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BizException("原密码错误");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);
        refreshTokenMapper.revokeAllByUser(user.getId(), new Date());
    }

    private User findUserByUsername(String username) {
        QueryWrapper<User> query = new QueryWrapper<>();
        query.eq("username", username).last("limit 1");
        return userMapper.selectOne(query);
    }

    private RefreshToken findRefreshToken(String rawRefreshToken) {
        if (!StringUtils.hasText(rawRefreshToken)) {
            return null;
        }
        return refreshTokenMapper.selectByHashForUpdate(sha256(rawRefreshToken));
    }

    private AuthSession createSession(User user, String family) {
        String rawRefreshToken = generateRefreshToken();
        persistRefreshToken(user.getId(), family, sha256(rawRefreshToken), new Date());
        return buildSession(user, rawRefreshToken);
    }

    private void persistRefreshToken(Long userId, String family, String tokenHash, Date now) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(userId);
        refreshToken.setTokenHash(tokenHash);
        refreshToken.setTokenFamily(family);
        refreshToken.setExpiresAt(new Date(now.getTime() + properties.getRefreshTokenSeconds() * 1000L));
        refreshTokenMapper.insert(refreshToken);
    }

    private AuthSession buildSession(User user, String rawRefreshToken) {
        AuthUser authUser = new AuthUser(user.getId(), user.getUsername(), user.getRole());
        return new AuthSession(
                tokenService.createAccessToken(authUser),
                properties.getAccessTokenSeconds(),
                authUser,
                rawRefreshToken
        );
    }

    private String generateRefreshToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256不可用", ex);
        }
    }
}
