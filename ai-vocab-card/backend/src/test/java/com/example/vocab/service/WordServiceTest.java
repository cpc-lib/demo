package com.example.vocab.service;

import com.example.vocab.dto.WordCardDTO;
import com.example.vocab.entity.WordCard;
import com.example.vocab.mapper.WordCardMapper;
import com.example.vocab.mapper.WordExampleMapper;
import com.example.vocab.mapper.WordSlangMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WordServiceTest {
    @Test
    void shouldClearAllSearchCachesAfterSavingWord() {
        WordCardMapper wordCardMapper = mock(WordCardMapper.class);
        WordSlangMapper wordSlangMapper = mock(WordSlangMapper.class);
        WordExampleMapper wordExampleMapper = mock(WordExampleMapper.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.keys("word:search:*")).thenReturn(Set.of("word:search:awesome:1:20", "word:search:great:1:20"));
        when(wordCardMapper.insert(any(WordCard.class))).thenAnswer(invocation -> {
            WordCard card = invocation.getArgument(0);
            card.setId(7L);
            return 1;
        });

        WordService service = new WordService(wordCardMapper, wordSlangMapper, wordExampleMapper, redisTemplate, new ObjectMapper());
        WordCardDTO dto = new WordCardDTO();
        dto.setWord("Awesome");
        dto.setEnglishDefinition("very impressive or enjoyable");

        Long id = service.save(dto);

        assertEquals(7L, id);
        verify(redisTemplate).delete("word:detail:7");
        verify(redisTemplate).delete(Set.of("word:search:awesome:1:20", "word:search:great:1:20"));
        ArgumentCaptor<WordCard> cardCaptor = ArgumentCaptor.forClass(WordCard.class);
        verify(wordCardMapper).insert(cardCaptor.capture());
        assertEquals(1, cardCaptor.getValue().getStatus());
    }
}
