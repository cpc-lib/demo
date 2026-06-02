package cc.ivera.openpdf.model.request;

import cc.ivera.openpdf.model.block.PdfElement;
import org.openpdf.text.PageSize;
import org.openpdf.text.Rectangle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PDF 渲染请求。
 */
public class PdfRenderRequest {

    private String outputPath;
    private String fontPath;
    private Rectangle pageSize = PageSize.A4;
    private String confidentialText;
    private final Map<String, Object> placeholders = new HashMap<>();
    private final List<PdfElement> elements = new ArrayList<>();
    private EncryptionOptions encryptionOptions;

    public String getOutputPath() {
        return outputPath;
    }

    public PdfRenderRequest setOutputPath(String outputPath) {
        this.outputPath = outputPath;
        return this;
    }

    public String getFontPath() {
        return fontPath;
    }

    public PdfRenderRequest setFontPath(String fontPath) {
        this.fontPath = fontPath;
        return this;
    }

    public Rectangle getPageSize() {
        return pageSize;
    }

    public PdfRenderRequest setPageSize(Rectangle pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    public String getConfidentialText() {
        return confidentialText;
    }

    public PdfRenderRequest setConfidentialText(String confidentialText) {
        this.confidentialText = confidentialText;
        return this;
    }

    public Map<String, Object> getPlaceholders() {
        return placeholders;
    }

    public PdfRenderRequest addPlaceholder(String key, Object value) {
        this.placeholders.put(key, value);
        return this;
    }

    public List<PdfElement> getElements() {
        return elements;
    }

    public PdfRenderRequest addElement(PdfElement element) {
        this.elements.add(element);
        return this;
    }

    public EncryptionOptions getEncryptionOptions() {
        return encryptionOptions;
    }

    public PdfRenderRequest setEncryptionOptions(EncryptionOptions encryptionOptions) {
        this.encryptionOptions = encryptionOptions;
        return this;
    }
}
