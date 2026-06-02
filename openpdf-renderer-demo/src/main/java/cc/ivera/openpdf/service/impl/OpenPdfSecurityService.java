package cc.ivera.openpdf.service.impl;

import cc.ivera.openpdf.service.PdfSecurityService;
import cc.ivera.openpdf.support.exception.PdfRenderException;
import cc.ivera.openpdf.support.io.FileSupport;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.PdfStamper;

import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * OpenPDF 安全服务实现。
 */
public class OpenPdfSecurityService implements PdfSecurityService {

    @Override
    public void decrypt(String inputPdfPath, String outputPdfPath, String userOrOwnerPassword) {
        FileSupport.ensureParentDirectory(outputPdfPath);
        PdfReader reader = null;
        PdfStamper stamper = null;
        FileOutputStream outputStream = null;
        try {
            byte[] password = userOrOwnerPassword == null ? null : userOrOwnerPassword.getBytes(StandardCharsets.UTF_8);
            reader = password == null ? new PdfReader(inputPdfPath) : new PdfReader(inputPdfPath, password);
            outputStream = new FileOutputStream(outputPdfPath);
            stamper = new PdfStamper(reader, outputStream);
        } catch (Exception e) {
            throw new PdfRenderException("PDF 解密失败", e);
        } finally {
            closeQuietly(stamper);
            closeQuietly(reader);
            closeQuietly(outputStream);
        }
    }

    private void closeQuietly(PdfStamper stamper) {
        try {
            if (stamper != null) {
                stamper.close();
            }
        } catch (Exception ignored) {
        }
    }

    private void closeQuietly(PdfReader reader) {
        try {
            if (reader != null) {
                reader.close();
            }
        } catch (Exception ignored) {
        }
    }

    private void closeQuietly(FileOutputStream outputStream) {
        try {
            if (outputStream != null) {
                outputStream.close();
            }
        } catch (Exception ignored) {
        }
    }
}
