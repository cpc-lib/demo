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
		"CREATE TABLE t_payment_channel",
		"channel_code VARCHAR(50)",
		"config_params CLOB",
		"CREATE UNIQUE INDEX uk_channel_code ON t_payment_channel(channel_code)",
		"CREATE TABLE t_payment_app",
		"app_code VARCHAR(50)",
		"channel_code VARCHAR(50)",
		"app_config CLOB",
		"CREATE UNIQUE INDEX uk_app_code ON t_payment_app(app_code)",
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
		"payment_app_id BIGINT",
		"CREATE INDEX idx_payment_app_status ON t_order_info(payment_app_id, order_status)",
		"id BIGINT IDENTITY(1,1) NOT NULL",
	}

	for _, needle := range required {
		if !strings.Contains(script, needle) {
			t.Fatalf("payment_demo.sql does not contain order app field %q", needle)
		}
	}
}

func TestPaymentDemoSQL_CurrentBehaviorCreatesAndSelectsPaymentSchema(t *testing.T) {
	script := readPaymentDemoSQL(t)

	required := []string{
		"FROM SYSOBJECTS",
		"TYPE$ = 'SCH'",
		"EXECUTE IMMEDIATE 'CREATE SCHEMA PAYMENT_DEMO AUTHORIZATION SYSDBA'",
		"SET SCHEMA PAYMENT_DEMO",
	}

	for _, needle := range required {
		if !strings.Contains(script, needle) {
			t.Fatalf("payment_demo.sql does not contain schema bootstrap %q", needle)
		}
	}

	schemaIndex := strings.Index(script, "SET SCHEMA PAYMENT_DEMO")
	tableIndex := strings.Index(script, "CREATE TABLE t_order_info")
	if schemaIndex < 0 || tableIndex < 0 || schemaIndex > tableIndex {
		t.Fatalf("payment_demo.sql must select PAYMENT_DEMO schema before creating tables")
	}
}

func TestPaymentDemoSQL_CurrentBehaviorDoesNotUseMySQLOnlySyntax(t *testing.T) {
	script := readPaymentDemoSQL(t)

	forbidden := []string{
		"`",
		"AUTO_INCREMENT",
		"ENGINE =",
		"CHARACTER SET",
		"COLLATE",
		"ROW_FORMAT",
		"SET FOREIGN_KEY_CHECKS",
		"ON UPDATE CURRENT_TIMESTAMP",
		"bigint(20)",
		"int(11)",
		"tinyint(1)",
	}

	for _, needle := range forbidden {
		if strings.Contains(script, needle) {
			t.Fatalf("payment_demo.sql still contains MySQL-only syntax %q", needle)
		}
	}
}
