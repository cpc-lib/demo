package service

import (
	"context"
	"encoding/json"
	"fmt"
	"net/url"
	"strings"
	"time"

	"payment-demo-go/internal/config"
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
	return s.AliTradeCreateWithApp(ctx, productID, 0)
}

func (s *Service) AliTradeCreateWithApp(ctx context.Context, productID int64, paymentAppID int64) (string, error) {
	cfg, resolvedPaymentAppID, err := s.aliPayConfigForApp(ctx, paymentAppID)
	if err != nil {
		return "", err
	}
	aliClient, err := s.aliClientForConfig(cfg, resolvedPaymentAppID)
	if err != nil {
		return "", err
	}
	if aliClient == nil {
		return "", util.Biz("alipay client is not configured")
	}
	lockKey := fmt.Sprintf("payment:ali:pagepay:%d", productID)
	if resolvedPaymentAppID > 0 {
		lockKey += ":" + i64(resolvedPaymentAppID)
	}
	v, err := s.Lock.ExecuteValue(ctx, lockKey, 3*time.Second, 15*time.Second, func() (interface{}, error) {
		order, err := s.CreateOrReuseOrderWithApp(ctx, productID, constant.PayTypeAliPay, resolvedPaymentAppID)
		if err != nil {
			return nil, err
		}
		biz := map[string]interface{}{
			"out_trade_no": order.OrderNo,
			"total_amount": util.CentsToYuan(order.TotalFee),
			"subject":      order.Title,
			"product_code": "FAST_INSTANT_TRADE_PAY",
		}
		form, err := aliClient.PagePayForm(util.ToJSON(biz))
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
	return s.AliProcessOrderWithApp(ctx, params, 0)
}

func (s *Service) AliProcessOrderWithApp(ctx context.Context, params map[string]string, paymentAppID int64) error {
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
			if err := validateOrderPaymentAppBinding(order, paymentAppID, "支付宝支付通知"); err != nil {
				return err
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

func (s *Service) VerifyAliNotifyApp(ctx context.Context, params url.Values) (config.AliPayConfig, int64, error) {
	cfg, paymentAppID, err := s.aliPayConfigByProvider(ctx, params.Get("app_id"), params.Get("seller_id"))
	if err != nil {
		return config.AliPayConfig{}, 0, err
	}
	aliClient, err := s.aliClientForConfig(cfg, paymentAppID)
	if err != nil {
		return config.AliPayConfig{}, 0, err
	}
	if aliClient == nil || !aliClient.VerifyNotify(params) {
		return config.AliPayConfig{}, 0, util.Biz("支付宝通知验签失败")
	}
	return cfg, paymentAppID, nil
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
	order, err := s.GetOrderByOrderNo(orderNo)
	if err != nil {
		return err
	}
	if order == nil {
		return util.Biz("订单不存在，orderNo=" + orderNo)
	}
	if order.OrderStatus != constant.OrderStatusNotPay {
		return nil
	}
	if err := s.aliCloseOrderForOrder(ctx, order); err != nil {
		return err
	}
	_, err = s.UpdateOrderStatusIf(orderNo, constant.OrderStatusNotPay, constant.OrderStatusCancel)
	return err
}

func (s *Service) AliQueryOrder(ctx context.Context, orderNo string) (string, error) {
	order, err := s.GetOrderByOrderNo(orderNo)
	if err != nil {
		return "", err
	}
	if order == nil {
		return "", util.Biz("订单不存在，orderNo=" + orderNo)
	}
	return s.aliQueryOrderForOrder(ctx, order)
}

func (s *Service) aliQueryOrderForOrder(ctx context.Context, order *model.OrderInfo) (string, error) {
	aliClient, _, _, err := s.aliClientForOrder(ctx, order)
	if err != nil {
		return "", err
	}
	if aliClient == nil {
		return "", util.Biz("alipay client is not configured")
	}
	biz := util.ToJSON(map[string]interface{}{"out_trade_no": order.OrderNo})
	body, err := aliClient.Execute(ctx, "alipay.trade.query", biz)
	if err != nil {
		return "", util.Biz("查单接口调用失败: " + err.Error())
	}
	if !pay.AliPaySuccess(body, aliRespTradeQuery) {
		return "", nil
	}
	return body, nil
}

func (s *Service) AliCheckOrderStatus(ctx context.Context, orderNo string) error {
	order, err := s.GetOrderByOrderNo(orderNo)
	if err != nil {
		return err
	}
	if order == nil {
		return util.Biz("订单不存在，orderNo=" + orderNo)
	}
	result, err := s.aliQueryOrderForOrder(ctx, order)
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
				if err := s.aliCloseOrderForOrder(ctx, order); err != nil {
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
	order, err := s.GetOrderByOrderNo(orderNo)
	if err != nil {
		return err
	}
	if order == nil {
		return util.Biz("订单不存在，orderNo=" + orderNo)
	}
	return s.aliCloseOrderForOrder(ctx, order)
}

func (s *Service) aliCloseOrderForOrder(ctx context.Context, order *model.OrderInfo) error {
	aliClient, _, _, err := s.aliClientForOrder(ctx, order)
	if err != nil {
		return err
	}
	if aliClient == nil {
		return util.Biz("alipay client is not configured")
	}
	_, err = aliClient.Execute(ctx, "alipay.trade.close", util.ToJSON(map[string]interface{}{"out_trade_no": order.OrderNo}))
	if err != nil {
		return util.Biz("关单接口调用失败: " + err.Error())
	}
	return nil
}

func (s *Service) AliExecuteRefund(ctx context.Context, refund *model.RefundInfo) error {
	if refund == nil {
		return util.Biz("退款申请单不能为空")
	}
	order, err := s.GetOrderByOrderNo(refund.OrderNo)
	if err != nil {
		return err
	}
	if order == nil {
		return util.Biz("退款对应订单不存在，orderNo=" + refund.OrderNo)
	}
	aliClient, _, _, err := s.aliClientForOrder(ctx, order)
	if err != nil {
		return err
	}
	if aliClient == nil {
		return util.Biz("alipay client is not configured")
	}
	biz := map[string]interface{}{
		"out_trade_no":   refund.OrderNo,
		"out_request_no": refund.RefundNo,
		"refund_amount":  util.CentsToYuan(refund.Refund),
		"refund_reason":  refund.Reason,
	}
	body, err := aliClient.Execute(ctx, "alipay.trade.refund", util.ToJSON(biz))
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
	order, err := s.GetOrderByOrderNo(refund.OrderNo)
	if err != nil {
		return "", err
	}
	if order == nil {
		return "", util.Biz("退款对应订单不存在，orderNo=" + refund.OrderNo)
	}
	aliClient, _, _, err := s.aliClientForOrder(ctx, order)
	if err != nil {
		return "", err
	}
	if aliClient == nil {
		return "", util.Biz("alipay client is not configured")
	}
	biz := map[string]interface{}{"out_trade_no": refund.OrderNo, "out_request_no": refund.RefundNo}
	body, err := aliClient.Execute(ctx, "alipay.trade.fastpay.refund.query", util.ToJSON(biz))
	if err != nil {
		return "", util.Biz("退款查询接口调用失败: " + err.Error())
	}
	return body, nil
}

func (s *Service) aliClientForConfig(cfg config.AliPayConfig, paymentAppID int64) (*pay.AliPayClient, error) {
	if paymentAppID <= 0 && s.AliPay != nil {
		return s.AliPay, nil
	}
	aliClient, err := pay.NewAliPayClient(cfg)
	if err != nil {
		return nil, util.Biz("init alipay client failed: " + err.Error())
	}
	return aliClient, nil
}

func (s *Service) aliClientForOrder(ctx context.Context, order *model.OrderInfo) (*pay.AliPayClient, config.AliPayConfig, int64, error) {
	cfg, paymentAppID, err := s.aliPayConfigForOrder(ctx, order)
	if err != nil {
		return nil, config.AliPayConfig{}, 0, err
	}
	client, err := s.aliClientForConfig(cfg, paymentAppID)
	if err != nil {
		return nil, config.AliPayConfig{}, 0, err
	}
	return client, cfg, paymentAppID, nil
}

func (s *Service) AliQueryBill(ctx context.Context, billDate, typ string, paymentAppID ...int64) (string, error) {
	appID := int64(0)
	if len(paymentAppID) > 0 {
		appID = paymentAppID[0]
	}
	client := s.AliPay
	if appID > 0 {
		cfg, resolvedAppID, err := s.aliPayConfigForApp(ctx, appID)
		if err != nil {
			return "", err
		}
		client, err = s.aliClientForConfig(cfg, resolvedAppID)
		if err != nil {
			return "", err
		}
	}
	biz := map[string]interface{}{"bill_type": typ, "bill_date": billDate}
	body, err := client.Execute(ctx, "alipay.data.dataservice.bill.downloadurl.query", util.ToJSON(biz))
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
