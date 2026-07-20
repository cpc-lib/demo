package service

import (
	"context"

	"payment-demo-go/internal/constant"
	"payment-demo-go/internal/model"
	"payment-demo-go/internal/util"

	"gorm.io/gorm"
)

type MatchResult struct {
	TotalCount   int
	MatchedCount int
	DiffCount    int
}

func (s *Service) getLocalRecordsForReconciliation(ctx context.Context, task *model.ReconciliationTask, billType string) ([]model.ChannelBillRecord, error) {
	var records []model.ChannelBillRecord
	if billType == constant.ReconciliationBillTypeTrade {
		orders, err := s.getLocalOrdersForReconciliation(task)
		if err != nil {
			return nil, err
		}
		for _, o := range orders {
			records = append(records, model.ChannelBillRecord{
				DetailType:    constant.ReconciliationDetailTypeOrder,
				OrderNo:       o.OrderNo,
				TransactionID: "",
				Amount:        o.TotalFee,
				Status:        o.OrderStatus,
			})
		}
		payments, err := s.getLocalPaymentsForReconciliation(task)
		if err != nil {
			return nil, err
		}
		paymentMap := make(map[string]model.PaymentInfo)
		for _, p := range payments {
			paymentMap[p.OrderNo] = p
		}
		for i := range records {
			if p, ok := paymentMap[records[i].OrderNo]; ok {
				records[i].TransactionID = p.TransactionID
				if p.PayerTotal != nil && *p.PayerTotal > 0 {
					records[i].Amount = *p.PayerTotal
				}
			}
		}
	} else {
		refunds, err := s.getLocalRefundsForReconciliation(task)
		if err != nil {
			return nil, err
		}
		for _, r := range refunds {
			records = append(records, model.ChannelBillRecord{
				DetailType:    constant.ReconciliationDetailTypeRefund,
				OrderNo:       r.OrderNo,
				RefundNo:      r.RefundNo,
				TransactionID: r.RefundID,
				Amount:        r.Refund,
				Status:        r.RefundStatus,
			})
		}
	}
	return records, nil
}

func (s *Service) getLocalOrdersForReconciliation(task *model.ReconciliationTask) ([]model.OrderInfo, error) {
	var orders []model.OrderInfo
	query := s.DB.Where("payment_type=? AND order_status=?", task.PaymentType, constant.OrderStatusSuccess)
	if task.PaymentAppID != nil && *task.PaymentAppID > 0 {
		query = query.Where("payment_app_id=?", *task.PaymentAppID)
	}
	billDate := task.BillDate
	startDate := billDate + " 00:00:00"
	endDate := billDate + " 23:59:59"
	query = query.Where("update_time BETWEEN ? AND ?", startDate, endDate)
	err := query.Find(&orders).Error
	return orders, err
}

func (s *Service) getLocalPaymentsForReconciliation(task *model.ReconciliationTask) ([]model.PaymentInfo, error) {
	var payments []model.PaymentInfo
	query := s.DB.Where("payment_type=?", task.PaymentType)
	err := query.Find(&payments).Error
	return payments, err
}

func (s *Service) getLocalRefundsForReconciliation(task *model.ReconciliationTask) ([]model.RefundInfo, error) {
	var refunds []model.RefundInfo
	billDate := task.BillDate
	startDate := billDate + " 00:00:00"
	endDate := billDate + " 23:59:59"
	err := s.DB.Where("update_time BETWEEN ? AND ?", startDate, endDate).
		Where("approval_status=?", constant.RefundApprovalApproved).
		Find(&refunds).Error
	return refunds, err
}

func (s *Service) matchReconciliationRecords(ctx context.Context, taskID int64, detailType string, localRecords, channelRecords []model.ChannelBillRecord) (*MatchResult, error) {
	result := &MatchResult{}
	channelMap := make(map[string]model.ChannelBillRecord)
	for _, cr := range channelRecords {
		key := buildReconciliationKey(cr)
		if key != "" {
			channelMap[key] = cr
		}
	}
	matchedKeys := make(map[string]bool)
	for _, lr := range localRecords {
		key := buildReconciliationKey(lr)
		if key == "" {
			continue
		}
		result.TotalCount++
		cr, ok := channelMap[key]
		if !ok {
			result.DiffCount++
			_ = s.saveReconciliationDiff(taskID, detailType, constant.ReconciliationDiffTypeLocalOnly, lr, model.ChannelBillRecord{})
			continue
		}
		matchedKeys[key] = true
		if lr.Amount != cr.Amount {
			result.DiffCount++
			_ = s.saveReconciliationDiff(taskID, detailType, constant.ReconciliationDiffTypeAmountMismatch, lr, cr)
			continue
		}
		statusDiff := !isStatusMatched(lr.Status, cr.Status, detailType)
		if statusDiff {
			result.DiffCount++
			_ = s.saveReconciliationDiff(taskID, detailType, constant.ReconciliationDiffTypeStatusMismatch, lr, cr)
			continue
		}
		result.MatchedCount++
		_ = s.saveReconciliationDetail(taskID, detailType, constant.ReconciliationMatchStatusMatched, "", lr, cr)
	}
	for key, cr := range channelMap {
		if !matchedKeys[key] {
			result.TotalCount++
			result.DiffCount++
			_ = s.saveReconciliationDiff(taskID, detailType, constant.ReconciliationDiffTypeChannelOnly, model.ChannelBillRecord{}, cr)
		}
	}
	return result, nil
}

