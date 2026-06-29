package com.example.vocab.service.vector;

import com.example.vocab.config.vector.MilvusProperties;
import com.example.vocab.dto.WordCardDTO;
import com.example.vocab.dto.search.SearchResultDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Production boundary for Milvus. This class deliberately uses a small REST adapter contract so that
 * the business layer is not coupled to a specific Milvus Java SDK version.
 * Expected adapter endpoints:
 * POST /vectors/upsert  {collection, id, text, metadata}
 * POST /vectors/search  {collection, query, topK}
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.milvus", name = "enabled", havingValue = "true")
public class MilvusVectorSearchProvider implements VectorSearchProvider {
    private final MilvusProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();

    public String provider() { return "milvus"; }

    public void upsert(WordCardDTO dto) {
        if (dto == null || dto.getId() == null) return;
        String text = String.join(" ", dto.getWord(), dto.getEnglishDefinition(), dto.getChineseMeaning() == null ? "" : dto.getChineseMeaning());
        restTemplate.postForEntity(properties.getEndpoint() + "/vectors/upsert", new UpsertRequest(properties.getCollection(), dto.getId(), text, dto), Void.class);
    }

    public List<SearchResultDTO> search(String query, int topK) {
        ResponseEntity<SearchResultDTO[]> response = restTemplate.postForEntity(properties.getEndpoint() + "/vectors/search", new SearchRequest(properties.getCollection(), query, topK), SearchResultDTO[].class);
        SearchResultDTO[] body = response.getBody();
        return body == null ? List.of() : List.of(body);
    }

    public record UpsertRequest(String collection, Long id, String text, Object metadata) {}
    public record SearchRequest(String collection, String query, int topK) {}
}
