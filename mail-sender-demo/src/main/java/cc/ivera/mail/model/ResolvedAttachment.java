package cc.ivera.mail.model;

/**
 * 已解析完成的附件对象。
 */
public class ResolvedAttachment {

    private final String fileName;
    private final String contentType;
    private final byte[] content;

    public ResolvedAttachment(String fileName, String contentType, byte[] content) {
        this.fileName = fileName;
        this.contentType = contentType;
        this.content = content;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getContent() {
        return content;
    }
}
