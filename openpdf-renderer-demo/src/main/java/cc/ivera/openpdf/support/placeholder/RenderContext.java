package cc.ivera.openpdf.support.placeholder;

import cc.ivera.openpdf.support.font.FontSupport;

import java.util.Collections;
import java.util.Map;

/**
 * 渲染上下文。
 */
public class RenderContext {

    private final Map<String, Object> placeholders;
    private final PlaceholderResolver placeholderResolver;
    private final FontSupport fontSupport;

    public RenderContext(Map<String, Object> placeholders,
                         PlaceholderResolver placeholderResolver,
                         FontSupport fontSupport) {
        this.placeholders = placeholders == null ? Collections.emptyMap() : Collections.unmodifiableMap(placeholders);
        this.placeholderResolver = placeholderResolver;
        this.fontSupport = fontSupport;
    }

    public String resolve(String template) {
        return placeholderResolver.resolve(template, placeholders);
    }

    public FontSupport getFontSupport() {
        return fontSupport;
    }
}
