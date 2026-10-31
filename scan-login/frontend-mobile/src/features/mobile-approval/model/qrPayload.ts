const TICKET_PATTERN = /^[A-Za-z0-9_-]{43}$/

/**
 * PC QR contains a URL of the independent Mobile app ROOT with ?ticket=....
 * Never follow scanned URLs: parse the ticket locally and send only the ticket.
 */
export function parseQrTicket(raw: string): string {
  const value = raw.trim()
  if (TICKET_PATTERN.test(value)) return value

  let url: URL
  try {
    url = new URL(value)
  } catch {
    throw new Error('不是有效的扫码登录二维码')
  }
  if (!['https:', 'http:'].includes(url.protocol) ||
      url.username || url.password ||
      url.pathname !== '/') {
    throw new Error('请扫描本项目电脑端生成的登录二维码')
  }
  const ticket = url.searchParams.get('ticket')
  if (!ticket || !TICKET_PATTERN.test(ticket)) {
    throw new Error('二维码票据无效，请在电脑端刷新二维码')
  }
  return ticket
}
