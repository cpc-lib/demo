package cc.ivera.openpdf.render.watermark;

import cc.ivera.openpdf.support.font.FontSupport;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.PdfContentByte;
import org.openpdf.text.pdf.PdfGState;
import org.openpdf.text.pdf.PdfPageEventHelper;
import org.openpdf.text.pdf.PdfWriter;

/**
 * 机密水印页事件。
 */
public class ConfidentialWatermarkPageEvent extends PdfPageEventHelper {

    private final String watermarkText;
    private final FontSupport fontSupport;

    public ConfidentialWatermarkPageEvent(String watermarkText, FontSupport fontSupport) {
        this.watermarkText = watermarkText;
        this.fontSupport = fontSupport;
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
        try {
            Rectangle pageSize = document.getPageSize();
            float x = (pageSize.getLeft() + pageSize.getRight()) / 2F;
            float y = (pageSize.getBottom() + pageSize.getTop()) / 2F;

            PdfContentByte under = writer.getDirectContentUnder();
            PdfGState gState = new PdfGState();
            gState.setFillOpacity(0.16F);
            under.saveState();
            under.setGState(gState);
            under.beginText();
            under.setFontAndSize(fontSupport.getBaseFont(), 52F);
            under.showTextAligned(Element.ALIGN_CENTER, watermarkText, x, y, 45F);
            under.endText();
            under.restoreState();
        } catch (Exception e) {
            throw new IllegalStateException("写入机密水印失败", e);
        }
    }
}
