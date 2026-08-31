import request from '@/utils/request'

export default {

    //TODO 查询微信对接表单表的订单列表
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

    //TODO 微信对接单子的订单数据
    queryOrderStatus(orderNo) {
        return request({
            url: '/api/order-info/query-order-status/' + orderNo,
            method: 'get'
        })
    },

    checkPaymentStatus(orderNo, channelCode) {
      const rawChannel = String(channelCode || '')
      const normalizedChannel = rawChannel.trim().toUpperCase()
      const isAlipay = normalizedChannel === 'ALIPAY'
        || normalizedChannel.indexOf('ALI') >= 0
        || rawChannel.indexOf('支付宝') >= 0
      const path = isAlipay ? 'ali-pay' : 'wx-pay'
      return request.get(`/api/${path}/check-order-status/${orderNo}`)
    }
}
