package cc.ivera.ragdemo.model.query;

import java.util.Map;

public record RagSearchItem(
        int rank,
        Double score,
        Long knowledgeBaseId,
        String documentId,
        String documentName,
        String chunkId,
        Integer version,
        String contentType,
        Integer pageNo,
        String sectionTitle,
        String imageCaption,
        String imageNumber,
        String imageUrl,
        String content,
        Map<String, Object> metadata,
        String modality,
        String retrievalSource,
        Long imageAssetId,
        Double fusionScore
) {

    public RagSearchItem(int rank,
                         Double score,
                         Long knowledgeBaseId,
                         String documentId,
                         String documentName,
                         String chunkId,
                         Integer version,
                         String contentType,
                         Integer pageNo,
                         String sectionTitle,
                         String imageCaption,
                         String imageNumber,
                         String imageUrl,
                         String content,
                         Map<String, Object> metadata) {
        this(rank, score, knowledgeBaseId, documentId, documentName, chunkId, version, contentType, pageNo,
                sectionTitle, imageCaption, imageNumber, imageUrl, content, metadata, null, null, null, null);
    }
}
