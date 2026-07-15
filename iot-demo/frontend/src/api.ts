import type { Sensor, SystemStatus, TemperatureReading } from './types'

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, init)
  if (!response.ok) {
    const detail = await response.text()
    throw new Error(detail || `请求失败: ${response.status}`)
  }
  return response.json() as Promise<T>
}

export const api = {
  sensors: () => request<Sensor[]>('/api/sensors'),
  latest: () => request<TemperatureReading[]>('/api/temperature/latest'),
  history: (sensorId: string, hours: number) =>
    request<TemperatureReading[]>(`/api/temperature/history?sensorId=${encodeURIComponent(sensorId)}&hours=${hours}`),
  status: () => request<SystemStatus>('/api/system/status'),
  simulator: (action: 'start' | 'stop') =>
    request<{ running: boolean }>(`/api/simulator/${action}`, { method: 'POST' }),
  generate: (sensorId: string) =>
    request<TemperatureReading>(`/api/simulator/generate/${encodeURIComponent(sensorId)}`, { method: 'POST' }),
}
