package com.example.vocab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.vocab.dto.*;
import com.example.vocab.entity.*;
import com.example.vocab.mapper.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WordService {
    private final WordCardMapper wordCardMapper;
    private final WordSlangMapper wordSlangMapper;
    private final WordExampleMapper wordExampleMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public Long save(WordCardDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getWord()) || !StringUtils.hasText(dto.getEnglishDefinition())) {
            throw new IllegalArgumentException("word and englishDefinition are required");
        }
        WordCard existed = wordCardMapper.selectOne(new LambdaQueryWrapper<WordCard>()
                .eq(WordCard::getWord, dto.getWord().trim().toLowerCase())
                .last("LIMIT 1"));

        WordCard card = new WordCard();
        card.setId(dto.getId() != null ? dto.getId() : existed == null ? null : existed.getId());
        card.setWord(dto.getWord().trim().toLowerCase());
        card.setPhonetic(dto.getPhonetic());
        card.setPartOfSpeech(dto.getPartOfSpeech());
        card.setEnglishDefinition(dto.getEnglishDefinition());
        card.setChineseMeaning(dto.getChineseMeaning());
        card.setUsageNote(dto.getUsageNote());
        card.setTags(String.join(",", Optional.ofNullable(dto.getTags()).orElse(List.of())));
        card.setStatus(1);
        if (card.getId() == null) wordCardMapper.insert(card); else wordCardMapper.updateById(card);

        wordSlangMapper.delete(new LambdaQueryWrapper<WordSlang>().eq(WordSlang::getWordCardId, card.getId()));
        wordExampleMapper.delete(new LambdaQueryWrapper<WordExample>().eq(WordExample::getWordCardId, card.getId()));
        for (SlangDTO s : Optional.ofNullable(dto.getSlangs()).orElse(List.of())) {
            if (!StringUtils.hasText(s.getPhrase())) continue;
            WordSlang slang = new WordSlang();
            slang.setWordCardId(card.getId());
            slang.setPhrase(s.getPhrase());
            slang.setMeaning(s.getMeaning());
            slang.setExample(s.getExample());
            wordSlangMapper.insert(slang);
        }
        for (ExampleDTO e : Optional.ofNullable(dto.getExamples()).orElse(List.of())) {
            if (!StringUtils.hasText(e.getSentence())) continue;
            WordExample ex = new WordExample();
            ex.setWordCardId(card.getId());
            ex.setSentence(e.getSentence());
            ex.setTranslation(e.getTranslation());
            ex.setScene(e.getScene());
            wordExampleMapper.insert(ex);
        }
        clearCaches(card.getId());
        return card.getId();
    }

    public List<WordCardDTO> search(String keyword, int page, int size) {
        return searchPage(keyword, page, size).getItems();
    }

    public WordSearchPageDTO searchPage(String keyword, int page, int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        String normalized = keyword == null ? "" : keyword.trim();
        String cacheKey = "word:search:page:" + normalized + ":" + safePage + ":" + safeSize;
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (StringUtils.hasText(cached)) {
                return objectMapper.readValue(cached, WordSearchPageDTO.class);
            }
        } catch (Exception ignored) { }
        int offset = (safePage - 1) * safeSize;
        List<WordCardDTO> items = wordCardMapper.search(normalized, offset, safeSize)
                .stream().map(this::toBrief).toList();
        long total = wordCardMapper.countSearch(normalized);
        WordSearchPageDTO result = new WordSearchPageDTO(total, safePage, safeSize, items);
        try { redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(result), java.time.Duration.ofMinutes(5)); } catch (Exception ignored) { }
        return result;
    }

    public WordCardDTO detail(Long id) {
        String cacheKey = "word:detail:" + id;
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (StringUtils.hasText(cached)) return objectMapper.readValue(cached, WordCardDTO.class);
        } catch (Exception ignored) { }
        WordCard card = wordCardMapper.selectById(id);
        if (card == null || Objects.equals(card.getStatus(), 0)) throw new IllegalArgumentException("word card not found");
        WordCardDTO dto = toBrief(card);
        dto.setSlangs(wordSlangMapper.selectList(new LambdaQueryWrapper<WordSlang>()
                        .eq(WordSlang::getWordCardId, id))
                .stream().map(s -> {
                    SlangDTO x = new SlangDTO();
                    x.setPhrase(s.getPhrase()); x.setMeaning(s.getMeaning()); x.setExample(s.getExample());
                    return x;
                }).toList());
        dto.setExamples(wordExampleMapper.selectList(new LambdaQueryWrapper<WordExample>()
                        .eq(WordExample::getWordCardId, id))
                .stream().map(e -> {
                    ExampleDTO x = new ExampleDTO();
                    x.setSentence(e.getSentence()); x.setTranslation(e.getTranslation()); x.setScene(e.getScene());
                    return x;
                }).toList());
        try { redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(dto), java.time.Duration.ofHours(1)); } catch (Exception ignored) { }
        return dto;
    }

    private WordCardDTO toBrief(WordCard c) {
        WordCardDTO dto = new WordCardDTO();
        dto.setId(c.getId()); dto.setWord(c.getWord()); dto.setPhonetic(c.getPhonetic()); dto.setPartOfSpeech(c.getPartOfSpeech());
        dto.setEnglishDefinition(c.getEnglishDefinition()); dto.setChineseMeaning(c.getChineseMeaning()); dto.setUsageNote(c.getUsageNote());
        dto.setTags(c.getTags() == null || c.getTags().isBlank() ? List.of() : Arrays.stream(c.getTags().split(",")).filter(StringUtils::hasText).collect(Collectors.toList()));
        return dto;
    }

    private void clearCaches(Long cardId) {
        try {
            redisTemplate.delete("word:detail:" + cardId);
            Set<String> searchKeys = redisTemplate.keys("word:search:*");
            if (searchKeys != null && !searchKeys.isEmpty()) {
                redisTemplate.delete(searchKeys);
            }
        } catch (Exception ignored) { }
    }
}
