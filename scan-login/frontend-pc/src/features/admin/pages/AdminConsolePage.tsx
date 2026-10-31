import { useCallback, useEffect, useState } from 'react'
import { AppShell } from '@/app/AppShell'
import { ApiError, request } from '@/shared/api/httpClient'

type UserSummary = {
  id: number
  username: string
  enabled: boolean
  tokenVersion: number
  createdAt: string
  activeRefreshTokens: number
}

const KEY_STORAGE = 'scan-login-admin-key'

export default function AdminConsolePage() {
  const [adminKey, setAdminKey] = useState(() => sessionStorage.getItem(KEY_STORAGE) ?? '')
  const [keyDraft, setKeyDraft] = useState(adminKey)
  const [users, setUsers] = useState<UserSummary[]>([])
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const load = useCallback(async () => {
    if (!adminKey) {
      setError('请先填写管理密钥')
      return
    }
    setError('')
    setMessage('')
    setBusy(true)
    try {
      setUsers(await request<UserSummary[]>('/api/admin/users', { headers: { 'X-Admin-Key': adminKey } }))
      setMessage('用户列表已刷新')
    } catch (cause) {
      setUsers([])
      setError(cause instanceof ApiError
        ? `加载失败（${cause.status}）：${cause.message}`
        : '加载失败：网络异常')
    } finally {
      setBusy(false)
    }
  }, [adminKey])

  const saveKey = useCallback(() => {
    setAdminKey(keyDraft.trim())
    sessionStorage.setItem(KEY_STORAGE, keyDraft.trim())
  }, [keyDraft])

  const kick = useCallback(async (user: UserSummary) => {
    if (!window.confirm(`确定踢出「${user.username}」吗？\n该用户所有端的登录态会立即失效，且无法自动续期。`)) return
    setError('')
    setMessage('')
    setBusy(true)
    try {
      const result = await request<{ message: string }>(`/api/admin/users/${user.id}/kick`,
        { method: 'POST', headers: { 'X-Admin-Key': adminKey } })
      setMessage(result.message)
      await load()
    } catch (cause) {
      setError(cause instanceof ApiError
        ? `操作失败（${cause.status}）：${cause.message}`
        : '操作失败：网络异常')
    } finally {
      setBusy(false)
    }
  }, [adminKey, load])

  useEffect(() => {
    if (adminKey) void load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  return <AppShell headerRight={<a className="admin-back" href="#/">返回首页</a>}>
    <div className="admin-panel">
      <header className="admin-hero">
        <p className="dashboard-eyebrow">ADMIN CONSOLE</p>
        <h1>管理控制台 · 会话踢出</h1>
        <p>
          踢出会立即作废该用户所有端的 Access/Refresh Token（会话版本 +1 并吊销全部 refresh token），
          被踢用户当场回到登录页，即使其 Access Token 尚未过期。全程由后端强制执行。
        </p>
      </header>

      <section className="admin-keycard">
        <label htmlFor="admin-key">管理密钥（X-Admin-Key）</label>
        <div className="admin-key-row">
          <input id="admin-key" type="password" value={keyDraft}
            placeholder="默认：dev-admin-key"
            onChange={event => setKeyDraft(event.target.value)} />
          <button type="button" onClick={saveKey} disabled={busy}>保存密钥</button>
          <button type="button" className="primary" onClick={() => void load()} disabled={busy}>
            {busy ? '处理中…' : '刷新用户列表'}
          </button>
        </div>
        {error && <p className="admin-note admin-error" role="alert">{error}</p>}
        {message && <p className="admin-note admin-ok" role="status">{message}</p>}
      </section>

      <section className="admin-demo">
        <h2>演示效果</h2>
        <ol>
          <li>打开一个<strong>无痕窗口</strong>，访问 <code>http://localhost:5174</code> 注册并登录新账号。</li>
          <li>回到本页，点该用户的「踢出下线」并确认。</li>
          <li>观察无痕窗口：任何操作立即 401、自动续期也失败，当场回到登录页。</li>
        </ol>
      </section>

      <section className="admin-table-wrap">
        <table className="admin-table">
          <caption className="sr-only">用户会话列表</caption>
          <thead>
            <tr>
              <th scope="col">ID</th>
              <th scope="col">账号名</th>
              <th scope="col">状态</th>
              <th scope="col">会话版本</th>
              <th scope="col">活跃 Refresh</th>
              <th scope="col">创建时间</th>
              <th scope="col">操作</th>
            </tr>
          </thead>
          <tbody>
            {users.map(user => <tr key={user.id}>
              <td>{user.id}</td>
              <td>{user.username}</td>
              <td><span className={user.enabled ? 'badge ok' : 'badge'}>{user.enabled ? '启用' : '停用'}</span></td>
              <td><code>{user.tokenVersion}</code></td>
              <td><code>{user.activeRefreshTokens}</code></td>
              <td>{new Date(user.createdAt).toLocaleString()}</td>
              <td>
                <button type="button" className="danger" disabled={busy}
                  onClick={() => void kick(user)}>踢出下线</button>
              </td>
            </tr>)}
            {users.length === 0 && <tr><td colSpan={7} className="admin-empty">暂无数据——填写密钥后点击「刷新用户列表」。</td></tr>}
          </tbody>
        </table>
      </section>
    </div>
  </AppShell>
}
