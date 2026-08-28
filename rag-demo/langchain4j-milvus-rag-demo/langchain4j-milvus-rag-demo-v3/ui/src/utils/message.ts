import type { ApiError } from '../types';

export function getErrorMessage(error: unknown): string {
  const apiError = error as Partial<ApiError> | undefined;
  return apiError?.error?.message || apiError?.message || 'Request failed, please try again later';
}
