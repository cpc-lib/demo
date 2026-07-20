import request from '@/utils/request'

export function createBatch(data) {
  return request({
    url: '/api/reconciliation/batch/create',
    method: 'post',
    data
  })
}

export function getBatchList(params) {
  return request({
    url: '/api/reconciliation/batch/list',
    method: 'get',
    params
  })
}

export function getBatchDetail(batchNo) {
  return request({
    url: `/api/reconciliation/batch/${batchNo}`,
    method: 'get'
  })
}

export function executeBatch(batchNo) {
  return request({
    url: `/api/reconciliation/batch/${batchNo}/execute`,
    method: 'post'
  })
}

export function getProgress(batchNo) {
  return request({
    url: `/api/reconciliation/batch/${batchNo}/progress`,
    method: 'get'
  })
}

export function getSummary() {
  return request({
    url: '/api/reconciliation/summary',
    method: 'get'
  })
}

export function getDetailList(batchNo, params) {
  return request({
    url: `/api/reconciliation/detail/list/${batchNo}`,
    method: 'get',
    params
  })
}

export function getDiscrepancyList(batchNo, params) {
  return request({
    url: `/api/reconciliation/discrepancy/list/${batchNo}`,
    method: 'get',
    params
  })
}

export function resolveDiscrepancy(id, data) {
  return request({
    url: `/api/reconciliation/discrepancy/${id}/resolve`,
    method: 'post',
    data
  })
}
