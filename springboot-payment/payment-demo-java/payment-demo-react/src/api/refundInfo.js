import request from '@/utils/request'

export default {
  list() {
    return request({
      url: '/api/refund-info/list',
      method: 'get'
    })
  },

  listByOrderNo(orderNo) {
    return request({
      url: `/api/refund-info/list/${orderNo}`,
      method: 'get'
    })
  },

  approve(refundNo, approveRemark) {
    return request({
      url: `/api/refund-info/approve/${refundNo}`,
      method: 'post',
      data: { approveRemark }
    })
  },

  reject(refundNo, approveRemark) {
    return request({
      url: `/api/refund-info/reject/${refundNo}`,
      method: 'post',
      data: { approveRemark }
    })
  },

  query(refundNo) {
    return request.post(`/api/refund-info/query/${refundNo}`)
  },

  reconcile(orderNo) {
    return request.post(`/api/refund-info/reconcile/${orderNo}`)
  }
}
