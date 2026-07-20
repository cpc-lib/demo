package service

import (
	"context"
	"fmt"
	"strings"
	"time"

	"payment-demo-go/internal/constant"
	"payment-demo-go/internal/model"
	"payment-demo-go/internal/util"

	"gorm.io/gorm"
)

const (
	reconciliationLockKeyPrefix     = "payment:reconciliation:lock:"
	reconciliationProgressKeyPrefix = "payment:reconciliation:progress:"
)

func (s *Service) CreateReconciliationTask(ctx context.Context, req model.ReconciliationTaskRequest) (*model.ReconciliationTask, error) {
	if err := validateReconciliationTaskRequest(req); err != nil {
		return nil, err
	}
	if req.PaymentAppID != nil && *req.PaymentAppID > 0 {
		if _, err := s.resolvePaymentAppByID(ctx, req.PaymentType, *req.PaymentAppID, true); err != nil {
			return nil, err
		}
	}
	var task *model.ReconciliationTask
	err := s.DB.Transaction(func(tx *gorm.DB) error {
		existing, err := s.getReconciliationTaskByUnique(tx, req.PaymentType, req.PaymentAppID, req.BillDate, req.BillType)
		if err != nil {
			return err
		}
		if existing != nil {
			task = existing
			return nil
		}
		task = &model.ReconciliationTask{
			TaskNo:        util.ReconciliationTaskNo(),
			PaymentType:   req.PaymentType,
			PaymentAppID:  req.PaymentAppID,
			BillDate:      req.BillDate,
			BillType:      req.BillType,
			TaskStatus:    constant.ReconciliationTaskStatusPending,
			TriggerSource: constant.ReconciliationTriggerManual,
			Remark:        req.Remark,
		}
		if err := tx.Create(task).Error; err != nil {
			if isDuplicate(err) {
				existing, findErr := s.getReconciliationTaskByUnique(tx, req.PaymentType, req.PaymentAppID, req.BillDate, req.BillType)
				if findErr != nil {
					return findErr
				}
				if existing != nil {
					task = existing
					return nil
				}
			}
			return err
		}
		return nil
	})
	return task, err
}

func validateReconciliationTaskRequest(req model.ReconciliationTaskRequest) error {
	if strings.TrimSpace(req.PaymentType) == "" {
		return util.Biz("支付方式不能为空")
	}
	if req.PaymentType != constant.PayTypeWxPay && req.PaymentType != constant.PayTypeAliPay {
		return util.Biz("不支持的支付方式: " + req.PaymentType)
	}
	if strings.TrimSpace(req.BillDate) == "" {
		return util.Biz("账单日期不能为空")
	}
	if _, err := time.ParseInLocation("2006-01-02", req.BillDate, time.Local); err != nil {
		return util.Biz("账单日期格式无效，应为 YYYY-MM-DD")
	}
	if strings.TrimSpace(req.BillType) == "" {
		req.BillType = constant.ReconciliationBillTypeAll
	}
	if req.BillType != constant.ReconciliationBillTypeTrade &&
		req.BillType != constant.ReconciliationBillTypeRefund &&
		req.BillType != constant.ReconciliationBillTypeAll {
		return util.Biz("不支持的账单类型: " + req.BillType)
	}
	return nil
}

func (s *Service) getReconciliationTaskByUnique(tx *gorm.DB, paymentType string, paymentAppID *int64, billDate, billType string) (*model.ReconciliationTask, error) {
	var task model.ReconciliationTask
	query := tx.Where("payment_type=? AND bill_date=? AND bill_type=?", paymentType, billDate, billType)
	if paymentAppID != nil && *paymentAppID > 0 {
		query = query.Where("payment_app_id=?", *paymentAppID)
	} else {
		query = query.Where("payment_app_id IS NULL OR payment_app_id=0")
	}
	err := query.First(&task).Error
	if err == gorm.ErrRecordNotFound {
		return nil, nil
	}
	return &task, err
}

