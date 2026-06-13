package sql_test

import (
	"os"
	"path/filepath"
	"strings"
	"testing"
)

func readPaymentDemoSQL(t *testing.T) string {
	t.Helper()
	body, err := os.ReadFile(filepath.Join(".", "payment_demo.sql"))
	if err != nil {
		t.Fatal(err)
	}
	return string(body)
}

func TestPaymentDemoSQL_Issue2DefinesPaymentChannelAndAppTables(t *testing.T) {
	script := readPaymentDemoSQL(t)

	required := []string{
		"CREATE TABLE `t_payment_channel`",
		"`channel_code` varchar(50)",
		"`config_params` text",
		"UNIQUE KEY `uk_channel_code` (`channel_code`)",
		"CREATE TABLE `t_payment_app`",
		"`app_code` varchar(50)",
		"`channel_code` varchar(50)",
		"`app_config` text",
		"UNIQUE KEY `uk_app_code` (`app_code`)",
	}

	for _, needle := range required {
		if !strings.Contains(script, needle) {
			t.Fatalf("payment_demo.sql does not contain %q", needle)
		}
	}
}

func TestPaymentDemoSQL_Issue2SeedsWxPayAndAliPayAppConfig(t *testing.T) {
	script := readPaymentDemoSQL(t)

	required := []string{
		"'wxpay'",
		"'微信支付'",
		"'wxpay-default'",
		`"appid":"wx74862e0dfcf69954"`,
		`"mchId":"1558950191"`,
		`"mchSerialNo":"34345964330B66427E0D3D28826C4993C77E631F"`,
		"'alipay'",
		"'支付宝'",
		"'alipay-sandbox'",
		`"appId":"9021000136667568"`,
		`"sellerId":"2088721034748965"`,
		`"gatewayUrl":"https://openapi-sandbox.dl.alipaydev.com/gateway.do"`,
	}

	for _, needle := range required {
		if !strings.Contains(script, needle) {
			t.Fatalf("payment_demo.sql does not contain seed value %q", needle)
		}
	}
}

func TestPaymentDemoSQL_PaymentConfigAddsPaymentAppIDToOrders(t *testing.T) {
	script := readPaymentDemoSQL(t)

	required := []string{
		"`payment_app_id` bigint(20)",
		"KEY `idx_payment_app_status` (`payment_app_id`, `order_status`)",
	}

	for _, needle := range required {
		if !strings.Contains(script, needle) {
			t.Fatalf("payment_demo.sql does not contain order app field %q", needle)
		}
	}
}
