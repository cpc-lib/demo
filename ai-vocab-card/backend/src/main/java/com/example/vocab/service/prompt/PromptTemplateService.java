package com.example.vocab.service.prompt;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.vocab.dto.prompt.PromptTemplateDTO;
import com.example.vocab.entity.prompt.PromptTemplateEntity;
import com.example.vocab.mapper.prompt.PromptTemplateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PromptTemplateService {
    public static final String WORD_CARD_GENERATE = "WORD_CARD_GENERATE";
    private final PromptTemplateMapper mapper;

    public String renderWordCardPrompt(String word) {
        PromptTemplateEntity tpl = mapper.selectOne(new LambdaQueryWrapper<PromptTemplateEntity>()
                .eq(PromptTemplateEntity::getCode, WORD_CARD_GENERATE)
                .eq(PromptTemplateEntity::getEnabled, 1)
                .orderByDesc(PromptTemplateEntity::getUpdatedAt)
                .last("LIMIT 1"));
        String content = tpl == null || isLegacyDefaultPrompt(tpl.getContent())
                ? defaultWordPrompt()
                : tpl.getContent();
        return content.replace("{{word}}", word);
    }

    public List<PromptTemplateDTO> list(String code) {
        LambdaQueryWrapper<PromptTemplateEntity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(code)) wrapper.eq(PromptTemplateEntity::getCode, code);
        wrapper.orderByDesc(PromptTemplateEntity::getUpdatedAt);
        return mapper.selectList(wrapper).stream().map(this::toDto).toList();
    }

    @Transactional
    public Long save(PromptTemplateDTO dto) {
        if (!StringUtils.hasText(dto.getCode()) || !StringUtils.hasText(dto.getVersion()) || !StringUtils.hasText(dto.getContent())) {
            throw new IllegalArgumentException("code, version and content are required");
        }
        PromptTemplateEntity entity = new PromptTemplateEntity();
        entity.setId(dto.getId());
        entity.setCode(dto.getCode().trim().toUpperCase());
        entity.setVersion(dto.getVersion().trim());
        entity.setTitle(dto.getTitle());
        entity.setContent(dto.getContent());
        entity.setEnabled(dto.getEnabled() == null ? 1 : dto.getEnabled());
        if (entity.getId() == null) mapper.insert(entity); else mapper.updateById(entity);
        return entity.getId();
    }

    public void disable(Long id) {
        PromptTemplateEntity entity = mapper.selectById(id);
        if (entity == null) throw new IllegalArgumentException("prompt template not found");
        entity.setEnabled(0);
        mapper.updateById(entity);
    }

    private PromptTemplateDTO toDto(PromptTemplateEntity e) {
        PromptTemplateDTO dto = new PromptTemplateDTO();
        dto.setId(e.getId()); dto.setCode(e.getCode()); dto.setVersion(e.getVersion()); dto.setTitle(e.getTitle()); dto.setContent(e.getContent()); dto.setEnabled(e.getEnabled());
        return dto;
    }

    private boolean isLegacyDefaultPrompt(String content) {
        return !StringUtils.hasText(content)
                || (content.contains("\"englishDefinition\":\"simple English definition\"")
                && content.contains("\"chineseMeaning\":\"Chinese meaning\""));
    }

    private String defaultWordPrompt() {
        return """
You are an English vocabulary assistant for Chinese learners. Given one English word, return valid JSON only.
Quality requirements:
- Give an accurate English definition for the most common learner meaning of the word.
- The English definition must be specific to this word, not a generic phrase.
- Give a natural Simplified Chinese meaning that matches the English definition.
- If the word has multiple common meanings, mention the main alternatives briefly in usageNote.
- Do not invent slang, idioms, or examples. Use an empty slangs array when there is no common informal expression.
- Examples must be natural, short, and translated accurately into Simplified Chinese.
- Return one JSON object only. Do not wrap it in markdown.

The exact JSON schema is:
{
  "word": "lowercase word",
  "phonetic": "IPA pronunciation if known",
  "partOfSpeech": "noun/verb/adjective/...",
  "englishDefinition": "accurate English definition",
  "chineseMeaning": "natural Simplified Chinese meaning",
  "usageNote": "short usage note",
  "slangs": [{"phrase":"...","meaning":"...","example":"..."}],
  "examples": [{"sentence":"...","translation":"...","scene":"..."}],
  "tags": ["learning tag"]
}
Word: {{word}}
""";
    }
}
