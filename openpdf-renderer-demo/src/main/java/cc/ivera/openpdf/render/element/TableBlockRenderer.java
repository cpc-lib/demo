package cc.ivera.openpdf.render.element;

import cc.ivera.openpdf.model.block.TableBlock;
import cc.ivera.openpdf.support.placeholder.RenderContext;
import org.openpdf.text.Document;
import org.openpdf.text.Paragraph;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;

import java.util.List;

/**
 * 表格块渲染器。
 */
public class TableBlockRenderer implements PdfElementRenderer<TableBlock> {

    @Override
    public Class<TableBlock> supportType() {
        return TableBlock.class;
    }

    @Override
    public void render(Document document, TableBlock block, RenderContext context) throws Exception {
        int columnCount = block.getHeaders().size();
        if (columnCount == 0) {
            return;
        }

        if (block.getTitle() != null && !block.getTitle().isBlank()) {
            Paragraph title = new Paragraph(
                    context.resolve(block.getTitle()),
                    context.getFontSupport().createBoldFont(block.getFontSize() + 1F)
            );
            title.setSpacingAfter(6F);
            document.add(title);
        }

        PdfPTable table = new PdfPTable(columnCount);
        table.setWidthPercentage(100F);
        if (block.getColumnWidths() != null && block.getColumnWidths().length == columnCount) {
            table.setWidths(block.getColumnWidths());
        }

        for (String header : block.getHeaders()) {
            table.addCell(createCell(context.resolve(header), context, block.getFontSize(), true));
        }

        for (List<String> row : block.getRows()) {
            for (int i = 0; i < columnCount; i++) {
                String value = i < row.size() ? row.get(i) : "";
                table.addCell(createCell(context.resolve(value), context, block.getFontSize(), false));
            }
        }

        table.setSpacingAfter(block.getSpacingAfter());
        document.add(table);
    }

    private PdfPCell createCell(String text, RenderContext context, float fontSize, boolean bold) {
        PdfPCell cell = new PdfPCell(new Paragraph(
                text,
                bold ? context.getFontSupport().createBoldFont(fontSize) : context.getFontSupport().createFont(fontSize)
        ));
        cell.setPadding(bold ? 6F : 5F);
        return cell;
    }
}
