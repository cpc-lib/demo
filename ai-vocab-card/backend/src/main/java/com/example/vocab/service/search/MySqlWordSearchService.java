package com.example.vocab.service.search;

import com.example.vocab.dto.WordCardDTO;
import com.example.vocab.dto.search.SearchResultDTO;
import com.example.vocab.service.WordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MySqlWordSearchService implements WordSearchService {
    private final WordService wordService;

    @Override
    public List<SearchResultDTO> keywordSearch(String keyword, int page, int size) {
        return wordService.search(keyword, page, size).stream()
                .map(dto -> SearchResultDTO.builder()
                        .id(dto.getId())
                        .word(dto.getWord())
                        .chineseMeaning(dto.getChineseMeaning())
                        .englishDefinition(dto.getEnglishDefinition())
                        .score(1.0D)
                        .source(provider())
                        .detail(dto)
                        .build())
                .toList();
    }

    @Override
    public void index(WordCardDTO wordCard) {
        // MySQL is already the source of truth, no external index action required.
    }

    @Override
    public String provider() { return "mysql"; }
}
