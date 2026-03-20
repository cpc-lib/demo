package cc.ivera.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(value = "email.smtp")
public class SmtpProperties {

    /**
     * smtp服务地址
     */
    private String host;

    /**
     * 发件人邮箱
     */
    private String email;

    /**
     * 发件人昵称
     */
    private String name;

    /**
     * 邮箱授权码
     */
    private String password;

    /**
     * 邮箱服务端口
     */
    private Integer port=25;
}
