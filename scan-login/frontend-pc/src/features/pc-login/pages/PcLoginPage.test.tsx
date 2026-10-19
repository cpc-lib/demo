import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { ApiError } from '@/shared/api/httpClient'

const mock = vi.hoisted(() => ({
  me: vi.fn(), createQr: vi.fn(), status: vi.fn(), consume: vi.fn(), logout: vi.fn(),
  login: vi.fn(), captcha: vi.fn()
}))
vi.mock('@/features/pc-login/api/pcAuthApi', () => ({ pcAuthApi: mock }))
import PcLoginPage from '@/features/pc-login/pages/PcLoginPage'

describe('PC workbench and login overlay', () => {
  beforeEach(() => {
    Object.values(mock).forEach(fn => fn.mockReset())
    mock.me.mockRejectedValueOnce(new ApiError(403, 'Forbidden'))
      .mockResolvedValue({ id: 1, username: 'scan_demo' })
    mock.createQr.mockResolvedValue({
      ticket: 'ticket-123', qrUrl: 'http://localhost:5173/mobile?ticket=ticket-123',
      verificationCode: '654321', expiresIn: 120
    })
    mock.consume.mockResolvedValue({ status: 'LOGGED_IN' })
    mock.logout.mockResolvedValue({ status: 'LOGGED_OUT' })
    mock.captcha.mockResolvedValue({ captchaId: 'cap-1', image: 'data:image/png;base64,x' })
  })

  it('shows a non-dismissable QR dialog above the homepage when unauthenticated', async () => {
    mock.status.mockResolvedValue({ status: 'SCANNED' })
    render(<PcLoginPage />)
    await screen.findByRole('dialog', { name: '请扫码登录' })
    expect(screen.getByText('欢迎使用安全工作台')).toBeTruthy()
    await screen.findByText(/已扫描，请在手机上确认/)
    expect(mock.consume).not.toHaveBeenCalled()
    expect(screen.queryByRole('button', { name: '用户头像' })).toBeNull()
  })

  it('consumes QR only after confirmation; closes overlay and shows account avatar', async () => {
    mock.status.mockResolvedValue({ status: 'CONFIRMED' })
    render(<PcLoginPage />)
    await screen.findByRole('button', { name: '用户头像' })
    expect(screen.queryByRole('dialog')).toBeNull()
    expect(screen.getByText('欢迎回来，scan_demo')).toBeTruthy()
    expect(mock.createQr).toHaveBeenCalledTimes(1)
    expect(mock.status).toHaveBeenCalledTimes(1)
    expect(mock.consume).toHaveBeenCalledExactlyOnceWith('ticket-123', expect.anything())
    expect(mock.me).toHaveBeenCalledTimes(2)
  })

  it('does not consume expired QR and keeps login overlay', async () => {
    mock.status.mockResolvedValue({ status: 'EXPIRED' })
    render(<PcLoginPage />)
    await screen.findByText(/二维码已过期，请刷新/)
    expect(screen.getByRole('dialog', { name: '请扫码登录' })).toBeTruthy()
    expect(mock.consume).not.toHaveBeenCalled()
  })

  it('shows the homepage without QR dialog for a valid existing PC cookie', async () => {
    mock.me.mockReset()
    mock.me.mockResolvedValue({ id: 1, username: 'scan_demo' })
    render(<PcLoginPage />)
    await screen.findByRole('button', { name: '用户头像' })
    expect(mock.createQr).not.toHaveBeenCalled()
    expect(screen.queryByRole('dialog')).toBeNull()
  })

  it('shows QR login again after avatar menu logout', async () => {
    mock.me.mockReset()
    mock.me.mockResolvedValue({ id: 1, username: 'scan_demo' })
    mock.status.mockResolvedValue({ status: 'WAIT_SCAN' })
    render(<PcLoginPage />)
    fireEvent.click(await screen.findByRole('button', { name: '用户头像' }))
    fireEvent.click(screen.getByRole('menuitem', { name: /退出登录/ }))
    await waitFor(() => expect(mock.logout).toHaveBeenCalledTimes(1))
    await screen.findByRole('dialog', { name: '请扫码登录' })
    expect(mock.createQr).toHaveBeenCalledTimes(1)
  })

  it('fetches a fresh captcha and clears the password after logout', async () => {
    mock.login.mockResolvedValue({ status: 'LOGGED_IN', username: 'scan_demo' })
    render(<PcLoginPage />)
    fireEvent.click(await screen.findByRole('button', { name: '使用账号密码登录' }))
    await screen.findByAltText('图形验证码')
    fireEvent.change(screen.getByLabelText('账号'), { target: { value: 'scan_demo' } })
    fireEvent.change(screen.getByLabelText('密码'), { target: { value: 'StrongPass123!' } })
    fireEvent.change(screen.getByLabelText('验证码'), { target: { value: 'abcd' } })
    fireEvent.click(screen.getByRole('button', { name: '登 录' }))
    await screen.findByRole('button', { name: '用户头像' })
    fireEvent.click(screen.getByRole('button', { name: '用户头像' }))
    fireEvent.click(screen.getByRole('menuitem', { name: /退出登录/ }))
    // The dialog returns in the still-selected password mode with a fresh captcha.
    await screen.findByAltText('图形验证码')
    await waitFor(() => expect(mock.captcha).toHaveBeenCalledTimes(2))
    expect((screen.getByLabelText('密码') as HTMLInputElement).value).toBe('')
    expect((screen.getByLabelText('验证码') as HTMLInputElement).value).toBe('')
    expect((screen.getByLabelText('账号') as HTMLInputElement).value).toBe('scan_demo')
  })

  it('logs in with account, password and image captcha', async () => {
    mock.login.mockResolvedValue({ status: 'LOGGED_IN', username: 'scan_demo' })
    render(<PcLoginPage />)
    fireEvent.click(await screen.findByRole('button', { name: '使用账号密码登录' }))
    await screen.findByAltText('图形验证码')
    expect(mock.captcha).toHaveBeenCalledTimes(1)
    expect(screen.getByRole('button', { name: '显示密码' })).toBeTruthy()
    fireEvent.change(screen.getByLabelText('账号'), { target: { value: 'scan_demo' } })
    fireEvent.change(screen.getByLabelText('密码'), { target: { value: 'StrongPass123!' } })
    fireEvent.change(screen.getByLabelText('验证码'), { target: { value: 'abcd' } })
    fireEvent.click(screen.getByRole('button', { name: '登 录' }))
    await screen.findByRole('button', { name: '用户头像' })
    expect(mock.login).toHaveBeenCalledExactlyOnceWith('scan_demo', 'StrongPass123!', 'cap-1', 'abcd')
  })

  it('fetches a fresh captcha after a failed password login', async () => {
    mock.login.mockRejectedValueOnce(new ApiError(400, '验证码错误或已过期，请刷新后重试'))
    render(<PcLoginPage />)
    fireEvent.click(await screen.findByRole('button', { name: '使用账号密码登录' }))
    await screen.findByAltText('图形验证码')
    fireEvent.change(screen.getByLabelText('账号'), { target: { value: 'scan_demo' } })
    fireEvent.change(screen.getByLabelText('密码'), { target: { value: 'StrongPass123!' } })
    fireEvent.change(screen.getByLabelText('验证码'), { target: { value: 'abcd' } })
    fireEvent.click(screen.getByRole('button', { name: '登 录' }))
    await screen.findByRole('alert')
    await waitFor(() => expect(mock.captcha).toHaveBeenCalledTimes(2))
  })
})
