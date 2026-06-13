import request from '@/utils/request'

export default {
  nativePay(productId, paymentAppId) {
    return request({
      url: `/api/wx-pay/native/${productId}`,
      method: 'post',
      params: paymentAppId ? { paymentAppId } : undefined
    })
  },

  nativePayV2(productId, paymentAppId) {
    return request({
      url: `/api/wx-pay-v2/native/${productId}`,
      method: 'post',
      params: paymentAppId ? { paymentAppId } : undefined
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
