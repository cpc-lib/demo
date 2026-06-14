package service

import (
	"context"
	"strings"
	"time"

	"payment-demo-go/internal/constant"
	"payment-demo-go/internal/model"
	"payment-demo-go/internal/util"
)

func (s *Service) CreateApplication(ctx context.Context, orderNo string, refundAmount *int, reason string) (*model.RefundInfo, error) {
	return s.CreateRefundApplication(orderNo, refundAmount, reason)
}

func (s *Service) Approve(ctx context.Context, refundNo, remark string) error {
	return s.Lock.Execute(ctx, "payment:refund:approve:"+refundNo, 5*time.Second, 30*time.Second, func() error {
		refund, err := s.GetRefundByRefundNo(refundNo)
		if err != nil {
			return err
		}
		if refund == nil {
			return util.Biz("退款申请单不存在")
		}
		if refund.ApprovalStatus == constant.RefundApprovalRejected {
			return util.Biz("退款申请单已拒绝，不能审核通过")
		}
		if refund.RefundStatus == constant.RefundStatusSuccess {
			return util.Biz("该退款申请单已退款成功，请勿重复处理")
		}
		if refund.RefundStatus == constant.RefundStatusProcessing {
			return util.Biz("该退款申请单已在退款处理中，请勿重复处理")
		}
		order, err := s.GetOrderByOrderNo(refund.OrderNo)
		if err != nil {
			return err
		}
		if order == nil {
			return util.Biz("订单不存在")
		}
		if err := validateSupportedPayType(order.PaymentType); err != nil {
			return err
		}
		if err := s.MarkApprovalPassed(refundNo, remark); err != nil {
			return err
		}
		if err := s.claimRefundForExecution(refundNo); err != nil {
			return err
		}
		latest, err := s.GetRefundByRefundNo(refundNo)
		if err != nil {
			return err
		}
		if err := s.executeRefund(ctx, order.PaymentType, latest); err != nil {
			_ = s.markRefundSubmitFailed(refundNo, err)
			return err
		}
		if err := s.scheduleRefundAutoQuery(ctx, refundNo, 1); err != nil {
			return util.Biz("投递退款自动查询消息失败: " + err.Error())
		}
		return nil
	})
}

func (s *Service) Reject(ctx context.Context, refundNo, remark string) error {
	return s.Lock.Execute(ctx, "payment:refund:reject:"+refundNo, 5*time.Second, 30*time.Second, func() error {
		return s.MarkApprovalRejected(refundNo, remark)
	})
}

func (s *Service) QueryRefundStatus(ctx context.Context, refundNo string) (*model.RefundInfo, error) {
	v, err := s.Lock.ExecuteValue(ctx, "payment:refund:query:"+refundNo, 5*time.Second, 30*time.Second, func() (interface{}, error) {
		refund, err := s.GetRefundByRefundNo(refundNo)
		if err != nil {
			return nil, err
		}
		if refund == nil {
			return nil, util.Biz("退款申请单不存在")
		}
		if refund.ApprovalStatus != constant.RefundApprovalApproved {
			return nil, util.Biz("退款申请尚未提交支付渠道，不能主动查询渠道状态")
		}
		order, err := s.GetOrderByOrderNo(refund.OrderNo)
		if err != nil {
			return nil, err
		}
		if order == nil {
			return nil, util.Biz("订单不存在")
		}
		if err := validateSupportedPayType(order.PaymentType); err != nil {
			return nil, err
		}
		if err := s.syncRefundByChannel(ctx, order.PaymentType, refund); err != nil {
			return nil, err
		}
		return s.GetRefundByRefundNo(refundNo)
	})
	if err != nil {
		return nil, err
	}
	return v.(*model.RefundInfo), nil
}

func (s *Service) ReconcileOrderRefundStatus(ctx context.Context, orderNo string) ([]model.RefundInfo, error) {
	v, err := s.Lock.ExecuteValue(ctx, "payment:refund:reconcile:"+orderNo, 5*time.Second, 30*time.Second, func() (interface{}, error) {
		order, err := s.GetOrderByOrderNo(orderNo)
		if err != nil {
			return nil, err
		}
		if order == nil {
			return nil, util.Biz("订单不存在")
		}
		if err := validateSupportedPayType(order.PaymentType); err != nil {
			return nil, err
		}
		channelRefundNos, err := s.reconcileWxOrderRefundsIfNeeded(ctx, order)
		if err != nil {
			return nil, err
		}
		if err := s.syncLocalRefundsForOrder(ctx, order, channelRefundNos); err != nil {
			return nil, err
		}
		if err := s.RefreshOrderRefundStatus(orderNo); err != nil {
			return nil, err
		}
		return s.ListRefundByOrderNo(orderNo)
	})
	if err != nil {
		return nil, err
	}
	return v.([]model.RefundInfo), nil
}

