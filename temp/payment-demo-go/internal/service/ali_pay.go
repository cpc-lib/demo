package service

import (
	"context"
	"encoding/json"
	"fmt"
	"strings"
	"time"

	"payment-demo-go/internal/constant"
	"payment-demo-go/internal/model"
	"payment-demo-go/internal/pay"
	"payment-demo-go/internal/util"

	"gorm.io/gorm"
)

const (
	aliRespTradeQuery  = "alipay_trade_query_response"
	aliRespTradeRefund = "alipay_trade_refund_response"
	aliRespRefundQuery = "alipay_trade_fastpay_refund_query_response"
	aliRespBillQuery   = "alipay_data_dataservice_bill_downloadurl_query_response"
)

func (s *Service) AliTradeCreate(ctx context.Context, productID int64) (string, error) {
	v, err := s.Lock.ExecuteValue(ctx, fmt.Sprintf("payment:ali:pagepay:%d", productID), 3*time.Second, 15*time.Second, func() (interface{}, error) {
		order, err := s.CreateOrReuseOrder(ctx, productID, constant.PayTypeAliPay)
		if err != nil {
			return nil, err
		}
		biz := map[string]interface{}{
			"out_trade_no": order.OrderNo,
			"total_amount": util.CentsToYuan(order.TotalFee),
			"subject":      order.Title,
			"product_code": "FAST_INSTANT_TRADE_PAY",
		}
		form, err := s.AliPay.PagePayForm(util.ToJSON(biz))
		if err != nil {
			return nil, util.Biz("创建支付宝支付交易失败: " + err.Error())
		}
		return form, nil
	})
	if err != nil {
		return "", err
	}
	return v.(string), nil
}

func (s *Service) AliProcessOrder(ctx context.Context, params map[string]string) error {
	orderNo := strings.TrimSpace(params["out_trade_no"])
	if orderNo == "" {
		return util.Biz("支付宝支付通知缺少商户订单号")
	}
	notifyID := params["notify_id"]
	return s.Lock.Execute(ctx, "payment:ali:notify:pay:"+orderNo, 5*time.Second, 30*time.Second, func() error {
		return s.DB.Transaction(func(tx *gorm.DB) error {
			order, err := s.getOrderByOrderNoForUpdateDB(tx, orderNo)
			if err != nil {
				return err
			}
			if order == nil {
				return util.Biz("支付宝支付通知对应订单不存在，orderNo=" + orderNo)
			}
			if err := validateAliPayOrderNotify(order, params); err != nil {
				return err
			}
			if order.OrderStatus != constant.OrderStatusNotPay {
				return nil
			}
			updated, err := s.updateOrderStatusIfDB(tx, orderNo, constant.OrderStatusNotPay, constant.OrderStatusSuccess)
			if err != nil || !updated {
				return err
			}
			_ = notifyID
			return s.CreatePaymentInfoAliPay(params)
		})
	})
}

func validateAliPayOrderNotify(order *model.OrderInfo, params map[string]string) error {
	if order.PaymentType != constant.PayTypeAliPay {
		return util.Biz("支付宝支付通知支付类型不匹配，orderNo=" + order.OrderNo)
	}
	totalAmount := strings.TrimSpace(params["total_amount"])
	if totalAmount == "" {
		return nil
	}
	notifyTotal, err := util.YuanToCents(totalAmount)
	if err != nil {
		return util.Biz("支付宝支付通知金额格式错误，orderNo=" + order.OrderNo)
	}
	if notifyTotal != order.TotalFee {
		return util.Biz("支付宝支付通知金额与订单金额不一致，orderNo=" + order.OrderNo)
	}
	return nil
}

func (s *Service) AliCancelOrder(ctx context.Context, orderNo string) error {
	status, err := s.GetOrderStatus(orderNo)
	if err != nil {
		return err
	}
	if status != constant.OrderStatusNotPay {
		return nil
	}
	if err := s.aliCloseOrder(ctx, orderNo); err != nil {
		return err
	}
	_, err = s.UpdateOrderStatusIf(orderNo, constant.OrderStatusNotPay, constant.OrderStatusCancel)
	return err
}

