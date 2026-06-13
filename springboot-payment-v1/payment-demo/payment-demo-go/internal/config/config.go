package config

import (
	"bufio"
	"fmt"
	"net/url"
	"os"
	"path/filepath"
	"regexp"
	"strconv"
	"strings"
	"time"

	"gopkg.in/yaml.v3"
)

type Config struct {
	ServerPort int
	MySQL      MySQLConfig
	Redis      RedisConfig
	RabbitMQ   RabbitMQConfig
	Payment    PaymentConfig
	WxPay      WxPayConfig
	AliPay     AliPayConfig
}

type MySQLConfig struct {
	DSN      string
	URL      string
	Username string
	Password string
}

type RedisConfig struct {
	Addr     string
	Password string
	DB       int
	Timeout  time.Duration
}

type RabbitMQConfig struct {
	URL      string
	Host     string
	Port     int
	Username string
	Password string
}

type PaymentConfig struct{ OrderCloseDelay time.Duration }

type WxPayConfig struct {
	MchID          string
	MchSerialNo    string
	PrivateKeyPath string
	ApiV3Key       string
	AppID          string
	Domain         string
	NotifyDomain   string
	PartnerKey     string
}

type AliPayConfig struct {
	AppID              string
	SellerID           string
	GatewayURL         string
	MerchantPrivateKey string
	AlipayPublicKey    string
	ContentKey         string
	ReturnURL          string
	NotifyURL          string
}

type rawAppConfig struct {
	Server struct {
		Port int `yaml:"port"`
	} `yaml:"server"`
	Spring struct {
		RabbitMQ struct {
			Host     string `yaml:"host"`
			Port     int    `yaml:"port"`
			Username string `yaml:"username"`
			Password string `yaml:"password"`
		} `yaml:"rabbitmq"`
		Redis struct {
			Host     string `yaml:"host"`
			Port     int    `yaml:"port"`
			Database int    `yaml:"database"`
			Timeout  string `yaml:"timeout"`
			Password string `yaml:"password"`
		} `yaml:"redis"`
		Datasource struct {
			URL      string `yaml:"url"`
			Username string `yaml:"username"`
			Password string `yaml:"password"`
		} `yaml:"datasource"`
	} `yaml:"spring"`
	Payment struct {
		Order struct {
			CloseDelayMS int64 `yaml:"close-delay-ms"`
		} `yaml:"order"`
	} `yaml:"payment"`
}

