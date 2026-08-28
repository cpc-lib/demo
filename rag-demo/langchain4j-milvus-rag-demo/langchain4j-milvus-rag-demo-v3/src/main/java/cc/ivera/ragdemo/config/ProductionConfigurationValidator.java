package cc.ivera.ragdemo.config;

import cc.ivera.ragdemo.service.ragops.ProductionConfigurationPolicy;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class ProductionConfigurationValidator {

    private final RagProperties properties;
    private final ProductionConfigurationPolicy policy;
    private final Environment environment;

    @PostConstruct
    public void validate() {
        List<String> violations = policy.violations(properties, productionProfileActive());
        if (!violations.isEmpty()) {
            throw new IllegalStateException("Unsafe production configuration: " + String.join("; ", violations));
        }
    }

    private boolean productionProfileActive() {
        return Arrays.stream(environment.getActiveProfiles())
                .map(profile -> profile.toLowerCase(Locale.ROOT))
                .anyMatch(profile -> "prod".equals(profile) || "production".equals(profile));
    }
}
