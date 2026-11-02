// 秒杀 Demo REST API 客户端。

export interface LoginResponse {
  token: string;
  userId: string;
  expiresInMs: number;
}

export interface ProductSnapshot {
  productId: string;
  name: string;
  stock: number;
  sold: number;
  fenceToken: number;
}

export interface SeckillResponse {
  productId: string;
  userId: string;
  orderId: number;
  fenceToken: number;
  remainingStock: number;
  replayed: boolean;
}

export interface ApiError {
  status: number;
  errorCode?: string;
  message: string;
}

export interface ResetResponse {
  products: number;
  orders: number;
  operations: number;
}

// ---- 计数器 Demo（CounterController，无需 JWT） ----
export interface CounterSnapshot {
  resourceId: string;
  value: number;
  fenceToken: number;
}

export interface MutationResponse {
  resourceId: string;
  idempotencyKey: string;
  delta: number;
  value: number;
  fenceToken: number;
  replayed: boolean;
}

const API = '/api';

async function request<T>(path: string, init: RequestInit = {}, token?: string): Promise<T> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(init.headers as Record<string, string> | undefined),
  };
  if (token) headers.Authorization = `Bearer ${token}`;

  const res = await fetch(`${API}${path}`, { ...init, headers });
  if (!res.ok) {
    let errorCode: string | undefined;
    let msg = `${res.status} ${res.statusText}`;
    try {
      // 后端统一返回 RFC7807 ProblemDetail：{ title, detail, errorCode }
      const body = await res.json();
      errorCode = body.errorCode ?? body.title;
      const detail = body.detail ?? body.message ?? body.error;
      if (detail) msg = `${res.status} ${detail}`;
    } catch {
      // 忽略非 JSON 错误体
    }
    throw { status: res.status, errorCode, message: msg } as ApiError;
  }
  return (await res.json()) as T;
}

export function login(username: string, password: string): Promise<LoginResponse> {
  return request<LoginResponse>('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  });
}

export function listProducts(token: string): Promise<ProductSnapshot[]> {
  return request<ProductSnapshot[]>('/seckill', {}, token);
}

export function resetAll(token: string): Promise<ResetResponse> {
  return request<ResetResponse>('/seckill/reset', { method: 'POST' }, token);
}

export function getProduct(productId: string, token: string): Promise<ProductSnapshot> {
  return request<ProductSnapshot>(`/seckill/${encodeURIComponent(productId)}`, {}, token);
}

export function buy(
  productId: string,
  token: string,
  idempotencyKey: string,
): Promise<SeckillResponse> {
  return request<SeckillResponse>(
    `/seckill/${encodeURIComponent(productId)}/buy`,
    { method: 'POST', headers: { 'Idempotency-Key': idempotencyKey } },
    token,
  );
}

export function generateKey(prefix = 'key'): string {
  return `${prefix}_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 8)}`;
}

// ---- 计数器 Demo ----

export async function getCounter(resourceId: string): Promise<CounterSnapshot | undefined> {
  try {
    return await request<CounterSnapshot>(`/counters/${encodeURIComponent(resourceId)}`);
  } catch (e) {
    if ((e as ApiError).status === 404) return undefined;
    throw e;
  }
}

export function addCounter(
  resourceId: string,
  idempotencyKey: string,
  delta: number,
): Promise<MutationResponse> {
  return request<MutationResponse>(`/counters/${encodeURIComponent(resourceId)}/add`, {
    method: 'POST',
    headers: { 'Idempotency-Key': idempotencyKey },
    body: JSON.stringify({ delta }),
  });
}
