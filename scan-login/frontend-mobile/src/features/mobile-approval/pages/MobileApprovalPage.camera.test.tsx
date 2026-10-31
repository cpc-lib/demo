import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'

const ticket = 'b'.repeat(43)
const mock = vi.hoisted(() => ({
  register: vi.fn(), login: vi.fn(), scan: vi.fn(), confirm: vi.fn()
}))
vi.mock('@/features/mobile-approval/api/mobileAuthApi', () => ({ mobileAuthApi: mock }))
vi.mock('@/features/mobile-approval/components/CameraQrScanner', () => ({
  CameraQrScanner: ({ onTicket }: { onTicket: (value: string) => void }) =>
    <button type="button" onClick={() => onTicket(ticket)}>测试摄像头识别结果</button>
}))
import MobileApprovalPage from '@/features/mobile-approval/pages/MobileApprovalPage'

describe('camera scan to manual confirmation', () => {
  beforeEach(() => {
    Object.values(mock).forEach(fn => fn.mockReset())
    mock.login.mockResolvedValue({ accessToken: 'mobile-jwt', username: 'scan_demo', userId: 1 })
    mock.scan.mockResolvedValue({ status: 'SCANNED', verificationCode: '567890' })
    mock.confirm.mockResolvedValue({ status: 'CONFIRMED' })
    window.history.replaceState(null, '', '/')
  })

  it('opens scan from avatar and invokes the backend without auto-confirming', async () => {
    render(<MobileApprovalPage />)
    fireEvent.change(screen.getByLabelText('账号'), { target: { value: 'scan_demo' } })
    fireEvent.change(screen.getByLabelText('密码'), { target: { value: 'valid-password' } })
    fireEvent.click(screen.getByRole('button', { name: '手机登录' }))
    await screen.findByRole('button', { name: '用户头像' })
    fireEvent.click(screen.getByRole('button', { name: '用户头像' }))
    fireEvent.click(screen.getByRole('menuitem', { name: /扫一扫/ }))
    fireEvent.click(screen.getByRole('button', { name: '测试摄像头识别结果' }))
    await waitFor(() => expect(mock.scan).toHaveBeenCalledExactlyOnceWith(ticket, 'mobile-jwt'))
    expect((screen.getByLabelText('二维码票据') as HTMLInputElement).value).toBe(ticket)
    await screen.findByText('567890')
    expect(mock.confirm).not.toHaveBeenCalled()
    fireEvent.click(screen.getByRole('button', { name: '确认登录该电脑' }))
    await waitFor(() => expect(mock.confirm).toHaveBeenCalledExactlyOnceWith(ticket, 'mobile-jwt'))
  })
})
