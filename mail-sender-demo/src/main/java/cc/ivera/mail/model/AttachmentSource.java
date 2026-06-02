package cc.ivera.mail.model;

import java.util.Objects;

/**
 * 邮件附件来源。
 */
public class AttachmentSource {

    public enum SourceType {
        LOCAL_FILE,
        REMOTE_URL
    }

    private final SourceType sourceType;
    private final String location;
    private String attachmentName;

    private AttachmentSource(SourceType sourceType, String location) {
        this.sourceType = Objects.requireNonNull(sourceType, "sourceType 不能为空");
        this.location = Objects.requireNonNull(location, "location 不能为空");
    }

    public static AttachmentSource fromLocalFile(String path) {
        return new AttachmentSource(SourceType.LOCAL_FILE, path);
    }

    public static AttachmentSource fromRemoteUrl(String url) {
        return new AttachmentSource(SourceType.REMOTE_URL, url);
    }

    public AttachmentSource withAttachmentName(String attachmentName) {
        this.attachmentName = attachmentName;
        return this;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public String getLocation() {
        return location;
    }

    public String getAttachmentName() {
        return attachmentName;
    }
}
