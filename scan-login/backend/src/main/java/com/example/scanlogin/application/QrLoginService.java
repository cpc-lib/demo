package com.example.scanlogin.application;

import com.example.scanlogin.domain.QrStatus;
import com.example.scanlogin.infrastructure.RedisQrStore;
import com.example.scanlogin.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class QrLoginService {
    private final RedisQrStore store;
    private final UserAccountService accounts;
    private final JwtService jwt;
    private final RefreshTokenService refreshTokens;
    private final SecureRandom random = new SecureRandom();
    private final int ttlSeconds;
    private final String scanUrl;

    public QrLoginService(RedisQrStore store, UserAccountService accounts, JwtService jwt,
                          RefreshTokenService refreshTokens,
                          @Value("${app.qr.ttl-seconds:120}") int ttlSeconds,
                          @Value("${app.qr.scan-url}") String scanUrl) {
        if (ttlSeconds < 1) throw new IllegalArgumentException("QR TTL must be positive");
        this.store = store;
        this.accounts = accounts;
        this.jwt = jwt;
        this.refreshTokens = refreshTokens;
        this.ttlSeconds = ttlSeconds;
        this.scanUrl = scanUrl;
    }

    public String randomSecret() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    public CreateResult create(String browserSecret) {
        String ticket = randomSecret();
        String code = "%06d".formatted(random.nextInt(1_000_000));
        store.create(ticket, hash(browserSecret), code, ttlSeconds);
        String url = scanUrl + (scanUrl.contains("?") ? "&" : "?") + "ticket=" + ticket;
        return new CreateResult(ticket, url, code, ttlSeconds);
    }

    public StatusResult status(String ticket, String browserSecret) {
        validateTicket(ticket);
        RedisQrStore.Snapshot snapshot = store.snapshot(ticket);
        if (snapshot == null) return new StatusResult(QrStatus.EXPIRED, null);
        verifyBrowser(browserSecret, snapshot.bindingHash());
        return new StatusResult(QrStatus.fromStored(snapshot.status()), snapshot.code());
    }

    public StatusResult scan(String ticket, long userId) {
        validateTicket(ticket);
        accounts.requireActive(userId);
        requireTransition(store.scan(ticket, userId), "SCANNED");
        RedisQrStore.Snapshot snapshot = store.snapshot(ticket);
        return new StatusResult(QrStatus.SCANNED, snapshot == null ? null : snapshot.code());
    }

    public StatusResult confirm(String ticket, long userId) {
        validateTicket(ticket);
        accounts.requireActive(userId);
        requireTransition(store.confirm(ticket, userId), "CONFIRMED");
        return new StatusResult(QrStatus.CONFIRMED, null);
    }

    public ConsumeResult consume(String ticket, String browserSecret) {
        validateTicket(ticket);
        RedisQrStore.Snapshot snapshot = store.snapshot(ticket);
        if (snapshot == null) throw new ResponseStatusException(HttpStatus.GONE, "QR expired");
        verifyBrowser(browserSecret, snapshot.bindingHash());
        if (snapshot.userId() == null || snapshot.userId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "QR not confirmed");
        }
        long userId = Long.parseLong(snapshot.userId());
        accounts.requireActive(userId);
        // Prepare JWTs before changing the atomic Redis state.
        long version = accounts.tokenVersionOf(userId);
        String accessToken = jwt.issue(userId, "pc", version);
        String refreshToken = jwt.issue(userId, "pc-refresh", version);
        refreshTokens.persist(userId, RefreshTokenService.PC, refreshToken);
        requireTransition(store.consume(ticket, hash(browserSecret), userId), "CONSUMED");
        return new ConsumeResult(accessToken, refreshToken, userId);
    }

    private static void validateTicket(String ticket) {
        if (ticket == null || !ticket.matches("[A-Za-z0-9_-]{43}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid QR ticket");
        }
    }

    private static void verifyBrowser(String secret, String expectedHash) {
        if (secret == null || expectedHash == null ||
                !MessageDigest.isEqual(hash(secret).getBytes(StandardCharsets.US_ASCII),
                        expectedHash.getBytes(StandardCharsets.US_ASCII))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Browser binding mismatch");
        }
    }

    private static void requireTransition(String actual, String expected) {
        if (expected.equals(actual)) return;
        if ("EXPIRED".equals(actual)) throw new ResponseStatusException(HttpStatus.GONE, "QR expired");
        if ("FORBIDDEN".equals(actual)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "QR denied");
        throw new ResponseStatusException(HttpStatus.CONFLICT, "QR state: " + actual);
    }

    public record CreateResult(String ticket, String qrUrl, String verificationCode, int expiresIn) {}
    public record StatusResult(QrStatus status, String verificationCode) {}
    public record ConsumeResult(String accessToken, String refreshToken, long userId) {}
}
