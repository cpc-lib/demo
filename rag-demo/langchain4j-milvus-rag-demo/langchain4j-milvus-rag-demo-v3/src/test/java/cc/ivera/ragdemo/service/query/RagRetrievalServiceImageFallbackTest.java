package cc.ivera.ragdemo.service.query;

import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.model.query.RagRetrievalCriteria;
import cc.ivera.ragdemo.model.query.RagSearchItem;
import cc.ivera.ragdemo.service.rag.Retriever;
import cc.ivera.ragdemo.service.ragops.RetrievalFusionPolicy;
import cc.ivera.ragdemo.service.ragops.RetrievalModePolicy;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RagRetrievalServiceImageFallbackTest {

    @Test
    void vectorSearchFallsBackToTextRetrievalWhenImageVectorIsDisabled() {
        Retriever retriever = mock(Retriever.class);
        RagSearchItemConverter converter = mock(RagSearchItemConverter.class);
        KeywordChunkSearchService keywordSearchService = mock(KeywordChunkSearchService.class);
        RagReranker reranker = mock(RagReranker.class);
        ImageVectorRetrievalService imageVectorRetrievalService = mock(ImageVectorRetrievalService.class);
        ImageQueryTextExtractionService imageQueryTextExtractionService = mock(ImageQueryTextExtractionService.class);
        RagSearchItem expected = new RagSearchItem(
                1,
                0.91D,
                1L,
                "doc-1",
                "architecture.pdf",
                "chunk-1",
                1,
                "text",
                3,
                "Architecture",
                null,
                null,
                null,
                "The architecture diagram contains an API gateway and login service.",
                Map.of("retrieval_source", "text_vector"),
                "text",
                "text_vector",
                null,
                null
        );
        RagRetrievalCriteria criteria = new RagRetrievalCriteria(
                null,
                null,
                null,
                "data:image/png;base64,AA==",
                List.of("image"),
                0L,
                List.of(1L),
                "vector",
                5,
                0.0D,
                0.0D,
                1.0D,
                0.0D,
                true,
                List.of("image", "chart", "table"),
                List.of()
        );

        when(imageVectorRetrievalService.hasImageInput(any())).thenReturn(true);
        when(imageVectorRetrievalService.enabled()).thenReturn(false);
        when(imageQueryTextExtractionService.extractQueryText(any()))
                .thenReturn(Optional.of("architecture diagram with login service"));
        when(retriever.search(any())).thenReturn(List.of());
        when(converter.fromMatches(any())).thenReturn(List.of(expected));

        RagRetrievalService service = new RagRetrievalService(
                retriever,
                converter,
                keywordSearchService,
                new RetrievalModePolicy(),
                reranker,
                new RagProperties(),
                new RetrievalFusionPolicy(),
                imageVectorRetrievalService,
                imageQueryTextExtractionService
        );

        List<RagSearchItem> items = service.retrieve(criteria);

        assertThat(items).containsExactly(expected);
        ArgumentCaptor<RagRetrievalCriteria> captor = ArgumentCaptor.forClass(RagRetrievalCriteria.class);
        verify(retriever).search(captor.capture());
        assertThat(captor.getValue().query()).contains("architecture diagram").contains("login service");
        assertThat(captor.getValue().imageBase64()).isNull();
        assertThat(captor.getValue().contentTypes()).contains("text", "image", "chart", "table");
    }
}
