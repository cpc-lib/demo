package service

import (
	"context"
	"encoding/json"
	"fmt"
	"strconv"
	"strings"
	"time"

	"payment-demo-go/internal/constant"
	"payment-demo-go/internal/model"
	"payment-demo-go/internal/pay"
	"payment-demo-go/internal/util"

	"github.com/google/uuid"
)

func (s *Service) WxExecuteRefund(ctx context.Context, refund *model.RefundInfo) error {
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
	wxV3, cfg, _, err := s.wxV3ClientForOrder(ctx, order)
	if err != nil {
		return err
	}
	if wxV3 == nil {
		return util.Biz("wxpay v3 client is not configured")
	}
	params := map[string]interface{}{
		"out_trade_no":  refund.OrderNo,
		"out_refund_no": refund.RefundNo,
		"reason":        refund.Reason,
		"notify_url":    strings.TrimRight(cfg.NotifyDomain, "/") + constant.WxNotifyRefund,
		"amount":        map[string]interface{}{"refund": refund.Refund, "total": refund.TotalFee, "currency": "CNY"},
	}
	resp, err := wxV3.PostJSONForResponse(ctx, strings.TrimRight(cfg.Domain, "/")+constant.WxAPIDomesticRefunds, util.ToJSON(params))
	if err != nil {
		_ = s.UpdateRefundToFailed(refund.RefundNo, err.Error())
		return util.Biz("微信退款异常: " + err.Error())
	}
	if resp.Successful() {
		return s.UpdateRefundToProcessing(refund.RefundNo, resp.Body)
	}
	_ = s.UpdateRefundToFailed(refund.RefundNo, resp.Body)
	return util.Biz(fmt.Sprintf("微信退款异常，响应码 = %d, 返回结果 = %s", resp.StatusCode, resp.Body))
}

func (s *Service) WxQueryRefund(ctx context.Context, refundNo string) (string, error) {
	refund, err := s.GetRefundByRefundNo(refundNo)
	if err != nil {
		return "", err
	}
	if refund == nil {
		return "", util.Biz("退款单不存在")
	}
	order, err := s.GetOrderByOrderNo(refund.OrderNo)
	if err != nil {
		return "", err
	}
	if order == nil {
		return "", util.Biz("退款对应订单不存在，orderNo=" + refund.OrderNo)
	}
	wxV3, cfg, _, err := s.wxV3ClientForOrder(ctx, order)
	if err != nil {
		return "", err
	}
	if wxV3 == nil {
		return "", util.Biz("wxpay v3 client is not configured")
	}
	path := fmt.Sprintf(constant.WxAPIDomesticRefundQuery, refundNo)
	return wxV3.Get(ctx, strings.TrimRight(cfg.Domain, "/")+path)
}

func (s *Service) WxQueryRefundStatusForSync(ctx context.Context, refundNo string) (model.RefundStatusSyncResult, error) {
	result, err := s.WxQueryRefund(ctx, refundNo)
	if err != nil {
		return model.RefundStatusSyncResult{}, err
	}
	m, err := pay.DecodeJSONMap(result)
	if err != nil {
		return model.RefundStatusSyncResult{}, err
	}
	status := str(m["status"])
	return buildWxRefundStatusSyncResult(m, status, result), nil
}

func (s *Service) WxQueryOrderRefundsForSync(ctx context.Context, orderNo string) ([]model.RefundStatusSyncResult, error) {
	order, err := s.GetOrderByOrderNo(orderNo)
	if err != nil {
		return nil, err
	}
	if order == nil {
		return nil, util.Biz("订单不存在，orderNo=" + orderNo)
	}
	var results []model.RefundStatusSyncResult
	offset := 0
	for {
		m, err := s.wxQueryOrderRefundsByV2(ctx, order, offset)
		if err != nil {
			return nil, err
		}
		if len(m) == 0 {
			return results, nil
		}
		pageCount := parseIntDefault(m["refund_count"], 0)
		totalCount := parseIntDefault(m["total_refund_count"], pageCount)
		if pageCount <= 0 {
			return results, nil
		}
		pageSize := 0
		for i := 0; i < pageCount; i++ {
			refundNo := m[fmt.Sprintf("out_refund_no_%d", i)]
			if strings.TrimSpace(refundNo) == "" {
				continue
			}
			pageSize++
			status := m[fmt.Sprintf("refund_status_%d", i)]
			totalFee := parseIntPtr(m["total_fee"])
			refundFee := parseIntPtr(m[fmt.Sprintf("refund_fee_%d", i)])
			item := map[string]interface{}{"out_trade_no": m["out_trade_no"], "out_refund_no": refundNo, "refund_id": m[fmt.Sprintf("refund_id_%d", i)], "refund_status": status, "total_fee": m["total_fee"], "refund_fee": m[fmt.Sprintf("refund_fee_%d", i)], "offset": offset}
			results = append(results, model.RefundStatusSyncResult{OrderNo: m["out_trade_no"], RefundNo: refundNo, RefundID: m[fmt.Sprintf("refund_id_%d", i)], ChannelStatus: status, RefundStatus: mapWxRefundStatus(status), Content: util.ToJSON(item), TotalFee: totalFee, RefundAmount: refundFee})
		}
		if pageSize == 0 || len(results) >= totalCount {
			return results, nil
		}
		offset += pageSize
	}
}

