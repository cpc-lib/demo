package cc.ivera.mail.support.attachment;

import cc.ivera.mail.model.AttachmentSource;
import cc.ivera.mail.model.ResolvedAttachment;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 默认附件解析器，支持本地文件和远程 URL。
 */
public class DefaultAttachmentResolver implements AttachmentResolver {

    @Override
    public ResolvedAttachment resolve(AttachmentSource attachmentSource) throws IOException {
        if (attachmentSource == null) {
            throw new IllegalArgumentException("attachmentSource 不能为空");
        }

        return switch (attachmentSource.getSourceType()) {
            case LOCAL_FILE -> resolveLocalFile(attachmentSource);
            case REMOTE_URL -> resolveRemoteUrl(attachmentSource);
        };
    }

    private ResolvedAttachment resolveLocalFile(AttachmentSource attachmentSource) throws IOException {
        Path path = Path.of(attachmentSource.getLocation());
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw new IllegalArgumentException("本地附件不存在或不是文件: " + attachmentSource.getLocation());
        }

        byte[] content = Files.readAllBytes(path);
        String contentType = fallbackContentType(Files.probeContentType(path));
        String fileName = hasText(attachmentSource.getAttachmentName())
                ? attachmentSource.getAttachmentName()
                : path.getFileName().toString();

        return new ResolvedAttachment(fileName, contentType, content);
    }

    private ResolvedAttachment resolveRemoteUrl(AttachmentSource attachmentSource) throws IOException {
        URI uri = URI.create(attachmentSource.getLocation());
        URLConnection connection = uri.toURL().openConnection();
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout(30_000);

        if (connection instanceof HttpURLConnection httpURLConnection) {
            httpURLConnection.setInstanceFollowRedirects(true);
            httpURLConnection.setRequestMethod("GET");
            int httpStatus = httpURLConnection.getResponseCode();
            if (httpStatus >= 400) {
                throw new IOException("远程附件下载失败，HTTP 状态码: " + httpStatus + ", url=" + attachmentSource.getLocation());
            }
        }

        String fileName = attachmentSource.getAttachmentName();
        if (!hasText(fileName)) {
            fileName = extractFileNameFromDisposition(connection.getHeaderField("Content-Disposition"));
        }
        if (!hasText(fileName)) {
            fileName = extractFileNameFromUrl(attachmentSource.getLocation());
        }

        try (InputStream inputStream = connection.getInputStream()) {
            byte[] content = inputStream.readAllBytes();
            String contentType = fallbackContentType(connection.getContentType());
            return new ResolvedAttachment(fileName, contentType, content);
        }
    }

    private String extractFileNameFromDisposition(String contentDisposition) {
        if (!hasText(contentDisposition)) {
            return null;
        }

        String[] fragments = contentDisposition.split(";");
        for (String fragment : fragments) {
            String trimmed = fragment.trim();
            if (trimmed.startsWith("filename*=")) {
                String value = trimmed.substring("filename*=".length()).trim();
                int index = value.lastIndexOf("''");
                if (index >= 0 && index + 2 < value.length()) {
                    return URLDecoder.decode(value.substring(index + 2), StandardCharsets.UTF_8);
                }
                return stripQuotes(value);
            }
            if (trimmed.startsWith("filename=")) {
                return stripQuotes(trimmed.substring("filename=".length()).trim());
            }
        }
        return null;
    }

    private String extractFileNameFromUrl(String url) {
        try {
            String path = URI.create(url).getPath();
            if (!hasText(path) || path.endsWith("/")) {
                return "attachment";
            }
            String rawName = path.substring(path.lastIndexOf('/') + 1);
            String decodedName = URLDecoder.decode(rawName, StandardCharsets.UTF_8);
            return hasText(decodedName) ? decodedName : "attachment";
        } catch (Exception e) {
            return "attachment";
        }
    }

    private String fallbackContentType(String contentType) {
        return hasText(contentType) ? contentType : "application/octet-stream";
    }

    private String stripQuotes(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    private boolean hasText(String text) {
        return text != null && !text.trim().isEmpty();
    }
}
