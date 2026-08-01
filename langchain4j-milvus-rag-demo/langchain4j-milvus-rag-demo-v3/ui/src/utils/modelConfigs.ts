import type { ModelConfigUpsertRequest, RagModelType, RagTenantModelConfig } from '../types';

export type ModelConfigFormValues = ModelConfigUpsertRequest & {
  changeApiKey?: boolean;
};

export function buildModelConfigFormValues(
  config: RagTenantModelConfig,
  apiKeySecretRef?: string,
  showApiKeyEditor = true
): ModelConfigFormValues {
  return removeUndefined({
    modelType: config.modelType as RagModelType,
    provider: config.provider,
    modelName: config.modelName,
    baseUrl: config.baseUrl,
    apiKeySecretRef: trimToUndefined(apiKeySecretRef),
    changeApiKey: showApiKeyEditor,
    temperature: config.temperature,
    dimension: config.dimension,
    imageSize: config.imageSize,
    imageQuality: config.imageQuality,
    pollIntervalMillis: config.pollIntervalMillis,
    rateLimitQps: config.rateLimitQps,
    monthlyBudgetCents: config.monthlyBudgetCents,
    enabled: config.enabled,
    timeoutSeconds: config.timeoutSeconds,
    maxRetries: config.maxRetries,
    maxTokens: config.maxTokens,
    frequencyPenalty: config.frequencyPenalty,
    presencePenalty: config.presencePenalty,
    topP: config.topP
  });
}

export function normalizeModelConfigFormRequest(
  values: ModelConfigFormValues,
  isEditing: boolean
): ModelConfigUpsertRequest {
  const { changeApiKey, ...requestValues } = values;
  return normalizeModelConfigRequest({
    ...requestValues,
    apiKeySecretRef: isEditing && !changeApiKey ? undefined : requestValues.apiKeySecretRef
  });
}

export function normalizeModelConfigRequest(values: ModelConfigUpsertRequest): ModelConfigUpsertRequest {
  const modelType = values.modelType;
  const request: ModelConfigUpsertRequest = {
    modelType,
    provider: trimToUndefined(values.provider) || 'openai-compatible',
    modelName: trimToUndefined(values.modelName) || '',
    baseUrl: trimToUndefined(values.baseUrl),
    apiKeySecretRef: trimToUndefined(values.apiKeySecretRef),
    rateLimitQps: values.rateLimitQps,
    monthlyBudgetCents: values.monthlyBudgetCents,
    enabled: values.enabled,
    timeoutSeconds: values.timeoutSeconds,
    maxRetries: values.maxRetries
  };

  if (modelType === 'EMBEDDING') {
    request.dimension = values.dimension;
  } else if (modelType === 'IMAGE') {
    request.imageSize = trimToUndefined(values.imageSize);
    request.imageQuality = trimToUndefined(values.imageQuality);
    request.pollIntervalMillis = values.pollIntervalMillis;
  } else {
    request.temperature = values.temperature;
    request.maxTokens = values.maxTokens;
    request.frequencyPenalty = values.frequencyPenalty;
    request.presencePenalty = values.presencePenalty;
    request.topP = values.topP;
  }

  return removeUndefined(request);
}

export function modelTypeColor(modelType?: string) {
  if (modelType === 'EMBEDDING') {
    return 'geekblue';
  }
  if (modelType === 'IMAGE') {
    return 'volcano';
  }
  return 'purple';
}

export function canEditModelConfig<T extends Pick<RagTenantModelConfig, 'id' | 'enabled'>>(
  config?: T | null
): config is T & { id: number; enabled: false } {
  return Boolean(config?.id && config.enabled === false);
}

export function canDisableModelConfig<T extends Pick<RagTenantModelConfig, 'id' | 'enabled'>>(
  config?: T | null
): config is T & { id: number; enabled: true } {
  return Boolean(config?.id && config.enabled === true);
}

export function canManageModelConfigApiKey(
  config?: { id?: number; modelType?: string; enabled?: unknown } | null
): boolean {
  return Boolean(config?.id || config?.modelType);
}

function trimToUndefined(value?: string) {
  if (!value || !value.trim()) {
    return undefined;
  }
  return value.trim();
}

function removeUndefined<T extends object>(value: T): T {
  return Object.fromEntries(
    Object.entries(value as Record<string, unknown>).filter(([, entryValue]) => entryValue !== undefined)
  ) as T;
}
