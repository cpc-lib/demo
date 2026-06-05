package service

import (
	"context"
	"strconv"
	"strings"
	"time"

	"payment-demo-go/internal/constant"
	"payment-demo-go/internal/model"
	"payment-demo-go/internal/util"

	"gorm.io/gorm"
	"gorm.io/gorm/clause"
)

func (s *Service) CreateOrReuseOrder(ctx context.Context, productID int64, paymentType string) (*model.OrderInfo, error) {
	if productID <= 0 {
		return nil, util.Biz("商品ID不能为空")
	}
	if strings.TrimSpace(paymentType) == "" {
		return nil, util.Biz("支付方式不能为空")
	}
	lockKey := "payment:order:create:" + i64(productID) + ":" + paymentType
	v, err := s.Lock.ExecuteValue(ctx, lockKey, 3*time.Second, 10*time.Second, func() (interface{}, error) {
		var order *model.OrderInfo
		created := false
		err := s.DB.Transaction(func(tx *gorm.DB) error {
			var noPay model.OrderInfo
			err := tx.Clauses(clause.Locking{Strength: "UPDATE"}).Where("product_id=? and payment_type=? and order_status=?", productID, paymentType, constant.OrderStatusNotPay).Order("create_time desc,id desc").First(&noPay).Error
			if err == nil {
				order = &noPay
				return nil
			}
			if err != gorm.ErrRecordNotFound {
				return err
			}
			var product model.Product
			if err := tx.First(&product, productID).Error; err != nil {
				if err == gorm.ErrRecordNotFound {
					return util.Biz("商品不存在")
				}
				return err
			}
			newOrder := &model.OrderInfo{Title: product.Title, OrderNo: util.OrderNo(), ProductID: productID, TotalFee: product.Price, OrderStatus: constant.OrderStatusNotPay, PaymentType: paymentType, Version: 0}
			if err := tx.Create(newOrder).Error; err != nil {
				return err
			}
			order = newOrder
			created = true
			return nil
		})
		if err != nil {
			return nil, err
		}
		if created && s.MQ != nil {
			_ = s.MQ.SendCloseOrderMessage(ctx, order.OrderNo, order.PaymentType)
		}
		return order, nil
	})
	if err != nil {
		return nil, err
	}
	return v.(*model.OrderInfo), nil
}

func (s *Service) SaveCodeURL(orderNo, codeURL string) error {
	if strings.TrimSpace(orderNo) == "" || strings.TrimSpace(codeURL) == "" {
		return nil
	}
	return s.DB.Model(&model.OrderInfo{}).Where("order_no=? AND (code_url IS NULL OR code_url='')", orderNo).Update("code_url", codeURL).Error
}

func (s *Service) ListOrders() ([]model.OrderInfo, error) {
	var list []model.OrderInfo
	err := s.DB.Order("create_time desc").Find(&list).Error
	return list, err
}

func (s *Service) UpdateOrderStatus(orderNo, status string) error {
	if strings.TrimSpace(orderNo) == "" || strings.TrimSpace(status) == "" {
		return nil
	}
	return s.DB.Model(&model.OrderInfo{}).Where("order_no=?", orderNo).Update("order_status", status).Error
}

func (s *Service) UpdateOrderStatusIf(orderNo, current, target string) (bool, error) {
	return s.updateOrderStatusIfDB(s.DB, orderNo, current, target)
}

func (s *Service) updateOrderStatusIfDB(db *gorm.DB, orderNo, current, target string) (bool, error) {
	if strings.TrimSpace(orderNo) == "" || current == "" || target == "" {
		return false, nil
	}
	res := db.Model(&model.OrderInfo{}).Where("order_no=? AND order_status=?", orderNo, current).Update("order_status", target)
	return res.RowsAffected > 0, res.Error
}

func (s *Service) GetOrderStatus(orderNo string) (string, error) {
	order, err := s.GetOrderByOrderNo(orderNo)
	if err != nil || order == nil {
		return "", err
	}
	return order.OrderStatus, nil
}

func (s *Service) GetOrderByOrderNo(orderNo string) (*model.OrderInfo, error) {
	return s.getOrderByOrderNoDB(s.DB, orderNo)
}

func (s *Service) getOrderByOrderNoDB(db *gorm.DB, orderNo string) (*model.OrderInfo, error) {
	if strings.TrimSpace(orderNo) == "" {
		return nil, nil
	}
	var order model.OrderInfo
	err := db.Where("order_no=?", orderNo).First(&order).Error
	if err == gorm.ErrRecordNotFound {
		return nil, nil
	}
	return &order, err
}

func (s *Service) getOrderByOrderNoForUpdateDB(db *gorm.DB, orderNo string) (*model.OrderInfo, error) {
	if strings.TrimSpace(orderNo) == "" {
		return nil, nil
	}
	var order model.OrderInfo
	err := db.Clauses(clause.Locking{Strength: "UPDATE"}).Where("order_no=?", orderNo).First(&order).Error
	if err == gorm.ErrRecordNotFound {
		return nil, nil
	}
	return &order, err
}

func i64(v int64) string              { return strconvFormatInt(v) }
func strconvFormatInt(v int64) string { return strconv.FormatInt(v, 10) }
