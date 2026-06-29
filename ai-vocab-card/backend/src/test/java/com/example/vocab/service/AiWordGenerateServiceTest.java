package com.example.vocab.service;

import com.example.vocab.dto.WordCardDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.vocab.config.LlmProperties;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AiWordGenerateServiceTest {
    @Test
    void shouldGenerateFallbackWordCardWhenLlmDisabled() {
        LlmProperties props = new LlmProperties();
        props.setEnabled(false);
        AiWordGenerateService service = new AiWordGenerateService(Optional.empty(), new ObjectMapper(), null, props, null);
        WordCardDTO dto = service.generate("Awesome");
        assertEquals("awesome", dto.getWord());
        assertNotNull(dto.getEnglishDefinition());
        assertFalse(dto.getExamples().isEmpty());
        assertFalse(dto.getSlangs().isEmpty());
    }

    @Test
    void shouldRejectBlankWord() {
        AiWordGenerateService service = new AiWordGenerateService(Optional.empty(), new ObjectMapper(), null, new LlmProperties(), null);
        assertThrows(IllegalArgumentException.class, () -> service.generate(" "));
    }

    @Test
    void shouldFailFastWhenLlmEnabledWithoutApiKey() {
        LlmProperties props = new LlmProperties();
        props.setEnabled(true);
        props.setApiKey("");
        AiWordGenerateService service = new AiWordGenerateService(Optional.empty(), new ObjectMapper(), null, props, null);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.generate("awesome"));
        assertTrue(ex.getMessage().contains("LLM_API_KEY"));
    }
}
