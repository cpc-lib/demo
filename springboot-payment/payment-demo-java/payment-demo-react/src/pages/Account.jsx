import { Button, Form, Input, message } from 'antd'
import { useNavigate } from 'react-router-dom'

import authApi from '@/api/auth'
import { useAuth } from '@/auth/AuthContext'

export default function Account() {
  const auth = useAuth()
  const navigate = useNavigate()

  const submit = async (values) => {
    await authApi.changePassword(values)
    message.success('密码已修改，请重新登录')
    await auth.logout()
    navigate('/login', { replace: true })
  }

  return (
    <main className="container page-shell narrow-page">
      <header className="page-heading">
        <h1>账号安全</h1>
        <p>修改密码后，所有设备上的刷新会话都会失效。</p>
      </header>
      <section className="surface account-panel">
        <div className="account-name">
          <span>当前账号</span>
          <strong>{auth.user?.username}</strong>
          <small>{auth.user?.role}</small>
        </div>
        <Form layout="vertical" requiredMark={false} onFinish={submit}>
          <Form.Item label="原密码" name="oldPassword" rules={[{ required: true, message: '请输入原密码' }]}>
            <Input.Password autoComplete="current-password" />
          </Form.Item>
          <Form.Item label="新密码" name="newPassword" rules={[{ required: true, message: '请输入新密码' }, { min: 8, message: '密码至少 8 位' }]}>
            <Input.Password autoComplete="new-password" />
          </Form.Item>
          <Button type="primary" htmlType="submit">修改密码</Button>
        </Form>
      </section>
    </main>
  )
}
