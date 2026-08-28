package cc.ivera.ragdemo.service.query;

import cc.ivera.ragdemo.model.query.RagSearchItem;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ImageSearchResultGrouperTest {

    @Test
    void splitsSimilarImagesAndRelatedKnowledge() {
        RagSearchItem imageHit = item(1, "image", "image_vector", 7L, "/api/knowledge/assets/doc/image.png");
        RagSearchItem textHit = item(2, "text", "text_vector", null, null);

        RagImageSearchResponse response = new ImageSearchResultGrouper().group(101L, List.of(imageHit, textHit));

        assertThat(response.queryLogId()).isEqualTo(101L);
        assertThat(response.items()).containsExactly(imageHit, textHit);
        assertThat(response.similarImages()).containsExactly(imageHit);
        assertThat(response.relatedKnowledge()).containsExactly(textHit);
    }

    private RagSearchItem item(int rank,
                               String modality,
                               String retrievalSource,
                               Long imageAssetId,
                               String imageUrl) {
        return new RagSearchItem(
                rank,
                0.9D,
                1L,
                "doc-1",
                "document.pdf",
                "chunk-" + rank,
                1,
                modality,
                rank,
                "Section",
                null,
                null,
                imageUrl,
                "content " + rank,
                Map.of(),
                modality,
                retrievalSource,
                imageAssetId,
                0.9D
        );
    }
}
