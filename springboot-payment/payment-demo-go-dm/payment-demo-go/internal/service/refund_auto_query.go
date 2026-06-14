package service

import (
	"context"
	"log"
	"strings"

	"payment-demo-go/internal/constant"
	"payment-demo-go/internal/model"
)

const refundQueryMaxAttempts = 5

func (s *Service) HandleRefundQueryMessage(ctx context.Context, msg model.RefundQueryMessage) error {
	refundNo := strings.TrimSpace(msg.RefundNo)
	if refundNo == "" {
		return nil
	}
	attempt := msg.Attempt
	if attempt <= 0 {
		attempt = 1
	}
	refund, err := s.GetRefundByRefundNo(refundNo)
	if err != nil {
		return s.retryRefundAutoQuery(ctx, refundNo, attempt, err)
	}
	if refund == nil {
		return nil
	}
	if refund.ApprovalStatus != constant.RefundApprovalApproved {
		return nil
	}
	if !isRefundAutoQueryPending(refund.RefundStatus) {
		return nil
	}
	latest, err := s.QueryRefundStatus(ctx, refundNo)
	if err != nil {
		return s.retryRefundAutoQuery(ctx, refundNo, attempt, err)
	}
	if latest != nil && shouldRetryRefundAutoQuery(latest.RefundStatus, attempt) {
		return s.scheduleRefundAutoQuery(ctx, refundNo, attempt+1)
	}
	return nil
}

func (s *Service) retryRefundAutoQuery(ctx context.Context, refundNo string, attempt int, queryErr error) error {
	if attempt >= refundQueryMaxAttempts {
		log.Printf("退款自动查询达到最大次数，refundNo=%s, attempt=%d, err=%v", refundNo, attempt, queryErr)
		return nil
	}
	log.Printf("退款自动查询失败，将延迟重试，refundNo=%s, attempt=%d, err=%v", refundNo, attempt, queryErr)
	return s.scheduleRefundAutoQuery(ctx, refundNo, attempt+1)
}

func (s *Service) scheduleRefundAutoQuery(ctx context.Context, refundNo string, attempt int) error {
	if s.MQ == nil {
		return nil
	}
	return s.MQ.SendRefundQueryMessage(ctx, refundNo, attempt)
}

func shouldRetryRefundAutoQuery(status string, attempt int) bool {
	return attempt < refundQueryMaxAttempts && isRefundAutoQueryPending(status)
}

func isRefundAutoQueryPending(status string) bool {
	return status == constant.RefundStatusCreated || status == constant.RefundStatusProcessing
}
