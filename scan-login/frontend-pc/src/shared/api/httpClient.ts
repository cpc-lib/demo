export class ApiError extends Error {
  constructor(public readonly status: number, message: string) {
    super(message)
    this.name = 'ApiError'
  }
}

type RequestOptions = {
  method?: 'GET' | 'POST'
  json?: unknown
  bearer?: string
  headers?: Record<string, string>
  signal?: AbortSignal
}

let csrfRequest: Promise<string> | undefined
const protectedPaths = new Set(['/api/auth/login', '/api/auth/qr/create', '/api/auth/qr/consume', '/api/auth/logout'])
/** PC endpoints whose 401 is recoverable by rotating the refresh cookie. */
const refreshablePaths = new Set(['/api/auth/me', '/api/auth/logout'])

async function csrfToken(): Promise<string> {
  if (!csrfRequest) {
    csrfRequest = fetch('/api/auth/csrf', { credentials: 'include' })
      .then(async response => {
        if (!response.ok) throw new ApiError(response.status, 'CSRF 初始化失败')
        const result: unknown = await response.json()
        if (typeof result !== 'object' || result === null || !('token' in result)
          || typeof result.token !== 'string' || !result.token) {
          throw new Error('CSRF 响应无效')
        }
        return result.token
      })
      .catch(error => {
        csrfRequest = undefined
        throw error
      })
  }
  return csrfRequest
}

/** Single-flight refresh: concurrent 401s share one rotation of the refresh cookie. */
let refreshRequest: Promise<boolean> | undefined
function refreshPcSession(): Promise<boolean> {
  refreshRequest ??= fetch('/api/auth/refresh', { method: 'POST', credentials: 'include' })
    .then(response => response.ok)
    .catch(() => false)
    .finally(() => { refreshRequest = undefined })
  return refreshRequest
}

/** JSON client: same-origin URLs, typed HTTP failures, no storage of JWT or passwords. */
export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  if (!path.startsWith('/api/') || path.startsWith('//')) throw new Error('Invalid API path')
  try {
    return await send<T>(path, options)
  } catch (cause) {
    if (cause instanceof ApiError && cause.status === 401 && refreshablePaths.has(path)
      && await refreshPcSession()) {
      return send<T>(path, options)
    }
    throw cause
  }
}

async function send<T>(path: string, options: RequestOptions): Promise<T> {
  const method = options.method ?? 'GET'
  const headers: Record<string, string> = { ...options.headers }
  if (options.json !== undefined) headers['Content-Type'] = 'application/json'
  if (options.bearer) headers.Authorization = 'Bearer ' + options.bearer
  if (method !== 'GET' && (protectedPaths.has(path) || path.startsWith('/api/admin/'))) {
    headers['X-XSRF-TOKEN'] = await csrfToken()
  }
  const response = await fetch(path, {
    method,
    headers,
    body: options.json === undefined ? undefined : JSON.stringify(options.json),
    credentials: 'include',
    signal: options.signal
  })
  const payload: unknown = await response.json().catch(() => null)
  if (!response.ok) {
    if (response.status === 403 && protectedPaths.has(path)) csrfRequest = undefined
    const body = payload && typeof payload === 'object'
      ? payload as { error?: string; message?: string } : {}
    throw new ApiError(response.status, body.error || body.message || 'HTTP ' + response.status)
  }
  return payload as T
}

export function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : '未知网络错误'
}
