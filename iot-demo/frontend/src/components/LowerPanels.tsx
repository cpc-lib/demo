import { AlertTriangle, CheckCircle2, DatabaseZap } from 'lucide-react'
import type { Sensor, TemperatureReading } from '../types'

type Props = { sensors: Sensor[]; latest: TemperatureReading[] }

export function LowerPanels({ sensors, latest }: Props) {
  const alerts = latest.filter((reading) => reading.status !== 'NORMAL')
  return (
    <div className="lower-grid">
      <section className="health-panel">
        <div className="panel-heading"><div><span>系统概况</span><h2>设备健康</h2></div><DatabaseZap size={20} /></div>
        <div className="health-body">
          <div className="health-ring"><strong>{latest.length}</strong><span>在线设备</span></div>
          <dl>
            <div><dt><i className="dot" />正常</dt><dd>{latest.length - alerts.length}</dd></div>
            <div><dt><i className="dot red" />告警</dt><dd>{alerts.length}</dd></div>
            <div><dt><i className="dot gray" />未采样</dt><dd>{Math.max(0, sensors.length - latest.length)}</dd></div>
          </dl>
        </div>
      </section>
      <section className="latest-panel">
        <div className="panel-heading"><div><span>WebSocket 数据流</span><h2>最新数据</h2></div><span className="live-mark"><i />LIVE</span></div>
        <div className="reading-table" role="table">
          <div className="table-row table-head" role="row"><span>传感器</span><span>位置</span><span>温度</span><span>状态</span></div>
          {latest.slice(0, 5).map((reading) => (
            <div className="table-row" role="row" key={reading.sensorId}>
              <strong>{reading.sensorName}</strong><span>{reading.location}</span><b>{reading.value.toFixed(1)}°C</b>
              <em className={reading.status === 'NORMAL' ? 'normal' : 'warning'}>
                {reading.status === 'NORMAL' ? <CheckCircle2 size={14} /> : <AlertTriangle size={14} />}
                {reading.status === 'NORMAL' ? '正常' : '告警'}
              </em>
            </div>
          ))}
        </div>
      </section>
    </div>
  )
}
