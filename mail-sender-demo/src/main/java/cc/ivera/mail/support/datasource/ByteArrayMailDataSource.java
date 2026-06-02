package cc.ivera.mail.support.datasource;

import jakarta.activation.DataSource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 基于字节数组的只读附件数据源。
 */
public class ByteArrayMailDataSource implements DataSource {

    private final byte[] content;
    private final String contentType;
    private final String name;

    public ByteArrayMailDataSource(byte[] content, String contentType, String name) {
        this.content = content;
        this.contentType = contentType;
        this.name = name;
    }

    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(content);
    }

    @Override
    public OutputStream getOutputStream() {
        throw new UnsupportedOperationException("只读数据源，不支持写入");
    }

    @Override
    public String getContentType() {
        return contentType == null || contentType.isBlank()
                ? "application/octet-stream"
                : contentType;
    }

    @Override
    public String getName() {
        return name;
    }
}
