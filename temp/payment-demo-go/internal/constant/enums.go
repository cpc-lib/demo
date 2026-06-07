package constant

const (
	OrderStatusNotPay           = "未支付"
	OrderStatusSuccess          = "支付成功"
	OrderStatusClosed           = "超时已关闭"
	OrderStatusCancel           = "用户已取消"
	OrderStatusRefundProcessing = "退款中"
	OrderStatusPartialRefund    = "部分退款"
	OrderStatusRefundSuccess    = "已退款"
	OrderStatusRefundAbnormal   = "退款异常"
)

const (
	PayTypeWxPay  = "微信"
	PayTypeAliPay = "支付宝"
)

const (
	RefundApprovalPending  = "PENDING"
	RefundApprovalApproved = "APPROVED"
	RefundApprovalRejected = "REJECTED"
)

const (
	RefundStatusCreated    = "CREATED"
	RefundStatusProcessing = "PROCESSING"
	RefundStatusSuccess    = "SUCCESS"
	RefundStatusFailed     = "FAILED"
	RefundStatusClosed     = "CLOSED"
	RefundStatusAbnormal   = "ABNORMAL"
)

const (
	WxTradeStateSuccess = "SUCCESS"
	WxTradeStateNotPay  = "NOTPAY"
	WxTradeStateClosed  = "CLOSED"
	WxTradeStateRefund  = "REFUND"
)

const (
	WxRefundStatusSuccess    = "SUCCESS"
	WxRefundStatusClosed     = "CLOSED"
	WxRefundStatusProcessing = "PROCESSING"
	WxRefundStatusAbnormal   = "ABNORMAL"
)

const (
	AliPayTradeSuccess  = "TRADE_SUCCESS"
	AliPayTradeNotPay   = "WAIT_BUYER_PAY"
	AliPayTradeClosed   = "TRADE_CLOSED"
	AliPayRefundSuccess = "REFUND_SUCCESS"
	AliPayRefundError   = "REFUND_ERROR"
)

const (
	WxAPINativePay           = "/v3/pay/transactions/native"
	WxAPINativePayV2         = "/pay/unifiedorder"
	WxAPIRefundQueryV2       = "/pay/refundquery"
	WxAPIJSAPIPay            = "/v3/pay/transactions/jsapi"
	WxAPIOrderQueryByNo      = "/v3/pay/transactions/out-trade-no/%s"
	WxAPICloseOrderByNo      = "/v3/pay/transactions/out-trade-no/%s/close"
	WxAPIDomesticRefunds     = "/v3/refund/domestic/refunds"
	WxAPIDomesticRefundQuery = "/v3/refund/domestic/refunds/%s"
	WxAPITradeBills          = "/v3/bill/tradebill"
	WxAPIFundFlowBills       = "/v3/bill/fundflowbill"
	WxAPICertificates        = "/v3/certificates"
)

const (
	WxNotifyNative   = "/api/wx-pay/native/notify"
	WxNotifyNativeV2 = "/api/wx-pay-v2/native/notify"
	WxNotifyRefund   = "/api/wx-pay/refunds/notify"
)
