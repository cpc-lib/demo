package cc.ivera.mail.support.session;

import cc.ivera.mail.config.MailProperties;
import jakarta.mail.Authenticator;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;

import java.util.Properties;

/**
 * SMTP Session 工厂。
 */
public class SmtpSessionFactory {

    public Session createSession(MailProperties properties) {
        Properties sessionProperties = new Properties();
        sessionProperties.put("mail.transport.protocol", "smtp");
        sessionProperties.put("mail.smtp.host", properties.getHost());
        sessionProperties.put("mail.smtp.port", String.valueOf(properties.getPort()));
        sessionProperties.put("mail.smtp.auth", String.valueOf(properties.isAuth()));
        sessionProperties.put("mail.smtp.starttls.enable", String.valueOf(properties.isStartTlsEnabled()));
        sessionProperties.put("mail.smtp.ssl.enable", String.valueOf(properties.isSslEnabled()));
        sessionProperties.put("mail.smtp.connectiontimeout", String.valueOf(properties.getConnectionTimeoutMillis()));
        sessionProperties.put("mail.smtp.timeout", String.valueOf(properties.getReadTimeoutMillis()));
        sessionProperties.put("mail.smtp.writetimeout", String.valueOf(properties.getWriteTimeoutMillis()));

        Session session;
        if (properties.isAuth()) {
            session = Session.getInstance(sessionProperties, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(properties.getUsername(), properties.getPassword());
                }
            });
        } else {
            session = Session.getInstance(sessionProperties);
        }

        session.setDebug(properties.isDebug());
        return session;
    }
}
