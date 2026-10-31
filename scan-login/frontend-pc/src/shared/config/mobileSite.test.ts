import { afterEach, describe, expect, it, vi } from 'vitest'
import { mobileScanUrl, mobileSiteUrl } from '@/shared/config/mobileSite'

afterEach(() => vi.unstubAllEnvs())

describe('independent Mobile URL', () => {
  it('constructs a QR deep link at the Mobile origin root, never the old prefix', () => {
    const ticket = 'a'.repeat(43)
    const url = new URL(mobileScanUrl(ticket))
    expect(url.pathname).toBe('/')
    expect(url.port).toBe('5173')
    expect(url.searchParams.get('ticket')).toBe(ticket)
  })

  it('uses the configured separate HTTPS Mobile origin', () => {
    vi.stubEnv('VITE_MOBILE_URL', 'https://mobile.example.test/')
    expect(mobileSiteUrl().toString()).toBe('https://mobile.example.test/')
    expect(mobileScanUrl('test-ticket')).toBe('https://mobile.example.test/?ticket=test-ticket')
  })

  it('rejects invalid Mobile origin configuration', () => {
    vi.stubEnv('VITE_MOBILE_URL', 'https://mobile.example.test/mobile/')
    expect(() => mobileSiteUrl()).toThrow(/根地址/)
    vi.stubEnv('VITE_MOBILE_URL', 'javascript:alert(1)')
    expect(() => mobileSiteUrl()).toThrow()
  })
})
