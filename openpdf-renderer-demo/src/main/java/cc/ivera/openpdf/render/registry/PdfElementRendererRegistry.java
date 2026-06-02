package cc.ivera.openpdf.render.registry;

import cc.ivera.openpdf.model.block.PdfElement;
import cc.ivera.openpdf.render.element.PdfElementRenderer;
import cc.ivera.openpdf.support.exception.PdfRenderException;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 元素渲染器注册表。
 */
public class PdfElementRendererRegistry {

    private final Map<Class<? extends PdfElement>, PdfElementRenderer<?>> rendererMap = new LinkedHashMap<>();

    public PdfElementRendererRegistry(Collection<PdfElementRenderer<?>> renderers) {
        for (PdfElementRenderer<?> renderer : renderers) {
            rendererMap.put(renderer.supportType(), renderer);
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends PdfElement> PdfElementRenderer<T> getRenderer(T element) {
        PdfElementRenderer<?> renderer = rendererMap.get(element.getClass());
        if (renderer == null) {
            throw new PdfRenderException("未找到元素渲染器: " + element.getClass().getName());
        }
        return (PdfElementRenderer<T>) renderer;
    }
}
