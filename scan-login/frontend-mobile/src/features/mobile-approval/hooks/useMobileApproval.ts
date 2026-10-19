import { useCallback, useEffect, useRef, useState } from 'react'
import { ApiError, errorMessage } from '@/shared/api/httpClient'
import { mobileAuthApi } from '@/features/mobile-approval/api/mobileAuthApi'
import type { Captcha, MobileOperation, MobileScan, MobileSession } from '@/features/mobile-approval/model/types'

export function useMobileApproval() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [password2, setPassword2] = useState('')
  const [ticket, setTicket] = useState(() => new URLSearchParams(window.location.search).get('ticket') ?? '')
  const [session, setSession] = useState<MobileSession | null>(null)
  const [scan, setScan] = useState<MobileScan | null>(null)
  const [scannedTicket, setScannedTicket] = useState<string | null>(null)
  const [confirmed, setConfirmed] = useState(false)
  const [mode, setMode] = useState<'login' | 'register'>('login')
  const [captcha, setCaptcha] = useState<Captcha | null>(null)
  const [captchaCode, setCaptchaCode] = useState('')
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const inFlight = useRef(false)
  const version = useRef(0)

  /** Captchas are single-use: a fresh image is needed after every failed login and every logout. */
  const refreshCaptcha = useCallback(async () => {
    try {
      setCaptcha(await mobileAuthApi.captcha())
    } catch {
      setCaptcha(null)
    }
  }, [])

  useEffect(() => {
    if (!session) void refreshCaptcha()
  }, [session, refreshCaptcha])

  const updateTicket = (value: string) => {
    version.current++
    setTicket(value)
    setScan(null)
    setScannedTicket(null)
    setConfirmed(false)
    setMessage('')
    setError('')
  }

  const logout = () => {
    version.current++
    setSession(null)
    setScan(null)
    setScannedTicket(null)
    setConfirmed(false)
    setPassword('')
    setPassword2('')
    setCaptchaCode('')
    setMessage('')
    setError('')
    // The captcha shown before logout was consumed by the login; fetch a fresh one.
    void refreshCaptcha()
  }

  const perform = async (operation: MobileOperation, cameraTicket?: string) => {
    if (inFlight.current) return
    inFlight.current = true
    setBusy(true)
    setMessage('')
    setError('')
    const requestVersion = version.current
    /** Retries once with a fresh token pair when the access token has expired. */
    const authedCall = async <T,>(call: (token: string) => Promise<T>): Promise<T> => {
      if (!session) throw new Error('请先登录并扫描二维码')
      try {
        return await call(session.accessToken)
      } catch (cause) {
        if (!(cause instanceof ApiError) || cause.status !== 401 || !session.refreshToken) throw cause
        const refreshed = await mobileAuthApi.refresh(session.refreshToken)
        if (requestVersion === version.current) setSession(refreshed)
        return call(refreshed.accessToken)
      }
    }
    try {
      switch (operation) {
        case 'login': {
          const result = await mobileAuthApi.login(username, password, captcha?.captchaId ?? '', captchaCode)
          if (requestVersion === version.current) {
            setSession(result)
            setPassword('')
            setCaptchaCode('')
            setMessage('手机身份验证成功')
          }
          break
        }
        case 'register': {
          if (password !== password2) throw new Error('两次输入的密码不一致，请重新输入')
          const result = await mobileAuthApi.register(username, password,
            captcha?.captchaId ?? '', captchaCode)
          if (requestVersion === version.current) {
            setPassword('')
            setPassword2('')
            // The register just consumed the captcha; the login form needs a fresh one.
            setCaptchaCode('')
            void refreshCaptcha()
            setMode('login')
            setMessage(`注册成功，欢迎 ${result.username}，请登录`)
          }
          break
        }
        case 'scan': {
          const scannedValue = cameraTicket ?? ticket
          if (!session || !scannedValue) throw new Error('请先登录并扫描二维码')
          const result = await authedCall(token => mobileAuthApi.scan(scannedValue, token))
          if (requestVersion === version.current) {
            setScan(result)
            setScannedTicket(scannedValue)
            setConfirmed(false)
            setMessage('已扫描，请核对电脑端验证码后确认')
          }
          break
        }
        case 'confirm':
          if (!session || !scan || !scannedTicket || scannedTicket !== ticket) {
            throw new Error('请重新扫描二维码')
          }
          await authedCall(token => mobileAuthApi.confirm(scannedTicket, token))
          if (requestVersion === version.current) {
            setConfirmed(true)
            setMessage('授权成功！请返回电脑查看登录结果。')
          }
          break
      }
    } catch (cause) {
      if (requestVersion === version.current) {
        const duplicate = cause instanceof ApiError && cause.status === 409
        setError(operation === 'register' && duplicate ? '账号名已被占用，请更换后重试' : errorMessage(cause))
        if (operation === 'login' || operation === 'register') {
          setCaptchaCode('')
          void refreshCaptcha()
        }
      }
    } finally {
      inFlight.current = false
      setBusy(false)
    }
  }

  return {
    username, setUsername, password, setPassword, password2, setPassword2, ticket, updateTicket,
    session, scan, confirmed, busy, message, error, perform, logout,
    mode, setMode, captcha, captchaCode, setCaptchaCode, refreshCaptcha
  }
}
