package cc.ivera.security;

import cc.ivera.config.AuthProperties;
import cc.ivera.enums.UserRole;
import cc.ivera.exception.UnauthorizedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtTokenService {

    private final AuthProperties properties;

    private final SecretKey signingKey;

    public JwtTokenService(AuthProperties properties) {
        this.properties = properties;
        String secret = properties.getJwtSecret();
        if (!StringUtils.hasText(secret) || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("PAYMENT_AUTH_JWT_SECRET 至少需要32字节");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(AuthUser user) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(String.valueOf(user.getUserId()))
                .claim("username", user.getUsername())
                .claim("role", user.getRole().name())
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + properties.getAccessTokenSeconds() * 1000L))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public AuthUser parseAccessToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            Long userId = Long.valueOf(claims.getSubject());
            String username = claims.get("username", String.class);
            UserRole role = UserRole.valueOf(claims.get("role", String.class));
            return new AuthUser(userId, username, role);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new UnauthorizedException("登录状态无效或已过期");
        }
    }
}
