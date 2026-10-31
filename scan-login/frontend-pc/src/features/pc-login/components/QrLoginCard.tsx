import { QRCodeSVG } from 'qrcode.react'
import type { QrStatus, QrTicket } from '@/features/pc-login/model/types'
import { QR_STATUS_LABELS } from '@/features/pc-login/model/types'

type Props = {
  qr: QrTicket | null
  status: QrStatus
  busy: boolean
  onRefresh: () => void
}
export function QrLoginCard({ qr, status, busy, onRefresh }: Props) {
  return <>
    <div className="qr-holder" aria-label="登录二维码">
      {qr && status !== 'EXPIRED'
        ? <QRCodeSVG value={qr.qrUrl} size={208} level="M" marginSize={2} />
        : <div className="empty-qr">{busy ? '二维码生成中...' : '二维码已失效'}</div>}
    </div>
    <div className="code">验证码：<b>{qr?.verificationCode ?? '------'}</b></div>
    <p className="status" role="status">
      <i className={status === 'EXPIRED' ? 'dot bad' : 'dot'} />{QR_STATUS_LABELS[status]}
    </p>
    <button type="button" className="action-button" disabled={busy} onClick={onRefresh}>
      {busy ? '正在处理...' : '刷新二维码'}
    </button>
    {qr && <a className="sample-link" href={qr.qrUrl} target="_blank" rel="noreferrer">
      在另一标签页打开手机端演示 ↗
    </a>}
  </>
}
