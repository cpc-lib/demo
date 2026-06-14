-- ===============================================================
-- 支付渠道配置 / 支付应用配置增量升级脚本（MySQL 8.0）
-- 说明：保留现有订单/支付/退款数据，仅新增配置表和订单绑定字段。
-- 初始化参数来源于项目现有 wxpay.properties / alipay-sandbox.properties，不使用 mock。
-- ===============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS `t_payment_channel`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '支付渠道ID',
  `channel_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '渠道名称，例如微信支付、支付宝',
  `channel_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '渠道编码：WXPAY、ALIPAY',
  `channel_status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'ENABLED' COMMENT '渠道状态：ENABLED-启用，DISABLED-禁用',
  `channel_desc` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '渠道描述',
  `config_params` json NULL COMMENT '渠道公共参数JSON，例如domain、gatewayUrl、contentKey',
  `sort_order` int(11) NOT NULL DEFAULT 0 COMMENT '排序号',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_channel_code` (`channel_code`) USING BTREE,
  KEY `idx_channel_status_sort` (`channel_status`, `sort_order`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

CREATE TABLE IF NOT EXISTS `t_payment_app`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '支付应用ID',
  `app_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '应用名称',
  `app_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '应用编码',
  `app_status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'ENABLED' COMMENT '应用状态：ENABLED-启用，DISABLED-禁用',
  `channel_id` bigint(20) UNSIGNED NOT NULL COMMENT '关联支付渠道ID',
  `app_desc` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '应用描述',
  `app_config` json NULL COMMENT '应用参数JSON，例如微信appid/mchId/API密钥，支付宝appId/公私钥/回调地址',
  `sort_order` int(11) NOT NULL DEFAULT 0 COMMENT '排序号',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_app_code` (`app_code`) USING BTREE,
  KEY `idx_channel_status_sort` (`channel_id`, `app_status`, `sort_order`) USING BTREE,
  CONSTRAINT `fk_payment_app_channel` FOREIGN KEY (`channel_id`) REFERENCES `t_payment_channel` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

DELIMITER $$
DROP PROCEDURE IF EXISTS add_payment_config_column_if_not_exists $$
CREATE PROCEDURE add_payment_config_column_if_not_exists(
    IN p_table_name varchar(64),
    IN p_column_name varchar(64),
    IN p_column_definition varchar(512)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table_name
          AND COLUMN_NAME = p_column_name
    ) THEN
        SET @sql = CONCAT('ALTER TABLE `', p_table_name, '` ADD COLUMN `', p_column_name, '` ', p_column_definition);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END $$
DROP PROCEDURE IF EXISTS add_payment_config_index_if_not_exists $$
CREATE PROCEDURE add_payment_config_index_if_not_exists(
    IN p_table_name varchar(64),
    IN p_index_name varchar(64),
    IN p_index_definition varchar(512)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table_name
          AND INDEX_NAME = p_index_name
    ) THEN
        SET @sql = CONCAT('ALTER TABLE `', p_table_name, '` ADD INDEX `', p_index_name, '` ', p_index_definition);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END $$
DELIMITER ;

CALL add_payment_config_column_if_not_exists('t_order_info', 'payment_app_id', 'bigint(20) UNSIGNED NULL DEFAULT NULL COMMENT ''支付应用ID'' AFTER `payment_type`');
CALL add_payment_config_column_if_not_exists('t_order_info', 'payment_channel_code', 'varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT ''支付渠道编码：WXPAY、ALIPAY'' AFTER `payment_app_id`');
CALL add_payment_config_index_if_not_exists('t_order_info', 'idx_payment_app', '(`payment_app_id`, `create_time`)');

DROP PROCEDURE IF EXISTS add_payment_config_column_if_not_exists;
DROP PROCEDURE IF EXISTS add_payment_config_index_if_not_exists;

INSERT INTO `t_payment_channel` (`id`, `channel_name`, `channel_code`, `channel_status`, `channel_desc`, `config_params`, `sort_order`) VALUES
(1, '微信支付', 'WXPAY', 'ENABLED', '微信支付渠道公共配置', '{"domain":"https://api.mch.weixin.qq.com","notifyUrl":"https://5834-2409-8a50-3e25-e7f0-ecc6-e42-7b4c-1466.ngrok-free.app"}', 10),
(2, '支付宝', 'ALIPAY', 'ENABLED', '支付宝渠道公共配置', '{"gatewayUrl":"https://openapi-sandbox.dl.alipaydev.com/gateway.do","contentKey":"GD8AS9VQy0hZROhMzOARQw=="}', 20)
ON DUPLICATE KEY UPDATE
  `channel_name` = VALUES(`channel_name`),
  `channel_status` = VALUES(`channel_status`),
  `channel_desc` = VALUES(`channel_desc`),
  `config_params` = VALUES(`config_params`),
  `sort_order` = VALUES(`sort_order`);

INSERT INTO `t_payment_app` (`id`, `app_name`, `app_code`, `app_status`, `channel_id`, `app_desc`, `app_config`, `sort_order`) VALUES
(1, '微信支付默认应用', 'WXPAY_DEFAULT', 'ENABLED', 1, '从现有微信支付配置文件迁移的默认应用', '{"appid":"wx74862e0dfcf69954","mchId":"1558950191","mchSerialNo":"34345964330B66427E0D3D28826C4993C77E631F","privateKeyPath":"apiclient_key.pem","apiV3Key":"UDuLFDcmy5Eb6o0nTNZdu6ek4DDh4K8B","partnerKey":"T6m9iK73b0kn9g5v426MKfHQH7X8rKwb","notifyUrl":"https://5834-2409-8a50-3e25-e7f0-ecc6-e42-7b4c-1466.ngrok-free.app"}', 10),
(2, '支付宝沙箱默认应用', 'ALIPAY_SANDBOX_DEFAULT', 'ENABLED', 2, '从现有支付宝沙箱配置文件迁移的默认应用', '{"appId":"9021000136667568","sellerId":"2088721034748965","merchantPrivateKey":"MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCCDhLjo3lEFNcWdMnehWhHPams3KbmxpVJTxmIglvMLt6j207pSlWxSvacT3wFebXW4herg9RWhjTTh7KFwkxRWwzBVfp63YvEUbNsF09X1K6NqT2Kqz/w77l38lhQxpVau8odV3nHR+KhtDD2YJ9z8C2AzJLzKCD5CdK5coP7k+mRx3/mlOEj07oA7H6CrDkm4j4oasqEeLfbyJrXYcpPMi2hIHMXkR21fX5905v9BxwPhkVFsxHOABPYMWu5+uPpjRH1e/HKZpo5sinud05oWI0b1waMRZ9AGUEJnNZ3Yg9b9b2k1ycnCkApzy8cgQOgGhr6Rr9V7ieAsOn0YahLAgMBAAECggEAeImcvjj0GsKJ+zkxJDlXRbgD+7/iPL/O+0wBqUDQ3fSOyyVnBNetho2o9YTBuL1uaIPSVlfvxGXMrkT1k/1aCIkv0Dzk011kvgbPGZ6dHhVz1r4F2PERaTh2GJKXgf4bzSWBlSJPLwEULrU4MBGrl6QCOH7ir9UAgnC1SsW1R8QkCbgXchZkh9s3LddJ0Skb9Qu144yhT3tSxIMDL/ZJqANIR0Smv9tUuP2+DehqBH8OEapW4c00mt7OWT0jQ8H/8BaFV3j0q8SCn3cmKBFeUzh9H2UlHjiVUeF9mRmiEJUK8/N/li/WZbiWwRXOcRlno54+aRbvlwo+A8gWZF2n+QKBgQC6EIONzns/n0wSB87X0ZWUb7Y3Q6aReu/JvFwNuUA1wbS4bw7PTAijvB0daLXke142ujt6tnghyQHQuQU+5xzA5q8Q3Ooy8OZo9YgCRzNmU20CltEWcWijOt7ndCxpEVDeB7dY55yavVI8hgWX5LFI0t4Zwa8u+c+QL9C48DUVNQKBgQCy8DbydcZdRx2XSYZGtd0p+yR6lFucxpIFIYt/CFXcKGLKasF7Yhds5RKderp/I/1WZl0kwpbTv17HuvJqdoDX4qLXhvlh30P9Pbvvk7YhIv9oR9NqRtTTr0E40jALTouAuEaWs1f49C6AfrWJC26jgNHWpdeEkUQ6NN6DaP73fwKBgAsDRTYUfZkDbbY3fheqEQdrIUbeGzLLKvwuyOgLCfDkmTS9ZgwA/RXr4XFHLFTstGPa3ABkYnHletUGznetqDcGsF/4I2iGd6zIs5cm7bTlxTL9CD0i00WuC1l5t9M0MiwiGskJVGyYPhDVAem+oHul931gyGSoZo+rNNhtZ0btAoGAU2gM9K9ZKxl+/YnUARm8YVkjA9Arc8RLRAEC2M+11c0tX1Sroytx59xO9QDD9Yd9CszkFcJuM308XLUTUfSy0e5eIUBU9f3v3xbrhxy/BGsfyifQr/UcNx+1sxqmMl8GP5WlsZEfLHgFRPfK/npJtATTys26y5w6xTbnkTFbx1kCgYB+2wDnTWavXJkQErniXh3ifmC7qLcBNBpSUrI9eAtI4AwqKfBnp7d+wGuh05epecb1gzE76bWPqgIX7N5zmpZaQ/qZL+CgcGpU/7PN/qKGrqa1JEu8ZMaDhFys6MkIpQIGMElgnbM0cXvNrY8ReKvcGGysdtLnIBrp9BXcm+uWSg==","alipayPublicKey":"MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAh9ivuRV+2eNbAzxXQ3bmx3wdhBHVaNQlhzv6wDXBMNhKG/AJDOAtYNbzPYJJPfySOrxyMlcTCvTGMS//Zn4yJHXx8IjJ4LSIguyy2BjDMwvDlF+TP7Xj6F/qtHWzuhztDnkLdAOwUx70Zyq1ajjzU5b2ku3J9WX5bwmnDpFGCVCwwG51mJYTd7Fwr4nE1qRZeMgGMhR7xR5Qdu1Nwx8Z+l1FCs47eZiirearT83/pwCRPD364SHq2uBJLtse9ozO7meBb8mzt6CDZKK6imEX1MKeVILbfE7GZAbtIAV4TLbvhB4VZuVxQrYmppPGhMpEiFDkpk0nFyNb7tz3HcHIQwIDAQAB","returnUrl":"http://localhost:8083/#/success","notifyUrl":"http://arhi.nat300.top/api/ali-pay/trade/notify"}', 20)
ON DUPLICATE KEY UPDATE
  `app_name` = VALUES(`app_name`),
  `app_status` = VALUES(`app_status`),
  `channel_id` = VALUES(`channel_id`),
  `app_desc` = VALUES(`app_desc`),
  `app_config` = VALUES(`app_config`),
  `sort_order` = VALUES(`sort_order`);

SET FOREIGN_KEY_CHECKS = 1;
