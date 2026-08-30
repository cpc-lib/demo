package cc.ivera.security;

import cc.ivera.config.AuthProperties;
import cc.ivera.enums.UserRole;
import cc.ivera.exception.ForbiddenException;
import cc.ivera.exception.UnauthorizedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthInterceptorTest {

    private final JwtTokenService tokenService = new JwtTokenService(properties());
    private final AuthInterceptor authInterceptor = new AuthInterceptor(tokenService);
    private final AdminInterceptor adminInterceptor = new AdminInterceptor();

    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    @Test
    void missingBearerTokenIsUnauthorized() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThrows(UnauthorizedException.class,
                () -> authInterceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
    }

    @Test
    void malformedBearerTokenIsUnauthorized() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic abc");

        assertThrows(UnauthorizedException.class,
                () -> authInterceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
    }

    @Test
    void validBearerTokenPopulatesAndThenClearsRequestContext() throws Exception {
        MockHttpServletRequest request = bearerRequest(UserRole.USER);

        assertTrue(authInterceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
        assertEquals(11L, AuthContext.requireUser().getUserId());
        assertEquals(UserRole.USER, AuthContext.requireUser().getRole());

        authInterceptor.afterCompletion(request, new MockHttpServletResponse(), new Object(), null);
        assertNull(AuthContext.getUser());
    }

    @Test
    void userRoleCannotEnterAdminRoute() throws Exception {
        authInterceptor.preHandle(bearerRequest(UserRole.USER), new MockHttpServletResponse(), new Object());

        assertThrows(ForbiddenException.class,
                () -> adminInterceptor.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), new Object()));
    }

    @Test
    void adminRoleCanEnterAdminRoute() throws Exception {
        authInterceptor.preHandle(bearerRequest(UserRole.ADMIN), new MockHttpServletResponse(), new Object());

        assertTrue(adminInterceptor.preHandle(
                new MockHttpServletRequest(), new MockHttpServletResponse(), new Object()));
    }

    private MockHttpServletRequest bearerRequest(UserRole role) {
        AuthUser user = new AuthUser(11L, "tester", role);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + tokenService.createAccessToken(user));
        return request;
    }

    private static AuthProperties properties() {
        AuthProperties properties = new AuthProperties();
        properties.setJwtSecret("0123456789abcdef0123456789abcdef");
        properties.setAccessTokenSeconds(900);
        return properties;
    }
}
