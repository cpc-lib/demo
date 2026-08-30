import axios from 'axios'

const API_BASE_URL = 'http://localhost:8080'

let snapshot = {
  accessToken: null,
  user: null,
  bootstrapped: false
}
let refreshPromise = null
const listeners = new Set()

const authClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 20000,
  withCredentials: true
})

function emit() {
  listeners.forEach((listener) => listener(snapshot))
}

export function getSession() {
  return snapshot
}

export function setSession(session, bootstrapped = true) {
  snapshot = {
    accessToken: session?.accessToken || null,
    user: session?.user || null,
    bootstrapped
  }
  emit()
}

export function clearSession(bootstrapped = true) {
  setSession(null, bootstrapped)
}

export function subscribeSession(listener) {
  listeners.add(listener)
  return () => listeners.delete(listener)
}

export function refreshSession() {
  if (!refreshPromise) {
    refreshPromise = authClient.post('/api/auth/refresh')
      .then(({ data }) => {
        if (!data || data.code < 0) {
          throw data || new Error('刷新会话失败')
        }
        setSession(data.data)
        return data.data
      })
      .catch((error) => {
        clearSession()
        throw error
      })
      .finally(() => {
        refreshPromise = null
      })
  }
  return refreshPromise
}

export { API_BASE_URL, authClient }
