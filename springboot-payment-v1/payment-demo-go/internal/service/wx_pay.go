package service

import (
	"context"
	"encoding/json"
	"fmt"
	"net/url"
	"strconv"
	"strings"
	"time"

	"payment-demo-go/internal/constant"
	"payment-demo-go/internal/model"
	"payment-demo-go/internal/pay"
	"payment-demo-go/internal/util"

	"github.com/google/uuid"
	"gorm.io/gorm"
)

func (s *Service) WxNativePay(ctx context.Context, productID int64) (map[string]interface{}, error) {
	v, err := s.Lock.ExecuteValue(ctx, fmt.Sprintf("payment:wx:native:v3:%d", productID), 3*time.Second, 30*time.Second, func() (interface{}, error) {
		order, err := s.CreateOrReuseOrder(ctx, productID, constant.PayTypeWxPay)
		if err != nil {
			return nil, err
		}
		if strings.TrimSpace(order.CodeURL) != "" {
			return nativePayResult(order.OrderNo, order.CodeURL), nil
		}
		params := map[string]interface{}{
			"appid": s.Cfg.WxPay.AppID, "mchid": s.Cfg.WxPay.MchID, "description": order.Title,
			"out_trade_no": order.OrderNo,
			"notify_url":   strings.TrimRight(s.Cfg.WxPay.NotifyDomain, "/") + constant.WxNotifyNative,
			"amount":       map[string]interface{}{"total": order.TotalFee, "currency": "CNY"},
		}
		body := util.ToJSON(params)
		respBody, err := s.WxV3.PostJSON(ctx, strings.TrimRight(s.Cfg.WxPay.Domain, "/")+constant.WxAPINativePay, body)
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
	var plain map[string]interface{}
	if err := json.Unmarshal([]byte(plainText), &plain); err != nil {
		return err
	}
	orderNo := str(plain["out_trade_no"])
	if strings.TrimSpace(orderNo) == "" {
		return util.Biz("微信支付通知缺少商户订单号")
	}
	notifyID := str(bodyMap["id"])
	_ = notifyID // ignore unused variable
	return s.Lock.Execute(ctx, "payment:wx:notify:pay:"+orderNo, 5*time.Second, 30*time.Second, func() error {
		return s.DB.Transaction(func(tx *gorm.DB) error {
			order, err := s.getOrderByOrderNoForUpdateDB(tx, orderNo)
			if err != nil {
				return err
			}
			if order == nil {
				return util.Biz("微信支付通知对应订单不存在，orderNo=" + orderNo)
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
			if err := s.wxCloseOrder(ctx, orderNo); err != nil {
				return err
			}
			_, err = s.updateOrderStatusIfDB(tx, orderNo, constant.OrderStatusNotPay, constant.OrderStatusCancel)
			return err
		})
	})
}

func (s *Service) WxQueryOrder(ctx context.Context, orderNo string) (string, error) {
	u := fmt.Sprintf(constant.WxAPIOrderQueryByNo, orderNo)
	q := url.Values{}
	q.Set("mchid", s.Cfg.WxPay.MchID)
	return s.WxV3.Get(ctx, pay.BuildURL(s.Cfg.WxPay.Domain, u, q))
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
	result, err := s.WxQueryOrder(ctx, orderNo)
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
	result, err := s.WxQueryOrder(ctx, orderNo)
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
			if err := s.wxCloseOrder(ctx, orderNo); err != nil {
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
	v, err := s.Lock.ExecuteValue(ctx, fmt.Sprintf("payment:wx:native:v2:%d", productID), 3*time.Second, 30*time.Second, func() (interface{}, error) {
		order, err := s.CreateOrReuseOrder(ctx, productID, constant.PayTypeWxPay)
		if err != nil {
			return nil, err
		}
		if order.CodeURL != "" {
			return nativePayResult(order.OrderNo, order.CodeURL), nil
		}
		params := map[string]string{"appid": s.Cfg.WxPay.AppID, "mch_id": s.Cfg.WxPay.MchID, "nonce_str": uuid.NewString(), "body": order.Title, "out_trade_no": order.OrderNo, "total_fee": strconv.Itoa(order.TotalFee), "spbill_create_ip": remoteAddr, "notify_url": strings.TrimRight(s.Cfg.WxPay.NotifyDomain, "/") + constant.WxNotifyNativeV2, "trade_type": "NATIVE"}
		resultXML, resultMap, err := s.WxV2.PostXML(ctx, strings.TrimRight(s.Cfg.WxPay.Domain, "/")+constant.WxAPINativePayV2, params)
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
	path := fmt.Sprintf(constant.WxAPICloseOrderByNo, orderNo)
	_, err := s.WxV3.PostJSON(ctx, strings.TrimRight(s.Cfg.WxPay.Domain, "/")+path, util.ToJSON(map[string]string{"mchid": s.Cfg.WxPay.MchID}))
	if err != nil {
		return util.Biz("Native关单失败: " + err.Error())
	}
	return nil
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
