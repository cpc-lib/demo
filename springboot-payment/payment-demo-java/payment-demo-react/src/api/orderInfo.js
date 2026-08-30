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
  }
}
