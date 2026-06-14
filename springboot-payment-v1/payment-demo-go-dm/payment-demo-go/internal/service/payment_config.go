package service

import (
	"bytes"
	"context"
	"encoding/json"
	"path/filepath"
	"strconv"
	"strings"

	"payment-demo-go/internal/config"
	"payment-demo-go/internal/constant"
	"payment-demo-go/internal/model"
	"payment-demo-go/internal/util"

	"gorm.io/gorm"
)

type wxPayAppConfig struct {
	MchID          string `json:"mchId"`
	MchSerialNo    string `json:"mchSerialNo"`
	PrivateKeyPath string `json:"privateKeyPath"`
	ApiV3Key       string `json:"apiV3Key"`
	AppID          string `json:"appid"`
	Domain         string `json:"domain"`
	NotifyDomain   string `json:"notifyDomain"`
	PartnerKey     string `json:"partnerKey"`
}

type aliPayAppConfig struct {
	AppID              string `json:"appId"`
	SellerID           string `json:"sellerId"`
	GatewayURL         string `json:"gatewayUrl"`
	MerchantPrivateKey string `json:"merchantPrivateKey"`
	AlipayPublicKey    string `json:"alipayPublicKey"`
	ContentKey         string `json:"contentKey"`
	ReturnURL          string `json:"returnUrl"`
	NotifyURL          string `json:"notifyUrl"`
}

func ParsePaymentAppIDValue(raw string) (int64, error) {
	raw = strings.TrimSpace(raw)
	if raw == "" {
		return 0, nil
	}
	id, err := strconv.ParseInt(raw, 10, 64)
	if err != nil {
		return 0, util.Biz("paymentAppId必须为数字")
	}
	if id <= 0 {
		return 0, util.Biz("paymentAppId必须大于0")
	}
	return id, nil
}

func ParseRequiredPaymentAppIDValue(raw string) (int64, error) {
	if strings.TrimSpace(raw) == "" {
		return 0, util.Biz("paymentAppId不能为空")
	}
	return ParsePaymentAppIDValue(raw)
}

func (s *Service) ListPaymentChannels() ([]model.PaymentChannel, error) {
	var list []model.PaymentChannel
	err := s.DB.Order("id asc").Find(&list).Error
	return list, err
}

func (s *Service) SavePaymentChannel(req model.PaymentChannelRequest) (*model.PaymentChannel, error) {
	channel, err := buildPaymentChannel(req, nil)
	if err != nil {
		return nil, err
	}
	if err := s.DB.Create(channel).Error; err != nil {
		return nil, err
	}
	return channel, nil
}

func (s *Service) UpdatePaymentChannel(channelCode string, req model.PaymentChannelRequest) (*model.PaymentChannel, error) {
	channelCode = strings.TrimSpace(channelCode)
	if channelCode == "" {
		return nil, util.Biz("支付渠道编码不能为空")
	}
	var current model.PaymentChannel
	err := s.DB.Where("channel_code=?", channelCode).First(&current).Error
	if err == gorm.ErrRecordNotFound {
		return nil, util.Biz("支付渠道不存在")
	}
	if err != nil {
		return nil, err
	}
	next, err := buildPaymentChannel(req, &current)
	if err != nil {
		return nil, err
	}
	next.ID = current.ID
	if next.ChannelCode == "" {
		next.ChannelCode = current.ChannelCode
	}
	if err := s.DB.Model(&current).Updates(map[string]interface{}{
		"channel_code":  next.ChannelCode,
		"channel_name":  next.ChannelName,
		"payment_type":  next.PaymentType,
		"enabled":       next.Enabled,
		"config_params": next.ConfigParams,
	}).Error; err != nil {
		return nil, err
	}
	return s.GetPaymentChannelByCode(next.ChannelCode)
}

func (s *Service) DeletePaymentChannel(channelCode string) error {
	channelCode = strings.TrimSpace(channelCode)
	if channelCode == "" {
		return util.Biz("支付渠道编码不能为空")
	}
	return s.DB.Where("channel_code=?", channelCode).Delete(&model.PaymentChannel{}).Error
}

