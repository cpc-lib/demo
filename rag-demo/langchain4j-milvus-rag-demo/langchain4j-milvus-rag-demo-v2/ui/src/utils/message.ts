import type { ApiError } from '../types';

export function getErrorMessage(error: unknown): string {
  const apiError = error as Partial<ApiError> | undefined;
  return apiError?.message || '操作失败，请稍后重试';
}
