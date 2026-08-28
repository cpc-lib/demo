package cc.ivera.ragdemo.service.query;


import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.domain.rag.RagImageAsset;
import cc.ivera.ragdemo.mapper.RagImageAssetMapper;
import cc.ivera.ragdemo.model.knowledge.ImageEmbeddingRequest;
import cc.ivera.ragdemo.model.knowledge.MultimodalVectorSearchHit;
import cc.ivera.ragdemo.model.knowledge.MultimodalVectorSearchRequest;
import cc.ivera.ragdemo.model.knowledge.VisionEmbeddingResult;
import cc.ivera.ragdemo.model.query.RagRetrievalCriteria;
import cc.ivera.ragdemo.model.query.RagSearchItem;
import cc.ivera.ragdemo.service.ingest.VisionEmbeddingClient;
import cc.ivera.ragdemo.service.vector.MultimodalVectorStore;
import cc.ivera.ragdemo.tenant.TenantContextHolder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.file.Path;
import java.util.*;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class ImageVectorRetrievalService {

    private final MultimodalVectorStore multimodalVectorStore;
    private final VisionEmbeddingClient visionEmbeddingClient;
    private final RagImageAssetMapper imageAssetMapper;
    private final RagProperties ragProperties;

    public boolean enabled() {
        return multimodalVectorStore.enabled();
    }

    public boolean hasImageInput(RagRetrievalCriteria criteria) {
        return criteria != null
                && (criteria.imageAssetId() != null
                || StringUtils.hasText(criteria.imageUrl())
                || StringUtils.hasText(criteria.imageBase64()));
    }

    public List<RagSearchItem> search(RagRetrievalCriteria criteria) {
        if (!enabled() || !hasImageInput(criteria) || !allowsImage(criteria)) {
            return List.of();
        }
        ImageEmbeddingRequest embeddingRequest = embeddingRequest(criteria);
        VisionEmbeddingResult embedding = visionEmbeddingClient.embed(embeddingRequest);
        if (!embedding.success()) {
            throw new IllegalStateException("Image embedding failed: " + firstNonBlank(embedding.errorMessage(), embedding.status()));
        }
        int topK = criteria.topK() == null ? ragProperties.getMilvus().getTopK() : Math.max(1, criteria.topK());
        double minScore = criteria.minScore() == null ? ragProperties.getMilvus().getMinScore() : criteria.minScore();
        List<MultimodalVectorSearchHit> hits = multimodalVectorStore.search(new MultimodalVectorSearchRequest(
                embedding.vector(),
                "image",
                criteria.tenantId(),
                criteria.knowledgeBaseIds(),
                imageContentTypes(criteria.contentTypes()),
                criteria.permissionTags(),
                Boolean.TRUE.equals(criteria.includeReviewPending()),
                topK,
                minScore
        ));
        List<RagSearchItem> items = new ArrayList<>();
        for (MultimodalVectorSearchHit hit : hits) {
            RagImageAsset asset = assetForHit(hit);
            Map<String, Object> metadata = metadata(hit, asset, embedding);
            items.add(new RagSearchItem(
                    items.size() + 1,
                    hit.score(),
                    firstNonNull(hit.knowledgeBaseId(), asset == null ? null : asset.getKnowledgeBaseId()),
                    firstNonBlank(stringValue(hit.documentId()), asset == null ? null : asset.getSourceDocumentId()),
                    asset == null ? null : asset.getImageCaption(),
                    firstNonBlank(hit.chunkUid(), asset == null ? null : asset.getChunkUid()),
                    null,
                    firstNonBlank(hit.contentType(), asset == null ? null : asset.getContentType()),
                    firstNonNull(hit.pageNo(), asset == null ? null : asset.getPageNo()),
                    firstNonBlank(hit.sectionTitle(), asset == null ? null : asset.getSectionTitle()),
                    asset == null ? null : asset.getImageCaption(),
                    asset == null ? null : asset.getImageNumber(),
                    asset == null ? null : asset.getImageUrl(),
                    content(asset),
                    metadata,
                    "image",
                    "image_vector",
                    asset == null ? null : asset.getId(),
                    hit.score()
            ));
        }
        return items;
    }

    private ImageEmbeddingRequest embeddingRequest(RagRetrievalCriteria criteria) {
        RagImageAsset asset = criteria.imageAssetId() == null ? null : imageAssetMapper.selectById(criteria.imageAssetId());
        if (asset != null) {
            Long tenantId = TenantContextHolder.currentTenantId().orElse(criteria.tenantId());
            if (tenantId != null && !tenantId.equals(asset.getTenantId())) {
                throw new IllegalArgumentException("Image asset belongs to another tenant");
            }
        }
        Path assetPath = null;
        if (asset != null && StringUtils.hasText(asset.getAssetPath())) {
            assetPath = Path.of(asset.getAssetPath());
        }
        return new ImageEmbeddingRequest(
                asset == null ? null : asset.getImageId(),
                assetPath,
                firstNonBlank(criteria.imageUrl(), asset == null ? null : asset.getImageUrl()),
                criteria.imageBase64(),
                null,
                ragProperties.getMultimodalIngest().getVisionEmbeddingDimension()
        );
    }

    private boolean allowsImage(RagRetrievalCriteria criteria) {
        if (criteria.modalities() == null || criteria.modalities().isEmpty()) {
            return true;
        }
        return criteria.modalities().stream()
                .filter(StringUtils::hasText)
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .anyMatch(value -> value.equals("image") || value.equals("multimodal"));
    }

    private List<String> imageContentTypes(List<String> contentTypes) {
        if (contentTypes == null || contentTypes.isEmpty()) {
            return List.of("image", "chart", "table");
        }
        return contentTypes;
    }

    private RagImageAsset assetForHit(MultimodalVectorSearchHit hit) {
        if (hit == null) {
            return null;
        }
        LambdaQueryWrapper<RagImageAsset> query = new LambdaQueryWrapper<RagImageAsset>()
                .eq(hit.tenantId() != null, RagImageAsset::getTenantId, hit.tenantId())
                .eq(RagImageAsset::getIsDeleted, 0)
                .last("LIMIT 1");
        if (StringUtils.hasText(hit.imageId())) {
            query.eq(RagImageAsset::getImageId, hit.imageId());
        } else if (StringUtils.hasText(hit.chunkUid())) {
            query.eq(RagImageAsset::getChunkUid, hit.chunkUid());
        } else {
            return null;
        }
        return imageAssetMapper.selectOne(query);
    }

    private Map<String, Object> metadata(MultimodalVectorSearchHit hit,
                                         RagImageAsset asset,
                                         VisionEmbeddingResult embedding) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (hit.metadata() != null) {
            metadata.putAll(hit.metadata());
        }
        if (asset != null) {
            metadata.put("image_asset_id", asset.getId());
            metadata.put("image_id", asset.getImageId());
            metadata.put("document_id", firstNonBlank(asset.getSourceDocumentId(), stringValue(asset.getDocumentId())));
            metadata.put("knowledge_base_id", stringValue(asset.getKnowledgeBaseId()));
            metadata.put("tenant_id", stringValue(asset.getTenantId()));
            metadata.put("content_type", asset.getContentType());
            metadata.put("image_url", asset.getImageUrl());
            metadata.put("page_no", asset.getPageNo());
            metadata.put("section_title", asset.getSectionTitle());
            metadata.put("image_caption", asset.getImageCaption());
            metadata.put("image_number", asset.getImageNumber());
            metadata.put("visual_confidence", asset.getVisualConfidence());
            metadata.put("visual_json", firstNonBlank(asset.getReviewUpdatedVisualJson(), asset.getVisualJson()));
            metadata.put("review_status", asset.getReviewStatus());
            metadata.put("ocr_confidence", asset.getOcrConfidence());
        }
        metadata.put("retrieval_source", "image_vector");
        metadata.put("modality", "image");
        metadata.put("image_embedding_model", embedding.model());
        metadata.put("image_embedding_provider", embedding.provider());
        metadata.put("fusion_score", hit.score());
        metadata.entrySet().removeIf(entry -> entry.getValue() == null);
        return metadata;
    }

    private String content(RagImageAsset asset) {
        if (asset == null) {
            return "";
        }
        StringBuilder content = new StringBuilder();
        append(content, "section_title", asset.getSectionTitle());
        append(content, "image_caption", asset.getImageCaption());
        append(content, "ocr_text", firstNonBlank(asset.getReviewUpdatedOcrText(), asset.getOcrText()));
        append(content, "visual_structured_content", firstNonBlank(asset.getReviewUpdatedVisualJson(), asset.getVisualJson()));
        return content.toString().trim();
    }

    private void append(StringBuilder content, String label, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        if (!content.isEmpty()) {
            content.append('\n');
        }
        content.append(label).append(": ").append(value.trim());
    }

    private String firstNonBlank(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }

    private <T> T firstNonNull(T first, T second) {
        return first != null ? first : second;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
