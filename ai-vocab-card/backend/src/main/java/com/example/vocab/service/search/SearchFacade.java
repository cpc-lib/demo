package com.example.vocab.service.search;

import com.example.vocab.config.search.SearchProperties;
import com.example.vocab.dto.WordCardDTO;
import com.example.vocab.dto.search.SearchResultDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchFacade {
    private final List<WordSearchService> services;
    private final SearchProperties properties;

    public List<SearchResultDTO> keywordSearch(String keyword, int page, int size) {
        WordSearchService selected = active();
        try {
            List<SearchResultDTO> results = selected.keywordSearch(keyword, page, size);
            if ("mysql".equalsIgnoreCase(selected.provider()) || !results.isEmpty()) {
                return results;
            }
        } catch (Exception e) {
            if ("mysql".equalsIgnoreCase(selected.provider())) throw e;
        }
        return mysql()
                .filter(service -> service != selected)
                .map(service -> service.keywordSearch(keyword, page, size))
                .orElse(List.of());
    }

    public void index(WordCardDTO dto) {
        for (WordSearchService service : services) {
            if ("mysql".equals(service.provider()) || properties.isSyncElasticsearch()) {
                try { service.index(dto); } catch (Exception ignored) { }
            }
        }
    }

    private WordSearchService active() {
        return services.stream()
                .filter(s -> s.provider().equalsIgnoreCase(properties.getProvider()))
                .findFirst()
                .orElseGet(() -> services.stream().filter(s -> "mysql".equals(s.provider())).findFirst().orElseThrow());
    }

    private java.util.Optional<WordSearchService> mysql() {
        return services.stream().filter(s -> "mysql".equalsIgnoreCase(s.provider())).findFirst();
    }
}
