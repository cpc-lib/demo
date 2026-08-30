import Vue from 'vue'
import axios from 'axios'

export const API_BASE_URL = 'http://localhost:8080'

export const authState = Vue.observable({
  accessToken: null,
  user: null,
  bootstrapped: false,
  cartCount: 0
})

let refreshPromise = null

const authClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 20000,
  withCredentials: true
})

export function setSession(session) {
  authState.accessToken = session && session.accessToken ? session.accessToken : null
  authState.user = session && session.user ? session.user : null
  authState.bootstrapped = true
}

export function clearSession() {
  setSession(null)
  authState.cartCount = 0
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
