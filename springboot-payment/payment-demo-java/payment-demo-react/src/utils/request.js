import axios from 'axios'
import { message } from 'antd'
import { API_BASE_URL, getSession, refreshSession } from '@/auth/session'

const service = axios.create({
  baseURL: API_BASE_URL,
  timeout: 20000,
  withCredentials: true
})

service.interceptors.request.use(
  (config) => {
    const token = getSession().accessToken
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

service.interceptors.response.use(
  (response) => {
    const res = response.data

    if (res && typeof res.code === 'number' && res.code < 0) {
      message.error(res.message || '请求失败')
      return Promise.reject(res)
    }

    return res
  },
  async (error) => {
    const originalRequest = error.config || {}
    const shouldRefresh = error?.response?.status === 401 &&
      !originalRequest._retry &&
      !originalRequest.skipAuthRefresh &&
      !String(originalRequest.url || '').includes('/api/auth/')

    if (shouldRefresh) {
      originalRequest._retry = true
      try {
        const session = await refreshSession()
        originalRequest.headers = originalRequest.headers || {}
        originalRequest.headers.Authorization = `Bearer ${session.accessToken}`
        return service(originalRequest)
      } catch (_) {
        message.warning('登录已过期，请重新登录')
        return Promise.reject(error)
      }
    }

    if (!originalRequest.silent) {
      message.error(error?.response?.data?.message || error.message || '网络异常')
    }
    return Promise.reject(error)
  }
)

export default service
