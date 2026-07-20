import request from '@/utils/request'

export default {
  downloadBillWxPay(billDate, type) {
    return request({
      url: `/api/wx-pay/downloadbill/${billDate}/${type}`,
      method: 'get'
    })
  },

  downloadBillAliPay(billDate, type) {
    return request({
      url: `/api/ali-pay/bill/downloadurl/query/${billDate}/${type}`,
      method: 'get'
    })
  },

  createReconciliationBatch(channelCode, paymentAppId, billDate) {
    return request({
      url: '/api/reconciliation/batch/create',
      method: 'post',
      params: { channelCode, paymentAppId, billDate }
    })
  },

  listReconciliationBatches(channelCode, status, billDate) {
    return request({
      url: '/api/reconciliation/batch/list',
      method: 'get',
      params: { channelCode, status, billDate }
    })
  },

  getReconciliationBatch(batchNo) {
    return request({
      url: `/api/reconciliation/batch/${batchNo}`,
      method: 'get'
    })
  },

  executeReconciliationBatch(batchNo) {
    return request({
      url: `/api/reconciliation/batch/${batchNo}/execute`,
      method: 'post'
    })
  },

  listReconciliationDetails(batchNo, matchStatus) {
    return request({
      url: `/api/reconciliation/detail/list/${batchNo}`,
      method: 'get',
      params: { matchStatus }
    })
  },

  listReconciliationDiscrepancies(batchNo, status) {
    return request({
      url: `/api/reconciliation/discrepancy/list/${batchNo}`,
      method: 'get',
      params: { status }
    })
  },

  resolveDiscrepancy(discrepancyId, resolveRemark) {
    return request({
      url: `/api/reconciliation/discrepancy/${discrepancyId}/resolve`,
      method: 'post',
      params: { resolveRemark }
    })
  }
}
