// axios 发送ajax请求
import request from '@/utils/request'

export default {

    //TODO Native下单 拿的时order_info->唯一一条没有支付地本地单子
    nativePay(productId) {
        return request({
            url: '/api/wx-pay/native/' + productId,
            method: 'post'
        })
    },

    //Native下单(v2)
    nativePayV2(productId) {
        return request({
            url: '/api/wx-pay-v2/native/' + productId,
            method: 'post'
        })
    },


    //TODO 微信用户主动去关闭某个订单
    cancel(orderNo) {
        return request({
            url: '/api/wx-pay/cancel/' + orderNo,
            method: 'post'
        })
    },

    refunds(orderNo, reason) {
        return request({
            url: '/api/wx-pay/refunds/' + orderNo + '/' + reason,
            method: 'post'
        })
    }
}
