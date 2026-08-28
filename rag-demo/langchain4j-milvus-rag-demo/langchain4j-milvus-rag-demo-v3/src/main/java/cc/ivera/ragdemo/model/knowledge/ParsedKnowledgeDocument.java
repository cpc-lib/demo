package cc.ivera.ragdemo.model.knowledge;

import dev.langchain4j.data.document.Document;

import java.util.List;

public record ParsedKnowledgeDocument(
        Document textDocument,
        List<ExtractedImageKnowledge> images
) {
}
