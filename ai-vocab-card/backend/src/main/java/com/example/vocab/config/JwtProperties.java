package com.example.vocab.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    private String secret = "ai-vocabulary-studio-v3-change-me-change-me-32bytes";
    private int expireHours = 168;
}
