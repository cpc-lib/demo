import { useState } from 'react'
import { Button, Form, Input, Segmented, message } from 'antd'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'

import { useAuth } from '@/auth/AuthContext'

export default function Login() {
  const auth = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [mode, setMode] = useState('login')
  const [submitting, setSubmitting] = useState(false)

  if (auth.bootstrapped && auth.user) {
    return <Navigate to="/" replace />
  }

  const submit = async (values) => {
    setSubmitting(true)
    try {
      if (mode === 'login') {
        await auth.login(values)
        message.success('登录成功')
      } else {
        await auth.register(values)
        message.success('注册成功')
      }
      navigate(location.state?.from || '/', { replace: true })
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="auth-page">
      <section className="auth-panel" aria-labelledby="auth-title">
        <div className="auth-copy">
          <span className="auth-mark">课程支付中心</span>
          <h1 id="auth-title">登录后统一管理购物车与订单</h1>
          <p>购物车按账号保存在服务端。刷新页面或重新登录后仍可继续结算。</p>
        </div>
        <div className="auth-form-wrap">
          <Segmented
            block
            value={mode}
            onChange={setMode}
            options={[{ label: '登录', value: 'login' }, { label: '注册', value: 'register' }]}
          />
          <Form layout="vertical" requiredMark={false} onFinish={submit}>
            <Form.Item label="用户名" name="username" rules={[{ required: true, message: '请输入用户名' }]}>
              <Input autoComplete="username" maxLength={50} />
            </Form.Item>
            <Form.Item label="密码" name="password" rules={[{ required: true, message: '请输入密码' }, { min: 8, message: '密码至少 8 位' }]}>
              <Input.Password autoComplete={mode === 'login' ? 'current-password' : 'new-password'} maxLength={72} />
            </Form.Item>
            <Button type="primary" htmlType="submit" loading={submitting} block size="large">
              {mode === 'login' ? '登录' : '创建账号'}
            </Button>
          </Form>
        </div>
      </section>
    </main>
  )
}
