package service

import (
	"reflect"
	"testing"

	"payment-demo-go/internal/constant"
	"payment-demo-go/internal/model"
)

func TestBuildReconciliationKey_RefundNoTakesPrecedenceOverOrderNo(t *testing.T) {
	r := model.ChannelBillRecord{OrderNo: "ORD001", RefundNo: "RFD001"}
	if got := buildReconciliationKey(r); got != "refund:RFD001" {
		t.Fatalf("buildReconciliationKey with refund no = %q, want %q", got, "refund:RFD001")
	}
}

func TestBuildReconciliationKey_OrderNoUsedWhenRefundNoAbsent(t *testing.T) {
	r := model.ChannelBillRecord{OrderNo: "ORD001"}
	if got := buildReconciliationKey(r); got != "order:ORD001" {
		t.Fatalf("buildReconciliationKey without refund no = %q, want %q", got, "order:ORD001")
	}
}

func TestBuildReconciliationKey_EmptyWhenBothAbsent(t *testing.T) {
	r := model.ChannelBillRecord{}
	if got := buildReconciliationKey(r); got != "" {
		t.Fatalf("buildReconciliationKey with empty record = %q, want empty", got)
	}
}

func TestIsStatusMatched_OrderType_SuccessStatesMatch(t *testing.T) {
	cases := []struct {
		localStatus   string
		channelStatus string
	}{
		{constant.OrderStatusSuccess, constant.WxTradeStateSuccess},
		{constant.OrderStatusSuccess, constant.AliPayTradeSuccess},
		{constant.OrderStatusSuccess, "SUCCESS"},
		{constant.OrderStatusSuccess, "TRADE_SUCCESS"},
		{constant.OrderStatusPartialRefund, constant.WxTradeStateSuccess},
		{constant.OrderStatusRefundSuccess, constant.AliPayTradeSuccess},
	}
	for _, c := range cases {
		if !isStatusMatched(c.localStatus, c.channelStatus, constant.ReconciliationDetailTypeOrder) {
			t.Fatalf("isStatusMatched(order: %q vs %q) = false, want true", c.localStatus, c.channelStatus)
		}
	}
}

func TestIsStatusMatched_OrderType_FailedStatesMatch(t *testing.T) {
	cases := []struct {
		localStatus   string
		channelStatus string
	}{
		{constant.OrderStatusNotPay, constant.WxTradeStateNotPay},
		{constant.OrderStatusNotPay, "NOTPAY"},
		{constant.OrderStatusClosed, constant.WxTradeStateClosed},
		{constant.OrderStatusClosed, "CLOSED"},
	}
	for _, c := range cases {
		if !isStatusMatched(c.localStatus, c.channelStatus, constant.ReconciliationDetailTypeOrder) {
			t.Fatalf("isStatusMatched(order: %q vs %q) = false, want true", c.localStatus, c.channelStatus)
		}
	}
}

func TestIsStatusMatched_OrderType_MismatchWhenSuccessVsNotPay(t *testing.T) {
	if isStatusMatched(constant.OrderStatusSuccess, constant.WxTradeStateNotPay, constant.ReconciliationDetailTypeOrder) {
		t.Fatalf("isStatusMatched(order: SUCCESS vs NOTPAY) = true, want false")
	}
	if isStatusMatched(constant.OrderStatusNotPay, constant.WxTradeStateSuccess, constant.ReconciliationDetailTypeOrder) {
		t.Fatalf("isStatusMatched(order: NOTPAY vs SUCCESS) = true, want false")
	}
}

func TestIsStatusMatched_RefundType_SuccessStatesMatch(t *testing.T) {
	cases := []struct {
		localStatus   string
		channelStatus string
	}{
		{constant.RefundStatusSuccess, constant.WxRefundStatusSuccess},
		{constant.RefundStatusSuccess, constant.AliPayRefundSuccess},
		{constant.RefundStatusSuccess, "SUCCESS"},
		{constant.RefundStatusSuccess, "REFUND_SUCCESS"},
	}
	for _, c := range cases {
		if !isStatusMatched(c.localStatus, c.channelStatus, constant.ReconciliationDetailTypeRefund) {
			t.Fatalf("isStatusMatched(refund: %q vs %q) = false, want true", c.localStatus, c.channelStatus)
		}
	}
}

