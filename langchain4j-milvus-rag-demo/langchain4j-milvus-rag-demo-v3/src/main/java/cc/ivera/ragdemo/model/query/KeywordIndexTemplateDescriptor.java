package cc.ivera.ragdemo.model.query;

public record KeywordIndexTemplateDescriptor(
        String engine,
        String profile,
        String templateName,
        String resourcePath,
        String renderedJson
) {
}
