import request from '@/utils/request'

export default {

  createBatch(channelCode, paymentAppId, billDate) {
    return request({
      url: '/api/reconciliation/batch/create',
      method: 'post',
      params: { channelCode, paymentAppId, billDate }
    })
  },

  listBatches(channelCode, status, billDateStart, billDateEnd) {
    return request({
      url: '/api/reconciliation/batch/list',
      method: 'get',
      params: { channelCode, status, billDateStart, billDateEnd }
    })
  },

  getBatch(batchNo) {
    return request({
      url: '/api/reconciliation/batch/' + batchNo,
      method: 'get'
    })
  },

  executeBatch(batchNo) {
    return request({
      url: '/api/reconciliation/batch/' + batchNo + '/execute',
      method: 'post'
    })
  },

  getBatchProgress(batchNo) {
    return request({
      url: '/api/reconciliation/batch/' + batchNo + '/progress',
      method: 'get'
    })
  },

  getSummary() {
    return request({
      url: '/api/reconciliation/summary',
      method: 'get'
    })
  },

  listDetails(batchNo, matchStatus, pageNum, pageSize) {
    return request({
      url: '/api/reconciliation/detail/list/' + batchNo,
      method: 'get',
      params: { matchStatus, pageNum, pageSize }
    })
  },

  listDiscrepancies(batchNo, status, pageNum, pageSize) {
    return request({
      url: '/api/reconciliation/discrepancy/list/' + batchNo,
      method: 'get',
      params: { status, pageNum, pageSize }
    })
  },

  resolveDiscrepancy(discrepancyId, resolveRemark) {
    return request({
      url: '/api/reconciliation/discrepancy/' + discrepancyId + '/resolve',
      method: 'post',
      params: { resolveRemark }
    })
  }
}