func TestIsStatusMatched_RefundType_MismatchWhenSuccessVsProcessing(t *testing.T) {
	if isStatusMatched(constant.RefundStatusSuccess, constant.WxRefundStatusProcessing, constant.ReconciliationDetailTypeRefund) {
		t.Fatalf("isStatusMatched(refund: SUCCESS vs PROCESSING) = true, want false")
	}
	if isStatusMatched(constant.RefundStatusProcessing, constant.WxRefundStatusSuccess, constant.ReconciliationDetailTypeRefund) {
		t.Fatalf("isStatusMatched(refund: PROCESSING vs SUCCESS) = true, want false")
	}
}

func TestResolveBillTypes_TradeOnly(t *testing.T) {
	got := resolveBillTypes(constant.ReconciliationBillTypeTrade)
	want := []string{constant.ReconciliationBillTypeTrade}
	if !reflect.DeepEqual(got, want) {
		t.Fatalf("resolveBillTypes(trade) = %#v, want %#v", got, want)
	}
}

func TestResolveBillTypes_RefundOnly(t *testing.T) {
	got := resolveBillTypes(constant.ReconciliationBillTypeRefund)
	want := []string{constant.ReconciliationBillTypeRefund}
	if !reflect.DeepEqual(got, want) {
		t.Fatalf("resolveBillTypes(refund) = %#v, want %#v", got, want)
	}
}

func TestResolveBillTypes_AllReturnsTradeAndRefund(t *testing.T) {
	got := resolveBillTypes(constant.ReconciliationBillTypeAll)
	want := []string{constant.ReconciliationBillTypeTrade, constant.ReconciliationBillTypeRefund}
	if !reflect.DeepEqual(got, want) {
		t.Fatalf("resolveBillTypes(all) = %#v, want %#v", got, want)
	}
}

func TestResolveBillTypes_EmptyDefaultsToAll(t *testing.T) {
	got := resolveBillTypes("")
	want := []string{constant.ReconciliationBillTypeTrade, constant.ReconciliationBillTypeRefund}
	if !reflect.DeepEqual(got, want) {
		t.Fatalf("resolveBillTypes('') = %#v, want %#v", got, want)
	}
}

func TestFirstNonBlankStr_PrimaryTakesPrecedence(t *testing.T) {
	if got := firstNonBlankStr("a", "b"); got != "a" {
		t.Fatalf("firstNonBlankStr(a, b) = %q, want %q", got, "a")
	}
}

func TestFirstNonBlankStr_FallbackWhenPrimaryEmpty(t *testing.T) {
	if got := firstNonBlankStr("", "b"); got != "b" {
		t.Fatalf("firstNonBlankStr('', b) = %q, want %q", got, "b")
	}
}

func TestFirstNonBlankStr_BothEmptyReturnsEmpty(t *testing.T) {
	if got := firstNonBlankStr("", ""); got != "" {
		t.Fatalf("firstNonBlankStr('', '') = %q, want empty", got)
	}
}

func TestParseWxBillContent_ExtractsOrderRecords(t *testing.T) {
	// trade 类型解析索引：5=TransactionID, 6=OrderNo, 9=Amount, 12=Status
	content := `交易时间,公众账号ID,特约商户号,特约商户AppID,设备号,微信订单号,商户订单号,用户标识,交易类型,订单金额,费率,手续费,交易状态,货币种类
2026-07-19 10:00:00,wx123,1234567890,wxappid001,device001,4200000000202607190001,ORD001,oUpF8uMuAJO_M2px,NATIVE,1.00,0.60%,0.00,SUCCESS,CNY
总交易单数,1
`
	records := parseWxBillContent(content, constant.ReconciliationBillTypeTrade)
	if len(records) != 1 {
		t.Fatalf("parseWxBillContent(trade) records = %d, want 1", len(records))
	}
	if records[0].OrderNo != "ORD001" {
		t.Fatalf("records[0].OrderNo = %q, want ORD001", records[0].OrderNo)
	}
	if records[0].Status != "SUCCESS" {
		t.Fatalf("records[0].Status = %q, want SUCCESS", records[0].Status)
	}
	if records[0].Amount != 100 {
		t.Fatalf("records[0].Amount = %d, want 100", records[0].Amount)
	}
	if records[0].DetailType != constant.ReconciliationDetailTypeOrder {
		t.Fatalf("records[0].DetailType = %q, want %q", records[0].DetailType, constant.ReconciliationDetailTypeOrder)
	}
}

func TestParseWxBillContent_SkipsSummaryLine(t *testing.T) {
	content := `交易时间,公众账号ID
总交易单数,1
`
	records := parseWxBillContent(content, constant.ReconciliationBillTypeTrade)
	if len(records) != 0 {
		t.Fatalf("parseWxBillContent with only summary = %d records, want 0", len(records))
	}
}

