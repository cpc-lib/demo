import { Button, Modal } from 'antd'
import { QRCodeCanvas } from 'qrcode.react'

export default function WxPayDialog({ open, codeUrl, onClose }) {
  return (
    <Modal open={open} footer={null} width={360} centered onCancel={onClose} destroyOnClose>
      <div className="wxpay-dialog">
        <h3>微信扫码支付</h3>
        <p>支付完成后，订单状态会自动更新。</p>
        {codeUrl ? <QRCodeCanvas value={codeUrl} size={280} /> : null}
        <Button onClick={onClose}>稍后支付</Button>
      </div>
    </Modal>
  )
}
