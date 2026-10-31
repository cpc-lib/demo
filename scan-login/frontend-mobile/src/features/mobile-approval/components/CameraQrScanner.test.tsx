import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { CameraQrScanner } from '@/features/mobile-approval/components/CameraQrScanner'

const decodeMock = vi.hoisted(() => vi.fn())
vi.mock('jsqr', () => ({ default: decodeMock }))

const ticket = 'a'.repeat(43)

describe('camera QR scanner', () => {
  let stopTrack: ReturnType<typeof vi.fn>
  let getUserMedia: ReturnType<typeof vi.fn>

  beforeEach(() => {
    decodeMock.mockReset()
    decodeMock.mockReturnValue(null)
    stopTrack = vi.fn()
    getUserMedia = vi.fn(async () => ({ getTracks: () => [{ stop: stopTrack }] }))
    Object.defineProperty(navigator, 'mediaDevices', { configurable: true, value: { getUserMedia } })
    Object.defineProperty(window, 'isSecureContext', { configurable: true, value: true })
    vi.spyOn(HTMLMediaElement.prototype, 'play').mockResolvedValue(undefined)
    vi.spyOn(HTMLMediaElement.prototype, 'pause').mockImplementation(() => {})
  })

  it('asks for rear camera and stops tracks when manually closed', async () => {
    const onTicket = vi.fn()
    render(<CameraQrScanner onTicket={onTicket} />)
    expect(getUserMedia).not.toHaveBeenCalled()
    fireEvent.click(screen.getByRole('button', { name: '打开摄像头扫码' }))
    await waitFor(() => expect(getUserMedia).toHaveBeenCalledTimes(1))
    expect(getUserMedia.mock.calls[0][0]).toMatchObject({
      audio: false, video: { facingMode: { ideal: 'environment' } }
    })
    await screen.findByRole('button', { name: '关闭摄像头' })
    fireEvent.click(screen.getByRole('button', { name: '关闭摄像头' }))
    expect(stopTrack).toHaveBeenCalledTimes(1)
    expect(onTicket).not.toHaveBeenCalled()
  })

  it('shows a camera permission error without calling the backend', async () => {
    getUserMedia.mockRejectedValueOnce(new DOMException('denied', 'NotAllowedError'))
    render(<CameraQrScanner onTicket={vi.fn()} />)
    fireEvent.click(screen.getByRole('button', { name: '打开摄像头扫码' }))
    await screen.findByRole('alert')
    expect(screen.getByRole('alert').textContent).toContain('权限被拒绝')
    expect(screen.getByRole('button', { name: '打开摄像头扫码' })).toBeTruthy()
  })

  it('decodes the QR, extracts ticket once and releases the camera', async () => {
    decodeMock.mockReturnValue({ data: `http://localhost:5174/?ticket=${ticket}` })
    vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockReturnValue({
      drawImage: vi.fn(),
      getImageData: () => ({
        data: new Uint8ClampedArray(40 * 40 * 4), width: 40, height: 40
      })
    } as unknown as CanvasRenderingContext2D)
    Object.defineProperty(HTMLVideoElement.prototype, 'readyState', {
      configurable: true, get: () => 4
    })
    Object.defineProperty(HTMLVideoElement.prototype, 'videoWidth', {
      configurable: true, get: () => 640
    })
    Object.defineProperty(HTMLVideoElement.prototype, 'videoHeight', {
      configurable: true, get: () => 480
    })

    const onTicket = vi.fn()
    render(<CameraQrScanner onTicket={onTicket} />)
    fireEvent.click(screen.getByRole('button', { name: '打开摄像头扫码' }))
    await waitFor(() => expect(onTicket).toHaveBeenCalledExactlyOnceWith(ticket))
    expect(stopTrack).toHaveBeenCalledTimes(1)
    expect(screen.getByRole('button', { name: '打开摄像头扫码' })).toBeTruthy()
  })

  it('decodes a photo from native camera capture without getUserMedia', async () => {
    decodeMock.mockReturnValue({ data: `http://localhost:5174/?ticket=${ticket}` })
    vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockReturnValue({
      drawImage: vi.fn(),
      getImageData: () => ({
        data: new Uint8ClampedArray(40 * 40 * 4), width: 40, height: 40
      })
    } as unknown as CanvasRenderingContext2D)

    const revoke = vi.fn()
    Object.defineProperty(URL, 'createObjectURL', { configurable: true, value: vi.fn(() => 'blob:test') })
    Object.defineProperty(URL, 'revokeObjectURL', { configurable: true, value: revoke })
    vi.stubGlobal('Image', class {
      naturalWidth = 600
      naturalHeight = 600
      onload: (() => void) | null = null
      onerror: (() => void) | null = null
      set src(_value: string) { queueMicrotask(() => this.onload?.()) }
    })
    const onTicket = vi.fn()
    render(<CameraQrScanner onTicket={onTicket} />)
    const photo = new File(['fake-image'], 'qr.png', { type: 'image/png' })
    fireEvent.change(screen.getByLabelText('拍照识别二维码'), { target: { files: [photo] } })
    await waitFor(() => expect(onTicket).toHaveBeenCalledExactlyOnceWith(ticket))
    expect(getUserMedia).not.toHaveBeenCalled()
    expect(revoke).toHaveBeenCalledExactlyOnceWith('blob:test')
  })

  it('releases a late stream when component unmounts before permission resolves', async () => {
    let resolve!: (stream: MediaStream) => void
    getUserMedia.mockReturnValueOnce(new Promise<MediaStream>(r => { resolve = r }))
    const view = render(<CameraQrScanner onTicket={vi.fn()} />)
    fireEvent.click(screen.getByRole('button', { name: '打开摄像头扫码' }))
    view.unmount()
    resolve({ getTracks: () => [{ stop: stopTrack }] } as unknown as MediaStream)
    await waitFor(() => expect(stopTrack).toHaveBeenCalledTimes(1))
  })
})
