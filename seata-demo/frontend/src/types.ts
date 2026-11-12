export const TRANSACTION_MODES = ['AT', 'TCC', 'SAGA', 'XA'] as const;

export type TransactionMode = (typeof TRANSACTION_MODES)[number];
export type OrderStatus = 'INIT' | 'SUCCESS' | 'FAILED' | 'CANCELLED';

export interface OrderCreateRequest {
  orderNo: string;
  userId: string;
  commodityCode: string;
  count: number;
  money: number;
  mockException: boolean;
}

export interface OrderInfo {
  id: number | null;
  orderNo: string;
  userId: string;
  commodityCode: string;
  count: number;
  money: number;
  status: OrderStatus;
  mode: TransactionMode;
  createTime: string | null;
  updateTime: string | null;
}

export interface ApiResult<T> {
  code: number;
  message: string;
  data: T | null;
  timestamp: number;
}

export interface HistoryRecord {
  id: string;
  mode: TransactionMode;
  request: OrderCreateRequest;
  response: ApiResult<OrderInfo> | null;
  error: string | null;
  success: boolean;
  createdAt: number;
  durationMs: number;
}
