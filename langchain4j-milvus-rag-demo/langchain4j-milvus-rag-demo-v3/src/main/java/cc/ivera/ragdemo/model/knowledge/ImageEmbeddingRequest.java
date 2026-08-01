package cc.ivera.ragdemo.model.knowledge;

import java.nio.file.Path;

public record ImageEmbeddingRequest(
        String imageId,
        Path assetPath,
        String imageUrl,
        String imageBase64,
        String mimeType,
        Integer expectedDimension
) {
}
