package cc.ivera.security;

import cc.ivera.config.AuthProperties;
import cc.ivera.enums.UserRole;
import cc.ivera.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtTokenServiceTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    @Test
    void createsAndParsesAccessTokenWithUserIdentityAndRole() {
        JwtTokenService tokenService = new JwtTokenService(properties(900));

        String token = tokenService.createAccessToken(new AuthUser(7L, "alice", UserRole.USER));
        AuthUser authUser = tokenService.parseAccessToken(token);

        assertEquals(7L, authUser.getUserId());
        assertEquals("alice", authUser.getUsername());
        assertEquals(UserRole.USER, authUser.getRole());
    }

    @Test
    void rejectsExpiredAccessToken() {
        JwtTokenService tokenService = new JwtTokenService(properties(-1));
        String token = tokenService.createAccessToken(new AuthUser(8L, "admin", UserRole.ADMIN));

        assertThrows(UnauthorizedException.class, () -> tokenService.parseAccessToken(token));
    }

    @Test
    void rejectsTokenSignedByAnotherSecret() {
        JwtTokenService issuer = new JwtTokenService(properties(900));
        AuthProperties verifierProperties = properties(900);
        verifierProperties.setJwtSecret("abcdef0123456789abcdef0123456789");
        JwtTokenService verifier = new JwtTokenService(verifierProperties);
        String token = issuer.createAccessToken(new AuthUser(9L, "bob", UserRole.USER));

        assertThrows(UnauthorizedException.class, () -> verifier.parseAccessToken(token));
    }

    private AuthProperties properties(long accessTokenSeconds) {
        AuthProperties properties = new AuthProperties();
        properties.setJwtSecret(SECRET);
        properties.setAccessTokenSeconds(accessTokenSeconds);
        return properties;
    }
}
