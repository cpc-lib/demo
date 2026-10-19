package com.example.scanlogin.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {
    /** Access tokens are short-lived; refresh tokens outlive them and can only mint new pairs. */
    private static final java.util.Set<String> KINDS = java.util.Set.of("pc", "mobile", "pc-refresh", "mobile-refresh");
    private final SecretKey key;
    private final String issuer;

    public JwtService(@Value("${app.jwt.secret-base64}") String base64,
                      @Value("${app.jwt.issuer}") String issuer) {
        if (base64 == null || base64.isBlank())
            throw new IllegalArgumentException("JWT_SECRET_BASE64 must be set; generate 32 random bytes and Base64-encode them");
        byte[] bytes = Base64.getDecoder().decode(base64);
        if (bytes.length < 32) throw new IllegalArgumentException("JWT secret must be at least 256 bits");
        this.key = Keys.hmacShaKeyFor(bytes);
        this.issuer = issuer;
    }

    public String issue(long userId, String kind, long tokenVersion) {
        if (!KINDS.contains(kind)) throw new IllegalArgumentException("Invalid token kind");
        Instant now = Instant.now();
        Instant expires = switch (kind) {
            case "pc-refresh", "mobile-refresh" -> now.plus(7, ChronoUnit.DAYS);
            default -> now.plus(30, ChronoUnit.MINUTES);
        };
        // "ver" binds the token to the user's session version: bumping the version
        // (kick) instantly invalidates every token issued before it.
        return Jwts.builder().issuer(issuer).subject(Long.toString(userId))
                .id(UUID.randomUUID().toString()).claim("kind", kind).claim("ver", tokenVersion)
                .issuedAt(Date.from(now)).expiration(Date.from(expires))
                .signWith(key).compact();
    }

    public Identity verify(String token) {
        Claims claims = Jwts.parser().verifyWith(key).requireIssuer(issuer)
                .build().parseSignedClaims(token).getPayload();
        String kind = claims.get("kind", String.class);
        if (!KINDS.contains(kind)) throw new IllegalArgumentException("Invalid JWT kind");
        long userId = Long.parseLong(claims.getSubject());
        if (userId <= 0) throw new IllegalArgumentException("Invalid JWT subject");
        Object ver = claims.get("ver");
        long tokenVersion = ver instanceof Number number ? number.longValue() : -1L;
        return new Identity(userId, kind, tokenVersion);
    }

    /** Expiration of a signed token; mirrors the JWT lifetime into the refresh table. */
    public Instant expiresAt(String token) {
        return Jwts.parser().verifyWith(key).requireIssuer(issuer)
                .build().parseSignedClaims(token).getPayload().getExpiration().toInstant();
    }

    public record Identity(long userId, String kind, long tokenVersion) {
    }
}
