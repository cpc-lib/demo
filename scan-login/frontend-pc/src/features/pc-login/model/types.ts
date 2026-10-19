export type QrStatus = 'WAIT_SCAN' | 'SCANNED' | 'CONFIRMED' | 'CONSUMED' | 'EXPIRED'
export type QrTicket = {
  ticket: string
  qrUrl: string
  verificationCode: string
  expiresIn: number
}
export type QrStatusResponse = { status: QrStatus; verificationCode?: string }
export type PcAccount = { id: number; username: string }
export type Captcha = { captchaId: string; image: string }

export const QR_STATUS_LABELS: Record<QrStatus, string> = {
  WAIT_SCAN: '等待手机扫描二维码',
  SCANNED: '已扫描，请在手机上确认',
  CONFIRMED: '手机确认成功，正在登录...',
  CONSUMED: '二维码已使用',
  EXPIRED: '二维码已过期，请刷新'
}
