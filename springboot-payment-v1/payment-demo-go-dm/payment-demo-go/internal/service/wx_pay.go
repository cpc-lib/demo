package service

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"net/url"
	"strconv"
	"strings"
	"time"

	"payment-demo-go/internal/config"
	"payment-demo-go/internal/constant"
	"payment-demo-go/internal/model"
	"payment-demo-go/internal/pay"
	"payment-demo-go/internal/util"

	"github.com/google/uuid"
	"gorm.io/gorm"
)

func (s *Service) WxNativePay(ctx context.Context, productID int64) (map[string]interface{}, error) {
	return s.WxNativePayWithApp(ctx, productID, 0)
}

func (s *Service) WxNativePayWithApp(ctx context.Context, productID int64, paymentAppID int64) (map[string]interface{}, error) {
	cfg, resolvedPaymentAppID, err := s.wxPayConfigForApp(ctx, paymentAppID)
	if err != nil {
		return nil, err
	}
	wxV3, err := s.wxV3ClientForConfig(cfg, resolvedPaymentAppID)
	if err != nil {
		return nil, err
	}
	if wxV3 == nil {
		return nil, util.Biz("wxpay v3 client is not configured")
	}
	lockKey := fmt.Sprintf("payment:wx:native:v3:%d", productID)
	if resolvedPaymentAppID > 0 {
		lockKey += ":" + i64(resolvedPaymentAppID)
	}
	v, err := s.Lock.ExecuteValue(ctx, lockKey, 3*time.Second, 30*time.Second, func() (interface{}, error) {
		order, err := s.CreateOrReuseOrderWithApp(ctx, productID, constant.PayTypeWxPay, resolvedPaymentAppID)
		if err != nil {
			return nil, err
		}
		if strings.TrimSpace(order.CodeURL) != "" {
			return nativePayResult(order.OrderNo, order.CodeURL), nil
		}
		params := map[string]interface{}{
			"appid": cfg.AppID, "mchid": cfg.MchID, "description": order.Title,
			"out_trade_no": order.OrderNo,
			"notify_url":   strings.TrimRight(cfg.NotifyDomain, "/") + constant.WxNotifyNative,
			"amount":       map[string]interface{}{"total": order.TotalFee, "currency": "CNY"},
		}
		body := util.ToJSON(params)
		respBody, err := wxV3.PostJSON(ctx, strings.TrimRight(cfg.Domain, "/")+constant.WxAPINativePay, body)
		if err != nil {
			return nil, util.Biz("Native下单失败: " + err.Error())
		}
		result, err := pay.DecodeJSONMap(respBody)
		if err != nil {
			return nil, err
		}
		codeURL := str(result["code_url"])
		if err := s.SaveCodeURL(order.OrderNo, codeURL); err != nil {
			return nil, err
		}
		return nativePayResult(order.OrderNo, codeURL), nil
	})
	if err != nil {
		return nil, err
	}
	return v.(map[string]interface{}), nil
}

func nativePayResult(orderNo, codeURL string) map[string]interface{} {
	return map[string]interface{}{"codeUrl": codeURL, "orderNo": orderNo}
}

func (s *Service) WxProcessOrderNotify(ctx context.Context, bodyMap map[string]interface{}) error {
	plainText, err := s.WxV3.DecryptResource(bodyMap)
	if err != nil {
		return util.Biz("微信支付通知解密失败: " + err.Error())
	}
	return s.WxProcessOrderNotifyPlain(ctx, plainText, 0)
}

func (s *Service) WxProcessOrderNotifyPlain(ctx context.Context, plainText string, paymentAppID int64) error {
	var plain map[string]interface{}
	if err := json.Unmarshal([]byte(plainText), &plain); err != nil {
		return err
	}
	orderNo := str(plain["out_trade_no"])
	if strings.TrimSpace(orderNo) == "" {
		return util.Biz("微信支付通知缺少商户订单号")
	}
	return s.Lock.Execute(ctx, "payment:wx:notify:pay:"+orderNo, 5*time.Second, 30*time.Second, func() error {
		return s.DB.Transaction(func(tx *gorm.DB) error {
			order, err := s.getOrderByOrderNoForUpdateDB(tx, orderNo)
			if err != nil {
				return err
			}
			if order == nil {
				return util.Biz("微信支付通知对应订单不存在，orderNo=" + orderNo)
			}
			if err := validateOrderPaymentAppBinding(order, paymentAppID, "微信支付通知"); err != nil {
				return err
			}
			if err := validateWxPayOrderNotify(order, plain); err != nil {
				return err
			}
			if order.OrderStatus != constant.OrderStatusNotPay {
				return nil
			}
			updated, err := s.updateOrderStatusIfDB(tx, orderNo, constant.OrderStatusNotPay, constant.OrderStatusSuccess)
			if err != nil {
				return err
			}
			if !updated {
				return nil
			}
			return s.CreatePaymentInfoWxV3(plainText)
		})
	})
}

