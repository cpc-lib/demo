import request from '@/utils/request'

export default {
  apps() {
    return request({
      url: '/api/payment-config/apps',
      method: 'get'
    })
  },

  reload() {
    return request({
      url: '/api/payment-config/reload',
      method: 'post'
    })
  }
}
