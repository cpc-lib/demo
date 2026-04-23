package cc.ivera.openpdf.support.font;

import cc.ivera.openpdf.support.exception.PdfRenderException;
import org.openpdf.text.pdf.BaseFont;

/**
 * 字体解析器。
 */
public class FontResolver {

    public FontSupport resolve(String fontPath) {
        try {
            BaseFont baseFont;
            if (fontPath != null && !fontPath.isBlank()) {
                baseFont = BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            } else {
                baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            }
            return new FontSupport(baseFont);
        } catch (Exception e) {
            throw new PdfRenderException("字体初始化失败", e);
        }
    }
}