func (s *Service) GetReconciliationTask(ctx context.Context, taskID int64) (*model.ReconciliationTask, error) {
	var task model.ReconciliationTask
	err := s.DB.Where("id=?", taskID).First(&task).Error
	if err == gorm.ErrRecordNotFound {
		return nil, util.Biz("对账任务不存在")
	}
	return &task, err
}

func (s *Service) ListReconciliationTasks(ctx context.Context, paymentType string, paymentAppID *int64, billDate, status string, page, pageSize int) ([]model.ReconciliationTask, int64, error) {
	if page <= 0 {
		page = 1
	}
	if pageSize <= 0 || pageSize > 100 {
		pageSize = 20
	}
	var list []model.ReconciliationTask
	var total int64
	query := s.DB.Model(&model.ReconciliationTask{})
	if strings.TrimSpace(paymentType) != "" {
		query = query.Where("payment_type=?", paymentType)
	}
	if paymentAppID != nil && *paymentAppID > 0 {
		query = query.Where("payment_app_id=?", *paymentAppID)
	}
	if strings.TrimSpace(billDate) != "" {
		query = query.Where("bill_date=?", billDate)
	}
	if strings.TrimSpace(status) != "" {
		query = query.Where("task_status=?", status)
	}
	if err := query.Count(&total).Error; err != nil {
		return nil, 0, err
	}
	err := query.Order("create_time desc").Offset((page - 1) * pageSize).Limit(pageSize).Find(&list).Error
	return list, total, err
}

func (s *Service) ExecuteReconciliationTask(ctx context.Context, taskID int64, trigger string) error {
	lockKey := reconciliationLockKeyPrefix + fmt.Sprintf("%d", taskID)
	lockTTL := s.Cfg.Reconciliation.LockTTL
	if lockTTL <= 0 {
		lockTTL = 30 * time.Minute
	}
	return s.Lock.Execute(ctx, lockKey, 3*time.Second, lockTTL, func() error {
		task, err := s.GetReconciliationTask(ctx, taskID)
		if err != nil {
			return err
		}
		if err := s.updateReconciliationTaskStatus(taskID, task.TaskStatus, constant.ReconciliationTaskStatusProcessing, ""); err != nil {
			return err
		}
		if strings.TrimSpace(trigger) == "" {
			trigger = constant.ReconciliationTriggerManual
		}
		runErr := s.runReconciliation(ctx, task)
		if runErr != nil {
			_ = s.updateReconciliationTaskStatus(taskID, constant.ReconciliationTaskStatusProcessing, constant.ReconciliationTaskStatusFailed, runErr.Error())
			return runErr
		}
		return nil
	})
}

func (s *Service) runReconciliation(ctx context.Context, task *model.ReconciliationTask) error {
	taskID := int64(uint64(task.ID))
	if err := s.clearReconciliationDetails(ctx, taskID); err != nil {
		return err
	}
	billTypes := resolveBillTypes(task.BillType)
	totalCount := 0
	matchedCount := 0
	diffCount := 0
	hasWarning := false

	for _, bt := range billTypes {
		channelRecords, err := s.downloadAndParseChannelBill(ctx, task, bt)
		if err != nil {
			hasWarning = true
			continue
		}
		localRecords, err := s.getLocalRecordsForReconciliation(ctx, task, bt)
		if err != nil {
			hasWarning = true
			continue
		}
		result, err := s.matchReconciliationRecords(ctx, taskID, bt, localRecords, channelRecords)
		if err != nil {
			hasWarning = true
			continue
		}
		totalCount += result.TotalCount
		matchedCount += result.MatchedCount
		diffCount += result.DiffCount
	}

	targetStatus := constant.ReconciliationTaskStatusCompleted
	if hasWarning {
		targetStatus = constant.ReconciliationTaskStatusCompletedWithWarning
	}
	_ = s.updateReconciliationTaskStats(taskID, targetStatus, totalCount, matchedCount, diffCount)
	return nil
}

