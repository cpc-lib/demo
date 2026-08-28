package cc.ivera.ragdemo.service.ingest;

import cc.ivera.ragdemo.model.knowledge.ImageEmbeddingRequest;
import cc.ivera.ragdemo.model.knowledge.VisionEmbeddingResult;

public interface VisionEmbeddingClient {

    VisionEmbeddingResult embed(ImageEmbeddingRequest request);
}
