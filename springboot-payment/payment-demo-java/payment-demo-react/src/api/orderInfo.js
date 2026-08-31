import request from '@/utils/request'

export default {
  list() {
    return request({
      url: '/api/order-info/list',
      method: 'get'
    })
  },

  myList() {
    return request.get('/api/order-info/my-list')
  },

  checkout(paymentAppId, checkoutRequestId) {
    return request.post('/api/order-info/checkout', { paymentAppId, checkoutRequestId })
  },

  items(orderNo) {
    return request.get(`/api/order-info/${orderNo}/items`)
  },

  queryOrderStatus(orderNo) {
    return request({
      url: `/api/order-info/query-order-status/${orderNo}`,
      method: 'get'
    })
  },

  checkPaymentStatus(orderNo, channelCode) {
    const normalizedChannel = String(channelCode || '').trim().toUpperCase()
    const isAlipay = normalizedChannel === 'ALIPAY'
      || normalizedChannel.includes('ALI')
      || String(channelCode || '').includes('支付宝')
    const path = isAlipay ? 'ali-pay' : 'wx-pay'

    return request.get(`/api/${path}/check-order-status/${orderNo}`)
  }
}
