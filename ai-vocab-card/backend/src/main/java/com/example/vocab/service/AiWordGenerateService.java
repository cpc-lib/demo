package com.example.vocab.service;

import com.example.vocab.dto.*;
import com.example.vocab.service.prompt.PromptTemplateService;
import com.example.vocab.config.LlmProperties;
import com.example.vocab.entity.ai.AiUsageLog;
import com.example.vocab.mapper.ai.AiUsageLogMapper;
import com.example.vocab.security.CurrentUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AiWordGenerateService {
    private final Optional<OpenAiChatModel> chatModel;
    private final ObjectMapper objectMapper;
    private final AiUsageLogMapper aiUsageLogMapper;
    private final LlmProperties llmProperties;
    private final PromptTemplateService promptTemplateService;

    public WordCardDTO generate(String word) {
        String normalized = normalize(word);
        if (llmProperties.isEnabled()) {
            if (!StringUtils.hasText(llmProperties.getApiKey())) {
                throw new IllegalStateException("LLM_API_KEY is required when LLM is enabled");
            }
            if (chatModel.isEmpty()) {
                throw new IllegalStateException("LangChain4j chat model is not available");
            }
            try {
                String prompt = promptTemplateService.renderWordCardPrompt(normalized);
                String json = chatModel.get().chat(prompt);
                recordUsage(normalized, "SUCCESS", null, prompt.length(), json == null ? 0 : json.length());
                WordCardDTO dto = objectMapper.readValue(cleanJson(json), WordCardDTO.class);
                dto.setWord(normalize(dto.getWord()));
                validateAiResult(dto);
                return dto;
            } catch (Exception e) {
                recordUsage(normalized, "FAILED", e.getMessage(), 0, 0);
                throw new IllegalStateException("LangChain4j generation failed: " + e.getMessage(), e);
            }
        }
        return fallback(normalized);
    }

    private String normalize(String word) {
        if (!StringUtils.hasText(word)) throw new IllegalArgumentException("word must not be blank");
        return word.trim().toLowerCase();
    }

    private String cleanJson(String content) {
        String text = content == null ? "" : content.trim();
        if (text.startsWith("```json")) text = text.substring(7);
        if (text.startsWith("```")) text = text.substring(3);
        if (text.endsWith("```")) text = text.substring(0, text.length() - 3);
        return text.trim();
    }

    private void validateAiResult(WordCardDTO dto) {
        if (!StringUtils.hasText(dto.getWord()) || !StringUtils.hasText(dto.getEnglishDefinition())) {
            throw new IllegalArgumentException("invalid AI result");
        }
    }

    private void recordUsage(String input, String status, String error, int promptChars, int completionChars) {
        AiUsageLog log = new AiUsageLog();
        log.setUserId(CurrentUser.id());
        log.setModel(llmProperties.getModel());
        log.setRequestType("WORD_GENERATE");
        log.setInputText(input);
        log.setPromptTokens(Math.max(1, promptChars / 4));
        log.setCompletionTokens(Math.max(0, completionChars / 4));
        log.setTotalTokens(log.getPromptTokens() + log.getCompletionTokens());
        log.setStatus(status);
        log.setErrorMessage(error);
        aiUsageLogMapper.insert(log);
    }

    private WordCardDTO fallback(String w) {
        WordCardDTO dto = new WordCardDTO();
        dto.setWord(w);
        dto.setPhonetic("/" + w + "/");
        dto.setPartOfSpeech("word");
        dto.setEnglishDefinition("A useful English word. Enable LangChain4j with an OpenAI-compatible API key to generate a precise definition.");
        dto.setChineseMeaning("可编辑的中文含义；配置大模型后自动生成更准确结果");
        dto.setUsageNote("This is a local fallback preview. Review and edit before saving.");
        SlangDTO slang = new SlangDTO();
        slang.setPhrase("That's " + w + "!");
        slang.setMeaning("An informal expression using the word in conversation.");
        slang.setExample("You finished the task quickly. That's " + w + "!");
        ExampleDTO ex = new ExampleDTO();
        ex.setSentence("This is an example sentence with the word \"" + w + "\".");
        ex.setTranslation("这是一个包含该英文单词的例句。");
        ex.setScene("daily conversation");
        dto.setSlangs(List.of(slang));
        dto.setExamples(List.of(ex));
        dto.setTags(List.of("langchain4j", "editable", "fallback"));
        return dto;
    }
}
