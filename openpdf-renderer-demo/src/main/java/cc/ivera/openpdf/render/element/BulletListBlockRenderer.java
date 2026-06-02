package cc.ivera.openpdf.render.element;

import cc.ivera.openpdf.model.block.BulletListBlock;
import cc.ivera.openpdf.support.placeholder.RenderContext;
import org.openpdf.text.Document;
import org.openpdf.text.ListItem;
import org.openpdf.text.Paragraph;

/**
 * 列表块渲染器。
 */
public class BulletListBlockRenderer implements PdfElementRenderer<BulletListBlock> {

    @Override
    public Class<BulletListBlock> supportType() {
        return BulletListBlock.class;
    }

    @Override
    public void render(Document document, BulletListBlock block, RenderContext context) throws Exception {
        if (block.getTitle() != null && !block.getTitle().isBlank()) {
            Paragraph title = new Paragraph(
                    context.resolve(block.getTitle()),
                    context.getFontSupport().createBoldFont(block.getFontSize() + 1F)
            );
            title.setSpacingAfter(6F);
            document.add(title);
        }

        org.openpdf.text.List list = new org.openpdf.text.List(false, 12F);
        list.setListSymbol("• ");
        list.setIndentationLeft(12F);
        for (String item : block.getItems()) {
            list.add(new ListItem(context.resolve(item), context.getFontSupport().createFont(block.getFontSize())));
        }
        document.add(list);

        if (block.getSpacingAfter() > 0) {
            Paragraph spacer = new Paragraph(" ", context.getFontSupport().createFont(1F));
            spacer.setSpacingAfter(block.getSpacingAfter());
            document.add(spacer);
        }
    }
}
