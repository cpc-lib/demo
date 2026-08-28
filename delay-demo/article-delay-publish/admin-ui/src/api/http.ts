import axios, { AxiosError } from 'axios'

export interface ProblemDetail {
  title?: string
  detail?: string
  status?: number
}

export const http = axios.create({
  baseURL: '/',
  timeout: 10_000,
  headers: {
    'Content-Type': 'application/json',
  },
})

export function getErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const axiosError = error as AxiosError<ProblemDetail>
    return axiosError.response?.data?.detail
      ?? axiosError.response?.data?.title
      ?? axiosError.message
  }

  return error instanceof Error ? error.message : '请求失败，请稍后重试'
}
