package com.example.vocab.service.vector;

import com.example.vocab.config.search.VectorProperties;
import com.example.vocab.dto.WordCardDTO;
import com.example.vocab.dto.search.SearchResultDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class VectorSearchFacadeTest {
    @Test
    void shouldFallbackToLocalSearchWhenMilvusReturnsNoResults() {
        VectorProperties properties = new VectorProperties();
        properties.setProvider("milvus-adapter");
        SearchResultDTO localResult = SearchResultDTO.builder().id(1L).word("awesome").source("local-semantic").build();
        VectorSearchFacade facade = new VectorSearchFacade(List.of(
                new StubVectorProvider("milvus", List.of(), false),
                new StubVectorProvider("local", List.of(localResult), false)
        ), properties);

        List<SearchResultDTO> results = facade.search("very good", 10);

        assertEquals(1, results.size());
        assertEquals("local-semantic", results.getFirst().getSource());
    }

    @Test
    void shouldFallbackToLocalUpsertWhenMilvusIsUnavailable() {
        VectorProperties properties = new VectorProperties();
        properties.setProvider("milvus-adapter");
        StubVectorProvider local = new StubVectorProvider("local", List.of(), false);
        VectorSearchFacade facade = new VectorSearchFacade(List.of(
                new StubVectorProvider("milvus", List.of(), true),
                local
        ), properties);

        assertDoesNotThrow(() -> facade.upsert(new WordCardDTO()));
        assertEquals(1, local.upsertCount);
    }

    private static final class StubVectorProvider implements VectorSearchProvider {
        private final String provider;
        private final List<SearchResultDTO> results;
        private final boolean fail;
        private int upsertCount;

        private StubVectorProvider(String provider, List<SearchResultDTO> results, boolean fail) {
            this.provider = provider;
            this.results = results;
            this.fail = fail;
        }

        @Override
        public String provider() {
            return provider;
        }

        @Override
        public void upsert(WordCardDTO dto) {
            if (fail) throw new IllegalStateException("provider unavailable");
            upsertCount++;
        }

        @Override
        public List<SearchResultDTO> search(String query, int topK) {
            if (fail) throw new IllegalStateException("provider unavailable");
            return results;
        }
    }
}
