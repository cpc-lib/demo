package cc.ivera.ragdemo.controller;

import cc.ivera.ragdemo.model.query.RagApiResponse;
import cc.ivera.ragdemo.model.query.RagSearchItem;
import cc.ivera.ragdemo.model.query.RagSearchRequest;
import cc.ivera.ragdemo.model.query.RagSearchResponse;
import cc.ivera.ragdemo.service.query.ImageSearchResultGrouper;
import cc.ivera.ragdemo.service.query.RagImageSearchResponse;
import cc.ivera.ragdemo.service.query.RagQueryService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RagImageSearchControllerTest {

    @Test
    void multipartSearchConvertsImageFileAndGroupsResults() throws Exception {
        RagQueryService queryService = mock(RagQueryService.class);
        RagSearchItem imageHit = item(1, "image", "image_vector", 3L, "/api/knowledge/assets/doc/image.png");
        RagSearchItem textHit = item(2, "text", "text_vector", null, null);
        when(queryService.search(any(RagSearchRequest.class), anyString()))
                .thenReturn(new RagSearchResponse(88L, List.of(imageHit, textHit)));
        RagImageSearchController controller = new RagImageSearchController(queryService, new ImageSearchResultGrouper());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "diagram.png",
                "image/png",
                "png-bytes".getBytes(StandardCharsets.UTF_8)
        );

        RagApiResponse<RagImageSearchResponse> response = controller.searchMultipart(
                file,
                List.of(1L),
                "architecture",
                "vector",
                5,
                0.0D,
                true,
                List.of("image", "text"),
                List.of("internal")
        );

        assertThat(response.ok()).isTrue();
        assertThat(response.data().queryLogId()).isEqualTo(88L);
        assertThat(response.data().similarImages()).containsExactly(imageHit);
        assertThat(response.data().relatedKnowledge()).containsExactly(textHit);
        ArgumentCaptor<RagSearchRequest> captor = ArgumentCaptor.forClass(RagSearchRequest.class);
        verify(queryService).search(captor.capture(), anyString());
        assertThat(captor.getValue().imageBase64()).startsWith("data:image/png;base64,");
        assertThat(captor.getValue().query()).isEqualTo("architecture");
        assertThat(captor.getValue().knowledgeBaseIds()).containsExactly(1L);
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
