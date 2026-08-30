// axios 发送ajax请求
import request from '@/utils/request'

export default {

    //TODO Native下单 拿的时order_info->唯一一条没有支付地本地单子
    nativePay(productId, paymentAppId) {
        return request({
            url: '/api/wx-pay/native/' + productId,
            method: 'post',
            params: { paymentAppId }
        })
    },

    //Native下单(v2)
  nativePayV2(productId, paymentAppId) {
        return request({
            url: '/api/wx-pay-v2/native/' + productId,
            method: 'post',
            params: { paymentAppId }
        })
  },

  nativePayOrder(orderNo) {
    return request.post(`/api/wx-pay/native/order/${orderNo}`)
  },

  nativePayV2Order(orderNo) {
    return request.post(`/api/wx-pay-v2/native/order/${orderNo}`)
  },


    //TODO 微信用户主动去关闭某个订单
    cancel(orderNo) {
        return request({
            url: '/api/wx-pay/cancel/' + orderNo,
            method: 'post'
        })
    },

    refunds(data) {
        return request({
            url: '/api/wx-pay/refunds',
            method: 'post',
            data
        })
    }
}
