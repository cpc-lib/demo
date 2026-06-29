package com.example.vocab.service.vector;

import com.example.vocab.dto.WordCardDTO;
import com.example.vocab.dto.search.SearchResultDTO;
import java.util.List;

public interface VectorSearchProvider {
    String provider();
    void upsert(WordCardDTO dto);
    List<SearchResultDTO> search(String query, int topK);
}
