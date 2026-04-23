package cc.ivera.openpdf.render.element;

import cc.ivera.openpdf.model.block.TextBlock;
import cc.ivera.openpdf.support.placeholder.RenderContext;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Paragraph;

/**
 * 文本块渲染器。
 */
public class TextBlockRenderer implements PdfElementRenderer<TextBlock> {

    @Override
    public Class<TextBlock> supportType() {
        return TextBlock.class;
    }

    @Override
    public void render(Document document, TextBlock block, RenderContext context) throws Exception {
        String content = context.resolve(block.getTemplate());
        Paragraph paragraph = new Paragraph(
                content,
                block.isBold()
                        ? context.getFontSupport().createBoldFont(block.getFontSize())
                        : context.getFontSupport().createFont(block.getFontSize())
        );
        if (block.isCenter()) {
            paragraph.setAlignment(Element.ALIGN_CENTER);
        }
        paragraph.setSpacingAfter(block.getSpacingAfter());
        document.add(paragraph);
    }
}
