package cc.ivera.ragdemo.model.dto;

import cc.ivera.ragdemo.domain.tenant.RagTenantModelConfig;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class EntityDtoConverterModelConfigTest {

    @Test
    void modelConfigDtoIncludesRuntimeParametersWithoutExposingApiKey() {
        RagTenantModelConfig entity = new RagTenantModelConfig();
        entity.setId(7L);
        entity.setApiKeySecretRef("encrypted-secret");
        entity.setProvider("openai-compatible");
        entity.setModelType("LLM");
        entity.setModelName("qwen-plus");
        entity.setBaseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1");
        entity.setTimeoutSeconds(45);
        entity.setMaxRetries(2);
        entity.setMaxTokens(8192);
        entity.setFrequencyPenalty(new BigDecimal("0.10"));
        entity.setPresencePenalty(new BigDecimal("0.20"));
        entity.setTopP(new BigDecimal("0.90"));

        RagTenantModelConfigDto dto = new EntityDtoConverter().toDto(entity);

        assertThat(dto.apiKeyConfigured()).isTrue();
        assertThat(dto.timeoutSeconds()).isEqualTo(45);
        assertThat(dto.maxRetries()).isEqualTo(2);
        assertThat(dto.maxTokens()).isEqualTo(8192);
        assertThat(dto.frequencyPenalty()).isEqualByComparingTo("0.10");
        assertThat(dto.presencePenalty()).isEqualByComparingTo("0.20");
        assertThat(dto.topP()).isEqualByComparingTo("0.90");
    }
}