func (s *Service) WxCancelOrder(ctx context.Context, orderNo string) error {
	if strings.TrimSpace(orderNo) == "" {
		return util.Biz("订单号不能为空")
	}
	return s.Lock.Execute(ctx, "payment:order:cancel:"+orderNo, 3*time.Second, 30*time.Second, func() error {
		return s.DB.Transaction(func(tx *gorm.DB) error {
			order, err := s.getOrderByOrderNoForUpdateDB(tx, orderNo)
			if err != nil {
				return err
			}
			if order == nil {
				return util.Biz("订单不存在，orderNo=" + orderNo)
			}
			if order.OrderStatus != constant.OrderStatusNotPay {
				return nil
			}
			if err := s.wxCloseOrderForOrder(ctx, order); err != nil {
				return err
			}
			_, err = s.updateOrderStatusIfDB(tx, orderNo, constant.OrderStatusNotPay, constant.OrderStatusCancel)
			return err
		})
	})
}

func (s *Service) WxQueryOrder(ctx context.Context, orderNo string) (string, error) {
	order, err := s.GetOrderByOrderNo(orderNo)
	if err != nil {
		return "", err
	}
	if order == nil {
		return "", util.Biz("订单不存在，orderNo=" + orderNo)
	}
	return s.wxQueryOrderForOrder(ctx, order)
}

func (s *Service) wxQueryOrderForOrder(ctx context.Context, order *model.OrderInfo) (string, error) {
	wxV3, cfg, _, err := s.wxV3ClientForOrder(ctx, order)
	if err != nil {
		return "", err
	}
	if wxV3 == nil {
		return "", util.Biz("wxpay v3 client is not configured")
	}
	u := fmt.Sprintf(constant.WxAPIOrderQueryByNo, order.OrderNo)
	q := url.Values{}
	q.Set("mchid", cfg.MchID)
	return wxV3.Get(ctx, pay.BuildURL(cfg.Domain, u, q))
}

func (s *Service) WxQueryPaymentStatus(ctx context.Context, orderNo string) (map[string]interface{}, error) {
	order, err := s.GetOrderByOrderNo(orderNo)
	if err != nil {
		return nil, err
	}
	if order == nil {
		return nil, util.Biz("订单不存在，orderNo=" + orderNo)
	}
	if order.PaymentType != constant.PayTypeWxPay {
		return nil, util.Biz("订单不是微信支付订单，orderNo=" + orderNo)
	}
	result, err := s.wxQueryOrderForOrder(ctx, order)
	if err != nil {
		return nil, err
	}
	resultMap, err := pay.DecodeJSONMap(result)
	if err != nil {
		return nil, err
	}
	tradeState := str(resultMap["trade_state"])
	tradeStateDesc := str(resultMap["trade_state_desc"])
	before := order.OrderStatus
	err = s.Lock.Execute(ctx, "payment:wx:query:order:"+orderNo, 5*time.Second, 30*time.Second, func() error {
		return s.DB.Transaction(func(tx *gorm.DB) error {
			return s.wxSyncOrderStatusFromQuery(ctx, tx, orderNo, resultMap, result, tradeState, false)
		})
	})
	if err != nil {
		return nil, err
	}
	after, _ := s.GetOrderStatus(orderNo)
	return map[string]interface{}{"orderNo": orderNo, "tradeState": tradeState, "tradeStateDesc": tradeStateDesc, "localStatusBefore": before, "localStatus": after, "wxPayResult": resultMap}, nil
}

func (s *Service) WxCheckOrderStatus(ctx context.Context, orderNo string) error {
	order, err := s.GetOrderByOrderNo(orderNo)
	if err != nil {
		return err
	}
	if order == nil {
		return util.Biz("订单不存在，orderNo=" + orderNo)
	}
	result, err := s.wxQueryOrderForOrder(ctx, order)
	if err != nil {
		return err
	}
	resultMap, err := pay.DecodeJSONMap(result)
	if err != nil {
		return err
	}
	tradeState := str(resultMap["trade_state"])
	return s.Lock.Execute(ctx, "payment:wx:check:order:"+orderNo, 5*time.Second, 30*time.Second, func() error {
		return s.DB.Transaction(func(tx *gorm.DB) error {
			return s.wxSyncOrderStatusFromQuery(ctx, tx, orderNo, resultMap, result, tradeState, true)
		})
	})
}

