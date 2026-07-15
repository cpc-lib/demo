import { Radio, Thermometer } from 'lucide-react'
import type { Sensor, TemperatureReading } from '../types'

type Props = {
  sensors: Sensor[]
  latest: Map<string, TemperatureReading>
  selectedId: string
  onSelect: (sensorId: string) => void
}

export function SensorRail({ sensors, latest, selectedId, onSelect }: Props) {
  return (
    <aside className="sensor-rail">
      <div className="section-heading">
        <div><span>设备目录</span><h2>传感器</h2></div>
        <Radio size={19} />
      </div>
      <div className="sensor-list">
        {sensors.map((sensor) => {
          const reading = latest.get(sensor.id)
          return (
            <button key={sensor.id} className={`sensor-row ${selectedId === sensor.id ? 'selected' : ''}`}
                    onClick={() => onSelect(sensor.id)}>
              <span className="sensor-icon"><Thermometer size={19} /></span>
              <span className="sensor-copy">
                <strong>{sensor.name}</strong>
                <small><i className="dot" /> {sensor.location}</small>
              </span>
              <span className={`sensor-value ${reading?.status !== 'NORMAL' ? 'alert' : ''}`}>
                {reading ? `${reading.value.toFixed(1)}°` : '--'}
              </span>
            </button>
          )
        })}
      </div>
      <div className="rail-note">
        <span>采集协议</span>
        <strong>WebSocket</strong>
        <small>5 秒自动采样 · 实时推送</small>
      </div>
    </aside>
  )
}
