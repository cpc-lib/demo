import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import {
  canDisableAgentPrompt,
  canEditAgentPrompt,
  canEnableAgentPrompt,
  buildAgentPromptUpdateRequest,
  getAgentPromptSourceLabel,
  getPromptTextStats
} from './agentPrompts';

describe('agent prompt helpers', () => {
  it('labels prompt source from tenant id', () => {
    assert.equal(getAgentPromptSourceLabel(undefined), '未配置');
    assert.equal(getAgentPromptSourceLabel({ tenantId: 0, promptContent: 'global prompt' }), '全局默认');
    assert.equal(getAgentPromptSourceLabel({ tenantId: 7, promptContent: 'tenant prompt' }), '租户自定义');
  });

  it('preserves prompt content when building update request', () => {
    const content = '  line one\nline two  ';

    assert.deepEqual(buildAgentPromptUpdateRequest(content), { promptContent: content });
  });

  it('counts prompt characters and lines', () => {
    assert.deepEqual(getPromptTextStats('one\ntwo\n'), { characters: 8, lines: 3 });
    assert.deepEqual(getPromptTextStats(''), { characters: 0, lines: 0 });
  });

  it('allows editing tenant prompts but not global fallback records', () => {
    assert.equal(canEditAgentPrompt({ id: 1, tenantId: 7, promptContent: 'tenant prompt' }), true);
    assert.equal(canEditAgentPrompt({ id: 1, tenantId: 0, promptContent: 'global prompt' }), false);
    assert.equal(canEditAgentPrompt({ id: 1, promptContent: 'legacy response without tenant id' }), false);
    assert.equal(canEditAgentPrompt({ tenantId: 7, promptContent: 'fallback without id' }), false);
  });

  it('enables disabled tenant prompts and disables active tenant prompts', () => {
    assert.equal(canEnableAgentPrompt({ id: 1, tenantId: 7, status: 0, promptContent: 'draft' }), true);
    assert.equal(canEnableAgentPrompt({ id: 2, tenantId: 7, status: 1, promptContent: 'active' }), false);
    assert.equal(canEnableAgentPrompt({ id: 5, status: 0, promptContent: 'legacy response without tenant id' }), false);
    assert.equal(canDisableAgentPrompt({ id: 2, tenantId: 7, status: 1, promptContent: 'active' }), true);
    assert.equal(canDisableAgentPrompt({ id: 3, tenantId: 7, status: 0, promptContent: 'draft' }), false);
    assert.equal(canDisableAgentPrompt({ id: 4, tenantId: 0, status: 1, promptContent: 'global' }), false);
    assert.equal(canDisableAgentPrompt({ id: 6, status: 1, promptContent: 'legacy response without tenant id' }), false);
  });
});
