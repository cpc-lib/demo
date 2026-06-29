package com.example.vocab.security;

import com.example.vocab.config.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class JwtService {
    private final JwtProperties properties;

    public String issue(Long userId, String username) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(properties.getExpireHours() * 3600L)))
                .signWith(key())
                .compact();
    }

    public Long parseUserId(String token) {
        String subject = Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload().getSubject();
        return Long.valueOf(subject);
    }

    private SecretKey key() {
        byte[] bytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) throw new IllegalArgumentException("JWT secret must be at least 32 bytes");
        return Keys.hmacShaKeyFor(bytes);
    }
}
