package com.example.vocab.service.search;

import com.example.vocab.dto.WordCardDTO;
import com.example.vocab.dto.search.SearchResultDTO;
import java.util.List;

public interface WordSearchService {
    List<SearchResultDTO> keywordSearch(String keyword, int page, int size);
    void index(WordCardDTO wordCard);
    String provider();
}
