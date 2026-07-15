import { useEffect, useRef, useState } from 'react'
import type { SocketMessage, TemperatureReading } from '../types'

type ConnectionState = 'connecting' | 'connected' | 'disconnected'

const socketUrl = () => {
  const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${location.host}/ws/temperature`
}

export function useTemperatureSocket(onReading: (reading: TemperatureReading) => void) {
  const [state, setState] = useState<ConnectionState>('connecting')
  const callbackRef = useRef(onReading)
  callbackRef.current = onReading

  useEffect(() => {
    let socket: WebSocket | undefined
    let connectTimer: number | undefined
    let retryTimer: number | undefined
    let disposed = false

    const connect = () => {
      if (disposed) return
      setState('connecting')
      socket = new WebSocket(socketUrl())
      socket.onopen = () => setState('connected')
      socket.onmessage = (event) => {
        const message = JSON.parse(event.data) as SocketMessage<TemperatureReading>
        if (message.type === 'temperature') callbackRef.current(message.data)
      }
      socket.onerror = () => socket?.close()
      socket.onclose = () => {
        setState('disconnected')
        if (!disposed) retryTimer = window.setTimeout(connect, 3000)
      }
    }

    connectTimer = window.setTimeout(connect, 0)
    return () => {
      disposed = true
      if (connectTimer) clearTimeout(connectTimer)
      if (retryTimer) clearTimeout(retryTimer)
      socket?.close()
    }
  }, [])

  return state
}
