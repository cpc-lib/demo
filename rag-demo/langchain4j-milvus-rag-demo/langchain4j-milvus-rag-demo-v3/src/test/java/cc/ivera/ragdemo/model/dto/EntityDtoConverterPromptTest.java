package cc.ivera.ragdemo.model.dto;

import cc.ivera.ragdemo.domain.rag.RagAgentPrompt;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EntityDtoConverterPromptTest {

    @Test
    void agentPromptDtoIncludesTenantIdForFallbackDisplay() {
        RagAgentPrompt entity = new RagAgentPrompt();
        entity.setId(5L);
        entity.setTenantId(0L);
        entity.setPromptName("default");
        entity.setPromptContent("global prompt");
        entity.setVersion(2);
        entity.setStatus(1);

        RagAgentPromptDto dto = new EntityDtoConverter().toDto(entity);

        assertThat(dto.tenantId()).isEqualTo(0L);
        assertThat(dto.promptContent()).isEqualTo("global prompt");
    }
}
