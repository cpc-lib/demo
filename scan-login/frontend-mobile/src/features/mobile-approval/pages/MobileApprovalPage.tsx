import { useEffect, useState } from 'react'
import { AppShell } from '@/app/AppShell'
import { LoginOverlay } from '@/shared/components/LoginOverlay'
import { SystemHome } from '@/shared/components/SystemHome'
import { UserAvatarMenu } from '@/shared/components/UserAvatarMenu'
import { CredentialsForm } from '@/features/mobile-approval/components/CredentialsForm'
import { MobileApprovalCard } from '@/features/mobile-approval/components/MobileApprovalCard'
import { useMobileApproval } from '@/features/mobile-approval/hooks/useMobileApproval'

export default function MobileApprovalPage() {
  const flow = useMobileApproval()
  const [scannerOpen, setScannerOpen] = useState(() =>
    Boolean(new URLSearchParams(window.location.search).get('ticket')) ||
      window.location.pathname === '/scan'
  )

  useEffect(() => {
    const path = !flow.session ? '/' : scannerOpen ? '/scan' : '/home'
    // Keep this single-page flow without reloading and discarding the memory-only mobile token.
    if (window.location.pathname !== path || window.location.search) {
      window.history.replaceState(null, '', path)
    }
  }, [flow.session, scannerOpen])

  const openScanner = () => {
    flow.updateTicket('')
    setScannerOpen(true)
  }
  const closeScanner = () => {
    flow.updateTicket('')
    setScannerOpen(false)
  }
  const logout = () => {
    flow.logout()
    setScannerOpen(false)
  }

  return <AppShell mobile headerRight={flow.session
    ? <UserAvatarMenu username={flow.session.username} mobile
        onScan={openScanner} onLogout={logout} />
    : undefined}>
    {flow.session && scannerOpen ? <section className="scan-page" aria-label="扫码授权">
      <header>
        <h1 className="scan-page-title">扫一扫</h1>
        <p className="scan-page-desc">将电脑屏幕上的二维码对准手机摄像头，核对验证码后确认授权。</p>
      </header>
      <MobileApprovalCard session={flow.session} ticket={flow.ticket}
        scan={flow.scan} confirmed={flow.confirmed} busy={flow.busy}
        onTicketChange={flow.updateTicket}
        onScan={() => void flow.perform('scan')}
        onCameraTicket={ticket => { flow.updateTicket(ticket); void flow.perform('scan', ticket) }}
        onConfirm={() => void flow.perform('confirm')}
        onReturnHome={closeScanner} />
      {flow.message && <p className="good" role="status">{flow.message}</p>}
      {flow.error && <p className="error" role="alert">{flow.error}</p>}
    </section> : <>
      <SystemHome mobile username={flow.session?.username}
        onScan={flow.session ? openScanner : undefined} />
      {!flow.session && <LoginOverlay title="手机账号登录" description="请先使用手机账号登录，登录成功后进入手机工作台。">
        <CredentialsForm mode={flow.mode} username={flow.username} password={flow.password}
          password2={flow.password2} busy={flow.busy}
          captcha={flow.captcha} captchaCode={flow.captchaCode}
          onUsername={flow.setUsername} onPassword={flow.setPassword}
          onPassword2={flow.setPassword2}
          onCaptchaCode={flow.setCaptchaCode} onRefreshCaptcha={() => void flow.refreshCaptcha()}
          onSwitchMode={flow.setMode} onSubmit={() => void flow.perform(flow.mode)} />
        {flow.message && <p className="good" role="status">{flow.message}</p>}
        {flow.error && <p className="error" role="alert">{flow.error}</p>}
      </LoginOverlay>}
    </>}
  </AppShell>
}
