import { useEffect, useRef, useState, type ChangeEvent } from 'react'
import jsQR from 'jsqr'
import { parseQrTicket } from '@/features/mobile-approval/model/qrPayload'

type ScannerMode = 'idle' | 'starting' | 'scanning'

type Props = {
  disabled?: boolean
  onTicket: (ticket: string) => void
}

function cameraError(error: unknown): string {
  const name = error instanceof DOMException ? error.name : ''
  if (name === 'NotAllowedError' || name === 'PermissionDeniedError') {
    return '摄像头权限被拒绝。请在浏览器站点设置中允许相机，然后重试。'
  }
  if (name === 'NotFoundError' || name === 'DevicesNotFoundError') {
    return '没有检测到可用摄像头，请连接摄像头或改用手动输入。'
  }
  if (name === 'NotReadableError' || name === 'TrackStartError') {
    return '摄像头被其他应用占用，请关闭占用相机的应用后重试。'
  }
  return '摄像头启动失败，请检查系统相机权限并重试。'
}

/** Camera only starts following an explicit click; decoded images never leave the browser. */
export function CameraQrScanner({ onTicket, disabled = false }: Props) {
  const [mode, setMode] = useState<ScannerMode>('idle')
  const [error, setError] = useState('')
  const [readingPhoto, setReadingPhoto] = useState(false)
  const videoRef = useRef<HTMLVideoElement>(null)
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const streamRef = useRef<MediaStream | null>(null)
  const timeoutRef = useRef<number | null>(null)
  const runRef = useRef(0)
  const startedRef = useRef(false)
  const callbackRef = useRef(onTicket)
  callbackRef.current = onTicket

  function release() {
    if (timeoutRef.current !== null) {
      window.clearTimeout(timeoutRef.current)
      timeoutRef.current = null
    }
    streamRef.current?.getTracks().forEach(track => track.stop())
    streamRef.current = null
    if (videoRef.current) {
      videoRef.current.pause()
      videoRef.current.srcObject = null
    }
  }

  function stop() {
    runRef.current += 1
    startedRef.current = false
    release()
    setMode('idle')
  }

  useEffect(() => () => {
    // Unmount, logout and page navigation must release the hardware camera.
    runRef.current += 1
    release()
  }, [])

  function scanFrame(runId: number) {
    if (runId !== runRef.current) return
    const video = videoRef.current
    const canvas = canvasRef.current
    if (video && canvas && video.readyState >= 2 && video.videoWidth && video.videoHeight) {
      const context = canvas.getContext('2d', { willReadFrequently: true })
      if (context) {
        const scale = Math.min(1, 800 / video.videoWidth)
        canvas.width = Math.max(1, Math.floor(video.videoWidth * scale))
        canvas.height = Math.max(1, Math.floor(video.videoHeight * scale))
        context.drawImage(video, 0, 0, canvas.width, canvas.height)
        const pixels = context.getImageData(0, 0, canvas.width, canvas.height)
        const decoded = jsQR(pixels.data, pixels.width, pixels.height, {
          inversionAttempts: 'attemptBoth'
        })
        if (decoded?.data) {
          try {
            const ticket = parseQrTicket(decoded.data)
            runRef.current++
            startedRef.current = false
            release()
            setMode('idle')
            setError('')
            callbackRef.current(ticket)
            return
          } catch (cause) {
            setError(cause instanceof Error ? cause.message : '无法识别登录二维码')
          }
        }
      }
    }
    timeoutRef.current = window.setTimeout(() => scanFrame(runId), 140)
  }

  async function start() {
    if (startedRef.current || disabled) return
    setError('')

    if (!window.isSecureContext && !['localhost', '127.0.0.1'].includes(window.location.hostname)) {
      setError('手机摄像头需要 HTTPS 安全连接。请用受设备信任的 HTTPS 地址打开此页面；局域网 HTTP 地址不能调用摄像头。')
      return
    }
    if (!navigator.mediaDevices?.getUserMedia) {
      setError('当前浏览器不支持摄像头接口，请使用 Chrome、Safari 等支持相机的浏览器。')
      return
    }

    startedRef.current = true
    const runId = ++runRef.current
    setMode('starting')
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        audio: false,
        video: { facingMode: { ideal: 'environment' }, width: { ideal: 1280 }, height: { ideal: 720 } }
      })
      // If user left while browser was asking for permission, immediately stop the late stream.
      if (runId !== runRef.current) {
        stream.getTracks().forEach(track => track.stop())
        return
      }
      streamRef.current = stream
      const video = videoRef.current
      if (!video) {
        stop()
        return
      }
      video.srcObject = stream
      await video.play()
      if (runId !== runRef.current) return
      setMode('scanning')
      scanFrame(runId)
    } catch (cause) {
      if (runId !== runRef.current) return
      release()
      startedRef.current = false
      setMode('idle')
      setError(cameraError(cause))
    }
  }

  async function importPhoto(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file || disabled || readingPhoto) return
    if (!file.type.startsWith('image/')) {
      setError('请拍摄或选择一张图片')
      return
    }
    if (file.size > 20 * 1024 * 1024) {
      setError('图片过大，请选择小于 20MB 的照片')
      return
    }

    setReadingPhoto(true)
    setError('')
    const sequence = ++runRef.current
    // A photo capture should stop a previous continuous scanner first.
    startedRef.current = false
    release()
    setMode('idle')
    let objectUrl: string | undefined
    try {
      objectUrl = URL.createObjectURL(file)
      const picture = new Image()
      await new Promise<void>((resolve, reject) => {
        picture.onload = () => resolve()
        picture.onerror = () => reject(new Error('无法读取图片，请重新拍照'))
        picture.src = objectUrl!
      })
      if (sequence !== runRef.current) return
      const canvas = canvasRef.current
      const ctx = canvas?.getContext('2d', { willReadFrequently: true })
      if (!canvas || !ctx) throw new Error('当前浏览器不支持图片二维码解码')
      const scale = Math.min(1, 1600 / Math.max(picture.naturalWidth, picture.naturalHeight))
      canvas.width = Math.max(1, Math.round(picture.naturalWidth * scale))
      canvas.height = Math.max(1, Math.round(picture.naturalHeight * scale))
      ctx.drawImage(picture, 0, 0, canvas.width, canvas.height)
      const pixels = ctx.getImageData(0, 0, canvas.width, canvas.height)
      const decoded = jsQR(pixels.data, pixels.width, pixels.height, {
        inversionAttempts: 'attemptBoth'
      })
      if (!decoded?.data) throw new Error('照片中未识别到二维码，请对准电脑二维码重新拍照')
      const ticket = parseQrTicket(decoded.data)
      callbackRef.current(ticket)
    } catch (cause) {
      if (sequence === runRef.current) {
        setError(cause instanceof Error ? cause.message : '图片识别失败，请重试')
      }
    } finally {
      if (objectUrl) URL.revokeObjectURL(objectUrl)
      if (sequence === runRef.current) setReadingPhoto(false)
    }
  }

  return <div className="camera-scanner">
    {mode === 'idle'
      ? <button type="button" disabled={disabled} onClick={() => void start()}>打开摄像头扫码</button>
      : <button type="button" className="secondary" onClick={stop}>关闭摄像头</button>}
    <div className="camera-photo-actions">
      <label className={disabled || readingPhoto ? 'camera-file-trigger disabled' : 'camera-file-trigger'}>
        {readingPhoto ? '正在识别照片...' : '拍照识别二维码'}
        <input aria-label="拍照识别二维码" type="file" accept="image/*" capture="environment"
          disabled={disabled || readingPhoto} onChange={event => void importPhoto(event)} />
      </label>
      <label className={disabled || readingPhoto ? 'camera-file-trigger disabled' : 'camera-file-trigger'}>
        从相册选择
        <input aria-label="从相册选择二维码" type="file" accept="image/*"
          disabled={disabled || readingPhoto} onChange={event => void importPhoto(event)} />
      </label>
    </div>
    <div className="camera-preview" hidden={mode === 'idle'}>
      <video ref={videoRef} autoPlay playsInline muted aria-label="摄像头扫码预览" />
      <div className="camera-guide" aria-hidden="true" />
      {mode === 'starting' && <p className="camera-loading">正在请求摄像头权限...</p>}
      {mode === 'scanning' && <p className="camera-loading">将电脑端二维码放入取景框</p>}
    </div>
    <canvas ref={canvasRef} hidden aria-hidden="true" />
    {error && <p className="error" role="alert">{error}</p>}
  </div>
}
