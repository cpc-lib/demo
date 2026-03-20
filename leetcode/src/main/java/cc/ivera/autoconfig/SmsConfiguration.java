package cc.ivera.autoconfig;


import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import cc.ivera.properties.SmsProperties;
import cc.ivera.template.SmsTemplate;

@EnableConfigurationProperties({
})
public class SmsConfiguration {
    @Bean
    public SmsTemplate smsTemplate(SmsProperties smsProperties) {
        return new SmsTemplate(smsProperties);
    }
}