func (s *Service) WxProcessRefundNotify(ctx context.Context, bodyMap map[string]interface{}) error {
	plain, err := s.WxV3.DecryptResource(bodyMap)
	if err != nil {
		return util.Biz("微信退款通知解密失败: " + err.Error())
	}
	return s.WxProcessRefundNotifyPlain(ctx, plain, 0)
}

func (s *Service) WxProcessRefundNotifyPlain(ctx context.Context, plain string, paymentAppID int64) error {
	m, err := pay.DecodeJSONMap(plain)
	if err != nil {
		return err
	}
	refundNo := str(m["out_refund_no"])
	if refundNo == "" {
		return util.Biz("微信退款通知缺少商户退款单号")
	}
	return s.Lock.Execute(ctx, "payment:wx:notify:refund:"+refundNo, 5*time.Second, 30*time.Second, func() error {
		orderNo := str(m["out_trade_no"])
		if orderNo == "" {
			refund, err := s.GetRefundByRefundNo(refundNo)
			if err != nil {
				return err
			}
			if refund != nil {
				orderNo = refund.OrderNo
			}
		}
		if orderNo != "" {
			order, err := s.GetOrderByOrderNo(orderNo)
			if err != nil {
				return err
			}
			if order != nil {
				if err := validateOrderPaymentAppBinding(order, paymentAppID, "微信退款通知"); err != nil {
					return err
				}
			}
		}
		status := str(m["refund_status"])
		sync := buildWxRefundStatusSyncResult(m, status, plain)
		_, err := s.SyncRefundStatus(sync)
		return err
	})
}

func buildWxRefundStatusSyncResult(m map[string]interface{}, channelStatus, content string) model.RefundStatusSyncResult {
	return model.RefundStatusSyncResult{OrderNo: str(m["out_trade_no"]), RefundNo: str(m["out_refund_no"]), RefundID: str(m["refund_id"]), ChannelStatus: channelStatus, RefundStatus: mapWxRefundStatus(channelStatus), Content: content, TotalFee: amountPtr(m, "total"), RefundAmount: amountPtr(m, "refund")}
}

func (s *Service) wxQueryOrderRefundsByV2(ctx context.Context, order *model.OrderInfo, offset int) (map[string]string, error) {
	cfg, paymentAppID, err := s.wxPayConfigForOrder(ctx, order)
	if err != nil {
		return nil, err
	}
	wxV2 := s.WxV2
	if paymentAppID > 0 || wxV2 == nil {
		wxV2 = pay.NewWxV2Client(cfg)
	}
	params := map[string]string{"appid": cfg.AppID, "mch_id": cfg.MchID, "nonce_str": uuid.NewString(), "out_trade_no": order.OrderNo, "offset": strconv.Itoa(offset)}
	_, result, err := wxV2.PostXML(ctx, strings.TrimRight(cfg.Domain, "/")+constant.WxAPIRefundQueryV2, params)
	if err != nil {
		return nil, util.Biz("微信订单退款查询异常: " + err.Error())
	}
	if result["return_code"] != "SUCCESS" {
		return nil, util.Biz("微信订单退款查询通信失败：" + result["return_msg"])
	}
	if result["result_code"] != "SUCCESS" {
		if result["err_code"] == "REFUNDNOTEXIST" {
			return map[string]string{}, nil
		}
		return nil, util.Biz("微信订单退款查询失败：" + result["err_code_des"])
	}
	return result, nil
}

func amountPtr(m map[string]interface{}, key string) *int {
	amount, _ := m["amount"].(map[string]interface{})
	if amount == nil {
		return nil
	}
	return parseAnyIntPtr(amount[key])
}
func parseAnyIntPtr(v interface{}) *int {
	if v == nil {
		return nil
	}
	switch x := v.(type) {
	case float64:
		n := int(x)
		return &n
	case json.Number:
		i, _ := x.Int64()
		n := int(i)
		return &n
	case string:
		return parseIntPtr(x)
	default:
		return nil
	}
}
func parseIntPtr(s string) *int {
	if strings.TrimSpace(s) == "" {
		return nil
	}
	n, err := strconv.Atoi(s)
	if err != nil {
		return nil
	}
	return &n
}
func parseIntDefault(s string, def int) int {
	n, err := strconv.Atoi(s)
	if err != nil {
		return def
	}
	return n
}
