package com.example.scanlogin;

import com.example.scanlogin.application.CaptchaService;
import com.example.scanlogin.application.QrLoginService;
import com.example.scanlogin.application.UserAccountService;
import com.example.scanlogin.infrastructure.CaptchaStore;
import com.example.scanlogin.infrastructure.RedisQrStore;
import com.example.scanlogin.infrastructure.UserRepository;
import com.example.scanlogin.security.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class QrFlowIntegrationTest {
    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("scan_login").withUsername("scan").withPassword("test_password");
    @Container
    static final GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine")).withExposedPorts(6379);
    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper mapper;
    @Autowired
    RedisQrStore store;
    @Autowired
    JwtService jwt;
    @Autowired
    QrLoginService qr;
    @Autowired
    CaptchaService captcha;
    @Autowired
    CaptchaStore captchaStore;
    @Autowired
    UserAccountService accounts;
    @Autowired
    UserRepository users;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.sql.init.mode", () -> "always");
        registry.add("spring.sql.init.schema-locations", () -> "classpath:test-schema.sql");
        registry.add("app.jwt.secret-base64", () -> "W7mFLO2UP6HPtwN9bnTs11NHJEBOeSQCLoP6lUL6Xdc=");
    }

    @Test
    void unknownQrAndUntrustedRequestsAreRejected() throws Exception {
        String unknownTicket = qr.randomSecret();
        String payload = mapper.writeValueAsString(java.util.Map.of("ticket", unknownTicket));
        mvc.perform(get("/api/auth/qr/status").param("ticket", unknownTicket))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("EXPIRED"));
        // Ticket endpoints are anonymous capabilities: no CSRF token required anymore.
        mvc.perform(post("/api/auth/qr/create")).andExpect(status().isOk());
        mvc.perform(post("/api/auth/qr/consume").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isGone());
        mvc.perform(post("/api/auth/qr/consume").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isGone());
        mvc.perform(post("/api/auth/qr/scan").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void snapshotHasCoherentFields() {
        String ticket = qr.randomSecret();
        String binding = QrLoginService.hash("browser-for-snapshot");
        store.create(ticket, binding, "987654", 120);
        RedisQrStore.Snapshot snapshot = store.snapshot(ticket);
        assertThat(snapshot).isNotNull();
        assertThat(snapshot.status()).isEqualTo("WAIT_SCAN");
        assertThat(snapshot.bindingHash()).isEqualTo(binding);
        assertThat(snapshot.code()).isEqualTo("987654");
    }

    @Test
    void endToEndAndNoReplay() throws Exception {
        String username = "test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        mvc.perform(post("/api/mobile/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerWithCaptcha(username)))
                .andExpect(status().isOk());
        String loginBody = mvc.perform(post("/api/mobile/login")
                        .contentType(MediaType.APPLICATION_JSON).content(loginWithCaptcha(username)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode login = mapper.readTree(loginBody);
        String mobileJwt = login.get("accessToken").asText();
        String mobileRefresh = login.get("refreshToken").asText();
        assertThat(jwt.verify(mobileJwt).kind()).isEqualTo("mobile");
        assertThat(jwt.verify(mobileRefresh).kind()).isEqualTo("mobile-refresh");
        mvc.perform(get("/api/mobile/me").header("Authorization", "Bearer " + mobileJwt))
                .andExpect(status().isOk());
        JsonNode refreshed = mapper.readTree(mvc.perform(post("/api/mobile/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(java.util.Map.of("refreshToken", mobileRefresh))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        mvc.perform(get("/api/mobile/me").header("Authorization", "Bearer " + refreshed.path("accessToken").asText()))
                .andExpect(status().isOk());
        // Mobile refresh tokens are single-use as well: the rotated one is dead.
        mvc.perform(post("/api/mobile/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(java.util.Map.of("refreshToken", mobileRefresh))))
                .andExpect(status().isUnauthorized());
        var create = mvc.perform(post("/api/auth/qr/create").with(csrf()))
                .andExpect(status().isOk()).andReturn().getResponse();
        Cookie browser = create.getCookie("qr_browser");
        assertThat(browser).isNotNull();
        JsonNode created = mapper.readTree(create.getContentAsString());
        String ticket = created.get("ticket").asText();
        String payload = mapper.writeValueAsString(java.util.Map.of("ticket", ticket));
        mvc.perform(get("/api/auth/qr/status").param("ticket", ticket).cookie(browser))
                .andExpect(jsonPath("$.status").value("WAIT_SCAN"));
        mvc.perform(post("/api/auth/qr/scan").header("Authorization", "Bearer " + mobileJwt)
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(jsonPath("$.status").value("SCANNED"));
        mvc.perform(post("/api/auth/qr/confirm").header("Authorization", "Bearer " + mobileJwt)
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
        mvc.perform(post("/api/auth/qr/consume").with(csrf())
                .cookie(new Cookie("qr_browser", "wrong-browser"))
                .contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isForbidden());
        var consumed = mvc.perform(post("/api/auth/qr/consume").with(csrf()).cookie(browser)
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk()).andReturn().getResponse();
        Cookie pc = consumed.getCookie("PC_ACCESS");
        assertThat(pc).isNotNull();
        mvc.perform(get("/api/auth/me").cookie(pc)).andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username));
        mvc.perform(post("/api/auth/qr/consume").with(csrf()).cookie(browser)
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/auth/qr/scan").header("Authorization", "Bearer " + mobileJwt)
                .contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isForbidden());
    }

    @Test
    void pcPasswordLoginRequiresCaptchaAndIssuesRefreshableTokens() throws Exception {
        String username = "test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        mvc.perform(post("/api/mobile/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerWithCaptcha(username)))
                .andExpect(status().isOk());
        // A wrong or stale captcha is rejected before credentials are ever checked.
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(java.util.Map.of("username", username,
                                "password", "StrongPass123!", "captchaId", "missing", "captchaCode", "ABCD"))))
                .andExpect(status().isBadRequest());
        var challenge = captcha.issue();
        var login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(java.util.Map.of("username", username,
                                "password", "StrongPass123!", "captchaId", challenge.captchaId(),
                                "captchaCode", captchaStore.peek(challenge.captchaId())))))
                .andExpect(status().isOk()).andReturn().getResponse();
        Cookie access = login.getCookie("PC_ACCESS");
        Cookie refresh = login.getCookie("PC_REFRESH");
        assertThat(access).isNotNull();
        assertThat(refresh).isNotNull();
        assertThat(jwt.verify(access.getValue()).kind()).isEqualTo("pc");
        assertThat(jwt.verify(refresh.getValue()).kind()).isEqualTo("pc-refresh");
        mvc.perform(get("/api/auth/me").cookie(access)).andExpect(status().isOk());
        // The refresh cookie alone mints a fresh, usable access token.
        var rotated = mvc.perform(post("/api/auth/refresh").cookie(refresh))
                .andExpect(status().isOk()).andReturn().getResponse();
        Cookie rotatedAccess = rotated.getCookie("PC_ACCESS");
        assertThat(rotatedAccess).isNotNull();
        mvc.perform(get("/api/auth/me").cookie(rotatedAccess)).andExpect(status().isOk());
        Cookie rotatedRefresh = rotated.getCookie("PC_REFRESH");
        assertThat(rotatedRefresh).isNotNull();
        // Replaying the rotated token signals theft: the whole token family dies,
        // including the replacement, forcing a fresh login.
        mvc.perform(post("/api/auth/refresh").cookie(refresh))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/refresh").cookie(rotatedRefresh))
                .andExpect(status().isUnauthorized());
        // A refresh token cannot act as an access token (wrong kind -> unauthenticated).
        mvc.perform(get("/api/auth/me").cookie(refresh)).andExpect(status().isUnauthorized());
        // Spent captchas cannot be replayed.
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(java.util.Map.of("username", username,
                                "password", "StrongPass123!", "captchaId", challenge.captchaId(), "captchaCode", "ABCD"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void kickedUserLosesAccessAndRefreshOnBothClients() throws Exception {
        String username = "test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        mvc.perform(post("/api/mobile/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerWithCaptcha(username)))
                .andExpect(status().isOk());
        String pcIp = "X-Forwarded-For", kickIp = "203.0.113.190";
        var pcLogin = mvc.perform(post("/api/auth/login").header(pcIp, kickIp)
                        .contentType(MediaType.APPLICATION_JSON).content(loginWithCaptcha(username)))
                .andExpect(status().isOk()).andReturn().getResponse();
        Cookie pcAccess = pcLogin.getCookie("PC_ACCESS");
        Cookie pcRefresh = pcLogin.getCookie("PC_REFRESH");
        JsonNode mobileLogin = mapper.readTree(mvc.perform(post("/api/mobile/login").header(pcIp, kickIp)
                        .contentType(MediaType.APPLICATION_JSON).content(loginWithCaptcha(username)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        String mobileAccess = mobileLogin.get("accessToken").asText();
        String mobileRefresh = mobileLogin.get("refreshToken").asText();
        long userId = users.findByUsername(username).orElseThrow().id();
        mvc.perform(get("/api/auth/me").cookie(pcAccess)).andExpect(status().isOk());
        mvc.perform(get("/api/mobile/me").header("Authorization", "Bearer " + mobileAccess))
                .andExpect(status().isOk());
        accounts.kickUser(userId);
        // Unexpired access tokens die instantly thanks to the session-version check.
        mvc.perform(get("/api/auth/me").cookie(pcAccess)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/mobile/me").header("Authorization", "Bearer " + mobileAccess))
                .andExpect(status().isUnauthorized());
        // Refresh tokens were revoked too, so no session can renew itself.
        mvc.perform(post("/api/auth/refresh").cookie(pcRefresh)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/mobile/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(java.util.Map.of("refreshToken", mobileRefresh))))
                .andExpect(status().isUnauthorized());
        // A fresh login recovers with the bumped version.
        var relogin = mvc.perform(post("/api/auth/login").header(pcIp, kickIp)
                        .contentType(MediaType.APPLICATION_JSON).content(loginWithCaptcha(username)))
                .andExpect(status().isOk()).andReturn().getResponse();
        mvc.perform(get("/api/auth/me").cookie(relogin.getCookie("PC_ACCESS"))).andExpect(status().isOk());
    }

    @Test
    void adminKickRequiresKeyAndKillsSessions() throws Exception {
        String username = "test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        mvc.perform(post("/api/mobile/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerWithCaptcha(username)))
                .andExpect(status().isOk());
        var login = mvc.perform(post("/api/auth/login").header("X-Forwarded-For", "203.0.113.191")
                        .contentType(MediaType.APPLICATION_JSON).content(loginWithCaptcha(username)))
                .andExpect(status().isOk()).andReturn().getResponse();
        Cookie access = login.getCookie("PC_ACCESS");
        long userId = users.findByUsername(username).orElseThrow().id();

        // Missing or wrong admin key -> forbidden.
        mvc.perform(get("/api/admin/users")).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/users").header("X-Admin-Key", "nope")).andExpect(status().isForbidden());
        // Correct key kicks the user server-side (POST carries the CSRF token).
        mvc.perform(post("/api/admin/users/" + userId + "/kick").with(csrf())
                        .header("X-Admin-Key", "dev-admin-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("KICKED"));
        // The kicked session is dead despite its unexpired access token.
        mvc.perform(get("/api/auth/me").cookie(access)).andExpect(status().isUnauthorized());
        // The console listing reflects the bumped version and revoked tokens.
        mvc.perform(get("/api/admin/users").header("X-Admin-Key", "dev-admin-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.username=='" + username + "')].tokenVersion").value(1))
                .andExpect(jsonPath("$[?(@.username=='" + username + "')].activeRefreshTokens").value(0));
        // Unknown users 404.
        mvc.perform(post("/api/admin/users/999999/kick").with(csrf())
                        .header("X-Admin-Key", "dev-admin-key"))
                .andExpect(status().isNotFound());
    }

    @Test
    void logoutRevokesTheStoredRefreshToken() throws Exception {
        String username = "test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        mvc.perform(post("/api/mobile/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerWithCaptcha(username)))
                .andExpect(status().isOk());
        var login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginWithCaptcha(username)))
                .andExpect(status().isOk()).andReturn().getResponse();
        Cookie refresh = login.getCookie("PC_REFRESH");
        assertThat(refresh).isNotNull();
        mvc.perform(post("/api/auth/logout").cookie(refresh)).andExpect(status().isOk());
        // The revoked token no longer mints pairs.
        mvc.perform(post("/api/auth/refresh").cookie(refresh))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginEndpointsAreRateLimitedPerClientIp() throws Exception {
        String body = mapper.writeValueAsString(java.util.Map.of("username", "nobody",
                "password", "WrongPass123!", "captchaId", "missing", "captchaCode", "ABCD"));
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/auth/login").header("X-Forwarded-For", "203.0.113.77")
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        // The sixth attempt from the same IP is throttled before any validation.
        mvc.perform(post("/api/auth/login").header("X-Forwarded-For", "203.0.113.77")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isTooManyRequests());
        // Another IP keeps its own budget.
        mvc.perform(post("/api/auth/login").header("X-Forwarded-For", "203.0.113.78")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        // Both login endpoints share the same per-IP bucket.
        mvc.perform(post("/api/mobile/login").header("X-Forwarded-For", "203.0.113.77")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isTooManyRequests());
    }

    private String loginWithCaptcha(String username) throws Exception {
        var challenge = captcha.issue();
        return mapper.writeValueAsString(java.util.Map.of(
                "username", username, "password", "StrongPass123!",
                "captchaId", challenge.captchaId(),
                "captchaCode", captchaStore.peek(challenge.captchaId())));
    }

    private String registerWithCaptcha(String username) throws Exception {
        var challenge = captcha.issue();
        return mapper.writeValueAsString(java.util.Map.of(
                "username", username, "password", "StrongPass123!",
                "captchaId", challenge.captchaId(),
                "captchaCode", captchaStore.peek(challenge.captchaId())));
    }

    @Test
    void registerRequiresValidCaptcha() throws Exception {
        String username = "test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        // Missing captcha is rejected before credentials are ever checked.
        mvc.perform(post("/api/mobile/register").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(java.util.Map.of(
                                "username", username, "password", "StrongPass123!",
                                "captchaId", "missing", "captchaCode", "ABCD"))))
                .andExpect(status().isBadRequest());
        // A valid captcha lets the registration through.
        mvc.perform(post("/api/mobile/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerWithCaptcha(username)))
                .andExpect(status().isOk());
        // Each captcha is single-use: replaying it is rejected.
        mvc.perform(post("/api/mobile/register").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(java.util.Map.of(
                                "username", "test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12),
                                "password", "StrongPass123!",
                                "captchaId", "replayed", "captchaCode", "ABCD"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void concurrentConsumesHaveExactlyOneWinner() throws Exception {
        String ticket = UUID.randomUUID().toString();
        String binding = QrLoginService.hash("browser-test");
        store.create(ticket, binding, "123456", 120);
        assertThat(store.scan(ticket, 1001L)).isEqualTo("SCANNED");
        assertThat(store.confirm(ticket, 1001L)).isEqualTo("CONFIRMED");
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger winners = new AtomicInteger();
        try (var pool = Executors.newFixedThreadPool(16)) {
            List<Callable<Void>> jobs = new ArrayList<>();
            for (int i = 0; i < 48; i++) {
                jobs.add(() -> {
                    start.await(10, TimeUnit.SECONDS);
                    if ("CONSUMED".equals(store.consume(ticket, binding, 1001L))) winners.incrementAndGet();
                    return null;
                });
            }
            var futures = new ArrayList<java.util.concurrent.Future<Void>>();
            for (var job : jobs) futures.add(pool.submit(job));
            start.countDown();
            for (var future : futures) future.get(20, TimeUnit.SECONDS);
        }
        assertThat(winners.get()).isEqualTo(1);
        assertThat(store.field(ticket, "status")).isEqualTo("CONSUMED");
    }

    @Test
    void wrongMobileCannotConfirmOrHijack() {
        String ticket = UUID.randomUUID().toString();
        store.create(ticket, QrLoginService.hash("pc"), "654321", 120);
        assertThat(store.scan(ticket, 10)).isEqualTo("SCANNED");
        assertThat(store.scan(ticket, 11)).isEqualTo("FORBIDDEN");
        assertThat(store.confirm(ticket, 11)).isEqualTo("FORBIDDEN");
        assertThat(store.confirm(ticket, 10)).isEqualTo("CONFIRMED");
        assertThat(store.consume(ticket, QrLoginService.hash("wrong-pc"), 10)).isEqualTo("FORBIDDEN");
        assertThat(store.consume(ticket, QrLoginService.hash("pc"), 10)).isEqualTo("CONSUMED");
    }
}
