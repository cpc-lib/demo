package service

import (
	"strings"
	"time"

	"payment-demo-go/internal/constant"
	"payment-demo-go/internal/model"
	"payment-demo-go/internal/types"
	"payment-demo-go/internal/util"

	"gorm.io/gorm"
	"gorm.io/gorm/clause"
)

func (s *Service) CreateRefundApplication(orderNo string, refundAmount *int, reason string) (*model.RefundInfo, error) {
	if strings.TrimSpace(orderNo) == "" {
		return nil, util.Biz("订单号不能为空")
	}
	var refund *model.RefundInfo
	err := s.DB.Transaction(func(tx *gorm.DB) error {
		order, err := s.getOrderByOrderNoForUpdateDB(tx, orderNo)
		if err != nil {
			return err
		}
		if order == nil {
			return util.Biz("订单不存在")
		}
		if order.TotalFee <= 0 {
			return util.Biz("订单金额非法")
		}
		if !(order.OrderStatus == constant.OrderStatusSuccess || order.OrderStatus == constant.OrderStatusPartialRefund || order.OrderStatus == constant.OrderStatusRefundProcessing) {
			return util.Biz("当前订单状态不允许申请退款：" + order.OrderStatus)
		}
		reserved, err := s.getReservedRefundAmountDB(tx, orderNo)
		if err != nil {
			return err
		}
		remain := order.TotalFee - reserved
		if remain <= 0 {
			return util.Biz("金额已经全部退还处理")
		}
		actual := remain
		if refundAmount != nil {
			actual = *refundAmount
		}
		if actual <= 0 {
			return util.Biz("退款金额必须大于0")
		}
		if actual > remain {
			return util.Bizf("退款申请金额超过可退余额，可退金额为：%d分", remain)
		}
		reason = strings.TrimSpace(reason)
		if reason == "" {
			reason = "正常退款"
		}
		r := &model.RefundInfo{OrderNo: orderNo, RefundNo: util.RefundNo(), TotalFee: order.TotalFee, Refund: actual, Reason: reason, ApprovalStatus: constant.RefundApprovalPending, RefundStatus: constant.RefundStatusCreated}
		if err := tx.Create(r).Error; err != nil {
			if isDuplicate(err) {
				return util.Biz("退款申请单重复提交，请勿重复操作")
			}
			return err
		}
		refund = r
		return nil
	})
	return refund, err
}

func (s *Service) getReservedRefundAmountDB(db *gorm.DB, orderNo string) (int, error) {
	var list []model.RefundInfo
	if err := db.Where("order_no=?", orderNo).Find(&list).Error; err != nil {
		return 0, err
	}
	total := 0
	for _, r := range list {
		reserved := r.ApprovalStatus == constant.RefundApprovalPending || (r.ApprovalStatus == constant.RefundApprovalApproved && r.RefundStatus != constant.RefundStatusFailed && r.RefundStatus != constant.RefundStatusClosed)
		if reserved {
			total += r.Refund
		}
	}
	return total, nil
}

func (s *Service) UpdateRefundToProcessing(refundNo, contentReturn string) error {
	_, err := s.UpdateRefundIfStatusIn(refundNo, "", constant.RefundStatusProcessing, contentReturn, "", []string{constant.RefundStatusCreated, constant.RefundStatusFailed, constant.RefundStatusAbnormal})
	return err
}
func (s *Service) UpdateRefundToSuccess(refundNo, refundID, content string) error {
	_, err := s.UpdateRefundIfStatusIn(refundNo, refundID, constant.RefundStatusSuccess, content, content, []string{constant.RefundStatusCreated, constant.RefundStatusProcessing, constant.RefundStatusFailed, constant.RefundStatusAbnormal})
	return err
}
func (s *Service) UpdateRefundToFailed(refundNo, content string) error {
	_, err := s.UpdateRefundIfStatusIn(refundNo, "", constant.RefundStatusFailed, content, "", []string{constant.RefundStatusCreated, constant.RefundStatusProcessing})
	return err
}

