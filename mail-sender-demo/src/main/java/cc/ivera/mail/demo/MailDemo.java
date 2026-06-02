package cc.ivera.mail.demo;

import cc.ivera.mail.config.MailProperties;
import cc.ivera.mail.model.AttachmentSource;
import cc.ivera.mail.model.MailMessage;
import cc.ivera.mail.service.MailSender;
import cc.ivera.mail.service.SmtpMailSender;

import java.util.ArrayList;
import java.util.List;

/**
 * 运行前请替换为你自己的 SMTP 配置。
 */
public class MailDemo {

    public static void main(String[] args) throws Exception {
        MailProperties properties = new MailProperties()
                .setHost("smtp.qq.com")
                .setPort(25)
                .setFromAddress("2607439502@qq.com")
                .setFromName("订单中心")
                .setUsername("2607439502@qq.com")
                .setPassword("hqovxyrpwanreabb")
                .setAuth(true)
                .setStartTlsEnabled(true)
                .setSslEnabled(false)
                .setDebug(true);

        List<String> bcc = new ArrayList(2);
        bcc.add("knife4j@gmail.com");
        bcc.add("hejiaming.work@outlook.com");

        MailMessage message = new MailMessage()
                .subject("测试邮件：支持抄送、密送、附件")
                .content("""
                        <h3>您好，这是一封测试邮件</h3>
                        <p>这封邮件支持：</p>
                        <ul>
                            <li>To</li>
                            <li>Cc</li>
                            <li>Bcc</li>
                            <li>本地附件</li>
                            <li>远程 URL 附件</li>
                        </ul>
                        """)
                .html(true)
                .addTo("bytedance4j@163.com")
                .addCc("bytedance4j@gmail.com")
                .addBcc(bcc)
                .addAttachment(AttachmentSource.fromLocalFile("D:/AI/Agent.pdf"))
                .addAttachment(AttachmentSource.fromRemoteUrl("https://pic.rmb.bdstatic.com/bjh/news/3fe6db1a8d291be39192f9a06c74ce99.png"));

        MailSender mailSender = new SmtpMailSender(properties);
        mailSender.send(message);

        System.out.println("邮件发送成功");
    }
}
