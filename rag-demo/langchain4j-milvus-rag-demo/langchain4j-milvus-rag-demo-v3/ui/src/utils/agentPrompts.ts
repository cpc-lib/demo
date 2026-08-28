import type { AgentPromptUpdateRequest, RagAgentPrompt } from '../types';

export function getAgentPromptSourceLabel(prompt?: RagAgentPrompt | null) {
  if (!prompt) {
    return '未配置';
  }
  return prompt.tenantId === 0 ? '全局默认' : '租户自定义';
}

export function buildAgentPromptUpdateRequest(promptContent: string): AgentPromptUpdateRequest {
  return { promptContent };
}

export function buildAgentPromptSaveRequest(values: {
  promptName?: string;
  promptContent: string;
  enabled?: boolean;
}): AgentPromptUpdateRequest {
  return {
    promptName: values.promptName?.trim() || 'default',
    promptContent: values.promptContent,
    enabled: values.enabled
  };
}

export function canEditAgentPrompt(prompt?: RagAgentPrompt | null) {
  return Boolean(prompt?.id && typeof prompt.tenantId === 'number' && prompt.tenantId > 0);
}

export function canEnableAgentPrompt(prompt?: RagAgentPrompt | null) {
  return canEditAgentPrompt(prompt) && prompt?.status !== 1;
}

export function canDisableAgentPrompt(prompt?: RagAgentPrompt | null) {
  return canEditAgentPrompt(prompt) && prompt?.status === 1;
}

export function getPromptTextStats(content?: string | null) {
  if (!content) {
    return { characters: 0, lines: 0 };
  }
  return {
    characters: content.length,
    lines: content.split(/\r\n|\r|\n/).length
  };
}
