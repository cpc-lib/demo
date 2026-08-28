package cc.ivera.ratelimit.core.config;

import cc.ivera.ratelimit.core.policy.FailPolicy;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ratelimit")
public class RateLimitProperties {

    private boolean enabled = true;
    private String keyPrefix = "rl:";
    private FailPolicy failPolicy = FailPolicy.OPEN;
    private String defaultKey = "'default:' + #methodName";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getKeyPrefix() { return keyPrefix; }
    public void setKeyPrefix(String keyPrefix) { this.keyPrefix = keyPrefix; }

    public FailPolicy getFailPolicy() { return failPolicy; }
    public void setFailPolicy(FailPolicy failPolicy) { this.failPolicy = failPolicy; }

    public String getDefaultKey() { return defaultKey; }
    public void setDefaultKey(String defaultKey) { this.defaultKey = defaultKey; }
}
