package cc.ivera.util;

import com.sun.mail.util.MailSSLSocketFactory;
import lombok.AllArgsConstructor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.mail.EmailAttachment;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import cc.ivera.properties.SmtpProperties;

import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;

@Component
@AllArgsConstructor
public class EmailUtil {
    private static final Log logger = LogFactory.getLog(EmailUtil.class);

    private final SmtpProperties emailProperties;

    /**
     * 邮件发送
     *
     * @param subject       邮件主题
     * @param content       邮件内容
     * @param contentIsHtml 内容是否为html格式
     * @param toMail        收件人邮箱
     * @param ccMail        抄送人邮箱
     * @param bccMail       秘密抄送人邮箱
     * @param fileList      附件
     */
    public void sendEmail(String subject, String content, boolean contentIsHtml,
                          String[] toMail, String[] ccMail, String[] bccMail, File[] fileList)
            throws EmailException, UnsupportedEncodingException {
        HtmlEmail email = new HtmlEmail();
        // smtp服务地址
        email.setHostName(emailProperties.getHost());
        // 邮件验证
        email.setAuthentication(emailProperties.getEmail(), emailProperties.getPassword());
        // smtp端口
        email.setSmtpPort(emailProperties.getPort());
        email.setCharset("utf-8");
        // 发件人邮箱地址及昵称
        email.setFrom(emailProperties.getEmail(), emailProperties.getName());
        // 收件人邮箱
        email.addTo(toMail);
        if (!ObjectUtils.isEmpty(ccMail)) {
            // 抄送人邮箱
            email.addCc(ccMail);
        }
        if (!ObjectUtils.isEmpty(bccMail)) {
            // 秘密抄送人邮箱
            email.addBcc(bccMail);
        }
        // 主题
        email.setSubject(subject);
        if (contentIsHtml) {
            email.setHtmlMsg(content);
        } else {
            email.setMsg(content);
        }
        // 设置附件
        if (!ObjectUtils.isEmpty(fileList)) {
            for (File file : fileList) {
                EmailAttachment emailAttachment = new EmailAttachment();
                emailAttachment.setName(MimeUtility.encodeText(file.getName()));
                emailAttachment.setPath(file.getPath());
                email.attach(emailAttachment);
            }
        }
        // 发送邮件
        email.send();
        logger.info("邮件发送完成");
    }


    public static void main(String[] args) throws MessagingException, GeneralSecurityException, UnsupportedEncodingException {
        String fileName = "D:\\develop\\code\\cs-java\\leetcode\\output5.docx";
        String html = "<h1>统计数据如下所示：</h1>" +
                "<table border=\"1\">\n" +
                "  <tr>\n" +
                "    <th>月度销售额</th>\n" +
                "    <th>年度销售额</th>\n" +
                "  </tr>\n" +
                "  <tr>\n" +
                "    <td>10000</td>\n" +
                "    <td>2000000</td>\n" +
                "  </tr>\n" +
                "</table>";
        sendEmail("统计数据", html, true, "Arhi",
                "2607439502@qq.com",
                null, null,
                Collections.singletonList(fileName));
    }

    private static final String senderSmtpHost = "smtp.163.com";
    private static final String senderEmail = "e2607439502@163.com";
    private static final String senderPassword = "MHIMOBJWZFMUKYSC";
    private static final String senderSmtpPort = "25";

    /**
     * 邮件发送
     *
     * @param subject              邮件主题
     * @param content              邮件内容
     * @param contentIsHtml        内容是否为html格式
     * @param fromMailPersonalName 发件人昵称
     * @param toMail               收件人邮箱
     * @param ccMail               抄送人邮箱
     * @param bccMail              秘密抄送人邮箱
     * @param fileNames            文件名（本地路径）
     * @throws GeneralSecurityException
     * @throws UnsupportedEncodingException
     * @throws MessagingException
     */
    private static void sendEmail(String subject, String content, boolean contentIsHtml, String fromMailPersonalName,
                                  String toMail, String ccMail, String bccMail, List<String> fileNames)
            throws GeneralSecurityException, UnsupportedEncodingException, MessagingException {

        // 设置参数
        Properties properties = System.getProperties();
        // smtp服务地址
        properties.put("mail.smtp.host", senderSmtpHost);
        // smtp服务端口
        properties.put("mail.smtp.port", senderSmtpPort);
        // 开启验证
        properties.put("mail.smtp.auth", "true");
        // 开启TLS加密
        properties.put("mail.smtp.starttls.enable", "false");
        // 是否启用socketFactory，默认为true
        properties.put("mail.smtp.socketFactory.fallback", "true");
        MailSSLSocketFactory sf = new MailSSLSocketFactory();
        sf.setTrustAllHosts(true);
        properties.put("mail.smtp.ssl.enable", "true");
        properties.put("mail.smtp.ssl.socketFactory", sf);
        // 建立会话，利用内部类将邮箱授权给jvm
        Session session = Session.getDefaultInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(senderEmail, senderPassword);
            }
        });
        // 设置为true可以在控制台打印发送过程，生产环境关闭
        session.setDebug(true);
        // 创建邮件对象
        MimeMessage message = new MimeMessage(session);
        // 通过MimeMessageHelper设置正文和附件，否则会导致两者显示不全
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "utf-8");
        //设置收件人,to为收件人,cc为抄送,bcc为密送
        if (StringUtils.isEmpty(toMail)) {
            logger.error("邮件收件人为空");
            return;
        }
        //设置发件人
        helper.setFrom(new InternetAddress(senderEmail, fromMailPersonalName));
        helper.setTo(InternetAddress.parse(toMail, false));
        if (!StringUtils.isEmpty(ccMail)) {
            helper.setCc(InternetAddress.parse(ccMail, false));
        }
        if (!StringUtils.isEmpty(bccMail)) {
            helper.setBcc(InternetAddress.parse(bccMail, false));
        }
        // 设置邮件主题
        helper.setSubject(subject);
        //设置邮件正文内容
        helper.setText(content, contentIsHtml);
        //设置发送的日期
        helper.setSentDate(new Date());
        // 设置附件（注意这里的fileName必须是服务器本地文件名，不能是远程文件链接）
        if (!CollectionUtils.isEmpty(fileNames)) {
            for (String fileName : fileNames) {
                FileDataSource fileDataSource = new FileDataSource(fileName);
                helper.addAttachment(fileDataSource.getName(), fileDataSource);
            }
        }
        //调用Transport的send方法去发送邮件
        Transport.send(message);
    }

}
