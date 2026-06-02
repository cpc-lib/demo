package cc.ivera.openpdf.validator;

import cc.ivera.openpdf.model.request.PdfRenderRequest;
import cc.ivera.openpdf.support.exception.PdfRenderException;

/**
 * 渲染请求校验器。
 */
public class PdfRenderRequestValidator {

    public void validate(PdfRenderRequest request) {
        if (request == null) {
            throw new PdfRenderException("request 不能为空");
        }
        if (request.getOutputPath() == null || request.getOutputPath().isBlank()) {
            throw new PdfRenderException("输出路径不能为空");
        }
        if (request.getElements() == null || request.getElements().isEmpty()) {
            throw new PdfRenderException("至少需要一个渲染元素");
        }
    }
}
