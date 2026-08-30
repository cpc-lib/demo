package cc.ivera.controller;

import cc.ivera.config.AuthProperties;
import cc.ivera.dto.LoginRequest;
import cc.ivera.enums.UserRole;
import cc.ivera.exception.ForbiddenException;
import cc.ivera.exception.UnauthorizedException;
import cc.ivera.handler.GlobalExceptionHandler;
import cc.ivera.security.AuthUser;
import cc.ivera.service.AuthService;
import cc.ivera.vo.AuthSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private AuthService authService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        AuthProperties properties = new AuthProperties();
        properties.setAccessTokenSeconds(900);
        properties.setRefreshTokenSeconds(604800);
        properties.setSecureCookie(false);
        AuthController controller = new AuthController(authService, properties);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void loginReturnsAccessTokenAndKeepsRefreshTokenOnlyInHttpOnlyCookie() throws Exception {
        AuthUser user = new AuthUser(3L, "alice", UserRole.USER);
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(new AuthSession("access-token", 900, user, "raw-refresh-token"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"StrongPass1!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(content().string(not(containsString("raw-refresh-token"))))
                .andExpect(header().string("Set-Cookie", containsString("payment_refresh_token=raw-refresh-token")))
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
                .andExpect(header().string("Set-Cookie", containsString("SameSite=Lax")))
                .andExpect(header().string("Set-Cookie", containsString("Path=/api/auth")));
    }

    @Test
    void refreshWithoutCookieReturnsHttp401WithExistingResponseEnvelope() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("刷新令牌不存在"));
    }

    @Test
    void refreshRejectsAnUntrustedBrowserOriginBeforeUsingCookie() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .header("Origin", "https://malicious.example")
                        .cookie(new javax.servlet.http.Cookie("payment_refresh_token", "refresh-token")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("刷新请求来源不受信任"));

        verify(authService, never()).refresh("refresh-token");
    }

    @Test
    void logoutPassesCookieToServiceAndExpiresIt() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .cookie(new javax.servlet.http.Cookie("payment_refresh_token", "logout-token")))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));

        verify(authService).logout("logout-token");
    }

    @Test
    void unauthorizedAndForbiddenUseHttpStatusAndRFields() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        assertEquals(401, handler.handleUnauthorized(new UnauthorizedException("请先登录")).getStatusCodeValue());
        assertEquals(403, handler.handleForbidden(new ForbiddenException("禁止访问")).getStatusCodeValue());
        assertEquals(401, handler.handleUnauthorized(new UnauthorizedException("请先登录")).getBody().getCode());
        assertEquals(403, handler.handleForbidden(new ForbiddenException("禁止访问")).getBody().getCode());
    }
}
