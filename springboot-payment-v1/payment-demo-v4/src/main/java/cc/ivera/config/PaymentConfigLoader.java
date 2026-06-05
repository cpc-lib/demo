package cc.ivera.config;

import cc.ivera.entity.PaymentApp;
import cc.ivera.entity.PaymentChannel;
import cc.ivera.service.PaymentAppService;
import cc.ivera.service.PaymentChannelService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 支付配置加载器 - 从数据库加载支付渠道和应用配置
 */
@Component
@Slf4j
public class PaymentConfigLoader {

    private final PaymentAppService paymentAppService;
    private final PaymentChannelService paymentChannelService;
    private final ObjectMapper objectMapper;

    /**
     * 缓存的应用配置 - key: appId
     */
    private final Map<Long, PaymentAppConfig> appConfigCache = new ConcurrentHashMap<>();

    /**
     * 缓存的渠道配置 - key: channelCode
     */
    private final Map<String, PaymentChannel> channelCache = new ConcurrentHashMap<>();

    public PaymentConfigLoader(PaymentAppService paymentAppService,
                              PaymentChannelService paymentChannelService,
                              ObjectMapper objectMapper) {
        this.paymentAppService = paymentAppService;
        this.paymentChannelService = paymentChannelService;
        this.objectMapper = objectMapper;
        // 初始化加载配置
        reloadConfigs();
    }

    /**
     * 重新加载所有配置
     */
    public void reloadConfigs() {
        log.info("开始重新加载支付配置...");

        // 加载渠道配置
        loadChannels();

        // 加载应用配置
        loadApps();

        log.info("支付配置加载完成，渠道数: {}, 应用数: {}", channelCache.size(), appConfigCache.size());
    }

    /**
     * 加载渠道配置
     */
    private void loadChannels() {
        channelCache.clear();
        List<PaymentChannel> channels = paymentChannelService.listEnabledChannels();
        for (PaymentChannel channel : channels) {
            channelCache.put(channel.getChannelCode(), channel);
            log.debug("加载渠道: {} ({})", channel.getChannelName(), channel.getChannelCode());
        }
    }

    /**
     * 加载应用配置
     */
    private void loadApps() {
        appConfigCache.clear();
        List<PaymentApp> apps = paymentAppService.listEnabledApps();

        for (PaymentApp app : apps) {
            try {
                PaymentAppConfig config = convertToConfig(app);
                appConfigCache.put(app.getId(), config);
                log.debug("加载应用: {} ({})", app.getAppName(), app.getAppCode());
            } catch (Exception e) {
                log.error("加载支付应用配置失败，appId={}, appCode={}", app.getId(), app.getAppCode(), e);
            }
        }
    }

    /**
     * 将PaymentApp实体转换为PaymentAppConfig配置对象
     */
    private PaymentAppConfig convertToConfig(PaymentApp app) {
        PaymentAppConfig config = new PaymentAppConfig();
        config.setAppId(app.getId());
        config.setAppName(app.getAppName());
        config.setAppCode(app.getAppCode());
        config.setChannelId(app.getChannelId());

        // 获取渠道信息
        PaymentChannel channel = paymentChannelService.getById(app.getChannelId());
        if (channel != null) {
            config.setChannelCode(channel.getChannelCode());

            // 解析渠道级配置
            try {
                if (channel.getConfigParams() != null && !channel.getConfigParams().isEmpty()) {
                    Map<String, String> channelConfig = objectMapper.readValue(
                            channel.getConfigParams(),
                            new TypeReference<Map<String, String>>() {});

                    // 渠道级配置作为默认值
                    config.setDomain(channelConfig.get("domain"));
                    config.setApiV3Key(channelConfig.get("apiV3Key"));
                    config.setGatewayUrl(channelConfig.get("gatewayUrl"));
                    config.setContentKey(channelConfig.get("contentKey"));
                }
            } catch (Exception e) {
                log.warn("解析渠道配置参数失败，channelId={}", channel.getId());
            }
        }

        // 解析应用级配置（覆盖渠道级配置）
        if (app.getAppConfig() != null && !app.getAppConfig().isEmpty()) {
            try {
                Map<String, String> appConfig = objectMapper.readValue(
                        app.getAppConfig(),
                        new TypeReference<Map<String, String>>() {});

                // 微信支付配置
                config.setAppid(appConfig.get("appid"));
                config.setMchId(appConfig.get("mchId"));
                config.setMchSerialNo(appConfig.get("mchSerialNo"));
                config.setPrivateKeyPath(appConfig.get("privateKeyPath"));
                config.setPartnerKey(appConfig.get("partnerKey"));
                config.setNotifyUrl(appConfig.get("notifyUrl"));

                // 支付宝配置
                config.setAlipayAppId(appConfig.get("appId"));
                config.setSellerId(appConfig.get("sellerId"));
                config.setMerchantPrivateKey(appConfig.get("merchantPrivateKey"));
                config.setAlipayPublicKey(appConfig.get("alipayPublicKey"));
                config.setReturnUrl(appConfig.get("returnUrl"));

                // 应用级的notifyUrl覆盖支付宝通知地址
                if (config.getNotifyUrl() != null && "ALIPAY".equals(config.getChannelCode())) {
                    config.setAlipayNotifyUrl(config.getNotifyUrl());
                } else {
                    config.setAlipayNotifyUrl(appConfig.get("notifyUrl"));
                }
            } catch (Exception e) {
                log.error("解析应用配置参数失败，appId={}", app.getId(), e);
            }
        }

        return config;
    }

    /**
     * 根据应用ID获取配置
     */
    public PaymentAppConfig getAppConfig(Long appId) {
        if (appId == null) {
            return null;
        }
        PaymentAppConfig config = appConfigCache.get(appId);
        if (config == null) {
            // 尝试从数据库加载
            PaymentApp app = paymentAppService.getById(appId);
            if (app != null) {
                config = convertToConfig(app);
                appConfigCache.put(appId, config);
            }
        }
        return config;
    }

    /**
     * 根据渠道编码获取渠道配置
     */
    public PaymentChannel getChannel(String channelCode) {
        return channelCache.get(channelCode);
    }

    /**
     * 根据应用编码获取配置
     */
    public PaymentAppConfig getAppConfigByCode(String appCode) {
        return appConfigCache.values().stream()
                .filter(config -> appCode.equals(config.getAppCode()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取所有启用的应用配置
     */
    public Map<Long, PaymentAppConfig> getAllAppConfigs() {
        return new HashMap<>(appConfigCache);
    }
}
