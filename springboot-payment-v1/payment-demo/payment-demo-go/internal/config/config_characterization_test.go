package config

import (
	"os"
	"path/filepath"
	"testing"
	"time"
)

func TestLoad_CurrentBehaviorUsesEnvFilesAndDefaults(t *testing.T) {
	dir := t.TempDir()
	appPath := filepath.Join(dir, "application.yml")
	wxPath := filepath.Join(dir, "wxpay.properties")
	aliPath := filepath.Join(dir, "alipay-sandbox.properties")

	appYAML := []byte(`
server:
  port: 0
spring:
  rabbitmq:
    host: mq.local
    port: 5673
    username: guest-user
    password: guest-pass
  redis:
    host: redis.local
    port: 6380
    database: 2
    timeout: 1500ms
    password: redis-pass
  datasource:
    url: jdbc:mysql://mysql.local:3307/payment_demo_test?serverTimezone=GMT%2B8&characterEncoding=utf-8&useSSL=false
    username: db-user
    password: db-pass
payment:
  order:
    close-delay-ms: 0
`)
	if err := os.WriteFile(appPath, appYAML, 0600); err != nil {
		t.Fatal(err)
	}
	wxProps := []byte(`
wxpay.mch-id=mch-1
wxpay.mch-serial-no=serial-1
wxpay.private-key-path=apiclient_key.pem
wxpay.api-v3-key=api-v3-key
wxpay.appid=wx-app
wxpay.domain=https://wx.example.test
wxpay.notify-domain=https://notify.example.test
wxpay.partnerKey=partner-key
`)
	if err := os.WriteFile(wxPath, wxProps, 0600); err != nil {
		t.Fatal(err)
	}
	aliProps := []byte(`
alipay.appId=ali-app
alipay.sellerId=seller-1
alipay.gatewayUrl=https://ali.example.test
alipay.merchantPrivateKey=merchant-private
alipay.alipayPublicKey=ali-public
alipay.content-key=content-key
alipay.returnUrl=https://return.example.test
alipay.notifyUrl=https://notify.example.test/ali
`)
	if err := os.WriteFile(aliPath, aliProps, 0600); err != nil {
		t.Fatal(err)
	}

	t.Setenv("APP_CONFIG", appPath)
	t.Setenv("WXPAY_CONFIG", wxPath)
	t.Setenv("ALIPAY_CONFIG", aliPath)

	cfg, err := Load()
	if err != nil {
		t.Fatalf("Load() error = %v", err)
	}

	// 现状：server.port 为 0 时默认使用 8080。
	if cfg.ServerPort != 8080 {
		t.Fatalf("ServerPort = %d, want 8080", cfg.ServerPort)
	}
	if cfg.Redis.Addr != "redis.local:6380" {
		t.Fatalf("Redis.Addr = %q", cfg.Redis.Addr)
	}
	if cfg.Redis.DB != 2 || cfg.Redis.Password != "redis-pass" || cfg.Redis.Timeout != 1500*time.Millisecond {
		t.Fatalf("Redis config = %#v", cfg.Redis)
	}
	if cfg.RabbitMQ.URL != "amqp://guest-user:guest-pass@mq.local:5673/" {
		t.Fatalf("RabbitMQ.URL = %q", cfg.RabbitMQ.URL)
	}
	if cfg.MySQL.DSN != "db-user:db-pass@tcp(mysql.local:3307)/payment_demo_test?charset=utf8mb4&loc=Local&parseTime=True" {
		t.Fatalf("MySQL.DSN = %q", cfg.MySQL.DSN)
	}
	if cfg.Payment.OrderCloseDelay != time.Minute {
		t.Fatalf("OrderCloseDelay = %s, want 1m", cfg.Payment.OrderCloseDelay)
	}
	wantKeyPath := filepath.Join(dir, "apiclient_key.pem")
	if cfg.WxPay.PrivateKeyPath != wantKeyPath {
		t.Fatalf("WxPay.PrivateKeyPath = %q, want %q", cfg.WxPay.PrivateKeyPath, wantKeyPath)
	}
	if cfg.AliPay.AppID != "ali-app" || cfg.AliPay.NotifyURL != "https://notify.example.test/ali" {
		t.Fatalf("AliPay config = %#v", cfg.AliPay)
	}
}
