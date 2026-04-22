package cc.ivera.mail.service;

import cc.ivera.mail.config.MailProperties;
import cc.ivera.mail.model.AttachmentSource;
import cc.ivera.mail.model.MailMessage;
import cc.ivera.mail.model.ResolvedAttachment;
import cc.ivera.mail.support.attachment.AttachmentResolver;
import cc.ivera.mail.support.attachment.DefaultAttachmentResolver;
import cc.ivera.mail.support.datasource.ByteArrayMailDataSource;
import cc.ivera.mail.support.session.SmtpSessionFactory;
import cc.ivera.mail.validation.MailMessageValidator;
import jakarta.activation.DataHandler;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.internet.MimeUtility;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * SMTP 邮件发送器。
 */
public class SmtpMailSender implements MailSender {

    private final MailProperties properties;
    private final SmtpSessionFactory sessionFactory;
    private final AttachmentResolver attachmentResolver;

    public SmtpMailSender(MailProperties properties) {
        this(properties, new SmtpSessionFactory(), new DefaultAttachmentResolver());
    }

    public SmtpMailSender(MailProperties properties,
                          SmtpSessionFactory sessionFactory,
                          AttachmentResolver attachmentResolver) {
        this.properties = Objects.requireNonNull(properties, "properties 不能为空");
        this.sessionFactory = Objects.requireNonNull(sessionFactory, "sessionFactory 不能为空");
        this.attachmentResolver = Objects.requireNonNull(attachmentResolver, "attachmentResolver 不能为空");
    }

    @Override
    public void send(MailMessage message) throws MessagingException, IOException {
        MailMessageValidator.validate(properties, message);

        Session session = sessionFactory.createSession(properties);
        MimeMessage mimeMessage = new MimeMessage(session);

        mimeMessage.setFrom(buildFromAddress());
        addRecipients(mimeMessage, Message.RecipientType.TO, message.getToRecipients());
        addRecipients(mimeMessage, Message.RecipientType.CC, message.getCcRecipients());
        addRecipients(mimeMessage, Message.RecipientType.BCC, message.getBccRecipients());
        mimeMessage.setSubject(message.getSubject(), StandardCharsets.UTF_8.name());
        mimeMessage.setSentDate(new Date());
        mimeMessage.setContent(buildMimeContent(message));
        mimeMessage.saveChanges();

        Transport.send(mimeMessage);
    }

    private InternetAddress buildFromAddress() throws MessagingException {
        try {
            if (properties.getFromName() == null || properties.getFromName().isBlank()) {
                return new InternetAddress(properties.getFromAddress(), true);
            }
            return new InternetAddress(
                    properties.getFromAddress(),
                    properties.getFromName(),
                    StandardCharsets.UTF_8.name()
            );
        } catch (Exception e) {
            throw new MessagingException("构建发件人地址失败", e);
        }
    }

    private void addRecipients(MimeMessage mimeMessage,
                               Message.RecipientType recipientType,
                               List<String> emails) throws MessagingException {
        if (emails == null || emails.isEmpty()) {
            return;
        }
        InternetAddress[] addresses = emails.stream()
                .map(this::toInternetAddress)
                .toArray(InternetAddress[]::new);
        mimeMessage.addRecipients(recipientType, addresses);
    }

    private InternetAddress toInternetAddress(String email) {
        try {
            return new InternetAddress(email, true);
        } catch (AddressException e) {
            throw new IllegalArgumentException("邮箱地址格式非法: " + email, e);
        }
    }

    private MimeMultipart buildMimeContent(MailMessage message) throws MessagingException, IOException {
        MimeMultipart multipart = new MimeMultipart("mixed");
        multipart.addBodyPart(buildTextBodyPart(message));

        for (AttachmentSource attachment : message.getAttachments()) {
            multipart.addBodyPart(buildAttachmentBodyPart(attachment));
        }
        return multipart;
    }

    private MimeBodyPart buildTextBodyPart(MailMessage message) throws MessagingException {
        MimeBodyPart textBodyPart = new MimeBodyPart();
        if (message.isHtml()) {
            textBodyPart.setContent(message.getContent(), "text/html; charset=UTF-8");
        } else {
            textBodyPart.setText(message.getContent(), StandardCharsets.UTF_8.name());
        }
        return textBodyPart;
    }

    private MimeBodyPart buildAttachmentBodyPart(AttachmentSource attachment) throws MessagingException, IOException {
        ResolvedAttachment resolvedAttachment = attachmentResolver.resolve(attachment);
        MimeBodyPart attachmentBodyPart = new MimeBodyPart();
        attachmentBodyPart.setDataHandler(new DataHandler(
                new ByteArrayMailDataSource(
                        resolvedAttachment.getContent(),
                        resolvedAttachment.getContentType(),
                        resolvedAttachment.getFileName()
                )
        ));
        attachmentBodyPart.setFileName(MimeUtility.encodeText(
                resolvedAttachment.getFileName(),
                StandardCharsets.UTF_8.name(),
                null
        ));
        return attachmentBodyPart;
    }
}
