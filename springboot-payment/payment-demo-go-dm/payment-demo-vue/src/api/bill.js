import request from '@/utils/request'

export default{

  downloadBillWxPay(billDate, type, paymentAppId) {
    return request({
      url: '/api/wx-pay/downloadbill/' + billDate + '/' + type,
      method: 'get',
      params: { paymentAppId }
    })
  },

  downloadBillAliPay(billDate, type, paymentAppId) {
    return request({
      url: '/api/ali-pay/bill/downloadurl/query/' + billDate + '/' + type,
      method: 'get',
      params: { paymentAppId }
    })
  }
}
