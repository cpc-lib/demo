package cc.ivera.gray.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cc.ivera.gray.admin.security.JwtService;
import cc.ivera.gray.admin.security.TokenPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class JwtServiceTest {
    @Test
    void shouldIssueAndVerifyToken() {
        JwtService jwtService = new JwtService(new ObjectMapper(), "gray-release-platform-test-secret-32bytes", 3600);

        String token = jwtService.issue("admin", List.of("ADMIN"));
        TokenPrincipal principal = jwtService.verify(token);

        assertEquals("admin", principal.username());
        assertEquals(List.of("ADMIN"), principal.roles());
    }
}

