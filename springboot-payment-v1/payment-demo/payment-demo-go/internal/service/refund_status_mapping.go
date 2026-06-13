package service

import "payment-demo-go/internal/constant"

func mapAliPayRefundStatus(channelStatus string) string {
	if channelStatus == constant.AliPayRefundSuccess {
		return constant.RefundStatusSuccess
	}
	return ""
}

func mapWxRefundStatus(status string) string {
	switch status {
	case constant.WxRefundStatusSuccess:
		return constant.RefundStatusSuccess
	case constant.WxRefundStatusProcessing:
		return constant.RefundStatusProcessing
	case constant.WxRefundStatusAbnormal, "CHANGE":
		return constant.RefundStatusAbnormal
	case constant.WxRefundStatusClosed, "REFUNDCLOSE":
		return constant.RefundStatusClosed
	default:
		return ""
	}
}
