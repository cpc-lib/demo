import { request } from '@/shared/api/httpClient'
import type { Captcha, MobileScan, MobileSession } from '@/features/mobile-approval/model/types'

export const mobileAuthApi = {
  register: (username: string, password: string, captchaId: string, captchaCode: string) =>
    request<{ id: number; username: string }>('/api/mobile/register',
      { method: 'POST', json: { username, password, captchaId, captchaCode } }),
  captcha: () => request<Captcha>('/api/auth/captcha'),
  login: (username: string, password: string, captchaId: string, captchaCode: string) =>
    request<MobileSession>('/api/mobile/login',
      { method: 'POST', json: { username, password, captchaId, captchaCode } }),
  refresh: (refreshToken: string) =>
    request<MobileSession>('/api/mobile/refresh', { method: 'POST', json: { refreshToken } }),
  scan: (ticket: string, token: string) =>
    request<MobileScan>('/api/auth/qr/scan', { method: 'POST', json: { ticket }, bearer: token }),
  confirm: (ticket: string, token: string) =>
    request<MobileScan>('/api/auth/qr/confirm', { method: 'POST', json: { ticket }, bearer: token })
}
