import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { ApiError } from '@/shared/api/httpClient'

const mock = vi.hoisted(() => ({
  register: vi.fn(), login: vi.fn(), scan: vi.fn(), confirm: vi.fn(),
  captcha: vi.fn(), refresh: vi.fn()
}))
vi.mock('@/features/mobile-approval/api/mobileAuthApi', () => ({ mobileAuthApi: mock }))
import MobileApprovalPage from '@/features/mobile-approval/pages/MobileApprovalPage'

describe('mobile homepage and avatar scanner', () => {
  beforeEach(() => {
    Object.values(mock).forEach(fn => fn.mockReset())
    mock.captcha.mockResolvedValue({ captchaId: 'cap-1', image: 'data:image/png;base64,x' })
    mock.login.mockResolvedValue({
      accessToken: 'mobile-jwt', refreshToken: 'mobile-refresh', username: 'demo', userId: 1
    })
    mock.scan.mockResolvedValue({ status: 'SCANNED', verificationCode: '123456' })
    mock.confirm.mockResolvedValue({ status: 'CONFIRMED' })
    window.history.replaceState(null, '', '/')
  })

  async function login() {
    render(<MobileApprovalPage />)
    expect(screen.getByRole('dialog', { name: '手机账号登录' })).toBeTruthy()
    await screen.findByAltText('图形验证码')
    fireEvent.change(screen.getByLabelText('账号'), { target: { value: 'demo' } })
    fireEvent.change(screen.getByLabelText('密码'), { target: { value: 'valid-password' } })
    fireEvent.change(screen.getByLabelText('验证码'), { target: { value: 'ABCD' } })
    fireEvent.click(screen.getByRole('button', { name: '手机登录' }))
    await screen.findByRole('button', { name: '用户头像' })
    expect(screen.getByText('欢迎回来，demo')).toBeTruthy()
    expect(screen.queryByRole('dialog')).toBeNull()
    expect(window.location.pathname).toBe('/home')
  }

  function openScanner() {
    fireEvent.click(screen.getByRole('button', { name: '用户头像' }))
    fireEvent.click(screen.getByRole('menuitem', { name: /扫一扫/ }))
  }

  it('enters homepage after login, then opens scanner using avatar menu', async () => {
    await login()
    openScanner()
    expect(screen.getByRole('region', { name: '扫码授权' })).toBeTruthy()
    expect(screen.queryByRole('dialog')).toBeNull()
    await waitFor(() => expect(window.location.pathname).toBe('/scan'))
    fireEvent.change(screen.getByLabelText('二维码票据'), { target: { value: 'ticket-A' } })
    fireEvent.click(screen.getByRole('button', { name: '手动提交票据' }))
    await screen.findByText('123456')
    expect(mock.confirm).not.toHaveBeenCalled()
    fireEvent.click(screen.getByRole('button', { name: '确认登录该电脑' }))
    await waitFor(() => expect(mock.confirm).toHaveBeenCalledExactlyOnceWith('ticket-A', 'mobile-jwt'))
    await screen.findByText(/已确认本次登录/)
    fireEvent.click(screen.getByRole('button', { name: '返回系统首页' }))
    await waitFor(() => expect(window.location.pathname).toBe('/home'))
    expect(screen.queryByRole('region', { name: '扫码授权' })).toBeNull()
    expect(screen.getByText('欢迎回来，demo')).toBeTruthy()
  })

  it('invalidates old confirmation if scanned ticket changes', async () => {
    await login()
    openScanner()
    fireEvent.change(screen.getByLabelText('二维码票据'), { target: { value: 'ticket-A' } })
    fireEvent.click(screen.getByRole('button', { name: '手动提交票据' }))
    await screen.findByText('123456')
    fireEvent.change(screen.getByLabelText('二维码票据'), { target: { value: 'ticket-B' } })
    expect(screen.queryByRole('button', { name: '确认登录该电脑' })).toBeNull()
    expect(mock.confirm).not.toHaveBeenCalled()
  })

  it('registers a new account and returns to the login mode', async () => {
    mock.register.mockResolvedValue({ id: 2, username: 'new_user' })
    render(<MobileApprovalPage />)
    await screen.findByAltText('图形验证码')
    fireEvent.click(screen.getByRole('button', { name: '没有账号？注册新账号' }))
    fireEvent.change(screen.getByLabelText('账号'), { target: { value: 'new_user' } })
    fireEvent.change(screen.getByLabelText('密码'), { target: { value: 'valid-password' } })
    fireEvent.change(screen.getByLabelText('确认密码'), { target: { value: 'valid-password' } })
    expect(screen.getByLabelText('验证码')).toBeTruthy()
    fireEvent.change(screen.getByLabelText('验证码'), { target: { value: 'ABCD' } })
    fireEvent.click(screen.getByRole('button', { name: '注 册' }))
    await screen.findByText('注册成功，欢迎 new_user，请登录')
    expect(mock.register).toHaveBeenCalledExactlyOnceWith(
      'new_user', 'valid-password', 'cap-1', 'ABCD')
    expect(screen.getByRole('button', { name: '手机登录' })).toBeTruthy()
    expect((screen.getByLabelText('账号') as HTMLInputElement).value).toBe('new_user')
  })

  it('rejects mismatched register passwords without calling the API', async () => {
    render(<MobileApprovalPage />)
    await screen.findByAltText('图形验证码')
    fireEvent.click(screen.getByRole('button', { name: '没有账号？注册新账号' }))
    fireEvent.change(screen.getByLabelText('账号'), { target: { value: 'new_user' } })
    fireEvent.change(screen.getByLabelText('密码'), { target: { value: 'valid-password' } })
    fireEvent.change(screen.getByLabelText('确认密码'), { target: { value: 'other-password' } })
    fireEvent.click(screen.getByRole('button', { name: '注 册' }))
    await screen.findByText('两次输入的密码不一致，请重新输入')
    expect(mock.register).not.toHaveBeenCalled()
  })

  it('rejects a duplicated username with a friendly message', async () => {
    mock.register.mockRejectedValueOnce(new ApiError(409, 'Username already exists'))
    render(<MobileApprovalPage />)
    await screen.findByAltText('图形验证码')
    fireEvent.click(screen.getByRole('button', { name: '没有账号？注册新账号' }))
    fireEvent.change(screen.getByLabelText('账号'), { target: { value: 'demo' } })
    fireEvent.change(screen.getByLabelText('密码'), { target: { value: 'valid-password' } })
    fireEvent.change(screen.getByLabelText('确认密码'), { target: { value: 'valid-password' } })
    fireEvent.change(screen.getByLabelText('验证码'), { target: { value: 'ABCD' } })
    fireEvent.click(screen.getByRole('button', { name: '注 册' }))
    await screen.findByText('账号名已被占用，请更换后重试')
  })

  it('keeps a ticket from a PC QR deep link and opens scan after signing in', async () => {
    const qrTicket = 'x'.repeat(43)
    window.history.replaceState(null, '', '/?ticket=' + qrTicket)
    render(<MobileApprovalPage />)
    await screen.findByAltText('图形验证码')
    fireEvent.change(screen.getByLabelText('账号'), { target: { value: 'demo' } })
    fireEvent.change(screen.getByLabelText('密码'), { target: { value: 'valid-password' } })
    fireEvent.change(screen.getByLabelText('验证码'), { target: { value: 'ABCD' } })
    fireEvent.click(screen.getByRole('button', { name: '手机登录' }))
    await screen.findByRole('region', { name: '扫码授权' })
    expect((screen.getByLabelText('二维码票据') as HTMLInputElement).value).toBe(qrTicket)
    await waitFor(() => expect(window.location.pathname).toBe('/scan'))
  })

  it('avatar logout returns to blocked login homepage', async () => {
    await login()
    fireEvent.click(screen.getByRole('button', { name: '用户头像' }))
    fireEvent.click(screen.getByRole('menuitem', { name: /退出登录/ }))
    await screen.findByRole('dialog', { name: '手机账号登录' })
    expect(screen.getByText('欢迎使用安全工作台')).toBeTruthy()
    expect(window.location.pathname).toBe('/')
  })
})
