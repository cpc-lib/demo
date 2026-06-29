package com.example.vocab.service.vector;

import com.example.vocab.dto.WordCardDTO;
import com.example.vocab.dto.search.SearchResultDTO;
import com.example.vocab.service.search.SemanticSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
public class LocalVectorSearchProvider implements VectorSearchProvider {
    private final SemanticSearchService semanticSearchService;
    public String provider() { return "local"; }
    public void upsert(WordCardDTO dto) { semanticSearchService.upsertEmbedding(dto); }
    public List<SearchResultDTO> search(String query, int topK) { return semanticSearchService.semanticSearch(query, topK); }
}
