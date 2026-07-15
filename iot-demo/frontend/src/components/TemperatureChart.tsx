import { useMemo } from 'react'
import { Area, AreaChart, CartesianGrid, ReferenceLine, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import type { TemperatureReading } from '../types'

type Props = { readings: TemperatureReading[]; hours: number; onHoursChange: (hours: number) => void }

export function TemperatureChart({ readings, hours, onHoursChange }: Props) {
  const data = useMemo(() => readings.map((reading) => ({
    ...reading,
    label: new Date(reading.timestamp).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }),
  })), [readings])

  return (
    <section className="chart-section" id="history">
      <div className="chart-toolbar">
        <div><span className="overline">InfluxDB 时序查询</span><h2>温度趋势</h2></div>
        <div className="range-switch" aria-label="历史时间范围">
          {[1, 6, 24, 168].map((value) => (
            <button key={value} className={hours === value ? 'active' : ''} onClick={() => onHoursChange(value)}>
              {value === 168 ? '7天' : `${value}小时`}
            </button>
          ))}
        </div>
      </div>
      <div className="chart-wrap">
        {data.length ? (
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart data={data} margin={{ top: 18, right: 12, left: -22, bottom: 0 }}>
              <defs>
                <linearGradient id="temperatureFill" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor="#f0443b" stopOpacity={0.24} />
                  <stop offset="100%" stopColor="#f0443b" stopOpacity={0.015} />
                </linearGradient>
              </defs>
              <CartesianGrid stroke="#e9edf3" vertical={false} />
              <XAxis dataKey="label" tick={{ fill: '#68758a', fontSize: 12 }} axisLine={false} tickLine={false} minTickGap={45} />
              <YAxis domain={[0, 40]} tick={{ fill: '#68758a', fontSize: 12 }} axisLine={false} tickLine={false} />
              <Tooltip contentStyle={{ border: '1px solid #dde3ec', borderRadius: 10, boxShadow: '0 8px 30px rgba(17,37,66,.1)' }} formatter={(value) => [`${value} °C`, '温度']} />
              <ReferenceLine y={35} stroke="#f0443b" strokeDasharray="5 5" label={{ value: '高阈值 35°C', fill: '#d9342c', fontSize: 11, position: 'insideTopRight' }} />
              <ReferenceLine y={5} stroke="#2476dc" strokeDasharray="5 5" label={{ value: '低阈值 5°C', fill: '#2476dc', fontSize: 11, position: 'insideTopRight' }} />
              <Area type="monotone" dataKey="value" stroke="#ef3f37" strokeWidth={2.2} fill="url(#temperatureFill)" isAnimationActive={false} />
            </AreaChart>
          </ResponsiveContainer>
        ) : <div className="empty-chart"><span>等待 InfluxDB 数据</span><small>启动后约 5 秒产生第一批采样</small></div>}
      </div>
    </section>
  )
}
