import {
  buildModelConfigFormValues,
  canDisableModelConfig,
  canEditModelConfig,
  canManageModelConfigApiKey,
  normalizeModelConfigFormRequest,
  normalizeModelConfigRequest
} from './modelConfigs';

const llmRequest = normalizeModelConfigRequest({
  modelType: 'LLM',
  provider: ' openai-compatible ',
  modelName: ' qwen-plus ',
  baseUrl: ' ',
  apiKeySecretRef: ' ',
  temperature: 0.2,
  dimension: 1024,
  maxTokens: 8192,
  topP: 0.9
});

const disabledCandidateRequest = normalizeModelConfigRequest({
  modelType: 'LLM',
  provider: 'openai-compatible',
  modelName: 'qwen-candidate',
  enabled: false
});

if (disabledCandidateRequest.enabled !== false) {
  throw new Error('model config requests should preserve enabled=false for disabled candidates');
}

if (llmRequest.provider !== 'openai-compatible' || llmRequest.modelName !== 'qwen-plus') {
  throw new Error('model config text fields should be trimmed');
}

if ('baseUrl' in llmRequest || 'apiKeySecretRef' in llmRequest) {
  throw new Error('blank optional text fields should be removed');
}

const editingWithoutApiKeyChange = normalizeModelConfigFormRequest(
  {
    modelType: 'LLM',
    provider: 'openai-compatible',
    modelName: 'qwen-plus',
    apiKeySecretRef: ' sk-new ',
    changeApiKey: false
  },
  true
);

if ('apiKeySecretRef' in editingWithoutApiKeyChange) {
  throw new Error('editing without API key change should not submit apiKeySecretRef');
}

const editingWithApiKeyChange = normalizeModelConfigFormRequest(
  {
    modelType: 'LLM',
    provider: 'openai-compatible',
    modelName: 'qwen-plus',
    apiKeySecretRef: ' sk-new ',
    changeApiKey: true
  },
  true
);

if (editingWithApiKeyChange.apiKeySecretRef !== 'sk-new') {
  throw new Error('editing with API key change should submit trimmed apiKeySecretRef');
}

if ('dimension' in llmRequest) {
  throw new Error('LLM requests should not submit embedding dimension');
}

const embeddingRequest = normalizeModelConfigRequest({
  modelType: 'EMBEDDING',
  provider: 'openai-compatible',
  modelName: 'text-embedding-v4',
  dimension: 1024,
  temperature: 0.4,
  maxTokens: 4096
});

if (embeddingRequest.dimension !== 1024) {
  throw new Error('embedding requests should keep dimension');
}

if ('temperature' in embeddingRequest || 'maxTokens' in embeddingRequest) {
  throw new Error('embedding requests should not submit LLM generation controls');
}

const imageRequest = normalizeModelConfigRequest({
  modelType: 'IMAGE',
  provider: 'openai-compatible',
  modelName: 'wanx-v1',
  imageSize: ' 1024x1024 ',
  imageQuality: ' hd ',
  pollIntervalMillis: 1500,
  temperature: 0.4,
  dimension: 1024,
  maxTokens: 4096
});

if (
  imageRequest.imageSize !== '1024x1024' ||
  imageRequest.imageQuality !== 'hd' ||
  imageRequest.pollIntervalMillis !== 1500
) {
  throw new Error('image model requests should keep normalized image generation controls');
}

if ('temperature' in imageRequest || 'dimension' in imageRequest || 'maxTokens' in imageRequest) {
  throw new Error('image model requests should not submit LLM or embedding controls');
}

if (canEditModelConfig({ id: 1, enabled: true })) {
  throw new Error('enabled model configs should not be directly editable');
}

if (!canEditModelConfig({ id: 1, enabled: false })) {
  throw new Error('disabled tenant-owned model configs should be editable');
}

if (canEditModelConfig({ enabled: false })) {
  throw new Error('fallback configs without ids should not be editable');
}

if (!canDisableModelConfig({ id: 1, enabled: true })) {
  throw new Error('enabled tenant-owned model configs should be disableable');
}

if (canDisableModelConfig({ id: 1, enabled: false })) {
  throw new Error('disabled model configs should not be disableable');
}

if (!canManageModelConfigApiKey({ id: 1, enabled: true })) {
  throw new Error('enabled tenant-owned configs should allow API key management');
}

if (!canManageModelConfigApiKey({ modelType: 'LLM' })) {
  throw new Error('active fallback configs should allow API key management by model type');
}

if (canManageModelConfigApiKey({})) {
  throw new Error('configs without id or model type should not allow API key management');
}

const fallbackImageFormValues = buildModelConfigFormValues(
  {
    modelType: 'IMAGE',
    provider: 'openai-compatible',
    modelName: 'wanx-v1',
    baseUrl: 'https://dashscope.aliyuncs.com/api/v1',
    apiKeyConfigured: true,
    imageSize: '1024x1024',
    imageQuality: 'hd',
    pollIntervalMillis: 1500,
    timeoutSeconds: 45,
    maxRetries: 1,
    enabled: true
  },
  ' sk-image-current ',
  true
);

if (
  fallbackImageFormValues.modelType !== 'IMAGE' ||
  fallbackImageFormValues.imageSize !== '1024x1024' ||
  fallbackImageFormValues.imageQuality !== 'hd' ||
  fallbackImageFormValues.pollIntervalMillis !== 1500 ||
  fallbackImageFormValues.enabled !== true ||
  fallbackImageFormValues.apiKeySecretRef !== 'sk-image-current' ||
  fallbackImageFormValues.changeApiKey !== true
) {
  throw new Error('fallback image configs should map to complete editable form values with current API key');
}

const disabledLlmFormValues = buildModelConfigFormValues(
  {
    id: 7,
    modelType: 'LLM',
    provider: 'openai-compatible',
    modelName: 'qwen-plus',
    temperature: 0.2,
    maxTokens: 8192,
    topP: 0.9,
    apiKeyConfigured: true,
    enabled: false
  },
  'sk-current'
);

if (disabledLlmFormValues.apiKeySecretRef !== 'sk-current' || disabledLlmFormValues.changeApiKey !== true) {
  throw new Error('owned model config editor should show the current API key in full parameter edit mode');
}
