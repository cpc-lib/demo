import assert from 'node:assert/strict';
import test from 'node:test';

import { buildRulePayload, getRuleFormValues } from './ruleEditor.js';

test('maps an existing gray rule into modal form values', () => {
  const row = {
    id: 7,
    ruleName: 'User 1001',
    ruleType: 'USER',
    conditionKey: 'userId',
    conditionValue: '1001',
    trafficPercent: 0,
    priority: 1
  };

  assert.deepEqual(getRuleFormValues(row), {
    ruleName: 'User 1001',
    ruleType: 'USER',
    conditionKey: 'userId',
    conditionValue: '1001',
    trafficPercent: 0,
    priority: 1
  });
});

test('builds a complete update payload for PUT /api/rules/{id}', () => {
  const values = {
    ruleName: 'Header Canary',
    ruleType: 'HEADER',
    conditionKey: 'X-Gray',
    conditionValue: 'true',
    trafficPercent: 0,
    priority: 5
  };
  const currentRule = {
    serviceId: 'demo-order-service',
    targetVersion: 'v2',
    enabled: true,
    description: 'existing description'
  };

  assert.deepEqual(buildRulePayload(values, currentRule), {
    ruleName: 'Header Canary',
    ruleType: 'HEADER',
    conditionKey: 'X-Gray',
    conditionValue: 'true',
    trafficPercent: 0,
    priority: 5,
    serviceId: 'demo-order-service',
    targetVersion: 'v2',
    enabled: true,
    description: 'existing description'
  });
});
