package service

import (
	"payment-demo-go/internal/constant"
	"payment-demo-go/internal/model"

	"gorm.io/gorm"
)

const refundAmountSumExpression = "COALESCE(sum(refund),0)"

func (s *Service) RefreshOrderRefundStatus(orderNo string) error {
	return s.DB.Transaction(func(tx *gorm.DB) error { return s.refreshOrderRefundStatusDB(tx, orderNo) })
}

func (s *Service) refreshOrderRefundStatusDB(tx *gorm.DB, orderNo string) error {
	order, err := s.getOrderByOrderNoForUpdateDB(tx, orderNo)
	if err != nil || order == nil {
		return err
	}
	success, err := s.sumRefundAmount(tx, orderNo, []string{constant.RefundStatusSuccess})
	if err != nil {
		return err
	}
	processing, err := s.sumRefundAmount(tx, orderNo, []string{constant.RefundStatusProcessing})
	if err != nil {
		return err
	}
	abnormal, err := s.sumRefundAmount(tx, orderNo, []string{constant.RefundStatusAbnormal})
	if err != nil {
		return err
	}
	if success >= order.TotalFee {
		_, err = s.updateOrderStatusIfDB(tx, orderNo, order.OrderStatus, constant.OrderStatusRefundSuccess)
		return err
	}
	if success > 0 {
		_, err = s.updateOrderStatusIfDB(tx, orderNo, order.OrderStatus, constant.OrderStatusPartialRefund)
		return err
	}
	if processing > 0 {
		_, err = s.updateOrderStatusIfDB(tx, orderNo, order.OrderStatus, constant.OrderStatusRefundProcessing)
		return err
	}
	if abnormal > 0 {
		_, err = s.updateOrderStatusIfDB(tx, orderNo, order.OrderStatus, constant.OrderStatusRefundAbnormal)
		return err
	}
	_, err = s.updateOrderStatusIfDB(tx, orderNo, order.OrderStatus, constant.OrderStatusSuccess)
	return err
}

func (s *Service) RefreshOrderRefundStatusByRefundNo(refundNo string) error {
	refund, err := s.GetRefundByRefundNo(refundNo)
	if err != nil || refund == nil {
		return err
	}
	return s.RefreshOrderRefundStatus(refund.OrderNo)
}

func (s *Service) sumRefundAmount(db *gorm.DB, orderNo string, statuses []string) (int, error) {
	var amount int
	err := db.Model(&model.RefundInfo{}).Select(refundAmountSumExpression).Where("order_no=? and refund_status in ?", orderNo, statuses).Scan(&amount).Error
	return amount, err
}