func (s *Service) GetPaymentChannelByCode(channelCode string) (*model.PaymentChannel, error) {
	var channel model.PaymentChannel
	err := s.DB.Where("channel_code=?", channelCode).First(&channel).Error
	if err == gorm.ErrRecordNotFound {
		return nil, util.Biz("支付渠道不存在")
	}
	return &channel, err
}

func (s *Service) ListPaymentApps(channelCode, paymentType string, enabled *bool) ([]model.PaymentApp, error) {
	var list []model.PaymentApp
	query := s.DB.Order("id asc")
	if strings.TrimSpace(channelCode) != "" {
		query = query.Where("channel_code=?", strings.TrimSpace(channelCode))
	}
	if strings.TrimSpace(paymentType) != "" {
		query = query.Where("payment_type=?", strings.TrimSpace(paymentType))
	}
	if enabled != nil {
		query = query.Where("enabled=?", *enabled)
	}
	err := query.Find(&list).Error
	return list, err
}

func (s *Service) SavePaymentApp(req model.PaymentAppRequest) (*model.PaymentApp, error) {
	app, err := buildPaymentApp(req, nil)
	if err != nil {
		return nil, err
	}
	if err := s.DB.Create(app).Error; err != nil {
		return nil, err
	}
	return app, nil
}

func (s *Service) UpdatePaymentApp(appCode string, req model.PaymentAppRequest) (*model.PaymentApp, error) {
	appCode = strings.TrimSpace(appCode)
	if appCode == "" {
		return nil, util.Biz("支付应用编码不能为空")
	}
	var current model.PaymentApp
	err := s.DB.Where("app_code=?", appCode).First(&current).Error
	if err == gorm.ErrRecordNotFound {
		return nil, util.Biz("支付应用不存在")
	}
	if err != nil {
		return nil, err
	}
	next, err := buildPaymentApp(req, &current)
	if err != nil {
		return nil, err
	}
	next.ID = current.ID
	if next.AppCode == "" {
		next.AppCode = current.AppCode
	}
	if err := s.DB.Model(&current).Updates(map[string]interface{}{
		"app_code":     next.AppCode,
		"app_name":     next.AppName,
		"channel_code": next.ChannelCode,
		"payment_type": next.PaymentType,
		"enabled":      next.Enabled,
		"app_config":   next.AppConfig,
	}).Error; err != nil {
		return nil, err
	}
	return s.GetPaymentAppByCode(next.AppCode)
}

func (s *Service) DeletePaymentApp(appCode string) error {
	appCode = strings.TrimSpace(appCode)
	if appCode == "" {
		return util.Biz("支付应用编码不能为空")
	}
	return s.DB.Where("app_code=?", appCode).Delete(&model.PaymentApp{}).Error
}

func (s *Service) GetPaymentAppByCode(appCode string) (*model.PaymentApp, error) {
	var app model.PaymentApp
	err := s.DB.Where("app_code=?", appCode).First(&app).Error
	if err == gorm.ErrRecordNotFound {
		return nil, util.Biz("支付应用不存在")
	}
	return &app, err
}

func (s *Service) ListEnabledPaymentApps() ([]model.PaymentApp, error) {
	enabled := true
	return s.ListPaymentApps("", "", &enabled)
}

func (s *Service) resolvePaymentAppByID(ctx context.Context, paymentType string, paymentAppID int64, requireEnabled bool) (*model.PaymentApp, error) {
	if paymentAppID <= 0 {
		return nil, nil
	}
	var app model.PaymentApp
	query := s.DB.WithContext(ctx).Where("id=? and payment_type=?", paymentAppID, paymentType)
	if requireEnabled {
		query = query.Where("enabled=?", true)
	}
	err := query.First(&app).Error
	if err == gorm.ErrRecordNotFound {
		return nil, util.Biz("支付应用不存在或未启用")
	}
	return &app, err
}

func (s *Service) paymentChannelForApp(ctx context.Context, app model.PaymentApp) (*model.PaymentChannel, error) {
	channelCode := strings.TrimSpace(app.ChannelCode)
	if channelCode == "" {
		return nil, nil
	}
	var channel model.PaymentChannel
	err := s.DB.WithContext(ctx).Where("channel_code=? and payment_type=?", channelCode, app.PaymentType).First(&channel).Error
	if err == gorm.ErrRecordNotFound {
		return nil, nil
	}
	return &channel, err
}

