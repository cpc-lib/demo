import type {
  ApiResult,
  OrderCreateRequest,
  OrderInfo,
  TransactionMode,
} from './types';

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '');

export type ApiErrorKind = 'http' | 'business' | 'network' | 'invalid-response';

export class OrderApiError extends Error {
  constructor(
    message: string,
    public readonly kind: ApiErrorKind,
    public readonly response: ApiResult<OrderInfo> | null = null,
    public readonly status?: number,
  ) {
    super(message);
    this.name = 'OrderApiError';
  }
}

function isApiResult(value: unknown): value is ApiResult<OrderInfo> {
  if (!value || typeof value !== 'object') return false;
  const result = value as Partial<ApiResult<OrderInfo>>;
  return typeof result.code === 'number' && typeof result.message === 'string';
}

export async function createOrder(
  mode: TransactionMode,
  request: OrderCreateRequest,
): Promise<ApiResult<OrderInfo>> {
  let response: Response;

  try {
    response = await fetch(`${API_BASE_URL}/order/${mode.toLowerCase()}/create`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request),
    });
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') throw error;
    throw new OrderApiError('无法连接订单服务，请确认 order-service（8081）已启动。', 'network');
  }

  let payload: unknown;
  try {
    payload = await response.json();
  } catch {
    throw new OrderApiError(
      `订单服务返回了无法解析的响应（HTTP ${response.status}）。`,
      'invalid-response',
      null,
      response.status,
    );
  }

  if (!isApiResult(payload)) {
    throw new OrderApiError(
      `订单服务响应格式不正确（HTTP ${response.status}）。`,
      'invalid-response',
      null,
      response.status,
    );
  }

  if (!response.ok) {
    throw new OrderApiError(
      payload.message || `请求失败（HTTP ${response.status}）。`,
      'http',
      payload,
      response.status,
    );
  }

  if (payload.code !== 200) {
    throw new OrderApiError(payload.message || '订单创建失败。', 'business', payload);
  }

  return payload;
}
