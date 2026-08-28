import axios from 'axios';
import type { ApiError } from '../types';

export type TenantHeaderContext = {
  tenantId: string;
  userId: string;
  userName: string;
  roles: string;
  knowledgeBaseIds: string;
  permissionTags: string;
  impersonateTenantId: string;
  impersonationReason: string;
};

const STORAGE_KEYS: Record<keyof TenantHeaderContext, string> = {
  tenantId: 'rag.tenantId',
  userId: 'rag.userId',
  userName: 'rag.userName',
  roles: 'rag.roles',
  knowledgeBaseIds: 'rag.knowledgeBaseIds',
  permissionTags: 'rag.permissionTags',
  impersonateTenantId: 'rag.impersonateTenantId',
  impersonationReason: 'rag.impersonationReason'
};

const COOKIE_KEYS: Record<keyof TenantHeaderContext, string> = {
  tenantId: 'ragTenantId',
  userId: 'ragUserId',
  userName: 'ragUserName',
  roles: 'ragRoles',
  knowledgeBaseIds: 'ragKnowledgeBaseIds',
  permissionTags: 'ragPermissionTags',
  impersonateTenantId: 'ragImpersonateTenantId',
  impersonationReason: 'ragImpersonationReason'
};

const EMPTY_CONTEXT: TenantHeaderContext = {
  tenantId: '',
  userId: '',
  userName: '',
  roles: '',
  knowledgeBaseIds: '',
  permissionTags: '',
  impersonateTenantId: '',
  impersonationReason: ''
};

const AUTH_TOKEN_KEY = 'rag.authToken';

export const tenantContextStorage = {
  read(): TenantHeaderContext {
    if (typeof window === 'undefined') {
      return EMPTY_CONTEXT;
    }
    return Object.fromEntries(
      Object.entries(STORAGE_KEYS).map(([field, key]) => [
        field,
        window.localStorage.getItem(key) || ''
      ])
    ) as TenantHeaderContext;
  },
  write(values: Partial<TenantHeaderContext>) {
    if (typeof window === 'undefined') {
      return;
    }
    Object.entries(values).forEach(([field, value]) => {
      const key = STORAGE_KEYS[field as keyof TenantHeaderContext];
      if (!key) return;
      window.localStorage.setItem(key, value || '');
    });
    syncTenantCookies(this.read());
  },
  clear() {
    if (typeof window === 'undefined') {
      return;
    }
    Object.values(STORAGE_KEYS).forEach((key) => window.localStorage.removeItem(key));
    clearTenantCookies();
  }
};

export const authTokenStorage = {
  read(): string {
    if (typeof window === 'undefined') {
      return '';
    }
    return window.localStorage.getItem(AUTH_TOKEN_KEY) || '';
  },
  write(token: string) {
    if (typeof window === 'undefined') {
      return;
    }
    window.localStorage.setItem(AUTH_TOKEN_KEY, token || '');
  },
  clear() {
    if (typeof window === 'undefined') {
      return;
    }
    window.localStorage.removeItem(AUTH_TOKEN_KEY);
  }
};

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 60000
});

http.interceptors.request.use((config) => {
  const token = authTokenStorage.read();
  config.headers = config.headers || {};
  if (token) {
    const context = tenantContextStorage.read();
    syncTenantCookies(context);
    config.headers.Authorization = `Bearer ${token}`;
    if (context.tenantId) config.headers['X-Tenant-Id'] = context.tenantId;
    if (context.userId) config.headers['X-User-Id'] = context.userId;
    if (context.userName) config.headers['X-User-Name'] = context.userName;
    if (context.roles) config.headers['X-Roles'] = context.roles;
    if (context.knowledgeBaseIds) config.headers['X-Knowledge-Base-Ids'] = context.knowledgeBaseIds;
    if (context.permissionTags) config.headers['X-Permission-Tags'] = context.permissionTags;
    if (context.impersonateTenantId) config.headers['X-Impersonate-Tenant-Id'] = context.impersonateTenantId;
    if (context.impersonationReason) config.headers['X-Impersonation-Reason'] = context.impersonationReason;
  }
  return config;
});

http.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      authTokenStorage.clear();
      tenantContextStorage.clear();
    }
    if (error.response?.data) {
      return Promise.reject(error.response.data as ApiError);
    }
    return Promise.reject({
      ok: false,
      error: {
        code: 'NETWORK_ERROR',
        message: error.message || 'Request failed',
        details: {}
      }
    } as ApiError);
  }
);

function syncTenantCookies(context: TenantHeaderContext) {
  if (typeof document === 'undefined') {
    return;
  }
  Object.entries(COOKIE_KEYS).forEach(([field, cookieName]) => {
    const value = context[field as keyof TenantHeaderContext] || '';
    document.cookie = `${cookieName}=${encodeURIComponent(value)}; path=/; max-age=31536000; SameSite=Lax`;
  });
}

function clearTenantCookies() {
  if (typeof document === 'undefined') {
    return;
  }
  Object.values(COOKIE_KEYS).forEach((cookieName) => {
    document.cookie = `${cookieName}=; path=/; max-age=0; SameSite=Lax`;
  });
}

export default http;
