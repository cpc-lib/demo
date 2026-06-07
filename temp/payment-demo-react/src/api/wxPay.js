import request from '@/utils/request'

export default {
  nativePay(productId) {
    return request({
      url: `/api/wx-pay/native/${productId}`,
      method: 'post'
    })
  },

  nativePayV2(productId) {
    return request({
      url: `/api/wx-pay-v2/native/${productId}`,
      method: 'post'
    })
  },

  cancel(orderNo) {
    return request({
      url: `/api/wx-pay/cancel/${orderNo}`,
      method: 'post'
    })
  },

  refunds(data) {
    return request({
      url: '/api/wx-pay/refunds',
      method: 'post',
      data
    })
  }
}
