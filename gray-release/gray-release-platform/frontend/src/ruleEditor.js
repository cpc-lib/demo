export const RULE_TYPES = ['USER', 'TENANT', 'HEADER', 'COOKIE', 'IP', 'APP_VERSION', 'REGION', 'PERCENT'];

export const getRuleFormValues = (rule) => ({
  ruleName: rule?.ruleName ?? '',
  ruleType: rule?.ruleType ?? 'USER',
  conditionKey: rule?.conditionKey ?? '',
  conditionValue: rule?.conditionValue ?? '',
  trafficPercent: rule?.trafficPercent ?? 0,
  priority: rule?.priority ?? 10
});

export const buildRulePayload = (values, currentRule) => ({
  ...values,
  serviceId: currentRule.serviceId,
  targetVersion: currentRule.targetVersion ?? 'v2',
  enabled: currentRule.enabled ?? true,
  description: currentRule.description
});
