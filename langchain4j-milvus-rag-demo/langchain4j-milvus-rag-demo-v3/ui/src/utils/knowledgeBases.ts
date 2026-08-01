import type { RagKnowledgeBase } from '../types';

export function knowledgeBaseOptionLabel(kb: RagKnowledgeBase): string {
  const collection = safeText(kb.vectorCollection) || safeText(kb.kbCode) || String(kb.id);
  return `${kb.name} (${collection})`;
}

function safeText(value?: string) {
  if (!value || !value.trim()) {
    return undefined;
  }
  return value.trim();
}
