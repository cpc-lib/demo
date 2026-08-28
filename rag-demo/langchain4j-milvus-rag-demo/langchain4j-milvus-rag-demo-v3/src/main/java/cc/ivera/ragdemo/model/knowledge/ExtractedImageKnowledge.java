package cc.ivera.ragdemo.model.knowledge;

import java.nio.file.Path;

public record ExtractedImageKnowledge(
        String id,
        ContentType contentType,
        Path assetPath,
        String imageUrl,
        Integer pageNo,
        PageCoordinate coordinate,
        String sectionTitle,
        String imageCaption,
        String imageNumber,
        String previousText,
        String nextText,
        String ocrText,
        String visualStructuredContent
) {
    public String searchableText() {
        StringBuilder sb = new StringBuilder();
        append(sb, "content_type", contentType == null ? null : contentType.name().toLowerCase());
        append(sb, "section_title", sectionTitle);
        append(sb, "image_number", imageNumber);
        append(sb, "image_caption", imageCaption);
        append(sb, "previous_text", previousText);
        append(sb, "next_text", nextText);
        append(sb, "ocr_text", ocrText);
        append(sb, "visual_structured_content", visualStructuredContent);
        return sb.toString().trim();
    }

    private static void append(StringBuilder sb, String label, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (!sb.isEmpty()) {
            sb.append('\n');
        }
        sb.append(label).append(": ").append(value.trim());
    }
}
