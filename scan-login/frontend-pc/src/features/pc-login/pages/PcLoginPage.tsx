import { useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { AppShell } from '@/app/AppShell'
import { ApiError, request } from '@/shared/api/httpClient'
import { LoginOverlay } from '@/shared/components/LoginOverlay'
import { SystemHome } from '@/shared/components/SystemHome'
import { UserAvatarMenu } from '@/shared/components/UserAvatarMenu'
import { QrLoginCard } from '@/features/pc-login/components/QrLoginCard'
import { useCaptcha } from '@/features/pc-login/hooks/useCaptcha'
import { usePcQrLogin } from '@/features/pc-login/hooks/usePcQrLogin'

export default function PcLoginPage() {
  const { account, qr, status, busy, error, refresh, logout, loginWithPassword } = usePcQrLogin()
  const { captcha, refresh: refreshCaptcha } = useCaptcha()
  const [loginMode, setLoginMode] = useState<'qrcode' | 'password'>('qrcode')
  const [formMode, setFormMode] = useState<'login' | 'register'>('login')
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [password2, setPassword2] = useState('')
  const [captchaCode, setCaptchaCode] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [registerMessage, setRegisterMessage] = useState('')
  const [registerError, setRegisterError] = useState('')
  const [registering, setRegistering] = useState(false)

  const openPasswordMode = () => {
    setLoginMode('password')
    setFormMode('login')
    if (!captcha) void refreshCaptcha()
  }

  const handlePasswordSubmit = async (event: FormEvent) => {
    event.preventDefault()
    const ok = await loginWithPassword(username, password, captcha?.captchaId ?? '', captchaCode)
    if (!ok) {
      // Each captcha is single-use server-side: start over with a fresh image.
      setCaptchaCode('')
      void refreshCaptcha()
    }
  }

  const handleRegisterSubmit = async (event: FormEvent) => {
    event.preventDefault()
    setRegisterError('')
    setRegisterMessage('')
    if (password !== password2) {
      setRegisterError('两次输入的密码不一致，请重新输入')
      return
    }
    setRegistering(true)
    try {
      const result = await request<{ username: string }>('/api/mobile/register',
        { method: 'POST', json: {
          username, password,
          captchaId: captcha?.captchaId ?? '',
          captchaCode,
        } })
      setFormMode('login')
      setPassword('')
      setPassword2('')
      // The register just consumed the captcha; the login form needs a fresh one.
      setCaptchaCode('')
      void refreshCaptcha()
      setRegisterMessage(`注册成功，欢迎 ${result.username}，请登录`)
    } catch (cause) {
      // Captchas are single-use server-side: start over with a fresh image.
      setCaptchaCode('')
      setPassword2('')
      void refreshCaptcha()
      setRegisterError(cause instanceof ApiError
        ? `注册失败（${cause.status}）：${cause.message}`
        : '注册失败：网络异常')
    } finally {
      setRegistering(false)
    }
  }

  // Returning to the login screen after a logout: the previous captcha was already
  // consumed server-side, so the form must start over with a fresh one.
  const hadAccount = useRef(false)
  useEffect(() => {
    if (account) {
      hadAccount.current = true
      return
    }
    if (!hadAccount.current) return
    hadAccount.current = false
    setPassword('')
    setCaptchaCode('')
    setShowPassword(false)
    void refreshCaptcha()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [account])

  return <AppShell mobile={false} headerRight={account
    ? <UserAvatarMenu username={account.username} mobile={false} onLogout={() => void logout()} />
    : undefined}>
    <SystemHome mobile={false} username={account?.username} />
    {!account && <LoginOverlay description={loginMode === 'qrcode'
      ? '当前尚未登录。请使用手机端扫一扫，并在手机上确认后进入系统。'
      : '当前尚未登录。请输入账号、密码和图片验证码。'}>
      {loginMode === 'qrcode' ? <>
        <QrLoginCard qr={qr} status={status} busy={busy} onRefresh={() => void refresh()} />
        {error && <p role="alert" className="error">{error}</p>}
        <p className="login-dialog-note">扫码后需要手机主动确认，登录成功将自动进入系统首页。</p>
        <button type="button" className="mode-switch" onClick={openPasswordMode}>
          使用账号密码登录
        </button>
      </> : <>
        {formMode === 'login' ? <form className="form" onSubmit={event => void handlePasswordSubmit(event)}>
          <label htmlFor="pc-username">账号
            <input id="pc-username" type="text" autoComplete="username" value={username}
              placeholder="请输入账号" disabled={busy}
              onChange={event => setUsername(event.target.value)} />
          </label>
          <label htmlFor="pc-password">密码
            <span className="password-box">
              <input id="pc-password" type={showPassword ? 'text' : 'password'}
                autoComplete="current-password" value={password}
                placeholder="请输入密码" disabled={busy}
                onChange={event => setPassword(event.target.value)} />
              <button type="button" className="eye-toggle" tabIndex={-1}
                aria-label={showPassword ? '隐藏密码' : '显示密码'}
                onClick={() => setShowPassword(value => !value)}>
                {showPassword ? '🙈' : '👁️'}
              </button>
            </span>
          </label>
          <label htmlFor="pc-captcha">验证码
            <span className="captcha-row">
              <input id="pc-captcha" type="text" autoComplete="off" maxLength={4}
                value={captchaCode} placeholder="不区分大小写" disabled={busy}
                onChange={event => setCaptchaCode(event.target.value)} />
              {captcha
                ? <img className="captcha-image" src={captcha.image} alt="图形验证码"
                    title="看不清？点击刷新" onClick={() => void refreshCaptcha()} />
                : <button type="button" className="captcha-image captcha-placeholder"
                    onClick={() => void refreshCaptcha()}>点击加载</button>}
            </span>
          </label>
          {error && <p role="alert" className="error">{error}</p>}
          {registerMessage && <p role="status" className="register-ok">{registerMessage}</p>}
          <button type="submit" disabled={busy}>{busy ? '登录中...' : '登 录'}</button>
        </form> : <form className="form" onSubmit={event => void handleRegisterSubmit(event)}>
          <label htmlFor="pc-reg-username">账号
            <input id="pc-reg-username" type="text" autoComplete="username" value={username}
              placeholder="3～40 位英文、数字或下划线" minLength={3} maxLength={40}
              pattern="[A-Za-z0-9_]{3,40}" disabled={registering}
              title="账号只能包含英文、数字和下划线，长度 3～40 位"
              onChange={event => setUsername(event.target.value)} />
          </label>
          <label htmlFor="pc-reg-password">密码
            <span className="password-box">
              <input id="pc-reg-password" type={showPassword ? 'text' : 'password'}
                autoComplete="new-password" value={password}
                placeholder="至少 8 位" minLength={8} disabled={registering}
                onChange={event => setPassword(event.target.value)} />
              <button type="button" className="eye-toggle" tabIndex={-1}
                aria-label={showPassword ? '隐藏密码' : '显示密码'}
                onClick={() => setShowPassword(value => !value)}>
                {showPassword ? '🙈' : '👁️'}
              </button>
            </span>
          </label>
          <label htmlFor="pc-reg-password2">确认密码
            <span className="password-box">
              <input id="pc-reg-password2" type={showPassword ? 'text' : 'password'}
                autoComplete="new-password" value={password2}
                placeholder="请再次输入密码" minLength={8} disabled={registering}
                onChange={event => setPassword2(event.target.value)} />
              <button type="button" className="eye-toggle" tabIndex={-1}
                aria-label={showPassword ? '隐藏密码' : '显示密码'}
                onClick={() => setShowPassword(value => !value)}>
                {showPassword ? '🙈' : '👁️'}
              </button>
            </span>
          </label>
          <label htmlFor="pc-reg-captcha">验证码
            <span className="captcha-row">
              <input id="pc-reg-captcha" type="text" autoComplete="off" maxLength={4}
                value={captchaCode} placeholder="不区分大小写" disabled={registering}
                onChange={event => setCaptchaCode(event.target.value)} />
              {captcha
                ? <img className="captcha-image" src={captcha.image} alt="图形验证码"
                    title="看不清？点击刷新" onClick={() => void refreshCaptcha()} />
                : <button type="button" className="captcha-image captcha-placeholder"
                    onClick={() => void refreshCaptcha()}>点击加载</button>}
            </span>
          </label>
          {registerError && <p role="alert" className="error">{registerError}</p>}
          <button type="submit" disabled={registering}>{registering ? '注册中...' : '注 册'}</button>
        </form>}
        {formMode === 'login'
          ? <button type="button" className="mode-switch" onClick={() => {
            setFormMode('register')
            setRegisterError('')
            setRegisterMessage('')
            if (!captcha) void refreshCaptcha()
          }}>没有账号？注册新账号</button>
          : <button type="button" className="mode-switch" onClick={() => {
            setFormMode('login')
            setRegisterError('')
            setRegisterMessage('')
          }}>已有账号？返回登录</button>}
        <button type="button" className="mode-switch" onClick={() => setLoginMode('qrcode')}>
          返回扫码登录
        </button>
      </>}
    </LoginOverlay>}
  </AppShell>
}
