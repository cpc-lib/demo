package cc.ivera.openpdf.service.impl;

import cc.ivera.openpdf.model.block.PdfElement;
import cc.ivera.openpdf.model.request.PdfRenderRequest;
import cc.ivera.openpdf.render.element.BulletListBlockRenderer;
import cc.ivera.openpdf.render.element.ImageBlockRenderer;
import cc.ivera.openpdf.render.element.PdfElementRenderer;
import cc.ivera.openpdf.render.element.TableBlockRenderer;
import cc.ivera.openpdf.render.element.TextBlockRenderer;
import cc.ivera.openpdf.render.registry.PdfElementRendererRegistry;
import cc.ivera.openpdf.service.PdfRenderService;
import cc.ivera.openpdf.support.exception.PdfRenderException;
import cc.ivera.openpdf.support.font.FontResolver;
import cc.ivera.openpdf.support.font.FontSupport;
import cc.ivera.openpdf.support.io.FileSupport;
import cc.ivera.openpdf.support.placeholder.PlaceholderResolver;
import cc.ivera.openpdf.support.placeholder.RenderContext;
import cc.ivera.openpdf.validator.PdfRenderRequestValidator;
import org.openpdf.text.Document;
import org.openpdf.text.pdf.PdfWriter;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * OpenPDF 渲染服务实现。
 */
public class OpenPdfRenderService implements PdfRenderService {

    private final PdfRenderRequestValidator requestValidator;
    private final FontResolver fontResolver;
    private final PlaceholderResolver placeholderResolver;
    private final PdfElementRendererRegistry rendererRegistry;
    private final PdfWriterConfigurator writerConfigurator;

    public OpenPdfRenderService() {
        this(new PdfRenderRequestValidator(),
                new FontResolver(),
                new PlaceholderResolver(),
                new PdfElementRendererRegistry(List.of(
                        new TextBlockRenderer(),
                        new ImageBlockRenderer(),
                        new BulletListBlockRenderer(),
                        new TableBlockRenderer()
                )),
                new PdfWriterConfigurator());
    }

    public OpenPdfRenderService(PdfRenderRequestValidator requestValidator,
                                FontResolver fontResolver,
                                PlaceholderResolver placeholderResolver,
                                PdfElementRendererRegistry rendererRegistry,
                                PdfWriterConfigurator writerConfigurator) {
        this.requestValidator = requestValidator;
        this.fontResolver = fontResolver;
        this.placeholderResolver = placeholderResolver;
        this.rendererRegistry = rendererRegistry;
        this.writerConfigurator = writerConfigurator;
    }

    @Override
    public void render(PdfRenderRequest request) {
        requestValidator.validate(request);
        FileSupport.ensureParentDirectory(request.getOutputPath());

        FontSupport fontSupport = fontResolver.resolve(request.getFontPath());
        RenderContext renderContext = new RenderContext(request.getPlaceholders(), placeholderResolver, fontSupport);

        try (OutputStream outputStream = new FileOutputStream(request.getOutputPath())) {
            Document document = new Document(request.getPageSize());
            try {
                PdfWriter writer = PdfWriter.getInstance(document, outputStream);
                writerConfigurator.configure(writer, request, fontSupport);
                document.open();
                for (PdfElement element : request.getElements()) {
                    PdfElementRenderer<PdfElement> renderer = rendererRegistry.getRenderer(element);
                    renderer.render(document, element, renderContext);
                }
            } finally {
                document.close();
            }
        } catch (Exception e) {
            throw new PdfRenderException("OpenPDF 渲染失败", e);
        }
    }
}
