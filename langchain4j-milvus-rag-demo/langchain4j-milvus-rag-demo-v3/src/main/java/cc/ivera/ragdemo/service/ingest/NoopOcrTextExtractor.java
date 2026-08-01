package cc.ivera.ragdemo.service.ingest;

import cc.ivera.ragdemo.model.knowledge.ExtractedImageKnowledge;
import cc.ivera.ragdemo.model.knowledge.OcrExtractionResult;
import org.springframework.stereotype.Component;

@Component
public class NoopOcrTextExtractor implements OcrTextExtractor {

    @Override
    public String extract(ExtractedImageKnowledge image) {
        return image == null ? null : image.ocrText();
    }

    @Override
    public OcrExtractionResult extractResult(ExtractedImageKnowledge image) {
        return OcrExtractionResult.skipped(extract(image));
    }
}