func (s *Service) resolvePaymentAppForNewOrder(ctx context.Context, paymentType string, paymentAppID int64) (*model.PaymentApp, *model.PaymentChannel, error) {
	if paymentAppID <= 0 {
		return nil, nil, util.Biz("paymentAppId不能为空")
	}
	app, err := s.resolvePaymentAppByID(ctx, paymentType, paymentAppID, true)
	if err != nil {
		return nil, nil, err
	}
	channel, err := s.paymentChannelForApp(ctx, *app)
	if err != nil {
		return nil, nil, err
	}
	if err := validatePaymentAppUsableForNewOrder(app, channel); err != nil {
		return nil, nil, err
	}
	return app, channel, nil
}

func (s *Service) resolvePaymentAppForExistingOrder(ctx context.Context, paymentType string, paymentAppID int64) (*model.PaymentApp, *model.PaymentChannel, error) {
	if paymentAppID <= 0 {
		return nil, nil, nil
	}
	app, err := s.resolvePaymentAppByID(ctx, paymentType, paymentAppID, false)
	if err != nil {
		return nil, nil, err
	}
	channel, err := s.paymentChannelForApp(ctx, *app)
	if err != nil {
		return nil, nil, err
	}
	return app, channel, nil
}

func (s *Service) wxPayConfigForApp(ctx context.Context, paymentAppID int64) (config.WxPayConfig, int64, error) {
	app, channel, err := s.resolvePaymentAppForNewOrder(ctx, constant.PayTypeWxPay, paymentAppID)
	if err != nil {
		return config.WxPayConfig{}, 0, err
	}
	cfg, err := buildWxPayConfigFromChannelAndApp(s.Cfg.WxPay, channel, *app)
	if err != nil {
		return config.WxPayConfig{}, 0, err
	}
	return cfg, int64(app.ID), nil
}

func (s *Service) wxPayConfigForOrder(ctx context.Context, order *model.OrderInfo) (config.WxPayConfig, int64, error) {
	if order == nil || order.PaymentAppID == nil || *order.PaymentAppID <= 0 {
		return s.Cfg.WxPay, 0, nil
	}
	app, channel, err := s.resolvePaymentAppForExistingOrder(ctx, constant.PayTypeWxPay, *order.PaymentAppID)
	if err != nil {
		return config.WxPayConfig{}, 0, err
	}
	if app == nil {
		return s.Cfg.WxPay, 0, nil
	}
	cfg, err := buildWxPayConfigFromChannelAndApp(s.Cfg.WxPay, channel, *app)
	if err != nil {
		return config.WxPayConfig{}, 0, err
	}
	return cfg, int64(app.ID), nil
}

func (s *Service) aliPayConfigForApp(ctx context.Context, paymentAppID int64) (config.AliPayConfig, int64, error) {
	app, channel, err := s.resolvePaymentAppForNewOrder(ctx, constant.PayTypeAliPay, paymentAppID)
	if err != nil {
		return config.AliPayConfig{}, 0, err
	}
	cfg, err := buildAliPayConfigFromChannelAndApp(s.Cfg.AliPay, channel, *app)
	if err != nil {
		return config.AliPayConfig{}, 0, err
	}
	return cfg, int64(app.ID), nil
}

func (s *Service) aliPayConfigForOrder(ctx context.Context, order *model.OrderInfo) (config.AliPayConfig, int64, error) {
	if order == nil || order.PaymentAppID == nil || *order.PaymentAppID <= 0 {
		return s.Cfg.AliPay, 0, nil
	}
	app, channel, err := s.resolvePaymentAppForExistingOrder(ctx, constant.PayTypeAliPay, *order.PaymentAppID)
	if err != nil {
		return config.AliPayConfig{}, 0, err
	}
	if app == nil {
		return s.Cfg.AliPay, 0, nil
	}
	cfg, err := buildAliPayConfigFromChannelAndApp(s.Cfg.AliPay, channel, *app)
	if err != nil {
		return config.AliPayConfig{}, 0, err
	}
	return cfg, int64(app.ID), nil
}