func buildReconciliationKey(r model.ChannelBillRecord) string {
	if r.RefundNo != "" {
		return "refund:" + r.RefundNo
	}
	if r.OrderNo != "" {
		return "order:" + r.OrderNo
	}
	return ""
}

func isStatusMatched(localStatus, channelStatus, detailType string) bool {
	if detailType == constant.ReconciliationDetailTypeOrder {
		localSuccess := localStatus == constant.OrderStatusSuccess ||
			localStatus == constant.OrderStatusPartialRefund ||
			localStatus == constant.OrderStatusRefundSuccess ||
			localStatus == constant.OrderStatusRefundProcessing ||
			localStatus == constant.OrderStatusRefundAbnormal
		channelSuccess := channelStatus == constant.WxTradeStateSuccess ||
			channelStatus == constant.AliPayTradeSuccess ||
			channelStatus == "SUCCESS" ||
			channelStatus == "TRADE_SUCCESS"
		return localSuccess == channelSuccess
	}
	localSuccess := localStatus == constant.RefundStatusSuccess
	channelSuccess := channelStatus == constant.WxRefundStatusSuccess ||
		channelStatus == constant.AliPayRefundSuccess ||
		channelStatus == "SUCCESS" ||
		channelStatus == "REFUND_SUCCESS"
	return localSuccess == channelSuccess
}

func (s *Service) saveReconciliationDetail(taskID int64, detailType, matchStatus, diffType string, local, channel model.ChannelBillRecord) error {
	detail := &model.ReconciliationDetail{
		TaskID:         taskID,
		DetailType:     detailType,
		OrderNo:        local.OrderNo,
		RefundNo:       local.RefundNo,
		TransactionID:  local.TransactionID,
		ChannelTradeNo: channel.ChannelTradeNo,
		MatchStatus:    matchStatus,
		DiffType:       diffType,
	}
	if local.Amount > 0 {
		detail.LocalAmount = &local.Amount
		detail.LocalStatus = local.Status
	}
	if channel.Amount > 0 {
		detail.ChannelAmount = &channel.Amount
		detail.ChannelStatus = channel.Status
	}
	return s.DB.Create(detail).Error
}

func (s *Service) saveReconciliationDiff(taskID int64, detailType, diffType string, local, channel model.ChannelBillRecord) error {
	return s.DB.Transaction(func(tx *gorm.DB) error {
		detail := &model.ReconciliationDetail{
			TaskID:         taskID,
			DetailType:     detailType,
			OrderNo:        firstNonBlankStr(local.OrderNo, channel.OrderNo),
			RefundNo:       firstNonBlankStr(local.RefundNo, channel.RefundNo),
			TransactionID:  local.TransactionID,
			ChannelTradeNo: channel.ChannelTradeNo,
			MatchStatus:    constant.ReconciliationMatchStatusDiff,
			DiffType:       diffType,
		}
		if local.Amount > 0 {
			detail.LocalAmount = &local.Amount
			detail.LocalStatus = local.Status
		}
		if channel.Amount > 0 {
			detail.ChannelAmount = &channel.Amount
			detail.ChannelStatus = channel.Status
		}
		if err := tx.Create(detail).Error; err != nil {
			return err
		}
		diff := &model.ReconciliationDiff{
			TaskID:       taskID,
			DetailID:     int64(uint64(detail.ID)),
			DiffType:     diffType,
			DetailType:   detailType,
			OrderNo:      detail.OrderNo,
			RefundNo:     detail.RefundNo,
			HandleStatus: constant.ReconciliationHandleStatusPending,
		}
		if local.RawData != "" {
			diff.LocalData = local.RawData
		}
		if channel.RawData != "" {
			diff.ChannelData = channel.RawData
		}
		return tx.Create(diff).Error
	})
}

func firstNonBlankStr(a, b string) string {
	if a != "" {
		return a
	}
	return b
}

func (s *Service) ListReconciliationDiffs(ctx context.Context, taskID int64, diffType, handleStatus string, page, pageSize int) ([]model.ReconciliationDiff, int64, error) {
	if page <= 0 {
		page = 1
	}
	if pageSize <= 0 || pageSize > 100 {
		pageSize = 20
	}
	var list []model.ReconciliationDiff
	var total int64
	query := s.DB.Model(&model.ReconciliationDiff{}).Where("task_id=?", taskID)
	if diffType != "" {
		query = query.Where("diff_type=?", diffType)
	}
	if handleStatus != "" {
		query = query.Where("handle_status=?", handleStatus)
	}
	if err := query.Count(&total).Error; err != nil {
		return nil, 0, err
	}
	err := query.Order("create_time desc").Offset((page - 1) * pageSize).Limit(pageSize).Find(&list).Error
	return list, total, err
}

var _ = util.Biz
