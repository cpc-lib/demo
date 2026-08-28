package cc.ivera.ragdemo.service.tool;

import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.domain.tenant.RagTenantModelConfig;
import cc.ivera.ragdemo.service.tenant.ModelConfigService;
import cc.ivera.ragdemo.tenant.TenantContextHolder;
import cc.ivera.ragdemo.util.ApiKeyEncryptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OpenAiImageClientTest {

    @Test
    void resolveRequestConfigUsesTenantImageModelConfig() {
        RagProperties props = new RagProperties();
        props.getImage().setEnabled(true);
        props.getLlm().setBaseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1");
        props.getLlm().setApiKey("sk-llm-key");
        ApiKeyEncryptor encryptor = new ApiKeyEncryptor("test-encryption-key-123");
        ModelConfigService modelConfigService = mock(ModelConfigService.class);
        RagTenantModelConfig imageConfig = new RagTenantModelConfig();
        imageConfig.setTenantId(9L);
        imageConfig.setModelType("IMAGE");
        imageConfig.setProvider("openai-compatible");
        imageConfig.setModelName("wanx-v1");
        imageConfig.setBaseUrl("https://dashscope.aliyuncs.com/api/v1");
        imageConfig.setApiKeySecretRef(encryptor.encrypt("sk-image-db-key"));
        imageConfig.setImageSize("1024x1024");
        imageConfig.setImageQuality("hd");
        imageConfig.setTimeoutSeconds(45);
        imageConfig.setPollIntervalMillis(1500);
        when(modelConfigService.getActiveImageConfig(9L)).thenReturn(imageConfig);
        OpenAiImageClient client = new OpenAiImageClient(props, new ObjectMapper(), modelConfigService, encryptor);

        OpenAiImageClient.ImageRequestConfig requestConfig = TenantContextHolder.callWith(
                TenantContextHolder.systemContext(9L, "test"),
                () -> client.resolveRequestConfig("768x512")
        );

        assertThat(requestConfig.modelName()).isEqualTo("wanx-v1");
        assertThat(requestConfig.baseUrl()).isEqualTo("https://dashscope.aliyuncs.com/api/v1");
        assertThat(requestConfig.apiBaseUrl()).isEqualTo("https://dashscope.aliyuncs.com/api/v1");
        assertThat(requestConfig.apiKey()).isEqualTo("sk-image-db-key");
        assertThat(requestConfig.size()).isEqualTo("768x512");
        assertThat(requestConfig.quality()).isEqualTo("hd");
        assertThat(requestConfig.timeoutSeconds()).isEqualTo(45);
        assertThat(requestConfig.pollIntervalMillis()).isEqualTo(1500);
    }
}
