package service

import (
	"context"
	"encoding/json"
	"path/filepath"
	"strings"
	"testing"

	"payment-demo-go/internal/config"
	"payment-demo-go/internal/constant"
	"payment-demo-go/internal/model"
)

func TestBuildWxPayConfigFromApp_CurrentFallbackAndOverrideContract(t *testing.T) {
	base := config.WxPayConfig{
		MchID:          "base-mch",
		MchSerialNo:    "base-serial",
		PrivateKeyPath: "config/base.pem",
		ApiV3Key:       "base-v3",
		AppID:          "base-app",
		Domain:         "https://base.example",
		NotifyDomain:   "https://base-notify.example",
		PartnerKey:     "base-partner",
	}
	app := model.PaymentApp{
		PaymentType: constant.PayTypeWxPay,
		AppConfig: `{
			"mchId":"db-mch",
			"appid":"db-app",
			"domain":"https://db.example",
			"notifyDomain":"https://db-notify.example"
		}`,
	}

	got, err := buildWxPayConfigFromApp(base, app)
	if err != nil {
		t.Fatalf("buildWxPayConfigFromApp error = %v", err)
	}

	if got.MchID != "db-mch" || got.AppID != "db-app" || got.Domain != "https://db.example" || got.NotifyDomain != "https://db-notify.example" {
		t.Fatalf("db overrides not applied: %#v", got)
	}
	if got.MchSerialNo != "base-serial" || got.PrivateKeyPath != "config/base.pem" || got.ApiV3Key != "base-v3" || got.PartnerKey != "base-partner" {
		t.Fatalf("fallback fields not preserved: %#v", got)
	}
}

func TestBuildAliPayConfigFromApp_CurrentFallbackAndOverrideContract(t *testing.T) {
	base := config.AliPayConfig{
		AppID:              "base-app",
		SellerID:           "base-seller",
		GatewayURL:         "https://base.example",
		MerchantPrivateKey: "base-private",
		AlipayPublicKey:    "base-public",
		ContentKey:         "base-content",
		ReturnURL:          "https://base-return.example",
		NotifyURL:          "https://base-notify.example",
	}
	app := model.PaymentApp{
		PaymentType: constant.PayTypeAliPay,
		AppConfig: `{
			"appId":"db-app",
			"sellerId":"db-seller",
			"notifyUrl":"https://db-notify.example"
		}`,
	}

	got, err := buildAliPayConfigFromApp(base, app)
	if err != nil {
		t.Fatalf("buildAliPayConfigFromApp error = %v", err)
	}

	if got.AppID != "db-app" || got.SellerID != "db-seller" || got.NotifyURL != "https://db-notify.example" {
		t.Fatalf("db overrides not applied: %#v", got)
	}
	if got.GatewayURL != "https://base.example" || got.MerchantPrivateKey != "base-private" || got.AlipayPublicKey != "base-public" || got.ContentKey != "base-content" || got.ReturnURL != "https://base-return.example" {
		t.Fatalf("fallback fields not preserved: %#v", got)
	}
}

func TestPaymentAppQuery_CurrentBehaviorRejectsNegativePaymentAppID(t *testing.T) {
	id, err := ParsePaymentAppIDValue("-1")
	if err == nil {
		t.Fatalf("expected error for negative paymentAppId")
	}
	if id != 0 {
		t.Fatalf("id = %d, want 0", id)
	}
}

func TestPaymentAppQuery_RequiredPaymentAppIDRejectsBlank(t *testing.T) {
	id, err := ParseRequiredPaymentAppIDValue("")
	if err == nil {
		t.Fatalf("expected error for blank paymentAppId")
	}
	if id != 0 {
		t.Fatalf("id = %d, want 0", id)
	}
	if err.Error() != "paymentAppId不能为空" {
		t.Fatalf("error = %q, want paymentAppId不能为空", err.Error())
	}
}

func TestCreateOrReuseOrderWithApp_RequiresPaymentAppIDBeforeExternalState(t *testing.T) {
	s := &Service{}

	order, err := s.CreateOrReuseOrderWithApp(context.Background(), 1, constant.PayTypeWxPay, 0)

	if err == nil {
		t.Fatalf("expected missing paymentAppId error")
	}
	if order != nil {
		t.Fatalf("order = %#v, want nil", order)
	}
	if !strings.Contains(err.Error(), "paymentAppId不能为空") {
		t.Fatalf("error = %q, want paymentAppId不能为空", err.Error())
	}
}

