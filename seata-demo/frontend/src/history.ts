import type { HistoryRecord } from './types';

const STORAGE_KEY = 'seata-transaction-history-v1';
const HISTORY_LIMIT = 50;

export function loadHistory(): HistoryRecord[] {
  try {
    const value = localStorage.getItem(STORAGE_KEY);
    if (!value) return [];
    const records: unknown = JSON.parse(value);
    if (!Array.isArray(records)) return [];
    return records.filter(isHistoryRecord).slice(0, HISTORY_LIMIT);
  } catch {
    return [];
  }
}

export function saveHistory(record: HistoryRecord): HistoryRecord[] {
  const records = [record, ...loadHistory()].slice(0, HISTORY_LIMIT);
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(records));
  } catch {
    // 浏览器禁用本地存储时，仍由页面状态保留本次会话历史。
  }
  return records;
}

export function clearHistory(): void {
  try {
    localStorage.removeItem(STORAGE_KEY);
  } catch {
    // 清空内存历史不依赖本地存储是否可用。
  }
}

function isHistoryRecord(value: unknown): value is HistoryRecord {
  if (!value || typeof value !== 'object') return false;
  const record = value as Partial<HistoryRecord>;
  return (
    typeof record.id === 'string' &&
    typeof record.createdAt === 'number' &&
    typeof record.durationMs === 'number' &&
    typeof record.success === 'boolean' &&
    ['AT', 'TCC', 'SAGA', 'XA'].includes(record.mode ?? '') &&
    !!record.request &&
    typeof record.request.orderNo === 'string'
  );
}
