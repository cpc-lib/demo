package cc.ivera.ragdemo.security;

import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.tenant.AuthenticatedIdentity;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LocalJwtServiceTest {

    @Test
    void issuedTokenCanBeDecodedBySpringJwtDecoderWithConfiguredHmacSecret() {
        RagProperties properties = new RagProperties();
        String secret = "12345678901234567890123456789012";
        properties.getSecurity().getJwt().setHmacSecret(secret);
        properties.getSecurity().getJwt().setIssuer("rag-demo");
        LocalJwtService service = new LocalJwtService(properties);

        LocalJwtService.TokenIssue token = service.issue(new AuthenticatedIdentity(
                7L,
                "tenant-a",
                "alice",
                "Alice",
                List.of("TENANT_ADMIN"),
                List.of(),
                List.of(10L, 20L),
                List.of("finance"),
                "req-1",
                null,
                null,
                "local"
        ));

        Jwt decoded = NimbusJwtDecoder.withSecretKey(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
                .macAlgorithm(MacAlgorithm.HS256)
                .build()
                .decode(token.accessToken());
        assertThat(decoded.getSubject()).isEqualTo("alice");
        assertThat(decoded.getClaimAsString("name")).isEqualTo("Alice");
        assertThat(decoded.getClaimAsString("tenant_external_id")).isEqualTo("tenant-a");
        assertThat(decoded.getClaimAsStringList("roles")).containsExactly("TENANT_ADMIN");
        assertThat(decoded.getClaimAsStringList("knowledge_base_ids")).containsExactly("10", "20");
        assertThat(token.expiresAt()).isAfter(token.issuedAt());
    }

    @Test
    void platformAdminTokenOmitsTenantIdClaim() {
        RagProperties properties = new RagProperties();
        String secret = "12345678901234567890123456789012";
        properties.getSecurity().getJwt().setHmacSecret(secret);
        LocalJwtService service = new LocalJwtService(properties);

        LocalJwtService.TokenIssue token = service.issue(new AuthenticatedIdentity(
                null,
                "platform",
                "admin",
                "Super Administrator",
                List.of("SUPER_ADMIN"),
                List.of(),
                List.of(),
                List.of(),
                "req-platform",
                null,
                null,
                "local"
        ));

        Jwt decoded = NimbusJwtDecoder.withSecretKey(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
                .macAlgorithm(MacAlgorithm.HS256)
                .build()
                .decode(token.accessToken());
        assertThat((Object) decoded.getClaim("tenant_id")).isNull();
        assertThat(decoded.getClaimAsString("tenant_external_id")).isEqualTo("platform");
        assertThat(decoded.getClaimAsStringList("roles")).containsExactly("SUPER_ADMIN");
    }
}
