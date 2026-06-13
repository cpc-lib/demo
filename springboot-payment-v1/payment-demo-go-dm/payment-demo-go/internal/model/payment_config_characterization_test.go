package model

import "testing"

func TestIssue2PaymentChannelModelMapsToPaymentChannelTable(t *testing.T) {
	if got := (PaymentChannel{}).TableName(); got != "t_payment_channel" {
		t.Fatalf("PaymentChannel.TableName() = %q", got)
	}

	channel := PaymentChannel{
		ChannelCode:  "wxpay",
		ChannelName:  "微信支付",
		PaymentType:  "微信",
		Enabled:      true,
		ConfigParams: `{"domain":"https://api.mch.weixin.qq.com"}`,
	}

	if channel.ChannelCode != "wxpay" || !channel.Enabled || channel.ConfigParams == "" {
		t.Fatalf("PaymentChannel model did not retain config fields: %#v", channel)
	}
}

func TestIssue2PaymentAppModelMapsToPaymentAppTable(t *testing.T) {
	if got := (PaymentApp{}).TableName(); got != "t_payment_app" {
		t.Fatalf("PaymentApp.TableName() = %q", got)
	}

	app := PaymentApp{
		AppCode:     "alipay-sandbox",
		AppName:     "支付宝沙箱应用",
		ChannelCode: "alipay",
		PaymentType: "支付宝",
		Enabled:     true,
		AppConfig:   `{"appId":"9021000136667568"}`,
	}

	if app.AppCode != "alipay-sandbox" || app.ChannelCode != "alipay" || app.AppConfig == "" {
		t.Fatalf("PaymentApp model did not retain app config fields: %#v", app)
	}
}

func TestPaymentConfigOrderInfoKeepsPaymentAppID(t *testing.T) {
	appID := int64(12)
	order := OrderInfo{PaymentAppID: &appID}

	if order.PaymentAppID == nil || *order.PaymentAppID != 12 {
		t.Fatalf("OrderInfo.PaymentAppID = %#v, want 12", order.PaymentAppID)
	}
}
