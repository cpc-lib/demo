package com.example.vocab.service.search;

import com.example.vocab.config.search.SearchProperties;
import com.example.vocab.dto.WordCardDTO;
import com.example.vocab.dto.search.SearchResultDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SearchFacadeTest {
    @Test
    void shouldFallbackToMysqlWhenConfiguredSearchReturnsNoResults() {
        SearchProperties properties = new SearchProperties();
        properties.setProvider("elasticsearch");
        SearchResultDTO mysqlResult = SearchResultDTO.builder().id(1L).word("awesome").source("mysql").build();
        SearchFacade facade = new SearchFacade(List.of(
                new StubSearchService("elasticsearch", List.of()),
                new StubSearchService("mysql", List.of(mysqlResult))
        ), properties);

        List<SearchResultDTO> results = facade.keywordSearch("awesome", 1, 20);

        assertEquals(1, results.size());
        assertEquals("mysql", results.getFirst().getSource());
    }

    private record StubSearchService(String provider, List<SearchResultDTO> results) implements WordSearchService {
        @Override
        public List<SearchResultDTO> keywordSearch(String keyword, int page, int size) {
            return results;
        }

        @Override
        public void index(WordCardDTO wordCard) {
        }
    }
}
