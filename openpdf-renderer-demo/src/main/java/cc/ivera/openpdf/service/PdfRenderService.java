package cc.ivera.openpdf.service;

import cc.ivera.openpdf.model.request.PdfRenderRequest;

public interface PdfRenderService {

    void render(PdfRenderRequest request);
}