func (s *Service) claimRefundForExecution(refundNo string) error {
	ok, err := s.UpdateRefundIfStatusIn(refundNo, "", constant.RefundStatusProcessing, "", "", []string{constant.RefundStatusCreated, constant.RefundStatusFailed, constant.RefundStatusAbnormal})
	if err != nil {
		return err
	}
	if !ok {
		return util.Biz("该退款申请单状态已变化，请勿重复处理")
	}
	return nil
}

func (s *Service) executeRefund(ctx context.Context, paymentType string, refund *model.RefundInfo) error {
	if paymentType == constant.PayTypeWxPay {
		return s.WxExecuteRefund(ctx, refund)
	}
	if paymentType == constant.PayTypeAliPay {
		return s.AliExecuteRefund(ctx, refund)
	}
	return util.Biz("不支持的支付方式：" + paymentType)
}

func validateSupportedPayType(paymentType string) error {
	if paymentType != constant.PayTypeWxPay && paymentType != constant.PayTypeAliPay {
		return util.Biz("不支持的支付方式：" + paymentType)
	}
	return nil
}

func (s *Service) markRefundSubmitFailed(refundNo string, exception error) error {
	msg := "退款提交失败"
	if exception != nil && strings.TrimSpace(exception.Error()) != "" {
		msg = exception.Error()
	}
	_, err := s.UpdateRefundIfStatusIn(refundNo, "", constant.RefundStatusFailed, util.ToJSON(map[string]string{"message": msg}), "", []string{constant.RefundStatusCreated, constant.RefundStatusProcessing})
	return err
}

func (s *Service) queryRefundStatusFromChannel(ctx context.Context, paymentType, refundNo string) (model.RefundStatusSyncResult, error) {
	if paymentType == constant.PayTypeWxPay {
		return s.WxQueryRefundStatusForSync(ctx, refundNo)
	}
	if paymentType == constant.PayTypeAliPay {
		return s.AliQueryRefundStatusForSync(ctx, refundNo)
	}
	return model.RefundStatusSyncResult{}, util.Biz("不支持的支付方式：" + paymentType)
}

func (s *Service) reconcileWxOrderRefundsIfNeeded(ctx context.Context, order *model.OrderInfo) (map[string]bool, error) {
	channelRefundNos := map[string]bool{}
	if order.PaymentType != constant.PayTypeWxPay {
		return channelRefundNos, nil
	}
	results, err := s.WxQueryOrderRefundsForSync(ctx, order.OrderNo)
	if err != nil {
		return nil, err
	}
	for _, r := range results {
		if r.OrderNo != "" && r.OrderNo != order.OrderNo {
			return nil, util.Biz("渠道退款查询结果与本地订单不一致")
		}
		if _, err := s.RepairRefundFromChannel(r); err != nil {
			return nil, err
		}
		if _, err := s.SyncRefundStatus(r); err != nil {
			return nil, err
		}
		if r.RefundNo != "" {
			channelRefundNos[r.RefundNo] = true
		}
	}
	return channelRefundNos, nil
}

func (s *Service) syncLocalRefundsForOrder(ctx context.Context, order *model.OrderInfo, channelRefundNos map[string]bool) error {
	list, err := s.ListRefundByOrderNo(order.OrderNo)
	if err != nil {
		return err
	}
	for i := range list {
		r := list[i]
		if r.RefundNo == "" || r.ApprovalStatus != constant.RefundApprovalApproved {
			continue
		}
		if channelRefundNos[r.RefundNo] {
			continue
		}
		if err := s.syncRefundByChannel(ctx, order.PaymentType, &r); err != nil {
			return err
		}
	}
	return nil
}

func (s *Service) syncRefundByChannel(ctx context.Context, paymentType string, refund *model.RefundInfo) error {
	sync, err := s.queryRefundStatusFromChannel(ctx, paymentType, refund.RefundNo)
	if err != nil {
		return err
	}
	if sync.RefundNo != "" && sync.RefundNo != refund.RefundNo {
		return util.Biz("渠道退款查询结果与本地退款单不一致")
	}
	if sync.OrderNo != "" && sync.OrderNo != refund.OrderNo {
		return util.Biz("渠道退款查询结果与本地订单不一致")
	}
	if _, err := s.RepairRefundFromChannel(sync); err != nil {
		return err
	}
	_, err = s.SyncRefundStatus(sync)
	return err
}
