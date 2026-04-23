package cc.ivera.openpdf.service.impl;

import cc.ivera.openpdf.model.request.EncryptionOptions;
import cc.ivera.openpdf.model.request.PdfRenderRequest;
import cc.ivera.openpdf.render.watermark.ConfidentialWatermarkPageEvent;
import cc.ivera.openpdf.support.font.FontSupport;
import org.openpdf.text.DocumentException;
import org.openpdf.text.pdf.PdfWriter;

import java.nio.charset.StandardCharsets;

/**
 * PdfWriter 配置器。
 */
public class PdfWriterConfigurator {

    public void configure(PdfWriter writer, PdfRenderRequest request, FontSupport fontSupport) throws DocumentException {
        applyWatermark(writer, request, fontSupport);
        applyEncryption(writer, request.getEncryptionOptions());
    }

    private void applyWatermark(PdfWriter writer, PdfRenderRequest request, FontSupport fontSupport) {
        if (request.getConfidentialText() != null && !request.getConfidentialText().isBlank()) {
            writer.setPageEvent(new ConfidentialWatermarkPageEvent(request.getConfidentialText(), fontSupport));
        }
    }

    private void applyEncryption(PdfWriter writer, EncryptionOptions options) throws DocumentException {
        if (options == null) {
            return;
        }

        byte[] userPassword = toBytes(options.getUserPassword());
        byte[] ownerPassword = toBytes(options.getOwnerPassword());
        writer.setEncryption(userPassword, ownerPassword, options.getPermissions(), options.getEncryptionType());
    }

    private byte[] toBytes(String password) {
        return password == null ? null : password.getBytes(StandardCharsets.UTF_8);
    }
}
