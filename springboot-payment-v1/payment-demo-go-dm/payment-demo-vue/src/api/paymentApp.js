import request from '@/utils/request'

export default {
  list(params) {
    return request({
      url: '/api/payment-app/list',
      method: 'get',
      params
    })
  },

  save(data) {
    return request({
      url: '/api/payment-app/save',
      method: 'post',
      data
    })
  },

  update(appCode, data) {
    return request({
      url: '/api/payment-app/update/' + appCode,
      method: 'post',
      data
    })
  },

  delete(appCode) {
    return request({
      url: '/api/payment-app/delete/' + appCode,
      method: 'post'
    })
  }
}