func TestParseAliBillContent_ExtractsOrderRecords(t *testing.T) {
	// trade 类型解析索引：1=ChannelTradeNo, 2=OrderNo, 5=Status, 6=Amount
	// header 行第一列包含"账务时间"才会被识别为表头
	content := "账务时间,支付宝交易号,商户订单号,业务类型,创建时间,交易状态,金额\n" +
		"2026-07-19 10:00:00,2026071922001400001,ORD001,交易,2026-07-19 10:00:00,TRADE_SUCCESS,1.00\n"
	records := parseAliBillContent(content, constant.ReconciliationBillTypeTrade)
	if len(records) == 0 {
		t.Fatalf("parseAliBillContent(trade) returned 0 records, want at least 1")
	}
	if records[0].OrderNo != "ORD001" {
		t.Fatalf("records[0].OrderNo = %q, want ORD001", records[0].OrderNo)
	}
	if records[0].DetailType != constant.ReconciliationDetailTypeOrder {
		t.Fatalf("records[0].DetailType = %q, want %q", records[0].DetailType, constant.ReconciliationDetailTypeOrder)
	}
}

func TestParseWxAmount_ParsesYuanToCents(t *testing.T) {
	cases := map[string]int{
		"1.00":    100,
		"0.01":    1,
		"100.00":  10000,
		"99.99":   9999,
	}
	for input, want := range cases {
		if got := parseWxAmount(input); got != want {
			t.Fatalf("parseWxAmount(%q) = %d, want %d", input, got, want)
		}
	}
}

func TestParseWxAmount_EmptyStringReturnsZero(t *testing.T) {
	if got := parseWxAmount(""); got != 0 {
		t.Fatalf("parseWxAmount('') = %d, want 0", got)
	}
}

func TestValidateReconciliationTaskRequest_RejectsEmptyPaymentType(t *testing.T) {
	req := model.ReconciliationTaskRequest{BillDate: "2026-07-19", BillType: "all"}
	if err := validateReconciliationTaskRequest(req); err == nil {
		t.Fatalf("validateReconciliationTaskRequest with empty paymentType should fail")
	}
}

func TestValidateReconciliationTaskRequest_RejectsUnsupportedPaymentType(t *testing.T) {
	req := model.ReconciliationTaskRequest{PaymentType: "银联", BillDate: "2026-07-19", BillType: "all"}
	if err := validateReconciliationTaskRequest(req); err == nil {
		t.Fatalf("validateReconciliationTaskRequest with unsupported paymentType should fail")
	}
}

func TestValidateReconciliationTaskRequest_RejectsEmptyBillDate(t *testing.T) {
	req := model.ReconciliationTaskRequest{PaymentType: constant.PayTypeWxPay, BillType: "all"}
	if err := validateReconciliationTaskRequest(req); err == nil {
		t.Fatalf("validateReconciliationTaskRequest with empty billDate should fail")
	}
}

func TestValidateReconciliationTaskRequest_RejectsInvalidBillDateFormat(t *testing.T) {
	cases := []string{
		"2026/07/19",
		"2026-7-19",
		"19-07-2026",
		"2026-07-19 12:00:00",
		"invalid",
	}
	for _, billDate := range cases {
		req := model.ReconciliationTaskRequest{PaymentType: constant.PayTypeWxPay, BillDate: billDate, BillType: "all"}
		if err := validateReconciliationTaskRequest(req); err == nil {
			t.Fatalf("validateReconciliationTaskRequest with billDate=%q should fail", billDate)
		}
	}
}

func TestValidateReconciliationTaskRequest_RejectsUnsupportedBillType(t *testing.T) {
	req := model.ReconciliationTaskRequest{PaymentType: constant.PayTypeWxPay, BillDate: "2026-07-19", BillType: "invalid"}
	if err := validateReconciliationTaskRequest(req); err == nil {
		t.Fatalf("validateReconciliationTaskRequest with unsupported billType should fail")
	}
}

func TestValidateReconciliationTaskRequest_AcceptsValidRequest(t *testing.T) {
	cases := []model.ReconciliationTaskRequest{
		{PaymentType: constant.PayTypeWxPay, BillDate: "2026-07-19", BillType: "trade"},
		{PaymentType: constant.PayTypeAliPay, BillDate: "2026-07-19", BillType: "refund"},
		{PaymentType: constant.PayTypeWxPay, BillDate: "2026-07-19", BillType: "all"},
	}
	for _, req := range cases {
		if err := validateReconciliationTaskRequest(req); err != nil {
			t.Fatalf("validateReconciliationTaskRequest(%+v) failed: %v", req, err)
		}
	}
}
