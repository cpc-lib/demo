package com.example.vocab.config.quality;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.quality")
public class QualityProperties {
    private Boolean rateLimitEnabled = true;
    private Long rateLimitPerMinute = 60L;
    private Boolean idempotencyEnabled = true;
    private Long idempotencyExpireSeconds = 300L;
}
