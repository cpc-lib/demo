import { canUsePlatformTenantAdmin, getTenantAccessTabKeys } from './tenantAccess';

const platformAdmin = {
  tenantId: null,
  operatorTenantId: null,
  platformAdmin: true
};

const tenantAdmin = {
  tenantId: 1,
  operatorTenantId: 1,
  platformAdmin: false
};

const tenantScopedSuperAdmin = {
  tenantId: 1,
  operatorTenantId: 1,
  platformAdmin: true
};

const impersonatingPlatformAdmin = {
  tenantId: 1,
  operatorTenantId: null,
  platformAdmin: true
};

if (!canUsePlatformTenantAdmin(platformAdmin)) {
  throw new Error('platform super admin without tenant should use platform tenant management');
}

if (!canUsePlatformTenantAdmin(impersonatingPlatformAdmin)) {
  throw new Error('impersonating platform super admin should keep platform tenant management');
}

if (canUsePlatformTenantAdmin(tenantAdmin)) {
  throw new Error('tenant admin should not use platform tenant management');
}

if (canUsePlatformTenantAdmin(tenantScopedSuperAdmin)) {
  throw new Error('tenant-scoped roles must not unlock platform tenant management');
}

const tenantTabs = getTenantAccessTabKeys(tenantAdmin);
if (!tenantTabs.includes('users') || !tenantTabs.includes('knowledge-members')) {
  throw new Error('tenant login should render tenant user and knowledge member management tabs');
}

if (tenantTabs.includes('tenants') || tenantTabs.includes('ops')) {
  throw new Error('tenant login should not render platform-only tenant or ops tabs');
}

const platformTabs = getTenantAccessTabKeys(platformAdmin);
if (!platformTabs.includes('tenants') || !platformTabs.includes('users') || !platformTabs.includes('ops')) {
  throw new Error('platform admin should render platform tenant management tabs');
}
