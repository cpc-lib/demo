package service

import (
	"context"

	"payment-demo-go/internal/constant"
	"payment-demo-go/internal/model"
)

func (s *Service) HandleOrderCloseMessage(ctx context.Context, msg model.OrderCloseMessage) error {
	order, err := s.GetOrderByOrderNo(msg.OrderNo)
	if err != nil || order == nil {
		return err
	}
	if order.OrderStatus != constant.OrderStatusNotPay {
		return nil
	}
	paymentType := msg.PaymentType
	if paymentType == "" {
		paymentType = order.PaymentType
	}
	if paymentType == constant.PayTypeWxPay {
		return s.WxCheckOrderStatus(ctx, msg.OrderNo)
	}
	if paymentType == constant.PayTypeAliPay {
		return s.AliCheckOrderStatus(ctx, msg.OrderNo)
	}
	return nil
}
