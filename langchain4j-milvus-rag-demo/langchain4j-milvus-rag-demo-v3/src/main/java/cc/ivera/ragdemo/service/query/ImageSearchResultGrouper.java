package cc.ivera.ragdemo.service.query;

import cc.ivera.ragdemo.model.query.RagSearchItem;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class ImageSearchResultGrouper {

    public RagImageSearchResponse group(Long queryLogId, List<RagSearchItem> items) {
        List<RagSearchItem> safeItems = items == null ? List.of() : List.copyOf(items);
        List<RagSearchItem> similarImages = new ArrayList<>();
        List<RagSearchItem> relatedKnowledge = new ArrayList<>();
        for (RagSearchItem item : safeItems) {
            if (isImageHit(item)) {
                similarImages.add(item);
            } else {
                relatedKnowledge.add(item);
            }
        }
        return new RagImageSearchResponse(
                queryLogId,
                List.copyOf(similarImages),
                List.copyOf(relatedKnowledge),
                safeItems
        );
    }

    private boolean isImageHit(RagSearchItem item) {
        if (item == null) {
            return false;
        }
        if (item.imageAssetId() != null || StringUtils.hasText(item.imageUrl())) {
            return true;
        }
        String modality = normalize(item.modality());
        String retrievalSource = normalize(item.retrievalSource());
        String contentType = normalize(item.contentType());
        return "image".equals(modality)
                || "image_vector".equals(retrievalSource)
                || List.of("image", "chart", "table", "flowchart", "architecture").contains(contentType);
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : "";
    }
}