func (s *Service) wxSyncOrderStatusFromQuery(ctx context.Context, tx *gorm.DB, orderNo string, resultMap map[string]interface{}, result, tradeState string, closeUnpaid bool) error {
	order, err := s.getOrderByOrderNoForUpdateDB(tx, orderNo)
	if err != nil {
		return err
	}
	if order == nil {
		return util.Biz("查单同步对应订单不存在，orderNo=" + orderNo)
	}
	if order.OrderStatus != constant.OrderStatusNotPay {
		return nil
	}
	switch tradeState {
	case constant.WxTradeStateSuccess:
		if err := validateWxPayOrderNotify(order, resultMap); err != nil {
			return err
		}
		updated, err := s.updateOrderStatusIfDB(tx, orderNo, constant.OrderStatusNotPay, constant.OrderStatusSuccess)
		if err != nil {
			return err
		}
		if updated {
			return s.CreatePaymentInfoWxV3(result)
		}
	case constant.WxTradeStateNotPay:
		if closeUnpaid {
			if err := s.wxCloseOrderForOrder(ctx, order); err != nil {
				return err
			}
			_, err := s.updateOrderStatusIfDB(tx, orderNo, constant.OrderStatusNotPay, constant.OrderStatusClosed)
			return err
		}
	case constant.WxTradeStateClosed:
		_, err := s.updateOrderStatusIfDB(tx, orderNo, constant.OrderStatusNotPay, constant.OrderStatusClosed)
		return err
	}
	return nil
}

func (s *Service) WxNativePayV2(ctx context.Context, productID int64, remoteAddr string) (map[string]interface{}, error) {
	return s.WxNativePayV2WithApp(ctx, productID, remoteAddr, 0)
}

func (s *Service) WxNativePayV2WithApp(ctx context.Context, productID int64, remoteAddr string, paymentAppID int64) (map[string]interface{}, error) {
	cfg, resolvedPaymentAppID, err := s.wxPayConfigForApp(ctx, paymentAppID)
	if err != nil {
		return nil, err
	}
	wxV2 := s.WxV2
	if resolvedPaymentAppID > 0 || wxV2 == nil {
		wxV2 = pay.NewWxV2Client(cfg)
	}
	lockKey := fmt.Sprintf("payment:wx:native:v2:%d", productID)
	if resolvedPaymentAppID > 0 {
		lockKey += ":" + i64(resolvedPaymentAppID)
	}
	v, err := s.Lock.ExecuteValue(ctx, lockKey, 3*time.Second, 30*time.Second, func() (interface{}, error) {
		order, err := s.CreateOrReuseOrderWithApp(ctx, productID, constant.PayTypeWxPay, resolvedPaymentAppID)
		if err != nil {
			return nil, err
		}
		if order.CodeURL != "" {
			return nativePayResult(order.OrderNo, order.CodeURL), nil
		}
		params := map[string]string{"appid": cfg.AppID, "mch_id": cfg.MchID, "nonce_str": uuid.NewString(), "body": order.Title, "out_trade_no": order.OrderNo, "total_fee": strconv.Itoa(order.TotalFee), "spbill_create_ip": remoteAddr, "notify_url": strings.TrimRight(cfg.NotifyDomain, "/") + constant.WxNotifyNativeV2, "trade_type": "NATIVE"}
		resultXML, resultMap, err := wxV2.PostXML(ctx, strings.TrimRight(cfg.Domain, "/")+constant.WxAPINativePayV2, params)
		if err != nil {
			return nil, util.Biz("微信支付v2统一下单失败: " + err.Error())
		}
		if resultMap["return_code"] == "FAIL" || resultMap["result_code"] == "FAIL" {
			return nil, util.Biz("微信支付统一下单错误: " + resultXML)
		}
		codeURL := resultMap["code_url"]
		if err := s.SaveCodeURL(order.OrderNo, codeURL); err != nil {
			return nil, err
		}
		return nativePayResult(order.OrderNo, codeURL), nil
	})
	if err != nil {
		return nil, err
	}
	return v.(map[string]interface{}), nil
}

func (s *Service) WxProcessV2Notify(ctx context.Context, params map[string]string, raw string) error {
	return s.WxProcessV2NotifyWithApp(ctx, params, raw, 0)
}

