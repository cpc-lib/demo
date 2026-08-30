import request from '@/utils/request'

export default {
  nativePay(productId, paymentAppId) {
    return request({
      url: `/api/wx-pay/native/${productId}`,
      method: 'post',
      params: { paymentAppId }
    })
  },

  nativePayV2(productId, paymentAppId) {
    return request({
      url: `/api/wx-pay-v2/native/${productId}`,
      method: 'post',
      params: { paymentAppId }
    })
  },

  nativePayOrder(orderNo) {
    return request.post(`/api/wx-pay/native/order/${orderNo}`)
  },

  nativePayV2Order(orderNo) {
    return request.post(`/api/wx-pay-v2/native/order/${orderNo}`)
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
