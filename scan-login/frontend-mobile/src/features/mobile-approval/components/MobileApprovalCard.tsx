import type { MobileScan, MobileSession } from '@/features/mobile-approval/model/types'
import { CameraQrScanner } from '@/features/mobile-approval/components/CameraQrScanner'

type Props = {
  session: MobileSession
  ticket: string
  scan: MobileScan | null
  confirmed: boolean
  busy: boolean
  onTicketChange: (value: string) => void
  onScan: () => void
  onCameraTicket: (ticket: string) => void
  onConfirm: () => void
  onReturnHome: () => void
}

export function MobileApprovalCard({
  session, ticket, scan, confirmed, busy,
  onTicketChange, onScan, onCameraTicket, onConfirm, onReturnHome
}: Props) {
  return <div className="form">
    <p className="login-pill">✓ 手机已登录：{session.username}</p>
    <label htmlFor="qr-ticket">二维码票据
      <input id="qr-ticket" value={ticket} placeholder="扫码后自动填入，也可粘贴 ticket"
        onChange={event => onTicketChange(event.target.value)} />
    </label>
    {!scan ? <>
      <CameraQrScanner disabled={busy} onTicket={onCameraTicket} />
      <p className="camera-alternative">无法使用摄像头？可手动输入 ticket 后提交。</p>
      <button disabled={busy || !ticket} type="button" className="secondary" onClick={onScan}>
        手动提交票据
      </button>
    </> : <div className="verify">
      <p>请确认电脑端显示相同验证码</p>
      <div className="verify-code" aria-label="扫码验证码">{scan.verificationCode ?? '------'}</div>
      {confirmed ? <p className="good">✓ 已确认本次登录</p>
        : <button type="button" disabled={busy} onClick={onConfirm}>确认登录该电脑</button>}
    </div>}
    <button type="button" className="secondary" disabled={busy} onClick={onReturnHome}>
      返回系统首页
    </button>
  </div>
}
