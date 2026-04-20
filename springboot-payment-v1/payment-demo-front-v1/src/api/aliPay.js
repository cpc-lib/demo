// axios 发送ajax请求
import request from '@/utils/request'

export default {

    //发起支付请求
    tradePagePay(productId) {
        return request({
            url: '/api/ali-pay/trade/page/pay/' + productId, method: 'post'
        })
    },

    cancel(orderNo) {
        return request({
            url: '/api/ali-pay/trade/close/' + orderNo, method: 'post'
        })
    },

    refunds(data) {
        return request({
            url: '/api/ali-pay/trade/refund', method: 'post', data
        })
    }
}