func Load() (*Config, error) {
	appPath := envOrDefault("APP_CONFIG", "config/application.yml")
	wxPath := envOrDefault("WXPAY_CONFIG", "config/wxpay.properties")
	aliPath := envOrDefault("ALIPAY_CONFIG", "config/alipay-sandbox.properties")

	data, err := os.ReadFile(appPath)
	if err != nil {
		return nil, fmt.Errorf("读取 application.yml 失败: %w", err)
	}
	var raw rawAppConfig
	if err := yaml.Unmarshal(data, &raw); err != nil {
		return nil, fmt.Errorf("解析 application.yml 失败: %w", err)
	}

	wxProps, err := readProperties(wxPath)
	if err != nil {
		return nil, fmt.Errorf("读取 wxpay.properties 失败: %w", err)
	}
	aliProps, err := readProperties(aliPath)
	if err != nil {
		return nil, fmt.Errorf("读取 alipay-sandbox.properties 失败: %w", err)
	}

	redisTimeout := 3 * time.Second
	if raw.Spring.Redis.Timeout != "" {
		redisTimeout = parseDuration(raw.Spring.Redis.Timeout, redisTimeout)
	}

	closeDelay := time.Duration(raw.Payment.Order.CloseDelayMS) * time.Millisecond
	if closeDelay <= 0 {
		closeDelay = time.Minute
	}

	privateKeyPath := wxProps["wxpay.private-key-path"]
	if privateKeyPath != "" && !filepath.IsAbs(privateKeyPath) {
		privateKeyPath = filepath.Join(filepath.Dir(wxPath), privateKeyPath)
	}

	cfg := &Config{
		ServerPort: raw.Server.Port,
		MySQL: MySQLConfig{
			URL:      raw.Spring.Datasource.URL,
			Username: raw.Spring.Datasource.Username,
			Password: raw.Spring.Datasource.Password,
		},
		Redis: RedisConfig{
			Addr:     fmt.Sprintf("%s:%d", raw.Spring.Redis.Host, raw.Spring.Redis.Port),
			Password: raw.Spring.Redis.Password,
			DB:       raw.Spring.Redis.Database,
			Timeout:  redisTimeout,
		},
		RabbitMQ: RabbitMQConfig{
			Host:     raw.Spring.RabbitMQ.Host,
			Port:     raw.Spring.RabbitMQ.Port,
			Username: raw.Spring.RabbitMQ.Username,
			Password: raw.Spring.RabbitMQ.Password,
		},
		Payment: PaymentConfig{OrderCloseDelay: closeDelay},
		WxPay: WxPayConfig{
			MchID:          wxProps["wxpay.mch-id"],
			MchSerialNo:    wxProps["wxpay.mch-serial-no"],
			PrivateKeyPath: privateKeyPath,
			ApiV3Key:       wxProps["wxpay.api-v3-key"],
			AppID:          wxProps["wxpay.appid"],
			Domain:         wxProps["wxpay.domain"],
			NotifyDomain:   wxProps["wxpay.notify-domain"],
			PartnerKey:     wxProps["wxpay.partnerKey"],
		},
		AliPay: AliPayConfig{
			AppID:              aliProps["alipay.appId"],
			SellerID:           aliProps["alipay.sellerId"],
			GatewayURL:         aliProps["alipay.gatewayUrl"],
			MerchantPrivateKey: aliProps["alipay.merchantPrivateKey"],
			AlipayPublicKey:    aliProps["alipay.alipayPublicKey"],
			ContentKey:         aliProps["alipay.content-key"],
			ReturnURL:          aliProps["alipay.returnUrl"],
			NotifyURL:          aliProps["alipay.notifyUrl"],
		},
	}
	if cfg.ServerPort == 0 {
		cfg.ServerPort = 8080
	}
	cfg.MySQL.DSN = jdbcToDSN(cfg.MySQL.URL, cfg.MySQL.Username, cfg.MySQL.Password)
	cfg.RabbitMQ.URL = fmt.Sprintf("amqp://%s:%s@%s:%d/", cfg.RabbitMQ.Username, cfg.RabbitMQ.Password, cfg.RabbitMQ.Host, cfg.RabbitMQ.Port)
	return cfg, nil
}

func envOrDefault(k, def string) string {
	if v := os.Getenv(k); v != "" {
		return v
	}
	return def
}

func readProperties(path string) (map[string]string, error) {
	file, err := os.Open(path)
	if err != nil {
		return nil, err
	}
	defer file.Close()
	props := map[string]string{}
	scanner := bufio.NewScanner(file)
	for scanner.Scan() {
		line := strings.TrimSpace(scanner.Text())
		if line == "" || strings.HasPrefix(line, "#") {
			continue
		}
		idx := strings.IndexAny(line, "=")
		if idx < 0 {
			continue
		}
		props[strings.TrimSpace(line[:idx])] = strings.TrimSpace(line[idx+1:])
	}
	return props, scanner.Err()
}

func parseDuration(s string, def time.Duration) time.Duration {
	s = strings.TrimSpace(s)
	if strings.HasSuffix(s, "ms") {
		n, err := strconv.Atoi(strings.TrimSuffix(s, "ms"))
		if err == nil {
			return time.Duration(n) * time.Millisecond
		}
	}
	d, err := time.ParseDuration(s)
	if err == nil {
		return d
	}
	return def
}

func jdbcToDSN(jdbcURL, username, password string) string {
	// jdbc:mysql://192.168.220.200:3308/payment_demo?serverTimezone=GMT%2B8&characterEncoding=utf-8&useSSL=false
	re := regexp.MustCompile(`^jdbc:mysql://([^/]+)/([^?]+)(\?.*)?$`)
	m := re.FindStringSubmatch(jdbcURL)
	if len(m) < 3 {
		return fmt.Sprintf("%s:%s@tcp(127.0.0.1:3306)/payment_demo?charset=utf8mb4&parseTime=True&loc=Local", username, password)
	}
	hostPort, dbName := m[1], m[2]
	query := url.Values{}
	query.Set("charset", "utf8mb4")
	query.Set("parseTime", "True")
	query.Set("loc", "Local")
	return fmt.Sprintf("%s:%s@tcp(%s)/%s?%s", username, password, hostPort, dbName, query.Encode())
}
