import request from '@/utils/request'

export default {
  list() {
    return request({
      url: '/api/payment-channel/list',
      method: 'get'
    })
  },

  save(data) {
    return request({
      url: '/api/payment-channel/save',
      method: 'post',
      data
    })
  },

  update(channelCode, data) {
    return request({
      url: '/api/payment-channel/update/' + channelCode,
      method: 'post',
      data
    })
  },

  delete(channelCode) {
    return request({
      url: '/api/payment-channel/delete/' + channelCode,
      method: 'post'
    })
  }
}
