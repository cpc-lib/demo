package cc.ivera.openpdf.support.exception;

/**
 * PDF 渲染异常。
 */
public class PdfRenderException extends RuntimeException {

    public PdfRenderException(String message) {
        super(message);
    }

    public PdfRenderException(String message, Throwable cause) {
        super(message, cause);
    }
}
