package cc.ivera.openpdf.demo;

import cc.ivera.openpdf.model.block.BulletListBlock;
import cc.ivera.openpdf.model.block.ImageBlock;
import cc.ivera.openpdf.model.block.TableBlock;
import cc.ivera.openpdf.model.block.TextBlock;
import cc.ivera.openpdf.model.request.EncryptionOptions;
import cc.ivera.openpdf.model.request.PdfRenderRequest;
import org.openpdf.text.PageSize;
import org.openpdf.text.pdf.PdfWriter;

import java.util.List;

/**
 * 示例请求工厂。
 */
public final class SampleRequestFactory {

    private SampleRequestFactory() {
    }

    public static PdfRenderRequest create(String outputPath, String imagePath) {
        return new PdfRenderRequest()
                .setOutputPath(outputPath)
                .setPageSize(PageSize.A4)
                .setConfidentialText("CONFIDENTIAL")
                .setEncryptionOptions(new EncryptionOptions()
                        .setUserPassword("123456")
                        .setOwnerPassword("owner-123456")
                        .setPermissions(PdfWriter.ALLOW_PRINTING)
                        .setEncryptionType(PdfWriter.ENCRYPTION_AES_128))
                .addPlaceholder("companyName", "Ivera Tech")
                .addPlaceholder("reportDate", "2026-04-23")
                .addPlaceholder("owner", "HE JIAMING")
                .addElement(new TextBlock()
                        .setTemplate("${companyName} OpenPDF Rendering Demo")
                        .setFontSize(18F)
                        .setBold(true)
                        .setCenter(true)
                        .setSpacingAfter(14F))
                .addElement(new TextBlock()
                        .setTemplate("Owner: ${owner}   Date: ${reportDate}")
                        .setFontSize(11F)
                        .setSpacingAfter(10F))
                .addElement(new ImageBlock()
                        .setImagePath(imagePath)
                        .setWidth(140F)
                        .setHeight(70F)
                        .setCenter(true)
                        .setSpacingAfter(12F))
                .addElement(new BulletListBlock()
                        .setTitle("Feature List")
                        .addItem("Text rendering with placeholder support")
                        .addItem("Bullet list rendering")
                        .addItem("Table rendering for business rows")
                        .addItem("Image rendering from local file")
                        .addItem("PDF encryption + confidential watermark")
                        .setFontSize(11F)
                        .setSpacingAfter(12F))
                .addElement(new TableBlock()
                        .setTitle("User Overview")
                        .addHeader("ID")
                        .addHeader("Name")
                        .addHeader("City")
                        .addHeader("Email")
                        .setColumnWidths(new float[]{1.2F, 2F, 2F, 3F})
                        .addRow(List.of("1", "Alice", "Los Angeles", "alice@test.com"))
                        .addRow(List.of("2", "Bob", "Seattle", "bob@test.com"))
                        .addRow(List.of("3", "Cindy", "New York", "cindy@test.com"))
                        .setFontSize(10.5F));
    }
}
