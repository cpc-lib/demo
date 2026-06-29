package com.example.vocab.service.vector;

import com.example.vocab.config.search.VectorProperties;
import com.example.vocab.dto.WordCardDTO;
import com.example.vocab.dto.search.SearchResultDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VectorSearchFacade {
    private final List<VectorSearchProvider> providers;
    private final VectorProperties properties;

    public void upsert(WordCardDTO dto) {
        VectorSearchProvider selected = active();
        try {
            selected.upsert(dto);
            return;
        } catch (Exception ignored) { }
        local().filter(provider -> provider != selected).ifPresent(provider -> {
            try { provider.upsert(dto); } catch (Exception ignored) { }
        });
    }

    public List<SearchResultDTO> search(String query, int topK) {
        VectorSearchProvider selected = active();
        try {
            List<SearchResultDTO> results = selected.search(query, topK);
            if ("local".equalsIgnoreCase(selected.provider()) || !results.isEmpty()) {
                return results;
            }
        } catch (Exception ignored) { }
        return local()
                .filter(provider -> provider != selected)
                .map(provider -> provider.search(query, topK))
                .orElse(List.of());
    }

    private VectorSearchProvider active() {
        String configured = properties.getProvider();
        return providers.stream().filter(p -> p.provider().equalsIgnoreCase(configured) || ("milvus-adapter".equalsIgnoreCase(configured) && "milvus".equals(p.provider())))
                .findFirst()
                .orElseGet(() -> providers.stream().filter(p -> "local".equals(p.provider())).findFirst().orElseThrow());
    }

    private java.util.Optional<VectorSearchProvider> local() {
        return providers.stream().filter(p -> "local".equalsIgnoreCase(p.provider())).findFirst();
    }
}
