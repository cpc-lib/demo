package cc.ivera.ragdemo.service.ingest;

import cc.ivera.ragdemo.model.knowledge.ExtractedImageKnowledge;
import cc.ivera.ragdemo.model.knowledge.OcrExtractionResult;

public interface OcrTextExtractor {

    String extract(ExtractedImageKnowledge image);

    default OcrExtractionResult extractResult(ExtractedImageKnowledge image) {
        return OcrExtractionResult.skipped(extract(image));
    }
}
