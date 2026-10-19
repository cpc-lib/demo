package com.example.scanlogin.application;

import com.example.scanlogin.infrastructure.RefreshTokenRepository;
import com.example.scanlogin.infrastructure.UserRepository;
import com.example.scanlogin.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Persistent side of the dual-token scheme. Refresh JWTs are tracked in the
 * refresh_token table so they can be rotated on every use and revoked on
 * logout; the JWT signature remains the first validation layer.
 */
@Service
public class RefreshTokenService {
    public static final String PC = "PC";
    public static final String MOBILE = "MOBILE";

    private final RefreshTokenRepository repo;
    private final JwtService jwt;
    private final UserRepository users;

    public RefreshTokenService(RefreshTokenRepository repo, JwtService jwt, UserRepository users) {
        this.repo = repo;
        this.jwt = jwt;
        this.users = users;
    }

    /** Records a freshly issued refresh JWT; only its SHA-256 digest is stored. */
    public void persist(long userId, String client, String refreshToken) {
        repo.create(userId, hash(refreshToken), client, jwt.expiresAt(refreshToken));
    }

    /**
     * Verifies signature, kind, database presence, revocation state, client and owner.
     * Reuse of an already-rotated token suggests theft and revokes the whole family.
     */
    public JwtService.Identity requireActive(String refreshToken, String expectedKind, String client) {
        if (refreshToken == null || refreshToken.isBlank()) throw unauthorized("Missing refresh token");
        JwtService.Identity identity;
        try {
            identity = jwt.verify(refreshToken);
        } catch (RuntimeException ex) {
            throw unauthorized("Invalid refresh token");
        }
        if (!expectedKind.equals(identity.kind())) throw unauthorized("Invalid refresh token");
        RefreshTokenRepository.Row row = repo.find(hash(refreshToken))
                .orElseThrow(() -> unauthorized("Refresh token revoked"));
        if (row.revoked()) {
            repo.revokeAllActive(identity.userId(), client);
            throw unauthorized("Refresh token revoked");
        }
        if (row.expiresAt().isBefore(Instant.now()) || !client.equals(row.client())
                || row.userId() != identity.userId()
                || users.tokenVersion(identity.userId()) != identity.tokenVersion())
            throw unauthorized("Invalid refresh token");
        return identity;
    }

    /** Revokes the used token after its replacement has been issued. */
    public void rotate(String oldRefreshToken) {
        repo.revoke(hash(oldRefreshToken));
    }

    /** Logout: revocation by raw token; unknown tokens are ignored. */
    public void revoke(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) repo.revoke(hash(refreshToken));
    }

    /** Kick: revoke every live refresh token of the user on all clients. */
    public void revokeAllForUser(long userId) {
        repo.revokeAllActiveForUser(userId);
    }

    private static ResponseStatusException unauthorized(String message) {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
    }

    private static String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }
}
