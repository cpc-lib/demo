package service

import (
	"context"
	"strings"

	"payment-demo-go/internal/constant"
	"payment-demo-go/internal/model"
	"payment-demo-go/internal/types"
	"payment-demo-go/internal/util"
)

func (s *Service) HandleReconciliationDiff(ctx context.Context, diffID int64, req model.ReconciliationDiffHandleRequest) error {
	if diffID <= 0 {
		return util.Biz("差异记录ID不能为空")
	}
	if strings.TrimSpace(req.HandleType) == "" {
		return util.Biz("处理类型不能为空")
	}
	var diff model.ReconciliationDiff
	err := s.DB.Where("id=?", diffID).First(&diff).Error
	if err != nil {
		return util.Biz("差异记录不存在")
	}
	if diff.HandleStatus != constant.ReconciliationHandleStatusPending {
		return util.Biz("该差异记录已处理，不能重复处理")
	}
	now := types.Now()
	values := map[string]interface{}{
		"handle_status": constant.ReconciliationHandleStatusHandled,
		"handle_type":   req.HandleType,
		"handle_remark": req.Remark,
		"handled_time":  now.Time,
	}
	switch req.HandleType {
	case constant.ReconciliationHandleTypeSupplement:
		if err := s.supplementFromChannel(ctx, &diff); err != nil {
			return err
		}
	case constant.ReconciliationHandleTypeIgnore:
		values["handle_status"] = constant.ReconciliationHandleStatusIgnored
	case constant.ReconciliationHandleTypeManualProcess:
		values["handle_status"] = constant.ReconciliationHandleStatusHandled
	case constant.ReconciliationHandleTypeMarkRefunded:
		values["handle_status"] = constant.ReconciliationHandleStatusHandled
	default:
		return util.Biz("不支持的处理类型: " + req.HandleType)
	}
	return s.DB.Model(&model.ReconciliationDiff{}).Where("id=?", diffID).Updates(values).Error
}

func (s *Service) supplementFromChannel(ctx context.Context, diff *model.ReconciliationDiff) error {
	if diff.DiffType == constant.ReconciliationDiffTypeChannelOnly {
		if diff.DetailType == constant.ReconciliationDetailTypeOrder {
			return s.supplementOrderFromChannel(ctx, diff)
		}
		if diff.DetailType == constant.ReconciliationDetailTypeRefund {
			return s.supplementRefundFromChannel(ctx, diff)
		}
	}
	if diff.DiffType == constant.ReconciliationDiffTypeLocalOnly {
		if diff.DetailType == constant.ReconciliationDetailTypeOrder && diff.OrderNo != "" {
			return s.supplementOrderFromChannel(ctx, diff)
		}
		if diff.DetailType == constant.ReconciliationDetailTypeRefund && diff.RefundNo != "" {
			return s.supplementRefundFromChannel(ctx, diff)
		}
	}
	return nil
}

func (s *Service) supplementOrderFromChannel(ctx context.Context, diff *model.ReconciliationDiff) error {
	if diff.OrderNo == "" {
		return util.Biz("缺少订单号，无法补录")
	}
	order, err := s.GetOrderByOrderNo(diff.OrderNo)
	if err != nil {
		return err
	}
	if order != nil {
		if order.PaymentType == constant.PayTypeWxPay {
			_, _ = s.WxQueryPaymentStatus(ctx, diff.OrderNo)
		} else if order.PaymentType == constant.PayTypeAliPay {
			_ = s.AliCheckOrderStatus(ctx, diff.OrderNo)
		}
	}
	return nil
}

func (s *Service) supplementRefundFromChannel(ctx context.Context, diff *model.ReconciliationDiff) error {
	if diff.RefundNo == "" {
		return util.Biz("缺少退款单号，无法补录")
	}
	refund, err := s.GetRefundByRefundNo(diff.RefundNo)
	if err != nil {
		return err
	}
	if refund != nil {
		_, _ = s.QueryRefundStatus(ctx, diff.RefundNo)
	} else {
		_, _ = s.WxQueryRefund(ctx, diff.RefundNo)
		_, _ = s.AliQueryRefund(ctx, diff.RefundNo)
	}
	return nil
}
