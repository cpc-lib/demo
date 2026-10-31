import { useState } from 'react'
import type { Captcha } from '@/features/mobile-approval/model/types'

type Props = {
  mode: 'login' | 'register'
  username: string
  password: string
  password2: string
  busy: boolean
  captcha: Captcha | null
  captchaCode: string
  onUsername: (value: string) => void
  onPassword: (value: string) => void
  onPassword2: (value: string) => void
  onCaptchaCode: (value: string) => void
  onRefreshCaptcha: () => void
  onSwitchMode: (mode: 'login' | 'register') => void
  onSubmit: () => void
}
export function CredentialsForm(props: Props) {
  const [showPassword, setShowPassword] = useState(false)
  const register = props.mode === 'register'
  return <>
    <form className="form" onSubmit={event => { event.preventDefault(); props.onSubmit() }}>
      <label htmlFor="username">账号
        <input id="username" autoComplete="username" value={props.username}
          placeholder="3～40 位英文、数字或下划线" pattern="[A-Za-z0-9_]{3,40}"
          title="账号只能包含英文、数字和下划线，长度 3～40 位"
          onChange={event => props.onUsername(event.target.value)} />
      </label>
      <label htmlFor="password">密码
        <span className="password-box">
          <input id="password" type={showPassword ? 'text' : 'password'}
            autoComplete={register ? 'new-password' : 'current-password'}
            value={props.password} placeholder="至少 8 位" minLength={8}
            onChange={event => props.onPassword(event.target.value)} />
          <button type="button" className="eye-toggle" tabIndex={-1}
            aria-label={showPassword ? '隐藏密码' : '显示密码'}
            onClick={() => setShowPassword(value => !value)}>
            {showPassword ? '🙈' : '👁️'}
          </button>
        </span>
      </label>
      {register && <label htmlFor="password2">确认密码
        <span className="password-box">
          <input id="password2" type={showPassword ? 'text' : 'password'}
            autoComplete="new-password" value={props.password2}
            placeholder="请再次输入密码" minLength={8}
            onChange={event => props.onPassword2(event.target.value)} />
          <button type="button" className="eye-toggle" tabIndex={-1}
            aria-label={showPassword ? '隐藏密码' : '显示密码'}
            onClick={() => setShowPassword(value => !value)}>
            {showPassword ? '🙈' : '👁️'}
          </button>
        </span>
      </label>}
      <label htmlFor="captcha">验证码
        <span className="captcha-row">
          <input id="captcha" type="text" autoComplete="off" maxLength={4}
            value={props.captchaCode} placeholder="不区分大小写"
            onChange={event => props.onCaptchaCode(event.target.value)} />
          {props.captcha
            ? <img className="captcha-image" src={props.captcha.image} alt="图形验证码"
                title="看不清？点击刷新" onClick={props.onRefreshCaptcha} />
            : <button type="button" className="captcha-image captcha-placeholder"
                onClick={props.onRefreshCaptcha}>点击加载</button>}
        </span>
      </label>
      <div className="actions">
        <button type="submit" disabled={props.busy}>{register ? '注 册' : '手机登录'}</button>
      </div>
    </form>
    <button type="button" className="mode-switch"
      onClick={() => props.onSwitchMode(register ? 'login' : 'register')}>
      {register ? '已有账号？返回登录' : '没有账号？注册新账号'}
    </button>
  </>
}