func (s *Service) UpdateRefundIfStatusIn(refundNo, refundID, targetStatus, contentReturn, contentNotify string, currentStatuses []string) (bool, error) {
	if strings.TrimSpace(refundNo) == "" {
		return false, util.Biz("退款单号不能为空")
	}
	if targetStatus == "" {
		return false, util.Biz("目标退款状态不能为空")
	}
	if len(currentStatuses) == 0 {
		return false, util.Biz("当前退款状态不能为空")
	}
	updated := false
	err := s.DB.Transaction(func(tx *gorm.DB) error {
		values := map[string]interface{}{"refund_status": targetStatus}
		if refundID != "" {
			values["refund_id"] = refundID
		}
		if contentReturn != "" {
			values["content_return"] = contentReturn
		}
		if contentNotify != "" {
			values["content_notify"] = contentNotify
		}
		res := tx.Model(&model.RefundInfo{}).Where("refund_no=? and refund_status in ?", refundNo, currentStatuses).Updates(values)
		if res.Error != nil {
			return res.Error
		}
		updated = res.RowsAffected > 0
		if updated {
			return s.refreshOrderRefundStatusByRefundNoDB(tx, refundNo)
		}
		return nil
	})
	return updated, err
}

func (s *Service) refreshOrderRefundStatusByRefundNoDB(tx *gorm.DB, refundNo string) error {
	var refund model.RefundInfo
	if err := tx.Where("refund_no=?", refundNo).First(&refund).Error; err != nil {
		if err == gorm.ErrRecordNotFound {
			return nil
		}
		return err
	}
	return s.refreshOrderRefundStatusDB(tx, refund.OrderNo)
}

func (s *Service) SyncRefundStatus(sync model.RefundStatusSyncResult) (bool, error) {
	if strings.TrimSpace(sync.RefundNo) == "" {
		return false, util.Biz("退款单号不能为空")
	}
	if !sync.HasRefundStatus() {
		return false, nil
	}
	updated := false
	err := s.DB.Transaction(func(tx *gorm.DB) error {
		locked, err := s.getRefundByRefundNoForUpdateDB(tx, sync.RefundNo)
		if err != nil || locked == nil {
			return err
		}
		if err := validateRefundSyncData(locked, sync); err != nil {
			return err
		}
		statuses := syncableStatuses(sync.RefundStatus)
		if len(statuses) == 0 {
			return nil
		}
		values := map[string]interface{}{"refund_status": sync.RefundStatus}
		if sync.RefundID != "" {
			values["refund_id"] = sync.RefundID
		}
		if sync.Content != "" {
			values["content_notify"] = sync.Content
		}
		res := tx.Model(&model.RefundInfo{}).Where("refund_no=? and refund_status in ?", sync.RefundNo, statuses).Updates(values)
		if res.Error != nil {
			return res.Error
		}
		updated = res.RowsAffected > 0
		return s.refreshOrderRefundStatusByRefundNoDB(tx, sync.RefundNo)
	})
	return updated, err
}

func (s *Service) RepairRefundFromChannel(sync model.RefundStatusSyncResult) (*model.RefundInfo, error) {
	if strings.TrimSpace(sync.RefundNo) == "" {
		return nil, util.Biz("渠道退款单号不能为空")
	}
	if strings.TrimSpace(sync.OrderNo) == "" {
		return nil, util.Biz("渠道退款数据缺少订单号")
	}
	refund, err := s.GetRefundByRefundNo(sync.RefundNo)
	if err != nil {
		return nil, err
	}
	if refund == nil {
		return s.createRefundFromChannel(sync)
	}
	if refund.OrderNo != "" && refund.OrderNo != sync.OrderNo {
		return nil, util.Biz("本地退款单订单号与渠道不一致，refundNo=" + sync.RefundNo)
	}
	values := map[string]interface{}{}
	if sync.OrderNo != "" && refund.OrderNo != sync.OrderNo {
		values["order_no"] = sync.OrderNo
	}
	if sync.RefundID != "" && refund.RefundID != sync.RefundID {
		values["refund_id"] = sync.RefundID
	}
	if sync.TotalFee != nil && *sync.TotalFee > 0 && refund.TotalFee != *sync.TotalFee {
		values["total_fee"] = *sync.TotalFee
	}
	if sync.RefundAmount != nil && *sync.RefundAmount > 0 && refund.Refund != *sync.RefundAmount {
		values["refund"] = *sync.RefundAmount
	}
	if len(values) > 0 {
		if err := s.DB.Model(&model.RefundInfo{}).Where("refund_no=?", sync.RefundNo).Updates(values).Error; err != nil {
			return nil, err
		}
	}
	return s.GetRefundByRefundNo(sync.RefundNo)
}

