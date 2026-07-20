import request from '@/utils/request'

export default {
  createTask(data) {
    return request({
      url: '/api/reconciliation/task',
      method: 'post',
      data
    })
  },

  listTasks(params) {
    return request({
      url: '/api/reconciliation/task/list',
      method: 'get',
      params
    })
  },

  getTask(taskId) {
    return request({
      url: `/api/reconciliation/task/${taskId}`,
      method: 'get'
    })
  },

  executeTask(taskId) {
    return request({
      url: `/api/reconciliation/task/${taskId}/execute`,
      method: 'post'
    })
  },

  listDiffs(taskId, params) {
    return request({
      url: `/api/reconciliation/task/${taskId}/diff`,
      method: 'get',
      params
    })
  },

  handleDiff(diffId, data) {
    return request({
      url: `/api/reconciliation/diff/${diffId}/handle`,
      method: 'post',
      data
    })
  },

  getSummary(params) {
    return request({
      url: '/api/reconciliation/summary',
      method: 'get',
      params
    })
  }
}
