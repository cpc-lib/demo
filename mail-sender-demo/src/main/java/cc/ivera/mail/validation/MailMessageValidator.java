package cc.ivera.mail.validation;

import cc.ivera.mail.config.MailProperties;
import cc.ivera.mail.model.MailMessage;

/**
 * 邮件参数校验。
 */
public final class MailMessageValidator {

    private MailMessageValidator() {
    }

    public static void validate(MailProperties properties, MailMessage message) {
        if (properties == null) {
            throw new IllegalArgumentException("mail properties 不能为空");
        }
        if (message == null) {
            throw new IllegalArgumentException("mail message 不能为空");
        }

        requireText(properties.getHost(), "SMTP host 不能为空");
        requireText(properties.getFromAddress(), "发件邮箱不能为空");
        if (properties.getPort() <= 0) {
            throw new IllegalArgumentException("SMTP 端口非法");
        }

        if (properties.isAuth()) {
            requireText(properties.getUsername(), "SMTP 用户名不能为空");
            requireText(properties.getPassword(), "SMTP 密码不能为空");
        }

        boolean noRecipients = message.getToRecipients().isEmpty()
                && message.getCcRecipients().isEmpty()
                && message.getBccRecipients().isEmpty();
        if (noRecipients) {
            throw new IllegalArgumentException("收件人、抄送、密送不能同时为空");
        }

        requireText(message.getSubject(), "邮件主题不能为空");
        requireText(message.getContent(), "邮件正文不能为空");
    }

    private static void requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }
}
