/**
 * Mobile runs as an independent app at the ROOT of its own origin.
 * For public use set VITE_MOBILE_URL to the trusted Mobile HTTPS origin.
 */
export function mobileSiteUrl(): URL {
  const configured = import.meta.env.VITE_MOBILE_URL?.trim()
  const fallback = `${window.location.protocol}//${window.location.hostname}:5173/`
  const url = new URL(configured || fallback, window.location.origin)
  if (!['http:', 'https:'].includes(url.protocol) || url.username || url.password ||
      url.pathname !== '/' || url.search || url.hash) {
    throw new Error('VITE_MOBILE_URL 必须是 Mobile 的独立站点根地址，例如 https://mobile.example.com/')
  }
  return url
}

export function mobileScanUrl(ticket: string): string {
  const url = mobileSiteUrl()
  url.searchParams.set('ticket', ticket)
  return url.toString()
}
