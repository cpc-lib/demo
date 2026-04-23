package cc.ivera.openpdf.support.font;

import org.openpdf.text.Font;
import org.openpdf.text.pdf.BaseFont;

/**
 * 字体能力封装。
 */
public class FontSupport {

    private final BaseFont baseFont;

    public FontSupport(BaseFont baseFont) {
        this.baseFont = baseFont;
    }

    public BaseFont getBaseFont() {
        return baseFont;
    }

    public Font createFont(float size) {
        return new Font(baseFont, size, Font.NORMAL);
    }

    public Font createBoldFont(float size) {
        return new Font(baseFont, size, Font.BOLD);
    }
}
