package cc.ivera.ragdemo.model.knowledge;

public record ImageAssetReprocessRequest(
        Boolean ocr,
        Boolean visionAnalysis,
        Boolean imageEmbedding,
        String operator
) {
}
