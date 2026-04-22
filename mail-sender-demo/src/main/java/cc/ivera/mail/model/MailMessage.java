package cc.ivera.mail.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * 邮件请求对象。
 */
public class MailMessage {

    private String subject;
    private String content;
    private boolean html = true;

    private final List<String> toRecipients = new ArrayList<>();
    private final List<String> ccRecipients = new ArrayList<>();
    private final List<String> bccRecipients = new ArrayList<>();
    private final List<AttachmentSource> attachments = new ArrayList<>();

    public String getSubject() {
        return subject;
    }

    public MailMessage subject(String subject) {
        this.subject = subject;
        return this;
    }

    public String getContent() {
        return content;
    }

    public MailMessage content(String content) {
        this.content = content;
        return this;
    }

    public boolean isHtml() {
        return html;
    }

    public MailMessage html(boolean html) {
        this.html = html;
        return this;
    }

    public List<String> getToRecipients() {
        return toRecipients;
    }

    public List<String> getCcRecipients() {
        return ccRecipients;
    }

    public List<String> getBccRecipients() {
        return bccRecipients;
    }

    public List<AttachmentSource> getAttachments() {
        return attachments;
    }

    public MailMessage addTo(String email) {
        toRecipients.add(Objects.requireNonNull(email, "to email 不能为空"));
        return this;
    }

    public MailMessage addTo(Collection<String> emails) {
        if (emails != null) {
            emails.forEach(this::addTo);
        }
        return this;
    }

    public MailMessage addCc(String email) {
        ccRecipients.add(Objects.requireNonNull(email, "cc email 不能为空"));
        return this;
    }

    public MailMessage addCc(Collection<String> emails) {
        if (emails != null) {
            emails.forEach(this::addCc);
        }
        return this;
    }

    public MailMessage addBcc(String email) {
        bccRecipients.add(Objects.requireNonNull(email, "bcc email 不能为空"));
        return this;
    }

    public MailMessage addBcc(Collection<String> emails) {
        if (emails != null) {
            emails.forEach(this::addBcc);
        }
        return this;
    }

    public MailMessage addAttachment(AttachmentSource attachmentSource) {
        attachments.add(Objects.requireNonNull(attachmentSource, "attachmentSource 不能为空"));
        return this;
    }

    public MailMessage addAttachments(Collection<AttachmentSource> attachmentSources) {
        if (attachmentSources != null) {
            attachmentSources.forEach(this::addAttachment);
        }
        return this;
    }
}
