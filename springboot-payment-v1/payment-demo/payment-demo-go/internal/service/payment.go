package service

import (
	"encoding/json"
	"fmt"
	"strconv"
	"strings"

	"payment-demo-go/internal/constant"
	"payment-demo-go/internal/model"
	"payment-demo-go/internal/util"
)

func (s *Service) CreatePaymentInfoWxV3(plainText string) error {
	var m map[string]interface{}
	if err := json.Unmarshal([]byte(plainText), &m); err != nil {
		return err
	}
	orderNo := str(m["out_trade_no"])
	transactionID := str(m["transaction_id"])
	tradeType := str(m["trade_type"])
	tradeState := str(m["trade_state"])
	payerTotal := ptrIntFromAmount(m, "payer_total")
	pi := model.PaymentInfo{OrderNo: orderNo, PaymentType: constant.PayTypeWxPay, TransactionID: transactionID, TradeType: tradeType, TradeState: tradeState, PayerTotal: payerTotal, Content: plainText}
	return s.insertPaymentInfoIdempotently(&pi)
}

func (s *Service) CreatePaymentInfoWxV2(params map[string]string, content string) error {
	var payer *int
	if params["total_fee"] != "" {
		if n, err := strconv.Atoi(params["total_fee"]); err == nil {
			payer = &n
		}
	}
	pi := model.PaymentInfo{OrderNo: params["out_trade_no"], PaymentType: constant.PayTypeWxPay, TransactionID: params["transaction_id"], TradeType: params["trade_type"], TradeState: params["result_code"], PayerTotal: payer, Content: content}
	return s.insertPaymentInfoIdempotently(&pi)
}

func (s *Service) CreatePaymentInfoAliPay(params map[string]string) error {
	cents, err := util.YuanToCents(params["total_amount"])
	if err != nil {
		return err
	}
	pi := model.PaymentInfo{OrderNo: params["out_trade_no"], PaymentType: constant.PayTypeAliPay, TransactionID: params["trade_no"], TradeType: "电脑网站支付", TradeState: params["trade_status"], PayerTotal: &cents, Content: util.ToJSON(params)}
	return s.insertPaymentInfoIdempotently(&pi)
}

func (s *Service) insertPaymentInfoIdempotently(pi *model.PaymentInfo) error {
	err := s.DB.Create(pi).Error
	if err != nil && isDuplicate(err) {
		return nil
	}
	return err
}

func ptrIntFromAmount(m map[string]interface{}, key string) *int {
	amount, _ := m["amount"].(map[string]interface{})
	if amount == nil {
		return nil
	}
	v := amount[key]
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
		i, _ := strconv.Atoi(x)
		n = i
	default:
		return nil
	}
	return &n
}

func str(v interface{}) string {
	if v == nil {
		return ""
	}
	return fmt.Sprint(v)
}

func isDuplicate(err error) bool { return err != nil && strings.Contains(err.Error(), "Duplicate") }
