package service

import (
	"context"
	"encoding/csv"
	"io"
	"strings"

	"payment-demo-go/internal/constant"
	"payment-demo-go/internal/model"
	"payment-demo-go/internal/util"
)

func (s *Service) downloadAndParseChannelBill(ctx context.Context, task *model.ReconciliationTask, billType string) ([]model.ChannelBillRecord, error) {
	if task.PaymentType == constant.PayTypeWxPay {
		return s.downloadAndParseWxBill(ctx, task, billType)
	}
	if task.PaymentType == constant.PayTypeAliPay {
		return s.downloadAndParseAliBill(ctx, task, billType)
	}
	return nil, util.Biz("不支持的支付方式: " + task.PaymentType)
}

func (s *Service) downloadAndParseWxBill(ctx context.Context, task *model.ReconciliationTask, billType string) ([]model.ChannelBillRecord, error) {
	billDateForWx := strings.ReplaceAll(task.BillDate, "-", "")
	wxBillType := "ALL"
	if billType == constant.ReconciliationBillTypeRefund {
		wxBillType = "REFUND"
	}
	typ := "tradebill"
	if billType == constant.ReconciliationBillTypeRefund {
		typ = "fundflowbill"
	}
	paymentAppID := int64(0)
	if task.PaymentAppID != nil {
		paymentAppID = *task.PaymentAppID
	}
	content, err := s.WxDownloadBill(ctx, billDateForWx, typ, wxBillType, "", "", paymentAppID)
	if err != nil {
		return nil, util.Biz("下载微信账单失败: " + err.Error())
	}
	return parseWxBillContent(content, billType), nil
}

func parseWxBillContent(content, billType string) []model.ChannelBillRecord {
	var records []model.ChannelBillRecord
	lines := strings.Split(content, "\n")
	dataSection := false
	for _, line := range lines {
		line = strings.TrimSpace(line)
		if line == "" {
			continue
		}
		if strings.HasPrefix(line, "`") {
			line = strings.TrimPrefix(line, "`")
		}
		if strings.Contains(line, "交易时间") || strings.Contains(line, "入账时间") {
			dataSection = true
			continue
		}
		if !dataSection {
			continue
		}
		if strings.HasPrefix(line, "总") || strings.HasPrefix(line, "合") {
			break
		}
		record := parseWxBillLine(line, billType)
		if record.OrderNo != "" || record.RefundNo != "" {
			records = append(records, record)
		}
	}
	return records
}

func parseWxBillLine(line, billType string) model.ChannelBillRecord {
	fields := strings.Split(line, ",")
	record := model.ChannelBillRecord{RawData: line}
	if billType == constant.ReconciliationBillTypeRefund {
		for i, f := range fields {
			f = strings.Trim(f, "`")
			switch i {
			case 0:
			case 4:
				record.RefundNo = f
			case 5:
				record.OrderNo = f
			case 7:
				record.Amount = parseWxAmount(f)
			case 2:
				record.Status = f
			case 8:
				record.ChannelTradeNo = f
			}
		}
		record.DetailType = constant.ReconciliationDetailTypeRefund
	} else {
		for i, f := range fields {
			f = strings.Trim(f, "`")
			switch i {
			case 6:
				record.OrderNo = f
			case 5:
				record.TransactionID = f
			case 12:
				record.Status = f
			case 9:
				record.Amount = parseWxAmount(f)
			}
		}
		record.DetailType = constant.ReconciliationDetailTypeOrder
	}
	return record
}

func parseWxAmount(s string) int {
	s = strings.TrimSpace(s)
	if s == "" {
		return 0
	}
	amount, err := util.YuanToCents(s)
	if err != nil {
		return 0
	}
	return amount
}

func (s *Service) downloadAndParseAliBill(ctx context.Context, task *model.ReconciliationTask, billType string) ([]model.ChannelBillRecord, error) {
	billDateForAli := strings.ReplaceAll(task.BillDate, "-", "")
	aliBillType := "trade"
	if billType == constant.ReconciliationBillTypeRefund {
		aliBillType = "refund"
	}
	paymentAppID := int64(0)
	if task.PaymentAppID != nil {
		paymentAppID = *task.PaymentAppID
	}
	downloadURL, err := s.AliQueryBill(ctx, billDateForAli, aliBillType, paymentAppID)
	if err != nil {
		return nil, util.Biz("获取支付宝账单下载地址失败: " + err.Error())
	}
	content, err := s.downloadAliBill(ctx, downloadURL)
	if err != nil {
		return nil, util.Biz("下载支付宝账单失败: " + err.Error())
	}
	return parseAliBillContent(content, billType), nil
}

func (s *Service) downloadAliBill(ctx context.Context, url string) (string, error) {
	if strings.TrimSpace(url) == "" {
		return "", util.Biz("支付宝账单下载地址为空")
	}
	return s.AliPay.DownloadBill(ctx, url)
}

func parseAliBillContent(content, billType string) []model.ChannelBillRecord {
	var records []model.ChannelBillRecord
	reader := csv.NewReader(strings.NewReader(content))
	headerFound := false
	for {
		row, err := reader.Read()
		if err == io.EOF {
			break
		}
		if err != nil {
			continue
		}
		if len(row) == 0 {
			continue
		}
		first := strings.TrimSpace(row[0])
		if strings.Contains(first, "账务时间") || strings.Contains(first, "交易创建时间") || strings.Contains(first, "交易号") {
			headerFound = true
			continue
		}
		if !headerFound {
			continue
		}
		if first == "" || strings.HasPrefix(first, "#") {
			continue
		}
		record := parseAliBillRow(row, billType)
		if record.OrderNo != "" || record.RefundNo != "" {
			records = append(records, record)
		}
	}
	return records
}

func parseAliBillRow(row []string, billType string) model.ChannelBillRecord {
	record := model.ChannelBillRecord{RawData: strings.Join(row, ",")}
	if billType == constant.ReconciliationBillTypeRefund {
		for i, f := range row {
			f = strings.TrimSpace(f)
			switch i {
			case 2:
				record.ChannelTradeNo = f
			case 3:
				record.OrderNo = f
			case 4:
				record.RefundNo = f
			case 6:
				record.Amount = parseWxAmount(f)
			case 8:
				record.Status = f
			}
		}
		record.DetailType = constant.ReconciliationDetailTypeRefund
	} else {
		for i, f := range row {
			f = strings.TrimSpace(f)
			switch i {
			case 1:
				record.ChannelTradeNo = f
			case 2:
				record.OrderNo = f
			case 5:
				record.Status = f
			case 6:
				record.Amount = parseWxAmount(f)
			}
		}
		record.DetailType = constant.ReconciliationDetailTypeOrder
	}
	return record
}