type wxPayResolvedConfig struct {
	Config       config.WxPayConfig
	PaymentAppID int64
}

type aliPayResolvedConfig struct {
	Config       config.AliPayConfig
	PaymentAppID int64
}

func (s *Service) wxPayConfiguredApps(ctx context.Context) ([]wxPayResolvedConfig, error) {
	var resolved []wxPayResolvedConfig
	if s.DB != nil {
		var apps []model.PaymentApp
		if err := s.DB.WithContext(ctx).Where("payment_type=?", constant.PayTypeWxPay).Order("id asc").Find(&apps).Error; err != nil {
			return nil, err
		}
		for i := range apps {
			app := apps[i]
			channel, err := s.paymentChannelForApp(ctx, app)
			if err != nil {
				return nil, err
			}
			cfg, err := buildWxPayConfigFromChannelAndApp(s.Cfg.WxPay, channel, app)
			if err != nil {
				return nil, err
			}
			resolved = append(resolved, wxPayResolvedConfig{Config: cfg, PaymentAppID: int64(app.ID)})
		}
	}
	resolved = append(resolved, wxPayResolvedConfig{Config: s.Cfg.WxPay})
	return resolved, nil
}

func (s *Service) aliPayConfiguredApps(ctx context.Context) ([]aliPayResolvedConfig, error) {
	var resolved []aliPayResolvedConfig
	if s.DB != nil {
		var apps []model.PaymentApp
		if err := s.DB.WithContext(ctx).Where("payment_type=?", constant.PayTypeAliPay).Order("id asc").Find(&apps).Error; err != nil {
			return nil, err
		}
		for i := range apps {
			app := apps[i]
			channel, err := s.paymentChannelForApp(ctx, app)
			if err != nil {
				return nil, err
			}
			cfg, err := buildAliPayConfigFromChannelAndApp(s.Cfg.AliPay, channel, app)
			if err != nil {
				return nil, err
			}
			resolved = append(resolved, aliPayResolvedConfig{Config: cfg, PaymentAppID: int64(app.ID)})
		}
	}
	resolved = append(resolved, aliPayResolvedConfig{Config: s.Cfg.AliPay})
	return resolved, nil
}

func (s *Service) wxPayConfigByProvider(ctx context.Context, appID, mchID string) (config.WxPayConfig, int64, error) {
	configs, err := s.wxPayConfiguredApps(ctx)
	if err != nil {
		return config.WxPayConfig{}, 0, err
	}
	for _, item := range configs {
		if strings.TrimSpace(appID) != "" && item.Config.AppID != appID {
			continue
		}
		if strings.TrimSpace(mchID) != "" && item.Config.MchID != mchID {
			continue
		}
		return item.Config, item.PaymentAppID, nil
	}
	return config.WxPayConfig{}, 0, util.Biz("支付应用不存在或未启用")
}

func (s *Service) aliPayConfigByProvider(ctx context.Context, appID, sellerID string) (config.AliPayConfig, int64, error) {
	configs, err := s.aliPayConfiguredApps(ctx)
	if err != nil {
		return config.AliPayConfig{}, 0, err
	}
	for _, item := range configs {
		if strings.TrimSpace(appID) != "" && item.Config.AppID != appID {
			continue
		}
		if strings.TrimSpace(sellerID) != "" && item.Config.SellerID != sellerID {
			continue
		}
		return item.Config, item.PaymentAppID, nil
	}
	return config.AliPayConfig{}, 0, util.Biz("支付应用不存在或未启用")
}

func validateOrderPaymentAppBinding(order *model.OrderInfo, paymentAppID int64, action string) error {
	if order == nil || order.PaymentAppID == nil || *order.PaymentAppID <= 0 {
		return nil
	}
	if paymentAppID <= 0 || *order.PaymentAppID != paymentAppID {
		prefix := strings.TrimSpace(action)
		if prefix == "" {
			prefix = "支付"
		}
		return util.Biz(prefix + "支付应用不匹配，orderNo=" + order.OrderNo)
	}
	return nil
}

