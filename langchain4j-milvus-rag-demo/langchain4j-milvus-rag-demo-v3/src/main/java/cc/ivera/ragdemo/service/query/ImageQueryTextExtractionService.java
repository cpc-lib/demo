package cc.ivera.ragdemo.service.query;

import cc.ivera.ragdemo.domain.rag.RagImageAsset;
import cc.ivera.ragdemo.mapper.RagImageAssetMapper;
import cc.ivera.ragdemo.model.knowledge.ContentType;
import cc.ivera.ragdemo.model.knowledge.ExtractedImageKnowledge;
import cc.ivera.ragdemo.model.query.RagRetrievalCriteria;
import cc.ivera.ragdemo.service.ingest.VisionStructuredAnalysisClient;
import cc.ivera.ragdemo.tenant.TenantContextHolder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.file.Path;
import java.util.Iterator;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class ImageQueryTextExtractionService {

    private static final int MAX_QUERY_TEXT_LENGTH = 4000;

    private final VisionStructuredAnalysisClient visionStructuredAnalysisClient;
    private final RagImageAssetMapper imageAssetMapper;
    private final ObjectMapper objectMapper;

    public Optional<String> extractQueryText(RagRetrievalCriteria criteria) {
        if (criteria == null || !hasImageInput(criteria)) {
            return Optional.empty();
        }
        RagImageAsset asset = loadAsset(criteria);
        String existingText = assetText(asset);
        if (StringUtils.hasText(existingText)) {
            return Optional.of(truncate(combine(criteria.query(), existingText)));
        }
        String analysis = visionStructuredAnalysisClient.analyze(toImage(criteria, asset));
        String analyzedText = analysisText(analysis);
        if (!StringUtils.hasText(analyzedText)) {
            return StringUtils.hasText(criteria.query())
                    ? Optional.of(truncate(criteria.query().trim()))
                    : Optional.empty();
        }
        return Optional.of(truncate(combine(criteria.query(), analyzedText)));
    }

    private RagImageAsset loadAsset(RagRetrievalCriteria criteria) {
        if (criteria.imageAssetId() == null) {
            return null;
        }
        RagImageAsset asset = imageAssetMapper.selectById(criteria.imageAssetId());
        if (asset == null || Integer.valueOf(1).equals(asset.getIsDeleted())) {
            throw new IllegalArgumentException("Image asset not found: " + criteria.imageAssetId());
        }
        TenantContextHolder.currentTenantId().ifPresent(tenantId -> {
            if (!tenantId.equals(asset.getTenantId())) {
                throw new IllegalArgumentException("Image asset belongs to another tenant");
            }
        });
        return asset;
    }

    private ExtractedImageKnowledge toImage(RagRetrievalCriteria criteria, RagImageAsset asset) {
        return new ExtractedImageKnowledge(
                asset == null ? "query-image" : asset.getImageId(),
                contentType(asset == null ? null : asset.getContentType()),
                assetPath(asset),
                firstNonBlank(criteria.imageBase64(), criteria.imageUrl(), asset == null ? null : asset.getImageUrl()),
                asset == null ? null : asset.getPageNo(),
                null,
                asset == null ? null : asset.getSectionTitle(),
                asset == null ? null : asset.getImageCaption(),
                asset == null ? null : asset.getImageNumber(),
                null,
                null,
                asset == null ? null : firstNonBlank(asset.getReviewUpdatedOcrText(), asset.getOcrText()),
                asset == null ? null : firstNonBlank(asset.getReviewUpdatedVisualJson(), asset.getVisualJson())
        );
    }

    private String assetText(RagImageAsset asset) {
        if (asset == null) {
            return null;
        }
        StringBuilder text = new StringBuilder();
        append(text, asset.getSectionTitle());
        append(text, asset.getImageCaption());
        append(text, asset.getImageNumber());
        append(text, firstNonBlank(asset.getReviewUpdatedOcrText(), asset.getOcrText()));
        append(text, firstNonBlank(asset.getReviewUpdatedVisualJson(), asset.getVisualJson()));
        return text.toString().trim();
    }

    private String analysisText(String analysis) {
        if (!StringUtils.hasText(analysis) || "{}".equals(analysis.trim())) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(analysis);
            if (StringUtils.hasText(root.path("analysisStatus").asText(null))
                    && "failed".equalsIgnoreCase(root.path("analysisStatus").asText())) {
                return null;
            }
            StringBuilder text = new StringBuilder();
            appendJson(text, root.path("contentType"));
            appendJson(text, root.path("title"));
            appendJson(text, root.path("caption"));
            appendJson(text, root.path("imageNumber"));
            appendJson(text, root.path("sectionTitle"));
            appendJson(text, root.path("ocrText"));
            appendJson(text, root.path("summary"));
            appendJson(text, root.path("insights"));
            appendJson(text, root.path("entities"));
            appendJson(text, root.path("relationships"));
            appendJson(text, root.path("steps"));
            String flattened = text.toString().trim();
            return StringUtils.hasText(flattened) ? flattened : analysis;
        } catch (Exception ignored) {
            return analysis;
        }
    }

    private void appendJson(StringBuilder text, JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return;
        }
        if (node.isValueNode()) {
            append(text, node.asText());
            return;
        }
        Iterator<JsonNode> elements = node.elements();
        while (elements.hasNext()) {
            JsonNode child = elements.next();
            appendJson(text, child);
        }
    }

    private void append(StringBuilder text, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        if (!text.isEmpty()) {
            text.append('\n');
        }
        text.append(value.trim());
    }

    private String combine(String originalQuery, String imageText) {
        if (!StringUtils.hasText(originalQuery)) {
            return imageText;
        }
        if (!StringUtils.hasText(imageText)) {
            return originalQuery;
        }
        return originalQuery.trim() + "\n" + imageText.trim();
    }

    private boolean hasImageInput(RagRetrievalCriteria criteria) {
        return criteria.imageAssetId() != null
                || StringUtils.hasText(criteria.imageUrl())
                || StringUtils.hasText(criteria.imageBase64());
    }

    private Path assetPath(RagImageAsset asset) {
        if (asset == null || !StringUtils.hasText(asset.getAssetPath())) {
            return null;
        }
        return Path.of(asset.getAssetPath());
    }

    private ContentType contentType(String value) {
        if (!StringUtils.hasText(value)) {
            return ContentType.IMAGE;
        }
        try {
            return ContentType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return ContentType.IMAGE;
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String truncate(String value) {
        if (value == null || value.length() <= MAX_QUERY_TEXT_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_QUERY_TEXT_LENGTH);
    }
}
