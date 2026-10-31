type Props = {
  mobile: boolean
  username?: string
  onScan?: () => void
}

/** Shared public dashboard shell; sensitive account data is rendered only when authenticated. */
export function SystemHome({ mobile, username, onScan }: Props) {
  return <div className="dashboard">
    <div className="dashboard-breadcrumb">工作台 <span>/</span> 系统首页</div>
    <section className="dashboard-hero">
      <div>
        <span className="dashboard-eyebrow">SCANLOGIN · SECURITY CONSOLE</span>
        <h1>{username ? `欢迎回来，${username}` : '欢迎使用安全工作台'}</h1>
        <p>{mobile ? '使用手机验证身份，安全确认每一次电脑登录。'
          : '你的身份认证工作台。扫码通过手机确认后，即可安全访问系统。'}</p>
        <span className="dashboard-hero-tag">{username ? '● 已认证' : '○ 等待身份验证'}</span>
      </div>
      <div className="dashboard-symbol" aria-hidden="true">⌗</div>
    </section>

    <div className="dashboard-section-heading">
      <div><h2>快捷入口</h2><p>管理身份验证和跨设备授权</p></div>
      <span>WORKSPACE</span>
    </div>
    <div className="dashboard-cards">
      <article className="dashboard-card">
        <div className="dashboard-card-icon">◎</div>
        <h3>账户中心</h3>
        <p>{username ? `当前登录账号：${username}` : '登录后查看个人账户和安全状态'}</p>
        <span className="dashboard-card-meta">身份信息 · 安全管理</span>
      </article>
      <article className="dashboard-card">
        <div className="dashboard-card-icon">▣</div>
        <h3>扫码授权</h3>
        <p>{mobile ? '扫描电脑上的动态二维码，并在手机端手动确认授权。'
          : '电脑端等待手机扫码和身份确认，登录凭证仅可消费一次。'}</p>
        {mobile && username && onScan
          ? <button type="button" className="dashboard-card-action" onClick={onScan}>立即扫一扫 →</button>
          : <span className="dashboard-card-meta">Redis 原子状态机 · 120 秒有效</span>}
      </article>
      <article className="dashboard-card">
        <div className="dashboard-card-icon">◇</div>
        <h3>会话安全</h3>
        <p>跨设备认证隔离，PC 使用受保护 Cookie，手机使用独立的 Bearer 身份。</p>
        <span className="dashboard-card-meta">JWT · CSRF 防护</span>
      </article>
    </div>
    <section className="dashboard-activity">
      <div><h2>安全提示</h2><p>扫描后请核对电脑端验证码，只对本人发起的登录请求进行确认。</p></div>
      <span>安全第一</span>
    </section>
  </div>
}