func (s *Service) AliQueryOrder(ctx context.Context, orderNo string) (string, error) {
	biz := util.ToJSON(map[string]interface{}{"out_trade_no": orderNo})
	body, err := s.AliPay.Execute(ctx, "alipay.trade.query", biz)
	if err != nil {
		return "", util.Biz("查单接口调用失败: " + err.Error())
	}
	if !pay.AliPaySuccess(body, aliRespTradeQuery) {
		return "", nil
	}
	return body, nil
}

func (s *Service) AliCheckOrderStatus(ctx context.Context, orderNo string) error {
	result, err := s.AliQueryOrder(ctx, orderNo)
	if err != nil {
		return err
	}
	if strings.TrimSpace(result) == "" {
		_, err := s.UpdateOrderStatusIf(orderNo, constant.OrderStatusNotPay, constant.OrderStatusClosed)
		return err
	}
	m := pay.AliPayResponseMap(result, aliRespTradeQuery)
	if m == nil {
		return nil
	}
	tradeStatus := str(m["trade_status"])
	return s.Lock.Execute(ctx, "payment:ali:check:order:"+orderNo, 5*time.Second, 30*time.Second, func() error {
		return s.DB.Transaction(func(tx *gorm.DB) error {
			order, err := s.getOrderByOrderNoForUpdateDB(tx, orderNo)
			if err != nil || order == nil {
				return err
			}
			if order.OrderStatus != constant.OrderStatusNotPay {
				return nil
			}
			switch tradeStatus {
			case constant.AliPayTradeNotPay:
				if err := s.aliCloseOrder(ctx, orderNo); err != nil {
					return err
				}
				_, err := s.updateOrderStatusIfDB(tx, orderNo, constant.OrderStatusNotPay, constant.OrderStatusClosed)
				return err
			case constant.AliPayTradeSuccess:
				params := mapInterfaceToStringMap(m)
				if params["out_trade_no"] == "" {
					params["out_trade_no"] = orderNo
				}
				if err := validateAliPayOrderNotify(order, params); err != nil {
					return err
				}
				updated, err := s.updateOrderStatusIfDB(tx, orderNo, constant.OrderStatusNotPay, constant.OrderStatusSuccess)
				if err != nil || !updated {
					return err
				}
				return s.CreatePaymentInfoAliPay(params)
			}
			return nil
		})
	})
}

func (s *Service) aliCloseOrder(ctx context.Context, orderNo string) error {
	_, err := s.AliPay.Execute(ctx, "alipay.trade.close", util.ToJSON(map[string]interface{}{"out_trade_no": orderNo}))
	if err != nil {
		return util.Biz("关单接口调用失败: " + err.Error())
	}
	return nil
}

func (s *Service) AliExecuteRefund(ctx context.Context, refund *model.RefundInfo) error {
	if refund == nil {
		return util.Biz("退款申请单不能为空")
	}
	biz := map[string]interface{}{
		"out_trade_no":   refund.OrderNo,
		"out_request_no": refund.RefundNo,
		"refund_amount":  util.CentsToYuan(refund.Refund),
		"refund_reason":  refund.Reason,
	}
	body, err := s.AliPay.Execute(ctx, "alipay.trade.refund", util.ToJSON(biz))
	if err != nil {
		_ = s.UpdateRefundToFailed(refund.RefundNo, err.Error())
		return util.Biz("创建支付宝退款申请失败: " + err.Error())
	}
	m := pay.AliPayResponseMap(body, aliRespTradeRefund)
	if pay.AliPaySuccess(body, aliRespTradeRefund) {
		return s.UpdateRefundToSuccess(refund.RefundNo, str(m["trade_no"]), body)
	}
	_ = s.UpdateRefundToFailed(refund.RefundNo, body)
	return util.Biz("创建支付宝退款申请失败：" + firstNonBlank(str(m["sub_msg"]), str(m["msg"])))
}

