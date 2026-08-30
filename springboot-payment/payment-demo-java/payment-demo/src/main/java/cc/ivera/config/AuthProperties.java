package cc.ivera.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "payment.auth")
public class AuthProperties {

    private String jwtSecret;

    private long accessTokenSeconds = 900L;

    private long refreshTokenSeconds = 604800L;

    private boolean secureCookie;

    private List<String> allowedOrigins = Arrays.asList(
            "http://localhost:3000",
            "http://localhost:8081"
    );
}