func (s *Service) WxProcessV2NotifyWithApp(ctx context.Context, params map[string]string, raw string, paymentAppID int64) error {
	transactionID := params["transaction_id"]
	orderNo := params["out_trade_no"]
	if strings.TrimSpace(orderNo) == "" {
		return util.Biz("订单号不能为空")
	}
	notifyTotal, err := strconv.Atoi(params["total_fee"])
	if err != nil {
		return util.Biz("金额校验失败")
	}
	_ = transactionID // ignore unused variable
	return s.Lock.Execute(ctx, "payment:wx:v2:notify:pay:"+orderNo, 5*time.Second, 30*time.Second, func() error {
		return s.DB.Transaction(func(tx *gorm.DB) error {
			order, err := s.getOrderByOrderNoForUpdateDB(tx, orderNo)
			if err != nil {
				return err
			}
			if order == nil {
				return util.Biz("微信支付v2通知订单不存在，orderNo=" + orderNo)
			}
			if err := validateOrderPaymentAppBinding(order, paymentAppID, "微信支付v2通知"); err != nil {
				return err
			}
			if notifyTotal != order.TotalFee {
				return util.Biz("微信支付v2通知金额与本地订单金额不一致，orderNo=" + orderNo)
			}
			if order.OrderStatus != constant.OrderStatusNotPay {
				return nil
			}
			updated, err := s.updateOrderStatusIfDB(tx, orderNo, constant.OrderStatusNotPay, constant.OrderStatusSuccess)
			if err != nil {
				return err
			}
			if updated {
				return s.CreatePaymentInfoWxV2(params, raw)
			}
			return nil
		})
	})
}

func (s *Service) VerifyWxV2NotifyApp(ctx context.Context, params map[string]string) (int64, error) {
	cfg, paymentAppID, err := s.wxPayConfigByProvider(ctx, params["appid"], params["mch_id"])
	if err != nil {
		return 0, err
	}
	if !pay.VerifyV2Sign(params, cfg.PartnerKey) {
		return 0, util.Biz("验签失败")
	}
	return paymentAppID, nil
}

func (s *Service) WxJSAPIPay(ctx context.Context, order model.OrderInfo, openid string) (map[string]interface{}, error) {
	if strings.TrimSpace(openid) == "" {
		return nil, util.Biz("openid不能为空")
	}
	v, err := s.Lock.ExecuteValue(ctx, "payment:wx:jsapi:"+order.OrderNo, 5*time.Second, 30*time.Second, func() (interface{}, error) {
		params := map[string]interface{}{"mchid": s.Cfg.WxPay.MchID, "appid": s.Cfg.WxPay.AppID, "description": order.Title, "notify_url": "回调链接自己写", "out_trade_no": order.OrderNo, "amount": map[string]interface{}{"total": order.TotalFee}, "payer": map[string]interface{}{"openid": openid}}
		body, err := s.WxV3.PostJSON(ctx, strings.TrimRight(s.Cfg.WxPay.Domain, "/")+constant.WxAPIJSAPIPay, util.ToJSON(params))
		if err != nil {
			return nil, err
		}
		m, _ := pay.DecodeJSONMap(body)
		prepay := "prepay_id=" + str(m["prepay_id"])
		timestamp := time.Now().Unix()
		nonce := strings.ToUpper(uuid.NewString())
		sign, err := s.WxV3.SignJSAPIPackage(s.Cfg.WxPay.AppID, prepay, timestamp, nonce)
		if err != nil {
			return nil, util.Biz("获取微信支付签名失败: " + err.Error())
		}
		return map[string]interface{}{"appId": s.Cfg.WxPay.AppID, "timeStamp": timestamp, "nonceStr": nonce, "package": prepay, "signType": "RSA", "paySign": sign}, nil
	})
	if err != nil {
		return nil, err
	}
	return v.(map[string]interface{}), nil
}

func (s *Service) wxCloseOrder(ctx context.Context, orderNo string) error {
	order, err := s.GetOrderByOrderNo(orderNo)
	if err != nil {
		return err
	}
	if order == nil {
		return util.Biz("订单不存在，orderNo=" + orderNo)
	}
	return s.wxCloseOrderForOrder(ctx, order)
}

func (s *Service) wxCloseOrderForOrder(ctx context.Context, order *model.OrderInfo) error {
	wxV3, cfg, _, err := s.wxV3ClientForOrder(ctx, order)
	if err != nil {
		return err
	}
	if wxV3 == nil {
		return util.Biz("wxpay v3 client is not configured")
	}
	path := fmt.Sprintf(constant.WxAPICloseOrderByNo, order.OrderNo)
	_, err = wxV3.PostJSON(ctx, strings.TrimRight(cfg.Domain, "/")+path, util.ToJSON(map[string]string{"mchid": cfg.MchID}))
	if err != nil {
		return util.Biz("Native关单失败: " + err.Error())
	}
	return nil
}

