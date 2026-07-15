export type Sensor = {
  id: string
  name: string
  location: string
  baseline: number
  enabled: boolean
}

export type TemperatureReading = {
  sensorId: string
  sensorName: string
  location: string
  value: number
  status: 'NORMAL' | 'HIGH' | 'LOW'
  timestamp: string
}

export type SocketMessage<T> = {
  type: string
  data: T
  sentAt: string
}

export type SystemStatus = {
  simulatorRunning: boolean
  webSocketConnections: number
  bucket: string
}