func (s *Service) createRefundFromChannel(sync model.RefundStatusSyncResult) (*model.RefundInfo, error) {
	order, err := s.GetOrderByOrderNo(sync.OrderNo)
	if err != nil {
		return nil, err
	}
	if order == nil {
		return nil, util.Biz("渠道退款对应订单不存在，orderNo=" + sync.OrderNo)
	}
	if sync.RefundAmount == nil || *sync.RefundAmount <= 0 {
		return nil, util.Biz("渠道退款缺少退款金额，无法补录退款单")
	}
	totalFee := order.TotalFee
	if sync.TotalFee != nil {
		totalFee = *sync.TotalFee
	}
	now := types.Now()
	refund := &model.RefundInfo{OrderNo: sync.OrderNo, RefundNo: sync.RefundNo, RefundID: sync.RefundID, TotalFee: totalFee, Refund: *sync.RefundAmount, Reason: "渠道对账补录", ApprovalStatus: constant.RefundApprovalApproved, ApproveRemark: "渠道对账补录", ApprovedTime: &now, RefundStatus: constant.RefundStatusCreated, ContentNotify: sync.Content}
	if err := s.DB.Create(refund).Error; err != nil {
		if isDuplicate(err) {
			return s.GetRefundByRefundNo(sync.RefundNo)
		}
		return nil, err
	}
	return refund, nil
}

func (s *Service) GetRefundByRefundNo(refundNo string) (*model.RefundInfo, error) {
	return s.getRefundByRefundNoDB(s.DB, refundNo)
}
func (s *Service) getRefundByRefundNoDB(db *gorm.DB, refundNo string) (*model.RefundInfo, error) {
	var r model.RefundInfo
	err := db.Where("refund_no=?", refundNo).First(&r).Error
	if err == gorm.ErrRecordNotFound {
		return nil, nil
	}
	return &r, err
}
func (s *Service) getRefundByRefundNoForUpdateDB(db *gorm.DB, refundNo string) (*model.RefundInfo, error) {
	var r model.RefundInfo
	err := db.Clauses(clause.Locking{Strength: "UPDATE"}).Where("refund_no=?", refundNo).First(&r).Error
	if err == gorm.ErrRecordNotFound {
		return nil, nil
	}
	return &r, err
}
func (s *Service) ListRefundByOrderNo(orderNo string) ([]model.RefundInfo, error) {
	var list []model.RefundInfo
	err := s.DB.Where("order_no=?", orderNo).Order("create_time desc").Find(&list).Error
	return list, err
}
func (s *Service) ListRefundAll() ([]model.RefundInfo, error) {
	var list []model.RefundInfo
	err := s.DB.Order("create_time desc").Find(&list).Error
	return list, err
}