func (s *Service) AliQueryRefund(ctx context.Context, refundNo string) (string, error) {
	refund, err := s.GetRefundByRefundNo(refundNo)
	if err != nil {
		return "", err
	}
	if refund == nil {
		return "", util.Biz("退款单不存在")
	}
	body, err := s.executeAliRefundQuery(ctx, refund)
	if err != nil {
		return "", err
	}
	if !pay.AliPaySuccess(body, aliRespRefundQuery) {
		return "", nil
	}
	return body, nil
}

func (s *Service) AliQueryRefundStatusForSync(ctx context.Context, refundNo string) (model.RefundStatusSyncResult, error) {
	refund, err := s.GetRefundByRefundNo(refundNo)
	if err != nil {
		return model.RefundStatusSyncResult{}, err
	}
	if refund == nil {
		return model.RefundStatusSyncResult{}, util.Biz("退款单不存在")
	}
	body, err := s.executeAliRefundQuery(ctx, refund)
	if err != nil {
		return model.RefundStatusSyncResult{}, err
	}
	if !pay.AliPaySuccess(body, aliRespRefundQuery) {
		return model.RefundStatusSyncResult{}, util.Biz("支付宝退款查询无结果")
	}
	m := pay.AliPayResponseMap(body, aliRespRefundQuery)
	channelStatus := str(m["refund_status"])
	return model.RefundStatusSyncResult{
		OrderNo:       firstNonBlank(str(m["out_trade_no"]), refund.OrderNo),
		RefundNo:      firstNonBlank(str(m["out_request_no"]), refundNo),
		RefundID:      str(m["trade_no"]),
		ChannelStatus: channelStatus,
		RefundStatus:  mapAliPayRefundStatus(channelStatus),
		Content:       body,
		TotalFee:      parseYuanToCentsPtr(str(m["total_amount"])),
		RefundAmount:  parseYuanToCentsPtr(str(m["refund_amount"])),
	}, nil
}

func (s *Service) executeAliRefundQuery(ctx context.Context, refund *model.RefundInfo) (string, error) {
	biz := map[string]interface{}{"out_trade_no": refund.OrderNo, "out_request_no": refund.RefundNo}
	body, err := s.AliPay.Execute(ctx, "alipay.trade.fastpay.refund.query", util.ToJSON(biz))
	if err != nil {
		return "", util.Biz("退款查询接口调用失败: " + err.Error())
	}
	return body, nil
}

func (s *Service) AliQueryBill(ctx context.Context, billDate, typ string) (string, error) {
	biz := map[string]interface{}{"bill_type": typ, "bill_date": billDate}
	body, err := s.AliPay.Execute(ctx, "alipay.data.dataservice.bill.downloadurl.query", util.ToJSON(biz))
	if err != nil {
		return "", util.Biz("申请支付宝账单失败: " + err.Error())
	}
	if !pay.AliPaySuccess(body, aliRespBillQuery) {
		return "", util.Biz("申请支付宝账单失败")
	}
	m := pay.AliPayResponseMap(body, aliRespBillQuery)
	downloadURL := str(m["bill_download_url"])
	if strings.TrimSpace(downloadURL) == "" {
		return "", util.Biz("申请支付宝账单失败")
	}
	return downloadURL, nil
}

func mapAliPayRefundStatus(channelStatus string) string {
	if channelStatus == constant.AliPayRefundSuccess {
		return constant.RefundStatusSuccess
	}
	return ""
}

func firstNonBlank(primary, fallback string) string {
	if strings.TrimSpace(primary) == "" {
		return fallback
	}
	return primary
}

func parseYuanToCentsPtr(amount string) *int {
	if strings.TrimSpace(amount) == "" {
		return nil
	}
	cents, err := util.YuanToCents(amount)
	if err != nil {
		return nil
	}
	return &cents
}

func mapInterfaceToStringMap(m map[string]interface{}) map[string]string {
	out := make(map[string]string, len(m))
	for k, v := range m {
		if v == nil {
			continue
		}
		switch x := v.(type) {
		case json.Number:
			out[k] = x.String()
		default:
			out[k] = fmt.Sprint(x)
		}
	}
	return out
}
