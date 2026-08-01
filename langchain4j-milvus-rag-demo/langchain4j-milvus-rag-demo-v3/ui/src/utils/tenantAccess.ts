import type { CurrentUserResponse } from '../types';

export type TenantAccessTabKey = 'tenants' | 'users' | 'knowledge-members' | 'ops';

const PLATFORM_TAB_KEYS: TenantAccessTabKey[] = ['tenants', 'users', 'knowledge-members', 'ops'];
const TENANT_TAB_KEYS: TenantAccessTabKey[] = ['users', 'knowledge-members'];

export function canUsePlatformTenantAdmin(
  user?: Pick<CurrentUserResponse, 'tenantId' | 'operatorTenantId' | 'platformAdmin'> | null
) {
  return Boolean(user?.platformAdmin && user.operatorTenantId == null);
}

export function getTenantAccessTabKeys(
  user?: Pick<CurrentUserResponse, 'tenantId' | 'operatorTenantId' | 'platformAdmin'> | null
) {
  return canUsePlatformTenantAdmin(user) ? PLATFORM_TAB_KEYS : TENANT_TAB_KEYS;
}
