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

  autoFetch(data) {
    return request({
      url: '/api/bill/auto-fetch',
      method: 'post',
      data
    })
  },

  upload(formData) {
    return request({
      url: '/api/bill/upload',
      method: 'post',
      data: formData,
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },

  list(params) {
    return request({
      url: '/api/bill/list',
      method: 'get',
      params
    })
  },

  getById(id) {
    return request({
      url: `/api/bill/${id}`,
      method: 'get'
    })
  },

  listRecords(id, params) {
    return request({
      url: `/api/bill/${id}/records`,
      method: 'get',
      params
    })
  },

  remove(id) {
    return request({
      url: `/api/bill/${id}`,
      method: 'delete'
    })
  }
}
