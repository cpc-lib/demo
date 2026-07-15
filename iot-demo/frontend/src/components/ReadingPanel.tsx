import { Pause, Play, RadioTower } from 'lucide-react'
import type { Sensor, TemperatureReading } from '../types'

type Props = {
  sensor?: Sensor
  current?: TemperatureReading
  readings: TemperatureReading[]
  simulatorRunning: boolean
  onToggle: () => void
  onGenerate: () => void
}

export function ReadingPanel({ sensor, current, readings, simulatorRunning, onToggle, onGenerate }: Props) {
  const values = readings.map((item) => item.value)
  const min = values.length ? Math.min(...values) : undefined
  const max = values.length ? Math.max(...values) : undefined
  const avg = values.length ? values.reduce((sum, value) => sum + value, 0) / values.length : undefined

  return (
    <aside className="reading-panel" id="live">
      <div className="reading-title"><div><span>当前读数</span><strong>{sensor?.name || '未选择'}</strong></div><RadioTower size={20} /></div>
      <div className={`hero-reading ${current?.status !== 'NORMAL' ? 'danger' : ''}`}>
        <strong>{current ? current.value.toFixed(1) : '--'}</strong><span>°C</span>
      </div>
      <div className="reading-meta">
        <span><i className="dot" />实时在线</span>
        <time>{current ? new Date(current.timestamp).toLocaleString('zh-CN') : '等待采样'}</time>
      </div>
      <div className="stat-row">
        <div><span>最小值</span><strong>{min?.toFixed(1) ?? '--'}°</strong></div>
        <div><span>最大值</span><strong>{max?.toFixed(1) ?? '--'}°</strong></div>
        <div><span>平均值</span><strong>{avg?.toFixed(1) ?? '--'}°</strong></div>
      </div>
      <div className="thresholds">
        <div className="threshold-heading"><strong>告警阈值</strong><span>服务端判定</span></div>
        <label><span><i className="line high" />高温</span><b>35.0 °C</b></label>
        <label><span><i className="line low" />低温</span><b>5.0 °C</b></label>
      </div>
      <div className="sim-actions">
        <button className="primary-action" onClick={onToggle}>
          {simulatorRunning ? <Pause size={17} /> : <Play size={17} />}
          {simulatorRunning ? '暂停模拟器' : '启动模拟器'}
        </button>
        <button className="secondary-action" onClick={onGenerate}>立即采样</button>
      </div>
    </aside>
  )
}
