package cc.ivera.config;

import lombok.Data;

/**
 * 支付应用配置模型 - 从数据库加载
 */
@Data
public class PaymentAppConfig {

    /**
     * 应用ID
     */
    private Long appId;

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 应用编码
     */
    private String appCode;

    /**
     * 关联渠道ID
     */
    private Long channelId;

    /**
     * 渠道编码
     */
    private String channelCode;

    // ========== 微信支付配置 ==========
    
    /**
     * 公众号/小程序/APP的appid
     */
    private String appid;

    /**
     * 商户号
     */
    private String mchId;

    /**
     * 商户API证书序列号
     */
    private String mchSerialNo;

    /**
     * 商户私钥文件路径
     */
    private String privateKeyPath;

    /**
     * APIv3密钥
     */
    private String apiV3Key;

    /**
     * APIv2密钥
     */
    private String partnerKey;

    /**
     * 微信服务器地址
     */
    private String domain;

    /**
     * 微信支付通知地址
     */
    private String notifyUrl;

    // ========== 支付宝配置 ==========
    
    /**
     * 支付宝应用ID
     */
    private String alipayAppId;

    /**
     * 卖家ID
     */
    private String sellerId;

    /**
     * 支付宝网关地址
     */
    private String gatewayUrl;

    /**
     * 商户私钥
     */
    private String merchantPrivateKey;

    /**
     * 支付宝公钥
     */
    private String alipayPublicKey;

    /**
     * 内容密钥
     */
    private String contentKey;

    /**
     * 同步返回地址
     */
    private String returnUrl;

    /**
     * 支付宝通知地址
     */
    private String alipayNotifyUrl;
}