func resolveBillTypes(billType string) []string {
	switch billType {
	case constant.ReconciliationBillTypeTrade:
		return []string{constant.ReconciliationBillTypeTrade}
	case constant.ReconciliationBillTypeRefund:
		return []string{constant.ReconciliationBillTypeRefund}
	default:
		return []string{constant.ReconciliationBillTypeTrade, constant.ReconciliationBillTypeRefund}
	}
}

func (s *Service) updateReconciliationTaskStatus(taskID int64, current, target, errorMsg string) error {
	values := map[string]interface{}{"task_status": target}
	if errorMsg != "" {
		values["error_msg"] = errorMsg
	}
	query := s.DB.Model(&model.ReconciliationTask{}).Where("id=?", taskID)
	if current != "" {
		query = query.Where("task_status=?", current)
	}
	return query.Updates(values).Error
}

func (s *Service) updateReconciliationTaskStats(taskID int64, status string, total, matched, diff int) error {
	return s.DB.Model(&model.ReconciliationTask{}).Where("id=?", taskID).Updates(map[string]interface{}{
		"task_status":   status,
		"total_count":   total,
		"matched_count": matched,
		"diff_count":    diff,
	}).Error
}

func (s *Service) clearReconciliationDetails(ctx context.Context, taskID int64) error {
	if err := s.DB.Where("task_id=?", taskID).Delete(&model.ReconciliationDiff{}).Error; err != nil {
		return err
	}
	return s.DB.Where("task_id=?", taskID).Delete(&model.ReconciliationDetail{}).Error
}

func (s *Service) GetReconciliationSummary(ctx context.Context, billDate string) (*model.ReconciliationSummary, error) {
	if strings.TrimSpace(billDate) == "" {
		billDate = time.Now().Format("2006-01-02")
	}
	summary := &model.ReconciliationSummary{BillDate: billDate}
	var tasks []model.ReconciliationTask
	if err := s.DB.Where("bill_date=?", billDate).Find(&tasks).Error; err != nil {
		return nil, err
	}
	taskIDs := make([]int64, 0, len(tasks))
	paymentTypes := []string{constant.PayTypeWxPay, constant.PayTypeAliPay}
	for _, pt := range paymentTypes {
		stat := model.ReconciliationChannelStat{PaymentType: pt, TaskStatus: constant.ReconciliationTaskStatusPending}
		for _, t := range tasks {
			if t.PaymentType == pt {
				stat.TaskID = int64(uint64(t.ID))
				stat.TaskStatus = t.TaskStatus
				stat.DiffCount = t.DiffCount
				taskIDs = append(taskIDs, stat.TaskID)
				break
			}
		}
		summary.ChannelStats = append(summary.ChannelStats, stat)
	}
	var pendingCount, handledCount int64
	if len(taskIDs) > 0 {
		s.DB.Model(&model.ReconciliationDiff{}).Where("task_id IN ? AND handle_status=?", taskIDs, constant.ReconciliationHandleStatusPending).Count(&pendingCount)
		s.DB.Model(&model.ReconciliationDiff{}).Where("task_id IN ? AND handle_status IN ?", taskIDs, []string{constant.ReconciliationHandleStatusHandled, constant.ReconciliationHandleStatusIgnored, constant.ReconciliationHandleStatusResolved}).Count(&handledCount)
	}
	summary.TotalPendingDiff = int(pendingCount)
	summary.TotalHandledDiff = int(handledCount)
	return summary, nil
}

func (s *Service) HandleReconciliationTaskMessage(ctx context.Context, msg model.ReconciliationTaskMessage) error {
	if msg.TaskID <= 0 {
		return nil
	}
	return s.ExecuteReconciliationTask(ctx, msg.TaskID, msg.Trigger)
}
