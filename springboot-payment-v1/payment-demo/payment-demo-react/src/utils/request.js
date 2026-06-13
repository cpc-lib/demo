import axios from 'axios'
import { message } from 'antd'

const service = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 20000
})

service.interceptors.request.use(
  (config) => config,
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
  (error) => {
    message.error(error?.response?.data?.message || error.message || '网络异常')
    return Promise.reject(error)
  }
)

export default service
