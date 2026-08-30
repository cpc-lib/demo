import axios from 'axios'
import { Message } from 'element-ui'
import { API_BASE_URL, authState, refreshSession } from '../auth/session'

// 创建axios实例
const service = axios.create({
  baseURL: API_BASE_URL,
  timeout: 20000,
  withCredentials: true
})

// request拦截器
service.interceptors.request.use(
  config => {
    if (authState.accessToken) {
      config.headers.Authorization = `Bearer ${authState.accessToken}`
    }
    return config
  },
  error => {
    // Do something with request error
    Promise.reject(error)
  }
)

// response 拦截器
service.interceptors.response.use(
  response => {
 
    const res = response.data
    if (res.code < 0) {
      Message({
        message: res.message,
        type: 'error',
        duration: 5 * 1000
      })

      return Promise.reject('error')
    } else {
      return response.data
    }
  },
  async error => {
    const originalRequest = error.config || {}
    const shouldRefresh = error.response && error.response.status === 401 &&
      !originalRequest._retry &&
      !originalRequest.skipAuthRefresh &&
      String(originalRequest.url || '').indexOf('/api/auth/') < 0

    if (shouldRefresh) {
      originalRequest._retry = true
      try {
        const session = await refreshSession()
        originalRequest.headers = originalRequest.headers || {}
        originalRequest.headers.Authorization = `Bearer ${session.accessToken}`
        return service(originalRequest)
      } catch (_) {
        Message.warning('登录已过期，请重新登录')
        return Promise.reject(error)
      }
    }
    Message({
      message: (error.response && error.response.data && error.response.data.message) || error.message,
      type: 'error',
      duration: 5 * 1000
    })
    return Promise.reject(error)
  }
)

export default service