func validatePaymentAppUsableForNewOrder(app *model.PaymentApp, channel *model.PaymentChannel) error {
	if app == nil || !app.Enabled {
		return util.Biz("支付应用不存在或未启用")
	}
	if channel == nil || !channel.Enabled {
		return util.Biz("支付渠道不存在或未启用")
	}
	return nil
}

func buildPaymentChannel(req model.PaymentChannelRequest, current *model.PaymentChannel) (*model.PaymentChannel, error) {
	channel := &model.PaymentChannel{}
	if current != nil {
		*channel = *current
	}
	if strings.TrimSpace(req.ChannelCode) != "" {
		channel.ChannelCode = strings.TrimSpace(req.ChannelCode)
	}
	if strings.TrimSpace(req.ChannelName) != "" {
		channel.ChannelName = strings.TrimSpace(req.ChannelName)
	}
	if strings.TrimSpace(req.PaymentType) != "" {
		channel.PaymentType = strings.TrimSpace(req.PaymentType)
	}
	if req.Enabled != nil {
		channel.Enabled = *req.Enabled
	} else if current == nil {
		channel.Enabled = true
	}
	if req.ConfigParams != nil {
		configParams, err := normalizeJSONField(*req.ConfigParams)
		if err != nil {
			return nil, util.Biz("渠道配置必须是合法JSON")
		}
		channel.ConfigParams = configParams
	}
	if channel.ChannelCode == "" {
		return nil, util.Biz("支付渠道编码不能为空")
	}
	if channel.ChannelName == "" {
		return nil, util.Biz("支付渠道名称不能为空")
	}
	if channel.PaymentType == "" {
		return nil, util.Biz("支付类型不能为空")
	}
	return channel, nil
}

func buildPaymentApp(req model.PaymentAppRequest, current *model.PaymentApp) (*model.PaymentApp, error) {
	app := &model.PaymentApp{}
	if current != nil {
		*app = *current
	}
	if strings.TrimSpace(req.AppCode) != "" {
		app.AppCode = strings.TrimSpace(req.AppCode)
	}
	if strings.TrimSpace(req.AppName) != "" {
		app.AppName = strings.TrimSpace(req.AppName)
	}
	if strings.TrimSpace(req.ChannelCode) != "" {
		app.ChannelCode = strings.TrimSpace(req.ChannelCode)
	}
	if strings.TrimSpace(req.PaymentType) != "" {
		app.PaymentType = strings.TrimSpace(req.PaymentType)
	}
	if req.Enabled != nil {
		app.Enabled = *req.Enabled
	} else if current == nil {
		app.Enabled = true
	}
	if req.AppConfig != nil {
		appConfig, err := normalizeJSONField(*req.AppConfig)
		if err != nil {
			return nil, util.Biz("支付应用配置必须是合法JSON")
		}
		app.AppConfig = appConfig
	}
	if app.AppCode == "" {
		return nil, util.Biz("支付应用编码不能为空")
	}
	if app.AppName == "" {
		return nil, util.Biz("支付应用名称不能为空")
	}
	if app.ChannelCode == "" {
		return nil, util.Biz("支付渠道编码不能为空")
	}
	if app.PaymentType == "" {
		return nil, util.Biz("支付类型不能为空")
	}
	return app, nil
}

func buildWxPayConfigFromChannelAndApp(base config.WxPayConfig, channel *model.PaymentChannel, app model.PaymentApp) (config.WxPayConfig, error) {
	var err error
	if channel != nil && strings.TrimSpace(channel.ConfigParams) != "" {
		base, err = buildWxPayConfigFromRaw(base, channel.ConfigParams)
		if err != nil {
			return config.WxPayConfig{}, util.Biz("微信支付渠道配置不是合法JSON")
		}
	}
	return buildWxPayConfigFromApp(base, app)
}

func buildWxPayConfigFromApp(base config.WxPayConfig, app model.PaymentApp) (config.WxPayConfig, error) {
	return buildWxPayConfigFromRaw(base, app.AppConfig)
}

