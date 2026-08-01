package cc.ivera.ragdemo.service.tenant;

import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.domain.tenant.RagTenantModelConfig;
import cc.ivera.ragdemo.mapper.RagTenantModelConfigMapper;
import cc.ivera.ragdemo.model.query.ModelConfigUpsertRequest;
import cc.ivera.ragdemo.util.ApiKeyEncryptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ModelConfigServiceTest {

    @Test
    void modelConfigRequestAcceptsLongApiKeyValues() {
        String longApiKey = "sk-" + "a".repeat(300);
        ModelConfigUpsertRequest request = ModelConfigUpsertRequest.builder()
                .modelType("LLM")
                .provider("openai-compatible")
                .modelName("qwen-max")
                .apiKeySecretRef(longApiKey)
                .build();

        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            assertThat(validator.validate(request))
                    .noneMatch(violation -> "apiKeySecretRef".equals(violation.getPropertyPath().toString()));
        }
    }

    @Test
    void modelConfigRequestAcceptsImageTypeAndImageParameters() {
        ModelConfigUpsertRequest request = ModelConfigUpsertRequest.builder()
                .modelType("IMAGE")
                .provider("openai-compatible")
                .modelName("wanx-v1")
                .baseUrl("https://dashscope.aliyuncs.com/api/v1")
                .imageSize("1024x1024")
                .imageQuality("standard")
                .pollIntervalMillis(2000)
                .timeoutSeconds(60)
                .build();

        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            assertThat(validator.validate(request))
                    .noneMatch(violation -> "modelType".equals(violation.getPropertyPath().toString()))
                    .noneMatch(violation -> "imageSize".equals(violation.getPropertyPath().toString()))
                    .noneMatch(violation -> "pollIntervalMillis".equals(violation.getPropertyPath().toString()));
        }
    }

    @Test
    void getActiveImageConfigFallsBackToImageYmlProperties() {
        RagTenantModelConfigMapper mapper = mock(RagTenantModelConfigMapper.class);
        ApiKeyEncryptor encryptor = new ApiKeyEncryptor("test-encryption-key-123");
        RagProperties props = imageFallbackProperties("sk-image-yml-key");
        ModelConfigService service = new ModelConfigService(
                mapper,
                mockProvider(null),
                mockProvider(props),
                new ObjectMapper(),
                mock(ApplicationEventPublisher.class),
                encryptor
        );
        when(mapper.selectOne(any())).thenReturn(null);

        RagTenantModelConfig config = service.getActiveImageConfig(9L);
        String apiKey = service.revealActiveApiKey(9L, "IMAGE");

        assertThat(config.getModelType()).isEqualTo("IMAGE");
        assertThat(config.getModelName()).isEqualTo("wanx-v1");
        assertThat(config.getBaseUrl()).isEqualTo("https://dashscope.aliyuncs.com/api/v1");
        assertThat(config.getImageSize()).isEqualTo("1024x1024");
        assertThat(config.getImageQuality()).isEqualTo("standard");
        assertThat(config.getPollIntervalMillis()).isEqualTo(2000);
        assertThat(config.getTimeoutSeconds()).isEqualTo(60);
        assertThat(apiKey).isEqualTo("sk-image-yml-key");
    }

    @Test
    void listConfigsIncludesEffectiveFallbackModelsWhenTenantHasNoOwnedConfigs() {
        RagTenantModelConfigMapper mapper = mock(RagTenantModelConfigMapper.class);
        RagProperties props = allModelFallbackProperties();
        ModelConfigService service = new ModelConfigService(
                mapper,
                mockProvider(null),
                mockProvider(props),
                new ObjectMapper(),
                mock(ApplicationEventPublisher.class),
                new ApiKeyEncryptor("test-encryption-key-123")
        );
        when(mapper.selectList(any())).thenReturn(List.of());
        when(mapper.selectOne(any())).thenReturn(null);

        List<RagTenantModelConfig> configs = service.listConfigs(9L);

        assertThat(configs)
                .extracting(RagTenantModelConfig::getModelType)
                .containsExactly("LLM", "EMBEDDING", "IMAGE");
        assertThat(configs)
                .allMatch(config -> config.getId() == null)
                .allMatch(RagTenantModelConfig::getEnabled);
        assertThat(configs)
                .filteredOn(config -> "IMAGE".equals(config.getModelType()))
                .singleElement()
                .satisfies(config -> {
                    assertThat(config.getModelName()).isEqualTo("wanx-v1");
                    assertThat(config.getImageSize()).isEqualTo("1024x1024");
                });
    }

    @Test
    void updateConfigByIdPreservesExistingApiKeyWhenRequestKeyIsBlank() {
        RagTenantModelConfigMapper mapper = mock(RagTenantModelConfigMapper.class);
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        ModelConfigService service = new ModelConfigService(
                mapper,
                mockProvider(null),
                mockProvider(null),
                new ObjectMapper(),
                publisher,
                new ApiKeyEncryptor("test-encryption-key-123")
        );
        RagTenantModelConfig existing = new RagTenantModelConfig();
        existing.setId(7L);
        existing.setTenantId(9L);
        existing.setModelType("LLM");
        existing.setProvider("openai-compatible");
        existing.setModelName("qwen-plus");
        existing.setApiKeySecretRef("encrypted-existing-key");
        existing.setEnabled(false);
        when(mapper.selectById(7L)).thenReturn(existing);
        when(mapper.updateById(any(RagTenantModelConfig.class))).thenReturn(1);
        ModelConfigUpsertRequest request = ModelConfigUpsertRequest.builder()
                .modelType("LLM")
                .provider("openai-compatible")
                .modelName("qwen-max")
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .apiKeySecretRef("")
                .temperature(new BigDecimal("0.50"))
                .timeoutSeconds(45)
                .maxRetries(2)
                .maxTokens(8192)
                .frequencyPenalty(new BigDecimal("0.10"))
                .presencePenalty(new BigDecimal("0.20"))
                .topP(new BigDecimal("0.90"))
                .build();

        RagTenantModelConfig result = service.updateConfig(9L, 7L, request);

        assertThat(result.getApiKeySecretRef()).isEqualTo("encrypted-existing-key");
        assertThat(result.getModelName()).isEqualTo("qwen-max");
        assertThat(result.getEnabled()).isFalse();
        ArgumentCaptor<RagTenantModelConfig> captor = ArgumentCaptor.forClass(RagTenantModelConfig.class);
        verify(mapper).updateById(captor.capture());
        assertThat(captor.getValue().getMaxTokens()).isEqualTo(8192);
        assertThat(captor.getValue().getTopP()).isEqualByComparingTo("0.90");
    }

    @Test
    void updateConfigByIdReplacesExistingApiKeyWhenRequestKeyIsPresent() {
        RagTenantModelConfigMapper mapper = mock(RagTenantModelConfigMapper.class);
        ApiKeyEncryptor encryptor = new ApiKeyEncryptor("test-encryption-key-123");
        ModelConfigService service = new ModelConfigService(
                mapper,
                mockProvider(null),
                mockProvider(null),
                new ObjectMapper(),
                mock(ApplicationEventPublisher.class),
                encryptor
        );
        RagTenantModelConfig existing = new RagTenantModelConfig();
        existing.setId(7L);
        existing.setTenantId(9L);
        existing.setModelType("LLM");
        existing.setProvider("openai-compatible");
        existing.setModelName("qwen-plus");
        existing.setApiKeySecretRef(encryptor.encrypt("old-api-key"));
        existing.setEnabled(false);
        when(mapper.selectById(7L)).thenReturn(existing);
        when(mapper.updateById(any(RagTenantModelConfig.class))).thenReturn(1);
        String newApiKey = "sk-" + "b".repeat(300);
        ModelConfigUpsertRequest request = ModelConfigUpsertRequest.builder()
                .modelType("LLM")
                .provider("openai-compatible")
                .modelName("qwen-max")
                .apiKeySecretRef(newApiKey)
                .build();

        RagTenantModelConfig result = service.updateConfig(9L, 7L, request);

        assertThat(result.getApiKeySecretRef()).startsWith("ENC:");
        assertThat(encryptor.decrypt(result.getApiKeySecretRef())).isEqualTo(newApiKey);
    }

    @Test
    void revealApiKeyByIdDecryptsOwnedConfigSecret() {
        RagTenantModelConfigMapper mapper = mock(RagTenantModelConfigMapper.class);
        ApiKeyEncryptor encryptor = new ApiKeyEncryptor("test-encryption-key-123");
        ModelConfigService service = new ModelConfigService(
                mapper,
                mockProvider(null),
                mockProvider(null),
                new ObjectMapper(),
                mock(ApplicationEventPublisher.class),
                encryptor
        );
        RagTenantModelConfig existing = new RagTenantModelConfig();
        existing.setId(7L);
        existing.setTenantId(9L);
        existing.setModelType("LLM");
        existing.setApiKeySecretRef(encryptor.encrypt("sk-current-key"));
        existing.setEnabled(true);
        when(mapper.selectById(7L)).thenReturn(existing);

        String apiKey = service.revealApiKey(9L, 7L);

        assertThat(apiKey).isEqualTo("sk-current-key");
    }

    @Test
    void updateApiKeyByIdAllowsEnabledConfigAndKeepsItEnabled() {
        RagTenantModelConfigMapper mapper = mock(RagTenantModelConfigMapper.class);
        ApiKeyEncryptor encryptor = new ApiKeyEncryptor("test-encryption-key-123");
        ModelConfigService service = new ModelConfigService(
                mapper,
                mockProvider(null),
                mockProvider(null),
                new ObjectMapper(),
                mock(ApplicationEventPublisher.class),
                encryptor
        );
        RagTenantModelConfig existing = new RagTenantModelConfig();
        existing.setId(7L);
        existing.setTenantId(9L);
        existing.setModelType("LLM");
        existing.setApiKeySecretRef(encryptor.encrypt("old-api-key"));
        existing.setEnabled(true);
        when(mapper.selectById(7L)).thenReturn(existing);
        when(mapper.updateById(any(RagTenantModelConfig.class))).thenReturn(1);

        RagTenantModelConfig result = service.updateApiKey(9L, 7L, "sk-rotated-key");

        assertThat(result.getEnabled()).isTrue();
        assertThat(encryptor.decrypt(result.getApiKeySecretRef())).isEqualTo("sk-rotated-key");
        verify(mapper).updateById(existing);
    }

    @Test
    void revealActiveApiKeyDecryptsFallbackSecret() {
        RagTenantModelConfigMapper mapper = mock(RagTenantModelConfigMapper.class);
        ApiKeyEncryptor encryptor = new ApiKeyEncryptor("test-encryption-key-123");
        RagProperties props = llmFallbackProperties("sk-yml-key");
        ModelConfigService service = new ModelConfigService(
                mapper,
                mockProvider(null),
                mockProvider(props),
                new ObjectMapper(),
                mock(ApplicationEventPublisher.class),
                encryptor
        );
        when(mapper.selectOne(any())).thenReturn(null);

        String apiKey = service.revealActiveApiKey(9L, "LLM");

        assertThat(apiKey).isEqualTo("sk-yml-key");
    }

    @Test
    void updateActiveApiKeyCreatesTenantOverrideWhenOnlyFallbackExists() {
        RagTenantModelConfigMapper mapper = mock(RagTenantModelConfigMapper.class);
        ApiKeyEncryptor encryptor = new ApiKeyEncryptor("test-encryption-key-123");
        RagProperties props = llmFallbackProperties("sk-yml-key");
        ModelConfigService service = new ModelConfigService(
                mapper,
                mockProvider(null),
                mockProvider(props),
                new ObjectMapper(),
                mock(ApplicationEventPublisher.class),
                encryptor
        );
        when(mapper.selectOne(any())).thenReturn(null);
        when(mapper.insert(any(RagTenantModelConfig.class))).thenReturn(1);

        RagTenantModelConfig result = service.updateActiveApiKey(9L, "LLM", "sk-tenant-key");

        ArgumentCaptor<RagTenantModelConfig> captor = ArgumentCaptor.forClass(RagTenantModelConfig.class);
        verify(mapper).insert(captor.capture());
        assertThat(result.getTenantId()).isEqualTo(9L);
        assertThat(result.getEnabled()).isTrue();
        assertThat(result.getModelType()).isEqualTo("LLM");
        assertThat(result.getModelName()).isEqualTo("qwen-plus");
        assertThat(encryptor.decrypt(captor.getValue().getApiKeySecretRef())).isEqualTo("sk-tenant-key");
    }

    @Test
    void upsertConfigCanSaveDisabledCandidateWithoutDisablingCurrentActiveModel() {
        RagTenantModelConfigMapper mapper = mock(RagTenantModelConfigMapper.class);
        ModelConfigService service = serviceWith(mapper);
        when(mapper.selectOne(any())).thenReturn(null);
        when(mapper.insert(any(RagTenantModelConfig.class))).thenReturn(1);

        RagTenantModelConfig result = service.upsertConfig(
                9L,
                "LLM",
                "openai-compatible",
                "qwen-candidate",
                "https://dashscope.aliyuncs.com/compatible-mode/v1",
                "sk-candidate",
                new BigDecimal("0.40"),
                null,
                null,
                null,
                null,
                null,
                null,
                45,
                2,
                8192,
                null,
                null,
                new BigDecimal("0.90"),
                false
        );

        ArgumentCaptor<RagTenantModelConfig> captor = ArgumentCaptor.forClass(RagTenantModelConfig.class);
        verify(mapper).insert(captor.capture());
        assertThat(result.getEnabled()).isFalse();
        assertThat(captor.getValue().getEnabled()).isFalse();
        verify(mapper, never()).selectList(any());
    }

    @Test
    void updateDisabledConfigCanEnableItAndDisableOtherActiveSameType() {
        RagTenantModelConfigMapper mapper = mock(RagTenantModelConfigMapper.class);
        ModelConfigService service = serviceWith(mapper);
        RagTenantModelConfig target = new RagTenantModelConfig();
        target.setId(7L);
        target.setTenantId(9L);
        target.setModelType("LLM");
        target.setProvider("openai-compatible");
        target.setModelName("qwen-candidate");
        target.setEnabled(false);
        RagTenantModelConfig otherEnabled = new RagTenantModelConfig();
        otherEnabled.setId(8L);
        otherEnabled.setTenantId(9L);
        otherEnabled.setModelType("LLM");
        otherEnabled.setEnabled(true);
        when(mapper.selectById(7L)).thenReturn(target);
        when(mapper.selectList(any())).thenReturn(List.of(otherEnabled));
        when(mapper.updateById(any(RagTenantModelConfig.class))).thenReturn(1);
        ModelConfigUpsertRequest request = ModelConfigUpsertRequest.builder()
                .modelType("LLM")
                .provider("openai-compatible")
                .modelName("qwen-candidate")
                .enabled(true)
                .build();

        RagTenantModelConfig result = service.updateConfig(9L, 7L, request);

        assertThat(result.getEnabled()).isTrue();
        ArgumentCaptor<RagTenantModelConfig> captor = ArgumentCaptor.forClass(RagTenantModelConfig.class);
        verify(mapper, org.mockito.Mockito.times(2)).updateById(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(RagTenantModelConfig::getId)
                .containsExactly(8L, 7L);
        assertThat(captor.getAllValues().get(0).getEnabled()).isFalse();
        assertThat(captor.getAllValues().get(1).getEnabled()).isTrue();
    }

    @Test
    void updateConfigByIdRejectsEnabledConfigUntilItIsDisabled() {
        RagTenantModelConfigMapper mapper = mock(RagTenantModelConfigMapper.class);
        ModelConfigService service = serviceWith(mapper);
        RagTenantModelConfig existing = new RagTenantModelConfig();
        existing.setId(7L);
        existing.setTenantId(9L);
        existing.setModelType("LLM");
        existing.setEnabled(true);
        when(mapper.selectById(7L)).thenReturn(existing);
        ModelConfigUpsertRequest request = ModelConfigUpsertRequest.builder()
                .modelType("LLM")
                .provider("openai-compatible")
                .modelName("qwen-max")
                .build();

        assertThatThrownBy(() -> service.updateConfig(9L, 7L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("disable");

        verify(mapper, never()).updateById(any(RagTenantModelConfig.class));
    }

    @Test
    void disableConfigRequiresCurrentTenantOwnership() {
        RagTenantModelConfigMapper mapper = mock(RagTenantModelConfigMapper.class);
        ModelConfigService service = serviceWith(mapper);
        RagTenantModelConfig existing = new RagTenantModelConfig();
        existing.setId(7L);
        existing.setTenantId(10L);
        existing.setModelType("LLM");
        existing.setEnabled(true);
        when(mapper.selectById(7L)).thenReturn(existing);

        assertThatThrownBy(() -> service.disableConfig(9L, 7L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");

        verify(mapper, never()).updateById(any(RagTenantModelConfig.class));
    }

    @Test
    void enableConfigDisablesOtherEnabledConfigsForSameTenantAndType() {
        RagTenantModelConfigMapper mapper = mock(RagTenantModelConfigMapper.class);
        ModelConfigService service = serviceWith(mapper);
        RagTenantModelConfig target = new RagTenantModelConfig();
        target.setId(7L);
        target.setTenantId(9L);
        target.setModelType("LLM");
        target.setEnabled(false);
        RagTenantModelConfig otherEnabled = new RagTenantModelConfig();
        otherEnabled.setId(8L);
        otherEnabled.setTenantId(9L);
        otherEnabled.setModelType("LLM");
        otherEnabled.setEnabled(true);
        when(mapper.selectById(7L)).thenReturn(target);
        when(mapper.selectList(any())).thenReturn(List.of(otherEnabled));
        when(mapper.updateById(any(RagTenantModelConfig.class))).thenReturn(1);

        RagTenantModelConfig result = service.enableConfig(9L, 7L);

        assertThat(result.getEnabled()).isTrue();
        ArgumentCaptor<RagTenantModelConfig> captor = ArgumentCaptor.forClass(RagTenantModelConfig.class);
        verify(mapper, org.mockito.Mockito.times(2)).updateById(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(RagTenantModelConfig::getId)
                .containsExactly(8L, 7L);
        assertThat(captor.getAllValues().get(0).getEnabled()).isFalse();
        assertThat(captor.getAllValues().get(1).getEnabled()).isTrue();
    }

    private static ModelConfigService serviceWith(RagTenantModelConfigMapper mapper) {
        return new ModelConfigService(
                mapper,
                mockProvider(null),
                mockProvider(null),
                new ObjectMapper(),
                mock(ApplicationEventPublisher.class),
                new ApiKeyEncryptor("test-encryption-key-123")
        );
    }

    private static RagProperties llmFallbackProperties(String apiKey) {
        RagProperties props = new RagProperties();
        props.getLlm().setModel("qwen-plus");
        props.getLlm().setProvider("openai-compatible");
        props.getLlm().setBaseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1");
        props.getLlm().setApiKey(apiKey);
        props.getLlm().setTemperature(0.2);
        return props;
    }

    private static RagProperties imageFallbackProperties(String apiKey) {
        RagProperties props = new RagProperties();
        props.getImage().setModel("wanx-v1");
        props.getImage().setBaseUrl("https://dashscope.aliyuncs.com/api/v1");
        props.getImage().setApiKey(apiKey);
        props.getImage().setSize("1024x1024");
        props.getImage().setQuality("standard");
        props.getImage().setTimeoutSeconds(60);
        props.getImage().setPollIntervalMillis(2000);
        return props;
    }

    private static RagProperties allModelFallbackProperties() {
        RagProperties props = llmFallbackProperties("sk-llm-key");
        props.getEmbedding().setModel("text-embedding-v4");
        props.getEmbedding().setBaseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1");
        props.getEmbedding().setApiKey("sk-embedding-key");
        props.getEmbedding().setDimension(1024);
        props.getImage().setModel("wanx-v1");
        props.getImage().setBaseUrl("https://dashscope.aliyuncs.com/api/v1");
        props.getImage().setApiKey("sk-image-key");
        props.getImage().setSize("1024x1024");
        props.getImage().setQuality("standard");
        props.getImage().setTimeoutSeconds(60);
        props.getImage().setPollIntervalMillis(2000);
        return props;
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> mockProvider(T value) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(value);
        return provider;
    }
}
