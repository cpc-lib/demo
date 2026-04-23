package cc.ivera.openpdf.render.element;

import cc.ivera.openpdf.model.block.PdfElement;
import cc.ivera.openpdf.support.placeholder.RenderContext;
import org.openpdf.text.Document;

/**
 * PDF 元素渲染器。
 */
public interface PdfElementRenderer<T extends PdfElement> {

    Class<T> supportType();

    void render(Document document, T element, RenderContext context) throws Exception;
}
