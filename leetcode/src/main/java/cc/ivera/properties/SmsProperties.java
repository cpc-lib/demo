package cc.ivera.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "leetcode.sms")
@Data
public class SmsProperties {
    private String url;
    private String account;
    private String password;
}
