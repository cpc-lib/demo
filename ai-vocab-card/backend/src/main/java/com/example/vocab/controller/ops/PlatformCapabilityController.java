package com.example.vocab.controller.ops;

import com.example.vocab.config.search.SearchProperties;
import com.example.vocab.config.search.VectorProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api/ops/capabilities")
@RequiredArgsConstructor
public class PlatformCapabilityController {
    private final SearchProperties searchProperties;
    private final VectorProperties vectorProperties;
    private final Environment env;

    @GetMapping
    public Map<String, Object> capabilities() {
        return Map.of(
                "version", "V5 Production Enhanced",
                "keywordSearch", searchProperties.getProvider(),
                "vectorSearch", vectorProperties.getProvider(),
                "outbox", true,
                "rateLimit", env.getProperty("app.quality.rate-limit-enabled", "true"),
                "idempotency", env.getProperty("app.quality.idempotency-enabled", "true"),
                "observability", "spring-boot-actuator health/info/metrics"
        );
    }
}