func buildWxPayConfigFromRaw(base config.WxPayConfig, rawConfig string) (config.WxPayConfig, error) {
	var raw wxPayAppConfig
	if strings.TrimSpace(rawConfig) != "" {
		if err := json.Unmarshal([]byte(rawConfig), &raw); err != nil {
			return config.WxPayConfig{}, util.Biz("微信支付应用配置不是合法JSON")
		}
	}
	if raw.MchID != "" {
		base.MchID = raw.MchID
	}
	if raw.MchSerialNo != "" {
		base.MchSerialNo = raw.MchSerialNo
	}
	if raw.PrivateKeyPath != "" {
		base.PrivateKeyPath = resolvePrivateKeyPathFromBase(base.PrivateKeyPath, raw.PrivateKeyPath)
	}
	if raw.ApiV3Key != "" {
		base.ApiV3Key = raw.ApiV3Key
	}
	if raw.AppID != "" {
		base.AppID = raw.AppID
	}
	if raw.Domain != "" {
		base.Domain = raw.Domain
	}
	if raw.NotifyDomain != "" {
		base.NotifyDomain = raw.NotifyDomain
	}
	if raw.PartnerKey != "" {
		base.PartnerKey = raw.PartnerKey
	}
	return base, nil
}

func resolvePrivateKeyPathFromBase(basePath, overridePath string) string {
	overridePath = strings.TrimSpace(overridePath)
	if overridePath == "" || filepath.IsAbs(overridePath) {
		return overridePath
	}
	baseDir := filepath.Dir(strings.TrimSpace(basePath))
	if baseDir == "." || baseDir == "" {
		return overridePath
	}
	return filepath.Join(baseDir, overridePath)
}

func buildAliPayConfigFromChannelAndApp(base config.AliPayConfig, channel *model.PaymentChannel, app model.PaymentApp) (config.AliPayConfig, error) {
	var err error
	if channel != nil && strings.TrimSpace(channel.ConfigParams) != "" {
		base, err = buildAliPayConfigFromRaw(base, channel.ConfigParams)
		if err != nil {
			return config.AliPayConfig{}, util.Biz("支付宝渠道配置不是合法JSON")
		}
	}
	return buildAliPayConfigFromApp(base, app)
}

func buildAliPayConfigFromApp(base config.AliPayConfig, app model.PaymentApp) (config.AliPayConfig, error) {
	return buildAliPayConfigFromRaw(base, app.AppConfig)
}

func buildAliPayConfigFromRaw(base config.AliPayConfig, rawConfig string) (config.AliPayConfig, error) {
	var raw aliPayAppConfig
	if strings.TrimSpace(rawConfig) != "" {
		if err := json.Unmarshal([]byte(rawConfig), &raw); err != nil {
			return config.AliPayConfig{}, util.Biz("支付宝应用配置不是合法JSON")
		}
	}
	if raw.AppID != "" {
		base.AppID = raw.AppID
	}
	if raw.SellerID != "" {
		base.SellerID = raw.SellerID
	}
	if raw.GatewayURL != "" {
		base.GatewayURL = raw.GatewayURL
	}
	if raw.MerchantPrivateKey != "" {
		base.MerchantPrivateKey = raw.MerchantPrivateKey
	}
	if raw.AlipayPublicKey != "" {
		base.AlipayPublicKey = raw.AlipayPublicKey
	}
	if raw.ContentKey != "" {
		base.ContentKey = raw.ContentKey
	}
	if raw.ReturnURL != "" {
		base.ReturnURL = raw.ReturnURL
	}
	if raw.NotifyURL != "" {
		base.NotifyURL = raw.NotifyURL
	}
	return base, nil
}

func normalizeJSONField(raw json.RawMessage) (string, error) {
	trimmed := bytes.TrimSpace(raw)
	if len(trimmed) == 0 || bytes.Equal(trimmed, []byte("null")) {
		return "", nil
	}
	if trimmed[0] == '"' {
		var s string
		if err := json.Unmarshal(trimmed, &s); err != nil {
			return "", err
		}
		if strings.TrimSpace(s) == "" {
			return "", nil
		}
		trimmed = []byte(s)
	}
	if !json.Valid(trimmed) {
		return "", util.Biz("配置必须是合法JSON")
	}
	var buf bytes.Buffer
	if err := json.Compact(&buf, trimmed); err != nil {
		return "", err
	}
	return buf.String(), nil
}
