package cc.ivera.config;

import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;



@Configuration
public class EmailToLongConfig {

    @PostConstruct
    private void init() {
        // 解决邮件附件名称太长会自动截取，导致附件变成.bin格式问题
        System.setProperty("mail.mime.splitlongparameters", "false");
    }

}
