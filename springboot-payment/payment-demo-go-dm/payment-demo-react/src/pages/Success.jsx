import { Alert } from 'antd'

export default function Success() {
  return (
    <div className="bg-fa of" style={{ padding: 40 }}>
      <Alert message="支付成功！" type="success" showIcon />
    </div>
  )
}