func (s *Service) MarkApprovalPassed(refundNo, remark string) error {
	return s.DB.Transaction(func(tx *gorm.DB) error {
		if remark = strings.TrimSpace(remark); remark == "" {
			remark = "审核通过"
		}
		now := types.Now()
		res := tx.Model(&model.RefundInfo{}).Where("refund_no=? and approval_status=? and refund_status in ?", refundNo, constant.RefundApprovalPending, []string{constant.RefundStatusCreated, constant.RefundStatusFailed, constant.RefundStatusAbnormal}).Updates(map[string]interface{}{"approval_status": constant.RefundApprovalApproved, "approve_remark": remark, "approved_time": now.Time})
		if res.Error != nil {
			return res.Error
		}
		if res.RowsAffected > 0 {
			return nil
		}
		latest, err := s.getRefundByRefundNoDB(tx, refundNo)
		if err != nil {
			return err
		}
		if latest == nil {
			return util.Biz("退款申请单不存在")
		}
		if latest.ApprovalStatus == constant.RefundApprovalApproved {
			return nil
		}
		if latest.ApprovalStatus == constant.RefundApprovalRejected {
			return util.Biz("退款申请单已拒绝，不能再通过")
		}
		if latest.RefundStatus == constant.RefundStatusSuccess {
			return util.Biz("该退款申请单已退款成功，请勿重复处理")
		}
		if latest.RefundStatus == constant.RefundStatusProcessing {
			return util.Biz("该退款申请单已在退款处理中，请勿重复处理")
		}
		return util.Biz("退款申请单状态已变化，请刷新后重试")
	})
}

func (s *Service) MarkApprovalRejected(refundNo, remark string) error {
	return s.DB.Transaction(func(tx *gorm.DB) error {
		if remark = strings.TrimSpace(remark); remark == "" {
			remark = "审核拒绝"
		}
		now := types.Now()
		res := tx.Model(&model.RefundInfo{}).Where("refund_no=? and approval_status=?", refundNo, constant.RefundApprovalPending).Updates(map[string]interface{}{"approval_status": constant.RefundApprovalRejected, "approve_remark": remark, "approved_time": now.Time, "refund_status": constant.RefundStatusClosed})
		if res.Error != nil {
			return res.Error
		}
		if res.RowsAffected > 0 {
			return nil
		}
		latest, err := s.getRefundByRefundNoDB(tx, refundNo)
		if err != nil {
			return err
		}
		if latest == nil {
			return util.Biz("退款申请单不存在")
		}
		if latest.ApprovalStatus == constant.RefundApprovalRejected {
			return nil
		}
		if latest.ApprovalStatus == constant.RefundApprovalApproved {
			return util.Biz("退款申请单已审核通过，不能再拒绝")
		}
		return util.Biz("退款申请单状态已变化，请刷新后重试")
	})
}

func validateRefundSyncData(local *model.RefundInfo, sync model.RefundStatusSyncResult) error {
	if sync.OrderNo != "" && local.OrderNo != "" && sync.OrderNo != local.OrderNo {
		return util.Biz("退款通知订单号不一致，refundNo=" + sync.RefundNo)
	}
	if sync.RefundAmount != nil && local.Refund > 0 && *sync.RefundAmount > 0 && *sync.RefundAmount != local.Refund {
		return util.Biz("退款通知金额不一致，refundNo=" + sync.RefundNo)
	}
	if sync.TotalFee != nil && local.TotalFee > 0 && *sync.TotalFee > 0 && *sync.TotalFee != local.TotalFee {
		return util.Biz("退款通知原订单金额不一致，refundNo=" + sync.RefundNo)
	}
	return nil
}

func syncableStatuses(target string) []string {
	switch target {
	case constant.RefundStatusSuccess:
		return []string{constant.RefundStatusCreated, constant.RefundStatusProcessing, constant.RefundStatusFailed, constant.RefundStatusAbnormal}
	case constant.RefundStatusProcessing:
		return []string{constant.RefundStatusCreated, constant.RefundStatusFailed}
	case constant.RefundStatusAbnormal:
		return []string{constant.RefundStatusCreated, constant.RefundStatusProcessing, constant.RefundStatusFailed}
	case constant.RefundStatusClosed:
		return []string{constant.RefundStatusCreated, constant.RefundStatusProcessing, constant.RefundStatusFailed, constant.RefundStatusAbnormal}
	case constant.RefundStatusFailed:
		return []string{constant.RefundStatusCreated, constant.RefundStatusProcessing}
	default:
		return nil
	}
}

var _ = time.Now
