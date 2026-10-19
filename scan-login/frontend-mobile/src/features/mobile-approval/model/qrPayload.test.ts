import { describe, expect, it } from 'vitest'
import { parseQrTicket } from '@/features/mobile-approval/model/qrPayload'

const ticket = 'q'.repeat(43)

describe('QR payload validation', () => {
  it('accepts Mobile root URLs on local, LAN and HTTPS domains', () => {
    expect(parseQrTicket(`http://localhost:5173/?ticket=${ticket}`)).toBe(ticket)
    expect(parseQrTicket(`http://192.168.1.7:5173/?ticket=${ticket}`)).toBe(ticket)
    expect(parseQrTicket(`https://mobile.example.test/?v=2&ticket=${ticket}`)).toBe(ticket)
  })

  it('also accepts a raw 43-character ticket', () => {
    expect(parseQrTicket(ticket)).toBe(ticket)
  })

  it('rejects legacy /mobile path, arbitrary pages and invalid tickets', () => {
    expect(() => parseQrTicket('https://example.test/mobile?ticket=' + ticket)).toThrow()
    expect(() => parseQrTicket('https://example.test/scan?ticket=' + ticket)).toThrow()
    expect(() => parseQrTicket('javascript:alert(1)')).toThrow()
    expect(() => parseQrTicket('https://evil:user@example.test/?ticket=' + ticket)).toThrow()
    expect(() => parseQrTicket('https://example.test/?ticket=short')).toThrow()
    expect(() => parseQrTicket('')).toThrow()
  })
})
