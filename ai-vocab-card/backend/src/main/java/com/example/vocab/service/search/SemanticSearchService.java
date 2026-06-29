package com.example.vocab.service.search;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.vocab.dto.WordCardDTO;
import com.example.vocab.dto.search.SearchResultDTO;
import com.example.vocab.entity.search.WordEmbedding;
import com.example.vocab.mapper.search.WordEmbeddingMapper;
import com.example.vocab.service.WordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SemanticSearchService {
    private final WordEmbeddingMapper embeddingMapper;
    private final WordService wordService;

    public void upsertEmbedding(WordCardDTO dto) {
        if (dto == null || dto.getId() == null) return;
        String content = buildContent(dto);
        WordEmbedding entity = embeddingMapper.selectOne(new LambdaQueryWrapper<WordEmbedding>()
                .eq(WordEmbedding::getWordCardId, dto.getId()).last("LIMIT 1"));
        if (entity == null) entity = new WordEmbedding();
        entity.setWordCardId(dto.getId());
        entity.setContent(content);
        entity.setKeywords(String.join(" ", tokenize(content)));
        entity.setVectorProvider("local-keyword-jaccard");
        if (entity.getId() == null) embeddingMapper.insert(entity); else embeddingMapper.updateById(entity);
    }

    public List<SearchResultDTO> semanticSearch(String query, int topK) {
        if (!StringUtils.hasText(query)) throw new IllegalArgumentException("query must not be blank");
        int limit = Math.min(Math.max(topK, 1), 50);
        Set<String> queryTokens = tokenize(query);
        List<WordEmbedding> candidates;
        try {
            candidates = embeddingMapper.semanticCandidates(query, Math.max(limit * 5, 50));
        } catch (Exception e) {
            candidates = embeddingMapper.selectList(new LambdaQueryWrapper<WordEmbedding>().last("LIMIT 500"));
        }
        return candidates.stream()
                .map(e -> Map.entry(e, score(queryTokens, tokenize(e.getContent() + " " + e.getKeywords()))))
                .filter(e -> e.getValue() > 0.0D)
                .sorted(Map.Entry.<WordEmbedding, Double>comparingByValue().reversed())
                .limit(limit)
                .map(e -> toResult(e.getKey(), e.getValue()))
                .toList();
    }

    private SearchResultDTO toResult(WordEmbedding embedding, double score) {
        WordCardDTO detail = wordService.detail(embedding.getWordCardId());
        return SearchResultDTO.builder()
                .id(detail.getId())
                .word(detail.getWord())
                .chineseMeaning(detail.getChineseMeaning())
                .englishDefinition(detail.getEnglishDefinition())
                .score(score)
                .source("local-semantic")
                .detail(detail)
                .build();
    }

    private String buildContent(WordCardDTO dto) {
        String slangs = Optional.ofNullable(dto.getSlangs()).orElse(List.of()).stream()
                .map(s -> s.getPhrase() + " " + s.getMeaning() + " " + s.getExample()).collect(Collectors.joining(" "));
        String examples = Optional.ofNullable(dto.getExamples()).orElse(List.of()).stream()
                .map(e -> e.getSentence() + " " + e.getTranslation() + " " + e.getScene()).collect(Collectors.joining(" "));
        return String.join(" ", List.of(
                safe(dto.getWord()), safe(dto.getEnglishDefinition()), safe(dto.getChineseMeaning()), safe(dto.getUsageNote()), slangs, examples,
                dto.getTags() == null ? "" : String.join(" ", dto.getTags())
        ));
    }

    private String safe(String v) { return v == null ? "" : v; }

    private Set<String> tokenize(String text) {
        if (!StringUtils.hasText(text)) return Set.of();
        String normalized = text.toLowerCase(Locale.ROOT)
                .replaceAll("[\\p{Punct}，。！？；：、（）【】《》“”‘’]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        Set<String> tokens = new LinkedHashSet<>();
        for (String token : normalized.split(" ")) if (token.length() >= 2) tokens.add(token);
        for (int i = 0; i < normalized.length() - 1; i++) {
            char a = normalized.charAt(i), b = normalized.charAt(i + 1);
            if (Character.UnicodeScript.of(a) == Character.UnicodeScript.HAN && Character.UnicodeScript.of(b) == Character.UnicodeScript.HAN) {
                tokens.add("" + a + b);
            }
        }
        return tokens;
    }

    private double score(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) return 0.0D;
        Set<String> intersection = new HashSet<>(a); intersection.retainAll(b);
        Set<String> union = new HashSet<>(a); union.addAll(b);
        return intersection.size() * 1.0D / union.size();
    }
}
