import { beforeEach, describe, expect, it, vi } from 'vitest'

describe('shared HTTP client', () => {
  beforeEach(() => {
    vi.resetModules()
    vi.unstubAllGlobals()
  })

  it('fetches CSRF only once for concurrent PC mutations', async () => {
    const fetchMock = vi.fn(async (url: string, _options?: RequestInit) =>
      new Response(JSON.stringify(url === '/api/auth/csrf' ? { token: 'csrf-test' } : { status: 'OK' }),
        { status: 200, headers: { 'Content-Type': 'application/json' } }))
    vi.stubGlobal('fetch', fetchMock)
    const { request } = await import('@/shared/api/httpClient')
    await Promise.all([
      request('/api/auth/qr/create', { method: 'POST' }),
      request('/api/auth/qr/create', { method: 'POST' })
    ])
    expect(fetchMock).toHaveBeenCalledTimes(3)
    const mutationCalls = fetchMock.mock.calls.filter(([url]) => url === '/api/auth/qr/create')
    expect(mutationCalls).toHaveLength(2)
    for (const [, options] of mutationCalls) {
      expect((options as RequestInit).headers).toMatchObject({ 'X-XSRF-TOKEN': 'csrf-test' })
    }
  })

  it('separates Bearer authentication from cookie-only requests', async () => {
    const fetchMock = vi.fn(async (_url: string, _options?: RequestInit) => new Response('{}', { status: 200 }))
    vi.stubGlobal('fetch', fetchMock)
    const { request } = await import('@/shared/api/httpClient')
    await request('/api/auth/qr/scan', { method: 'POST', bearer: 'mobile-token', json: { ticket: 'abc' } })
    await request('/api/auth/me')
    const mobileOptions = fetchMock.mock.calls[0][1] as RequestInit
    expect(mobileOptions.headers).toMatchObject({ Authorization: 'Bearer mobile-token' })
    const pcOptions = fetchMock.mock.calls[1][1] as RequestInit
    expect(pcOptions.headers).not.toHaveProperty('Authorization')
    expect(pcOptions.credentials).toBe('include')
  })

  it('preserves HTTP error status and rejects non-API paths', async () => {
    vi.stubGlobal('fetch', vi.fn(async () =>
      new Response(JSON.stringify({ error: 'Forbidden' }), { status: 403 })))
    const { request, ApiError } = await import('@/shared/api/httpClient')
    await expect(request('/api/auth/me')).rejects.toMatchObject({
      name: 'ApiError', status: 403, message: 'Forbidden'
    } satisfies Partial<InstanceType<typeof ApiError>>)
    await expect(request('https://example.org/api/auth/me')).rejects.toThrow('Invalid API path')
  })
})
