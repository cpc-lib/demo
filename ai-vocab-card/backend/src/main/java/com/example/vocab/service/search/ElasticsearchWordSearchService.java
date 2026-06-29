package com.example.vocab.service.search;

import com.example.vocab.dto.WordCardDTO;
import com.example.vocab.dto.search.SearchResultDTO;
import com.example.vocab.entity.search.WordSearchDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.util.List;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.search.provider", havingValue = "elasticsearch")
public class ElasticsearchWordSearchService implements WordSearchService {
    private final ElasticsearchOperations operations;

    @Override
    public List<SearchResultDTO> keywordSearch(String keyword, int page, int size) {
        String q = StringUtils.hasText(keyword) ? keyword.trim() : "*";
        NativeQuery query = NativeQuery.builder()
                .withQuery(builder -> builder.multiMatch(mm -> mm
                        .query(q)
                        .fields("word^4", "chineseMeaning^3", "englishDefinition^2", "tags")))
                .withMaxResults(Math.min(Math.max(size, 1), 100))
                .build();
        return operations.search(query, WordSearchDocument.class).stream()
                .map(this::toResult)
                .toList();
    }

    @Override
    public void index(WordCardDTO dto) {
        if (dto == null || dto.getId() == null) return;
        WordSearchDocument doc = WordSearchDocument.builder()
                .id(String.valueOf(dto.getId()))
                .word(dto.getWord())
                .englishDefinition(dto.getEnglishDefinition())
                .chineseMeaning(dto.getChineseMeaning())
                .tags(dto.getTags() == null ? "" : String.join(",", dto.getTags()))
                .build();
        operations.save(doc);
    }

    @Override
    public String provider() { return "elasticsearch"; }

    private SearchResultDTO toResult(SearchHit<WordSearchDocument> hit) {
        WordSearchDocument d = hit.getContent();
        return SearchResultDTO.builder()
                .id(Long.valueOf(d.getId()))
                .word(d.getWord())
                .chineseMeaning(d.getChineseMeaning())
                .englishDefinition(d.getEnglishDefinition())
                .score((double) hit.getScore())
                .source(provider())
                .build();
    }
}
