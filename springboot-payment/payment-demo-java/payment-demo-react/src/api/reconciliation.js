import request from '@/utils/request'

export default {
  execute(data) {
    return request({
      url: '/api/reconciliation/execute',
      method: 'post',
      data
    })
  },

  list(params) {
    return request({
      url: '/api/reconciliation/list',
      method: 'get',
      params
    })
  },

  getById(id) {
    return request({
      url: `/api/reconciliation/${id}`,
      method: 'get'
    })
  },

  listDetails(id, params) {
    return request({
      url: `/api/reconciliation/${id}/details`,
      method: 'get',
      params
    })
  },

  listDiffDetails(id, params) {
    return request({
      url: `/api/reconciliation/${id}/diff`,
      method: 'get',
      params
    })
  },

  exportUrl(id) {
    return `/api/reconciliation/${id}/export`
  }
}
