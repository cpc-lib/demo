package cc.ivera.ragdemo.model.knowledge;

public record ImageAssetReviewRequest(
        String operator,
        String comment,
        String updatedVisualJson,
        String updatedOcrText
) {
}
