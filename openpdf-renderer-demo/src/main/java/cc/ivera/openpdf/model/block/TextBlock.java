package cc.ivera.openpdf.model.block;

/**
 * 文本块。
 */
public class TextBlock implements PdfElement {

    private String template;
    private float fontSize = 12F;
    private boolean bold;
    private boolean center;
    private float spacingAfter = 8F;

    public String getTemplate() {
        return template;
    }

    public TextBlock setTemplate(String template) {
        this.template = template;
        return this;
    }

    public float getFontSize() {
        return fontSize;
    }

    public TextBlock setFontSize(float fontSize) {
        this.fontSize = fontSize;
        return this;
    }

    public boolean isBold() {
        return bold;
    }

    public TextBlock setBold(boolean bold) {
        this.bold = bold;
        return this;
    }

    public boolean isCenter() {
        return center;
    }

    public TextBlock setCenter(boolean center) {
        this.center = center;
        return this;
    }

    public float getSpacingAfter() {
        return spacingAfter;
    }

    public TextBlock setSpacingAfter(float spacingAfter) {
        this.spacingAfter = spacingAfter;
        return this;
    }
}
