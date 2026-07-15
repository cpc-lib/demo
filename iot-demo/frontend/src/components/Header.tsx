import { Activity, Bell, Database } from 'lucide-react'

type Props = { connected: boolean; bucket: string }

export function Header({ connected, bucket }: Props) {
  return (
    <header className="app-header">
      <a className="brand" href="#top" aria-label="Therma 首页">Therma</a>
      <nav aria-label="主导航">
        <a className="active" href="#live">实时温度</a>
        <a href="#history">历史数据</a>
        <a href="#alerts">告警</a>
      </nav>
      <div className="header-status">
        <span className={`socket-state ${connected ? 'online' : ''}`}>
          <Activity size={15} /> WebSocket {connected ? '已连接' : '重连中'}
        </span>
        <span className="bucket"><Database size={15} /> {bucket || 'InfluxDB'}</span>
        <button className="icon-button" aria-label="告警通知"><Bell size={18} /></button>
      </div>
    </header>
  )
}
