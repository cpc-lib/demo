package com.example.vocab.service.prompt;

import com.example.vocab.entity.prompt.PromptTemplateEntity;
import com.example.vocab.mapper.prompt.PromptTemplateMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PromptTemplateServiceTest {
    @Test
    void shouldUsePreciseDefaultPromptWhenNoTemplateExists() {
        PromptTemplateMapper mapper = mock(PromptTemplateMapper.class);
        when(mapper.selectOne(any())).thenReturn(null);

        String prompt = new PromptTemplateService(mapper).renderWordCardPrompt("awesome");

        assertTrue(prompt.contains("accurate English definition"));
        assertTrue(prompt.contains("natural Simplified Chinese"));
        assertTrue(prompt.contains("Do not invent slang"));
        assertTrue(prompt.contains("Word: awesome"));
    }

    @Test
    void shouldUpgradeLegacySeedPromptThatProducesGenericMeanings() {
        PromptTemplateMapper mapper = mock(PromptTemplateMapper.class);
        PromptTemplateEntity legacy = new PromptTemplateEntity();
        legacy.setContent("You are an English vocabulary assistant. Given an English word, return valid JSON only. The JSON schema is: {\"englishDefinition\":\"simple English definition\",\"chineseMeaning\":\"Chinese meaning\"}. Word: {{word}}");
        when(mapper.selectOne(any())).thenReturn(legacy);

        String prompt = new PromptTemplateService(mapper).renderWordCardPrompt("awesome");

        assertTrue(prompt.contains("accurate English definition"));
        assertTrue(prompt.contains("natural Simplified Chinese"));
        assertTrue(prompt.contains("Do not invent slang"));
    }
}
