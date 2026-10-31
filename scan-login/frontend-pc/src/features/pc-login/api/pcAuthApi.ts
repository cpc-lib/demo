import { request } from '@/shared/api/httpClient'
import { mobileScanUrl } from '@/shared/config/mobileSite'
import type { Captcha, PcAccount, QrTicket, QrStatusResponse } from '@/features/pc-login/model/types'

export const pcAuthApi = {
  me: (signal?: AbortSignal) => request<PcAccount>('/api/auth/me', { signal }),
  captcha: () => request<Captcha>('/api/auth/captcha'),
  login: (username: string, password: string, captchaId: string, captchaCode: string) =>
    request<{ status: string; username: string }>('/api/auth/login',
      { method: 'POST', json: { username, password, captchaId, captchaCode } }),
  createQr: async () => {
    const result = await request<QrTicket>('/api/auth/qr/create', { method: 'POST' })
    return { ...result, qrUrl: mobileScanUrl(result.ticket) }
  },
  status: (ticket: string, signal?: AbortSignal) =>
    request<QrStatusResponse>('/api/auth/qr/status?ticket=' + encodeURIComponent(ticket), { signal }),
  consume: (ticket: string, signal?: AbortSignal) =>
    request<{ status: string }>('/api/auth/qr/consume',
      { method: 'POST', json: { ticket }, signal }),
  logout: () => request<{ status: string }>('/api/auth/logout', { method: 'POST' })
}
