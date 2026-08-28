package cc.ivera.ragdemo.model.knowledge;

import java.util.List;

public record MultimodalCollectionStatus(
        boolean enabled,
        String collection,
        String textVectorField,
        String imageVectorField,
        int textDimension,
        int imageDimension,
        List<String> fields,
        String status,
        String message
) {
}