func (s *Service) wxV3ClientForConfig(cfg config.WxPayConfig, paymentAppID int64) (*pay.WxV3Client, error) {
	if paymentAppID <= 0 && s.WxV3 != nil {
		return s.WxV3, nil
	}
	wxV3, err := pay.NewWxV3Client(cfg)
	if err != nil {
		return nil, util.Biz("init wxpay v3 client failed: " + err.Error())
	}
	return wxV3, nil
}

func (s *Service) wxV3ClientForOrder(ctx context.Context, order *model.OrderInfo) (*pay.WxV3Client, config.WxPayConfig, int64, error) {
	cfg, paymentAppID, err := s.wxPayConfigForOrder(ctx, order)
	if err != nil {
		return nil, config.WxPayConfig{}, 0, err
	}
	client, err := s.wxV3ClientForConfig(cfg, paymentAppID)
	if err != nil {
		return nil, config.WxPayConfig{}, 0, err
	}
	return client, cfg, paymentAppID, nil
}

func (s *Service) VerifyAndDecryptWxV3Notification(ctx context.Context, header http.Header, body []byte, bodyMap map[string]interface{}) (string, int64, error) {
	configs, err := s.wxPayConfiguredApps(ctx)
	if err != nil {
		return "", 0, err
	}
	var lastErr error
	for _, item := range configs {
		wxV3, err := s.wxV3ClientForConfig(item.Config, item.PaymentAppID)
		if err != nil {
			lastErr = err
			continue
		}
		if wxV3 == nil {
			continue
		}
		if err := wxV3.VerifyNotification(ctx, header, body); err != nil {
			lastErr = err
			continue
		}
		plain, err := wxV3.DecryptResource(bodyMap)
		if err != nil {
			lastErr = err
			continue
		}
		return plain, item.PaymentAppID, nil
	}
	if lastErr != nil {
		return "", 0, lastErr
	}
	return "", 0, util.Biz("微信支付通知验签失败")
}

func validateWxPayOrderNotify(order *model.OrderInfo, notify map[string]interface{}) error {
	if order.PaymentType != constant.PayTypeWxPay {
		return util.Biz("支付通知支付类型不匹配，orderNo=" + order.OrderNo)
	}
	amount, _ := notify["amount"].(map[string]interface{})
	if amount == nil {
		return nil
	}
	total := amountInt(amount, "total")
	if total == nil {
		total = amountInt(amount, "payer_total")
	}
	if total == nil {
		return nil
	}
	if *total != order.TotalFee {
		return util.Biz("支付通知金额与订单金额不一致，orderNo=" + order.OrderNo)
	}
	return nil
}

func amountInt(m map[string]interface{}, key string) *int {
	v := m[key]
	if v == nil {
		return nil
	}
	var n int
	switch x := v.(type) {
	case float64:
		n = int(x)
	case json.Number:
		i, _ := x.Int64()
		n = int(i)
	case string:
		i, err := strconv.Atoi(x)
		if err != nil {
			return nil
		}
		n = i
	default:
		return nil
	}
	return &n
}

func (s *Service) WxQueryBill(ctx context.Context, billDate, typ, billType, accountType, tarType string) (string, error) {
	path := constant.WxAPITradeBills
	q := url.Values{}
	q.Set("bill_date", billDate)
	if typ == "tradebill" {
		q.Set("bill_type", defaultBlank(billType, "ALL"))
	} else if typ == "fundflowbill" {
		path = constant.WxAPIFundFlowBills
		q.Set("account_type", defaultBlank(accountType, "BASIC"))
	} else {
		return "", util.Biz("不支持的微信账单类型：" + typ)
	}
	if strings.TrimSpace(tarType) != "" {
		q.Set("tar_type", strings.TrimSpace(tarType))
	}
	body, err := s.WxV3.Get(ctx, pay.BuildURL(s.Cfg.WxPay.Domain, path, q))
	if err != nil {
		return "", err
	}
	m, _ := pay.DecodeJSONMap(body)
	return str(m["download_url"]), nil
}

func (s *Service) WxDownloadBill(ctx context.Context, billDate, typ, billType, accountType, tarType string) (string, error) {
	downloadURL, err := s.WxQueryBill(ctx, billDate, typ, billType, accountType, tarType)
	if err != nil {
		return "", err
	}
	if downloadURL == "" {
		return "", util.Biz("微信账单下载地址为空")
	}
	return s.WxV3.GetNoSign(ctx, downloadURL)
}

func defaultBlank(v, def string) string {
	if strings.TrimSpace(v) == "" {
		return def
	}
	return strings.TrimSpace(v)
}
