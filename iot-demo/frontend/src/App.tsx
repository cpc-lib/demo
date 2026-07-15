import { useCallback, useEffect, useMemo, useState } from 'react'
import { api } from './api'
import { Header } from './components/Header'
import { LowerPanels } from './components/LowerPanels'
import { ReadingPanel } from './components/ReadingPanel'
import { SensorRail } from './components/SensorRail'
import { TemperatureChart } from './components/TemperatureChart'
import { useTemperatureSocket } from './hooks/useTemperatureSocket'
import type { Sensor, SystemStatus, TemperatureReading } from './types'

const MAX_LIVE_POINTS = 600

export default function App() {
  const [sensors, setSensors] = useState<Sensor[]>([])
  const [latest, setLatest] = useState<Map<string, TemperatureReading>>(() => new Map())
  const [history, setHistory] = useState<TemperatureReading[]>([])
  const [selectedId, setSelectedId] = useState('SN-10001')
  const [hours, setHours] = useState(24)
  const [status, setStatus] = useState<SystemStatus>({ simulatorRunning: true, webSocketConnections: 0, bucket: '' })
  const [error, setError] = useState('')

  const handleReading = useCallback((reading: TemperatureReading) => {
    setLatest((previous) => new Map(previous).set(reading.sensorId, reading))
    if (reading.sensorId === selectedId) {
      setHistory((previous) => [...previous, reading].slice(-MAX_LIVE_POINTS))
    }
  }, [selectedId])

  const socketState = useTemperatureSocket(handleReading)

  useEffect(() => {
    Promise.all([api.sensors(), api.latest(), api.status()])
      .then(([sensorData, latestData, systemStatus]) => {
        setSensors(sensorData)
        setLatest(new Map(latestData.map((reading) => [reading.sensorId, reading])))
        setStatus(systemStatus)
        setError('')
      })
      .catch(() => setError('无法连接 Java 服务，请确认 Docker Compose 已启动。'))
  }, [])

  useEffect(() => {
    api.history(selectedId, hours).then(setHistory).catch(() => setHistory([]))
  }, [selectedId, hours])

  const latestList = useMemo(() => sensors.flatMap((sensor) => {
    const reading = latest.get(sensor.id)
    return reading ? [reading] : []
  }), [latest, sensors])

  const selectedSensor = sensors.find((sensor) => sensor.id === selectedId)
  const toggleSimulator = async () => {
    const next = !status.simulatorRunning
    await api.simulator(next ? 'start' : 'stop')
    setStatus((value) => ({ ...value, simulatorRunning: next }))
  }

  return (
    <div className="app-shell" id="top">
      <Header connected={socketState === 'connected'} bucket={status.bucket} />
      {error ? <div className="connection-error" role="alert">{error}</div> : null}
      <main className="dashboard-layout">
        <SensorRail sensors={sensors} latest={latest} selectedId={selectedId} onSelect={setSelectedId} />
        <div className="workspace">
          <TemperatureChart readings={history} hours={hours} onHoursChange={setHours} />
          <LowerPanels sensors={sensors} latest={latestList} />
        </div>
        <ReadingPanel sensor={selectedSensor} current={latest.get(selectedId)} readings={history}
                      simulatorRunning={status.simulatorRunning} onToggle={toggleSimulator}
                      onGenerate={() => api.generate(selectedId)} />
      </main>
      <footer><span>数据写入 InfluxDB</span><i /> <span>通过 WebSocket 实时推送</span></footer>
    </div>
  )
}