func TestBuildWxPayConfigFromChannelAndApp_MergesDefaultChannelThenApp(t *testing.T) {
	base := config.WxPayConfig{
		MchID:          "base-mch",
		PrivateKeyPath: "config/base.pem",
		AppID:          "base-app",
		Domain:         "https://base.example",
		NotifyDomain:   "https://base-notify.example",
		PartnerKey:     "base-partner",
	}
	channel := &model.PaymentChannel{
		ConfigParams: `{"domain":"https://channel.example","notifyDomain":"https://channel-notify.example","mchId":"channel-mch","partnerKey":"channel-partner"}`,
	}
	app := model.PaymentApp{
		AppConfig: `{"mchId":"app-mch","appid":"app-id","privateKeyPath":"app.pem"}`,
	}

	got, err := buildWxPayConfigFromChannelAndApp(base, channel, app)
	if err != nil {
		t.Fatalf("buildWxPayConfigFromChannelAndApp error = %v", err)
	}

	if got.Domain != "https://channel.example" || got.NotifyDomain != "https://channel-notify.example" {
		t.Fatalf("channel overrides not applied: %#v", got)
	}
	if got.MchID != "app-mch" || got.AppID != "app-id" {
		t.Fatalf("app overrides not applied last: %#v", got)
	}
	if got.PartnerKey != "channel-partner" {
		t.Fatalf("channel fallback field = %q, want channel-partner", got.PartnerKey)
	}
	if got.PrivateKeyPath != filepath.Clean("config/app.pem") {
		t.Fatalf("PrivateKeyPath = %q, want %s", got.PrivateKeyPath, filepath.Clean("config/app.pem"))
	}
}

func TestBuildAliPayConfigFromChannelAndApp_MergesDefaultChannelThenApp(t *testing.T) {
	base := config.AliPayConfig{
		AppID:      "base-app",
		SellerID:   "base-seller",
		GatewayURL: "https://base.example",
		NotifyURL:  "https://base-notify.example",
		ReturnURL:  "https://base-return.example",
	}
	channel := &model.PaymentChannel{
		ConfigParams: `{"gatewayUrl":"https://channel.example","notifyUrl":"https://channel-notify.example","sellerId":"channel-seller"}`,
	}
	app := model.PaymentApp{
		AppConfig: `{"appId":"app-id","sellerId":"app-seller"}`,
	}

	got, err := buildAliPayConfigFromChannelAndApp(base, channel, app)
	if err != nil {
		t.Fatalf("buildAliPayConfigFromChannelAndApp error = %v", err)
	}

	if got.GatewayURL != "https://channel.example" || got.NotifyURL != "https://channel-notify.example" {
		t.Fatalf("channel overrides not applied: %#v", got)
	}
	if got.AppID != "app-id" || got.SellerID != "app-seller" {
		t.Fatalf("app overrides not applied last: %#v", got)
	}
	if got.ReturnURL != "https://base-return.example" {
		t.Fatalf("base fallback field = %q, want https://base-return.example", got.ReturnURL)
	}
}

func TestValidateOrderPaymentAppBindingRejectsMismatch(t *testing.T) {
	appID := int64(1)
	order := &model.OrderInfo{OrderNo: "ORD1", PaymentAppID: &appID}

	err := validateOrderPaymentAppBinding(order, 2, "支付通知")

	if err == nil {
		t.Fatalf("expected mismatch error")
	}
	if !strings.Contains(err.Error(), "支付应用不匹配") {
		t.Fatalf("error = %q, want payment app mismatch", err.Error())
	}
}

func TestValidatePaymentAppUsableForNewOrderRejectsDisabledAppOrChannel(t *testing.T) {
	channel := &model.PaymentChannel{Enabled: true}
	app := &model.PaymentApp{Enabled: false}

	if err := validatePaymentAppUsableForNewOrder(app, channel); err == nil {
		t.Fatalf("expected disabled app to be rejected")
	}

	app.Enabled = true
	channel.Enabled = false
	if err := validatePaymentAppUsableForNewOrder(app, channel); err == nil {
		t.Fatalf("expected disabled channel to be rejected")
	}
}

func TestBuildPaymentApp_CurrentBehaviorNormalizesStringJSONAndDefaultsEnabled(t *testing.T) {
	raw := json.RawMessage(`"{\"appId\":\"db-app\"}"`)
	req := model.PaymentAppRequest{
		AppCode:     "app",
		AppName:     "app name",
		ChannelCode: "wxpay",
		PaymentType: constant.PayTypeWxPay,
		AppConfig:   &raw,
	}

	app, err := buildPaymentApp(req, nil)
	if err != nil {
		t.Fatalf("buildPaymentApp error = %v", err)
	}

	if !app.Enabled {
		t.Fatalf("Enabled = false, want default true")
	}
	if app.AppConfig != `{"appId":"db-app"}` {
		t.Fatalf("AppConfig = %q, want compact JSON", app.AppConfig)
	}
}

func TestBuildPaymentChannel_CurrentBehaviorRejectsInvalidJSONConfig(t *testing.T) {
	raw := json.RawMessage(`{"domain":`)
	req := model.PaymentChannelRequest{
		ChannelCode:  "wxpay",
		ChannelName:  "wxpay",
		PaymentType:  constant.PayTypeWxPay,
		ConfigParams: &raw,
	}

	if _, err := buildPaymentChannel(req, nil); err == nil {
		t.Fatalf("expected invalid JSON config to be rejected")
	}
}
