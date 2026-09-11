-- ===============================================================
-- Payment Demo 完整数据库初始化脚本（一键执行）
-- 合并了所有版本的改进，包括：
-- 1. 基础表结构（商品、订单、支付、退款）
-- 2. 支付渠道/应用配置表
-- 3. V3 幂等性增强（乐观锁、唯一约束、索引优化）
-- 4. V4/V5/V6 并发控制增强（数据库层兜底能力）
-- 5. 对账功能（对账主表 + 对账明细表）
-- 6. 渠道账单导入（渠道账单表 + 对账账单关联）
-- 7. 微信进账与退款逐笔对账（业务类型 + 退款标识字段）
-- 8. 用户认证、服务端购物车与多课程合单
-- 9. 商品库存、订单幂等、按明细退款与可靠消息
-- 10. v0.6.0 SKU库存、独立支付尝试、Reservation、履约与退货验收
-- 本文件是唯一数据库初始化脚本，无需再执行其他升级脚本。
-- ===============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `t_message_consume_log`;
DROP TABLE IF EXISTS `t_message_outbox`;
DROP TABLE IF EXISTS `t_return_receipt`;
DROP TABLE IF EXISTS `t_fulfillment_operation`;
DROP TABLE IF EXISTS `t_inventory_operation`;
DROP TABLE IF EXISTS `t_refund_item`;
DROP TABLE IF EXISTS `t_inventory_reservation`;
DROP TABLE IF EXISTS `t_payment_order`;
DROP TABLE IF EXISTS `t_order_idempotency`;
DROP TABLE IF EXISTS `t_order_item`;
DROP TABLE IF EXISTS `t_cart_item`;
DROP TABLE IF EXISTS `t_cart`;
DROP TABLE IF EXISTS `t_refresh_token`;
DROP TABLE IF EXISTS `t_user`;
DROP TABLE IF EXISTS `t_sku_stock`;
DROP TABLE IF EXISTS `t_product_sku`;

-- ----------------------------
-- Table structure for t_payment_channel
-- 支付渠道配置表：保存渠道级公共参数，不保存具体商户应用密钥
-- ----------------------------
DROP TABLE IF EXISTS `t_payment_app`;
DROP TABLE IF EXISTS `t_payment_channel`;
CREATE TABLE `t_payment_channel`  (
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

-- ----------------------------
-- Records of t_payment_channel
-- 来源：现有 wxpay.properties / alipay-sandbox.properties
-- ----------------------------
INSERT INTO `t_payment_channel` (`id`, `channel_name`, `channel_code`, `channel_status`, `channel_desc`, `config_params`, `sort_order`) VALUES
(1, '微信支付', 'WXPAY', 'ENABLED', '微信支付渠道公共配置', '{"domain":"https://api.mch.weixin.qq.com","notifyUrl":"https://5834-2409-8a50-3e25-e7f0-ecc6-e42-7b4c-1466.ngrok-free.app"}', 10),
(2, '支付宝', 'ALIPAY', 'ENABLED', '支付宝渠道公共配置', '{"gatewayUrl":"https://openapi-sandbox.dl.alipaydev.com/gateway.do","contentKey":"GD8AS9VQy0hZROhMzOARQw=="}', 20);

-- ----------------------------
-- Table structure for t_payment_app
-- 支付应用配置表：保存支付渠道下的商户/应用维度参数
-- ----------------------------
CREATE TABLE `t_payment_app`  (
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

-- ----------------------------
-- Records of t_payment_app
-- 来源：现有 wxpay.properties / alipay-sandbox.properties
-- ----------------------------
INSERT INTO `t_payment_app` (`id`, `app_name`, `app_code`, `app_status`, `channel_id`, `app_desc`, `app_config`, `sort_order`) VALUES
(1, '微信支付默认应用', 'WXPAY_DEFAULT', 'ENABLED', 1, '从现有微信支付配置文件迁移的默认应用', '{"appid":"wx74862e0dfcf69954","mchId":"1558950191","mchSerialNo":"34345964330B66427E0D3D28826C4993C77E631F","privateKeyPath":"apiclient_key.pem","apiV3Key":"UDuLFDcmy5Eb6o0nTNZdu6ek4DDh4K8B","partnerKey":"T6m9iK73b0kn9g5v426MKfHQH7X8rKwb","notifyUrl":"https://5834-2409-8a50-3e25-e7f0-ecc6-e42-7b4c-1466.ngrok-free.app"}', 10),
(2, '支付宝沙箱默认应用', 'ALIPAY_SANDBOX_DEFAULT', 'ENABLED', 2, '从现有支付宝沙箱配置文件迁移的默认应用', '{"appId":"9021000136667568","sellerId":"2088721034748965","merchantPrivateKey":"MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCCDhLjo3lEFNcWdMnehWhHPams3KbmxpVJTxmIglvMLt6j207pSlWxSvacT3wFebXW4herg9RWhjTTh7KFwkxRWwzBVfp63YvEUbNsF09X1K6NqT2Kqz/w77l38lhQxpVau8odV3nHR+KhtDD2YJ9z8C2AzJLzKCD5CdK5coP7k+mRx3/mlOEj07oA7H6CrDkm4j4oasqEeLfbyJrXYcpPMi2hIHMXkR21fX5905v9BxwPhkVFsxHOABPYMWu5+uPpjRH1e/HKZpo5sinud05oWI0b1waMRZ9AGUEJnNZ3Yg9b9b2k1ycnCkApzy8cgQOgGhr6Rr9V7ieAsOn0YahLAgMBAAECggEAeImcvjj0GsKJ+zkxJDlXRbgD+7/iPL/O+0wBqUDQ3fSOyyVnBNetho2o9YTBuL1uaIPSVlfvxGXMrkT1k/1aCIkv0Dzk011kvgbPGZ6dHhVz1r4F2PERaTh2GJKXgf4bzSWBlSJPLwEULrU4MBGrl6QCOH7ir9UAgnC1SsW1R8QkCbgXchZkh9s3LddJ0Skb9Qu144yhT3tSxIMDL/ZJqANIR0Smv9tUuP2+DehqBH8OEapW4c00mt7OWT0jQ8H/8BaFV3j0q8SCn3cmKBFeUzh9H2UlHjiVUeF9mRmiEJUK8/N/li/WZbiWwRXOcRlno54+aRbvlwo+A8gWZF2n+QKBgQC6EIONzns/n0wSB87X0ZWUb7Y3Q6aReu/JvFwNuUA1wbS4bw7PTAijvB0daLXke142ujt6tnghyQHQuQU+5xzA5q8Q3Ooy8OZo9YgCRzNmU20CltEWcWijOt7ndCxpEVDeB7dY55yavVI8hgWX5LFI0t4Zwa8u+c+QL9C48DUVNQKBgQCy8DbydcZdRx2XSYZGtd0p+yR6lFucxpIFIYt/CFXcKGLKasF7Yhds5RKderp/I/1WZl0kwpbTv17HuvJqdoDX4qLXhvlh30P9Pbvvk7YhIv9oR9NqRtTTr0E40jALTouAuEaWs1f49C6AfrWJC26jgNHWpdeEkUQ6NN6DaP73fwKBgAsDRTYUfZkDbbY3fheqEQdrIUbeGzLLKvwuyOgLCfDkmTS9ZgwA/RXr4XFHLFTstGPa3ABkYnHletUGznetqDcGsF/4I2iGd6zIs5cm7bTlxTL9CD0i00WuC1l5t9M0MiwiGskJVGyYPhDVAem+oHul931gyGSoZo+rNNhtZ0btAoGAU2gM9K9ZKxl+/YnUARm8YVkjA9Arc8RLRAEC2M+11c0tX1Sroytx59xO9QDD9Yd9CszkFcJuM308XLUTUfSy0e5eIUBU9f3v3xbrhxy/BGsfyifQr/UcNx+1sxqmMl8GP5WlsZEfLHgFRPfK/npJtATTys26y5w6xTbnkTFbx1kCgYB+2wDnTWavXJkQErniXh3ifmC7qLcBNBpSUrI9eAtI4AwqKfBnp7d+wGuh05epecb1gzE76bWPqgIX7N5zmpZaQ/qZL+CgcGpU/7PN/qKGrqa1JEu8ZMaDhFys6MkIpQIGMElgnbM0cXvNrY8ReKvcGGysdtLnIBrp9BXcm+uWSg==","alipayPublicKey":"MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAh9ivuRV+2eNbAzxXQ3bmx3wdhBHVaNQlhzv6wDXBMNhKG/AJDOAtYNbzPYJJPfySOrxyMlcTCvTGMS//Zn4yJHXx8IjJ4LSIguyy2BjDMwvDlF+TP7Xj6F/qtHWzuhztDnkLdAOwUx70Zyq1ajjzU5b2ku3J9WX5bwmnDpFGCVCwwG51mJYTd7Fwr4nE1qRZeMgGMhR7xR5Qdu1Nwx8Z+l1FCs47eZiirearT83/pwCRPD364SHq2uBJLtse9ozO7meBb8mzt6CDZKK6imEX1MKeVILbfE7GZAbtIAV4TLbvhB4VZuVxQrYmppPGhMpEiFDkpk0nFyNb7tz3HcHIQwIDAQAB","returnUrl":"http://localhost:8083/#/success","notifyUrl":"http://arhi.nat300.top/api/ali-pay/trade/notify"}', 20);

-- ----------------------------
-- Table structure for t_product
-- 商品表
-- ----------------------------
DROP TABLE IF EXISTS `t_product`;
CREATE TABLE `t_product`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '商品id',
  `title` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '商品名称',
  `price` int(11) NULL DEFAULT NULL COMMENT '价格（分）',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'OFF_SHELF' COMMENT '商品状态：ON_SHELF、OFF_SHELF',
  `available_stock` int(11) UNSIGNED NOT NULL DEFAULT 0 COMMENT '可用库存',
  `locked_stock` int(11) UNSIGNED NOT NULL DEFAULT 0 COMMENT '订单预占库存',
  `sold_stock` int(11) UNSIGNED NOT NULL DEFAULT 0 COMMENT '已售库存',
  `version` int(11) NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_product_status` (`status`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_product
-- ----------------------------
-- 商品总库存由 available_stock + locked_stock + sold_stock 计算，不保存冗余总库存列。
INSERT INTO `t_product` (`id`, `title`, `price`, `status`, `available_stock`, `locked_stock`, `sold_stock`, `version`, `create_time`, `update_time`) VALUES
(1, 'Java课程', 1, 'ON_SHELF', 100, 0, 0, 0, '2023-02-04 23:34:20', '2023-02-04 23:34:20'),
(2, '大数据课程', 1, 'ON_SHELF', 100, 0, 0, 0, '2023-02-04 23:34:20', '2023-02-04 23:34:20'),
(3, '前端课程', 1, 'ON_SHELF', 100, 0, 0, 0, '2023-02-04 23:34:20', '2023-02-04 23:34:20'),
(4, 'UI课程', 1, 'ON_SHELF', 100, 0, 0, 0, '2023-02-04 23:34:20', '2023-02-04 23:34:20');

-- ----------------------------
-- Table structure for t_product_sku / t_sku_stock
-- 本期每个商品一个默认SKU；t_sku_stock是新模型唯一在线库存余额
-- ----------------------------
CREATE TABLE `t_product_sku`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'SKU ID',
  `product_id` bigint(20) NOT NULL COMMENT '商品ID，本期一商品一SKU',
  `sku_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '稳定SKU编码',
  `sku_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'SKU名称',
  `sku_spec` json NULL COMMENT '规格快照来源，本期默认空对象',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE、INACTIVE',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_product_sku_product` (`product_id`) USING BTREE,
  UNIQUE KEY `uk_product_sku_code` (`sku_code`) USING BTREE,
  CONSTRAINT `fk_product_sku_product` FOREIGN KEY (`product_id`) REFERENCES `t_product` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

CREATE TABLE `t_sku_stock`  (
  `sku_id` bigint(20) UNSIGNED NOT NULL COMMENT 'SKU ID',
  `available_stock` int(11) NOT NULL DEFAULT 0 COMMENT '可用库存',
  `locked_stock` int(11) NOT NULL DEFAULT 0 COMMENT '订单预占库存',
  `version` bigint(20) NOT NULL DEFAULT 0 COMMENT '库存版本',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`sku_id`) USING BTREE,
  CONSTRAINT `fk_sku_stock_sku` FOREIGN KEY (`sku_id`) REFERENCES `t_product_sku` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_sku_stock_available` CHECK (`available_stock` >= 0),
  CONSTRAINT `chk_sku_stock_locked` CHECK (`locked_stock` >= 0),
  CONSTRAINT `chk_sku_stock_version` CHECK (`version` >= 0)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

INSERT INTO `t_product_sku` (`id`, `product_id`, `sku_code`, `sku_name`, `sku_spec`, `status`) VALUES
(1, 1, 'DEFAULT-1', 'Java课程默认规格', JSON_OBJECT(), 'ACTIVE'),
(2, 2, 'DEFAULT-2', '大数据课程默认规格', JSON_OBJECT(), 'ACTIVE'),
(3, 3, 'DEFAULT-3', '前端课程默认规格', JSON_OBJECT(), 'ACTIVE'),
(4, 4, 'DEFAULT-4', 'UI课程默认规格', JSON_OBJECT(), 'ACTIVE');

INSERT INTO `t_sku_stock` (`sku_id`, `available_stock`, `locked_stock`, `version`) VALUES
(1, 100, 0, 0),
(2, 100, 0, 0),
(3, 100, 0, 0),
(4, 100, 0, 0);

-- ----------------------------
-- Table structure for t_user
-- 用户表：密码只保存 BCrypt 哈希
-- ----------------------------
CREATE TABLE `t_user`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '登录用户名',
  `password_hash` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'BCrypt密码哈希',
  `role` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'USER' COMMENT '角色：USER、ADMIN',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_user_username` (`username`) USING BTREE,
  KEY `idx_user_role` (`role`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- 演示管理员，初始登录信息见 README；首次登录后请立即修改密码。
INSERT INTO `t_user` (`id`, `username`, `password_hash`, `role`) VALUES
(1, 'admin', '$2a$10$adpxTm2WcYIlEBzTd2dh0ObMLt/uQcjZY4q7zfMoRD9SPl4zVF5ZW', 'ADMIN');

-- ----------------------------
-- Table structure for t_refresh_token
-- 刷新令牌表：只保存原始令牌的 SHA-256 哈希
-- ----------------------------
CREATE TABLE `t_refresh_token`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '刷新令牌ID',
  `user_id` bigint(20) UNSIGNED NOT NULL COMMENT '用户ID',
  `token_hash` char(64) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL COMMENT '刷新令牌SHA-256哈希',
  `token_family` char(36) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL COMMENT '令牌族UUID',
  `expires_at` datetime NOT NULL COMMENT '过期时间',
  `revoked_at` datetime NULL DEFAULT NULL COMMENT '撤销时间',
  `replaced_by_hash` char(64) CHARACTER SET ascii COLLATE ascii_general_ci NULL DEFAULT NULL COMMENT '轮换后的令牌哈希',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_refresh_token_hash` (`token_hash`) USING BTREE,
  KEY `idx_refresh_user_family` (`user_id`, `token_family`) USING BTREE,
  KEY `idx_refresh_expires` (`expires_at`) USING BTREE,
  CONSTRAINT `fk_refresh_user` FOREIGN KEY (`user_id`) REFERENCES `t_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for t_cart / t_cart_item
-- 每个用户一个购物车，同一课程只保留一条明细
-- ----------------------------
CREATE TABLE `t_cart`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '购物车ID',
  `user_id` bigint(20) UNSIGNED NOT NULL COMMENT '用户ID',
  `version` int(11) NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_cart_user` (`user_id`) USING BTREE,
  CONSTRAINT `fk_cart_user` FOREIGN KEY (`user_id`) REFERENCES `t_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

CREATE TABLE `t_cart_item`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '购物车明细ID',
  `cart_id` bigint(20) UNSIGNED NOT NULL COMMENT '购物车ID',
  `product_id` bigint(20) NOT NULL COMMENT '课程ID',
  `quantity` int(11) UNSIGNED NOT NULL COMMENT '数量，业务限制1到99',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_cart_product` (`cart_id`, `product_id`) USING BTREE,
  KEY `idx_cart_item_product` (`product_id`) USING BTREE,
  CONSTRAINT `fk_cart_item_cart` FOREIGN KEY (`cart_id`) REFERENCES `t_cart` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_cart_item_product` FOREIGN KEY (`product_id`) REFERENCES `t_product` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for t_order_info
-- 订单表：包含乐观锁 version 字段，支持幂等性
-- ----------------------------
DROP TABLE IF EXISTS `t_order_info`;
CREATE TABLE `t_order_info`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '订单id',
  `title` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '订单标题',
  `order_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '商户订单编号',
  `user_id` bigint(20) UNSIGNED NULL DEFAULT NULL COMMENT '用户id，历史订单可为空',
  `product_id` bigint(20) NULL DEFAULT NULL COMMENT '直购产品id，购物车合单为空',
  `total_fee` int(11) NOT NULL COMMENT '订单金额(分)',
  `goods_amount` bigint(20) NOT NULL DEFAULT 0 COMMENT '商品原始总额(分)，新模型快照',
  `discount_amount` bigint(20) NOT NULL DEFAULT 0 COMMENT '优惠总额(分)',
  `freight_amount` bigint(20) NOT NULL DEFAULT 0 COMMENT '原运费(分)',
  `payable_amount` bigint(20) NOT NULL DEFAULT 0 COMMENT '订单应付总额(分)',
  `paid_amount` bigint(20) NOT NULL DEFAULT 0 COMMENT '唯一有效成交实付金额(分)',
  `refund_frozen_amount` bigint(20) NOT NULL DEFAULT 0 COMMENT '正常退款冻结金额(分)',
  `refunded_amount` bigint(20) NOT NULL DEFAULT 0 COMMENT '正常退款成功金额(分)',
  `freight_refund_frozen_amount` bigint(20) NOT NULL DEFAULT 0 COMMENT '运费退款冻结金额(分)',
  `freight_refunded_amount` bigint(20) NOT NULL DEFAULT 0 COMMENT '运费退款成功金额(分)',
  `effective_payment_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '唯一有效成交支付单号',
  `code_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '订单二维码连接',
  `order_status` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '订单状态',
  `pay_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'UNPAID' COMMENT 'UNPAID、PAID、PAID_EXCEPTION',
  `fulfillment_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'WAIT_SHIP' COMMENT '履约状态',
  `refund_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'NONE' COMMENT '正常售后汇总状态',
  `close_reason` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'TIMEOUT、USER_CANCEL、STOCK_SHORTAGE',
  `expires_at` datetime NULL DEFAULT NULL COMMENT '订单支付截止时间',
  `close_requested_at` datetime NULL DEFAULT NULL COMMENT '进入关单门闩时间',
  `paid_at` datetime NULL DEFAULT NULL COMMENT '有效成交时间',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `payment_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '支付类型：支付宝，微信',
  `payment_app_id` bigint(20) UNSIGNED NULL DEFAULT NULL COMMENT '支付应用ID',
  `payment_channel_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '支付渠道编码：WXPAY、ALIPAY',
  `checkout_request_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '购物车结算幂等请求号',
  `version` int(11) NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_order_no` (`order_no`) USING BTREE,
  UNIQUE KEY `uk_order_effective_payment` (`effective_payment_no`) USING BTREE,
  UNIQUE KEY `uk_order_user_checkout` (`user_id`, `checkout_request_id`) USING BTREE,
  KEY `idx_product_status_pay_type` (`product_id`, `order_status`, `payment_type`) USING BTREE,
  KEY `idx_product_payment_status_time` (`product_id`, `payment_type`, `order_status`, `create_time`) USING BTREE,
  KEY `idx_payment_app` (`payment_app_id`, `create_time`) USING BTREE,
  KEY `idx_order_user_time` (`user_id`, `create_time`) USING BTREE,
  KEY `idx_order_close_scan` (`order_status`, `expires_at`, `close_requested_at`) USING BTREE,
  CONSTRAINT `fk_order_user` FOREIGN KEY (`user_id`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_order_amounts_nonnegative` CHECK (`goods_amount` >= 0 AND `discount_amount` >= 0 AND `freight_amount` >= 0 AND `payable_amount` >= 0 AND `paid_amount` >= 0 AND `refund_frozen_amount` >= 0 AND `refunded_amount` >= 0 AND `freight_refund_frozen_amount` >= 0 AND `freight_refunded_amount` >= 0),
  CONSTRAINT `chk_order_refund_cap` CHECK (`refunded_amount` + `refund_frozen_amount` <= `paid_amount`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for t_order_item
-- 订单课程快照：支付、退款和对账仍以订单总额为边界
-- ----------------------------
CREATE TABLE `t_order_item`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '订单明细ID',
  `order_id` bigint(20) UNSIGNED NOT NULL COMMENT '订单ID',
  `product_id` bigint(20) NOT NULL COMMENT '课程ID',
  `sku_id` bigint(20) UNSIGNED NULL DEFAULT NULL COMMENT '默认SKU ID；迁移完成后新记录必填',
  `product_title` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '下单时课程标题快照',
  `unit_price` int(11) NOT NULL COMMENT '下单时单价快照（分）',
  `quantity` int(11) UNSIGNED NOT NULL COMMENT '购买数量',
  `subtotal` int(11) NOT NULL COMMENT '小计（分）',
  `product_name_snapshot` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '商品名称成交快照',
  `sku_name_snapshot` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'SKU名称成交快照',
  `sku_spec_snapshot` json NULL COMMENT 'SKU规格成交快照',
  `original_unit_amount` bigint(20) NOT NULL DEFAULT 0 COMMENT '原始单价(分)',
  `deal_unit_amount` bigint(20) NOT NULL DEFAULT 0 COMMENT '成交单价(分)',
  `original_total_amount` bigint(20) NOT NULL DEFAULT 0 COMMENT '原始行总额(分)',
  `discount_amount` bigint(20) NOT NULL DEFAULT 0 COMMENT '行优惠分摊(分)',
  `pay_amount` bigint(20) NOT NULL DEFAULT 0 COMMENT '订单行最终承担金额(分)',
  `refund_frozen_qty` int(11) NOT NULL DEFAULT 0 COMMENT '退款申请冻结数量',
  `refunded_qty` int(11) NOT NULL DEFAULT 0 COMMENT '正常商品退款成功数量',
  `refund_frozen_amount` bigint(20) NOT NULL DEFAULT 0 COMMENT '退款申请冻结金额(分)',
  `refunded_amount` bigint(20) NOT NULL DEFAULT 0 COMMENT '正常商品退款成功金额(分)',
  `shipped_qty` int(11) NOT NULL DEFAULT 0 COMMENT '累计发货数量',
  `cancel_confirmed_qty` int(11) NOT NULL DEFAULT 0 COMMENT '不可逆取消履约数量',
  `received_qty` int(11) NOT NULL DEFAULT 0 COMMENT '用户确认收货数量',
  `restocked_qty` int(11) NOT NULL DEFAULT 0 COMMENT '有证据实际回到可售库存数量',
  `inventory_status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'RESERVED' COMMENT '库存状态：RESERVED、SOLD、RELEASED',
  `refunded_quantity` int(11) UNSIGNED NOT NULL DEFAULT 0 COMMENT '累计成功退款数量',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_order_item_order` (`order_id`) USING BTREE,
  KEY `idx_order_item_product` (`product_id`) USING BTREE,
  KEY `idx_order_item_sku` (`sku_id`) USING BTREE,
  CONSTRAINT `fk_order_item_order` FOREIGN KEY (`order_id`) REFERENCES `t_order_info` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_order_item_product` FOREIGN KEY (`product_id`) REFERENCES `t_product` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_order_item_sku` FOREIGN KEY (`sku_id`) REFERENCES `t_product_sku` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_order_item_quantity` CHECK (`quantity` > 0),
  CONSTRAINT `chk_order_item_amounts` CHECK (`original_unit_amount` >= 0 AND `deal_unit_amount` >= 0 AND `original_total_amount` >= 0 AND `discount_amount` >= 0 AND `pay_amount` >= 0 AND `refund_frozen_amount` >= 0 AND `refunded_amount` >= 0),
  CONSTRAINT `chk_order_item_refund_qty` CHECK (`refund_frozen_qty` >= 0 AND `refunded_qty` >= 0 AND `refund_frozen_qty` + `refunded_qty` <= `quantity`),
  CONSTRAINT `chk_order_item_refund_amount` CHECK (`refund_frozen_amount` + `refunded_amount` <= `pay_amount`),
  CONSTRAINT `chk_order_item_fulfillment` CHECK (`shipped_qty` >= 0 AND `cancel_confirmed_qty` >= 0 AND `received_qty` >= 0 AND `restocked_qty` >= 0 AND `shipped_qty` + `cancel_confirmed_qty` <= `quantity` AND `received_qty` <= `shipped_qty`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for t_order_idempotency
-- 后端签发的一次性下单幂等键；COMPLETED 记录忽略 expires_at
-- ----------------------------
CREATE TABLE `t_order_idempotency`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '幂等记录ID',
  `user_id` bigint(20) UNSIGNED NOT NULL COMMENT '签发目标用户ID',
  `idempotency_key` char(36) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL COMMENT '后端签发UUID',
  `request_fingerprint` char(64) CHARACTER SET ascii COLLATE ascii_general_ci NULL DEFAULT NULL COMMENT '订单意图SHA-256指纹',
  `order_id` bigint(20) UNSIGNED NULL DEFAULT NULL COMMENT '成功绑定的订单ID',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'ISSUED' COMMENT 'ISSUED、COMPLETED、EXPIRED',
  `expires_at` datetime NOT NULL COMMENT '未使用键的首次提交截止时间',
  `completed_at` datetime NULL DEFAULT NULL COMMENT '绑定订单时间',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_order_idempotency_key` (`idempotency_key`) USING BTREE,
  UNIQUE KEY `uk_order_idempotency_order` (`order_id`) USING BTREE,
  KEY `idx_order_idempotency_user_status` (`user_id`, `status`, `expires_at`) USING BTREE,
  CONSTRAINT `fk_order_idempotency_user` FOREIGN KEY (`user_id`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_order_idempotency_order` FOREIGN KEY (`order_id`) REFERENCES `t_order_info` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for t_inventory_reservation
-- 每个订单明细一条独立预占，库存归业务订单而不是支付尝试
-- ----------------------------
CREATE TABLE `t_inventory_reservation`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '库存预占ID',
  `reservation_no` varchar(64) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL COMMENT '全局预占号',
  `order_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '业务订单号',
  `order_item_id` bigint(20) UNSIGNED NOT NULL COMMENT '订单明细ID',
  `sku_id` bigint(20) UNSIGNED NOT NULL COMMENT 'SKU ID',
  `quantity` int(11) NOT NULL COMMENT '预占数量',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'LOCKED、COMMITTED、RELEASED、ANOMALY_PENDING、ANOMALY',
  `expires_at` datetime NOT NULL COMMENT '预占到期时间',
  `committed_at` datetime NULL DEFAULT NULL COMMENT '提交为成交时间',
  `released_at` datetime NULL DEFAULT NULL COMMENT '释放时间',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_reservation_no` (`reservation_no`) USING BTREE,
  UNIQUE KEY `uk_reservation_order_item` (`order_item_id`) USING BTREE,
  KEY `idx_reservation_order_status` (`order_no`, `status`) USING BTREE,
  KEY `idx_reservation_sku_status` (`sku_id`, `status`) USING BTREE,
  CONSTRAINT `fk_reservation_order_item` FOREIGN KEY (`order_item_id`) REFERENCES `t_order_item` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_reservation_sku` FOREIGN KEY (`sku_id`) REFERENCES `t_product_sku` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_reservation_quantity` CHECK (`quantity` > 0)
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for t_payment_order
-- 业务订单1:N支付尝试；status保存渠道事实，disposition保存本地裁决
-- ----------------------------
CREATE TABLE `t_payment_order`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '支付尝试ID',
  `payment_no` varchar(64) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL COMMENT '本地支付单号',
  `order_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '业务订单号',
  `payment_app_id` bigint(20) UNSIGNED NOT NULL COMMENT '固化支付应用ID',
  `channel` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'WXPAY、ALIPAY',
  `protocol` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'V2、V3、ALIPAY',
  `channel_merchant_order_no` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '已提交渠道的商户订单号，不可变',
  `channel_transaction_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '渠道交易号',
  `request_amount` bigint(20) NOT NULL COMMENT '请求金额(分)',
  `paid_amount` bigint(20) NOT NULL DEFAULT 0 COMMENT '渠道确认交易总额(分)',
  `payer_cash_amount` bigint(20) NULL DEFAULT NULL COMMENT '付款人现金金额(分)',
  `receipt_amount` bigint(20) NULL DEFAULT NULL COMMENT '商户实收金额(分)',
  `currency` varchar(8) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL DEFAULT 'CNY' COMMENT '币种',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'CREATED、CREATING、PENDING、UNKNOWN、SUCCESS、CLOSED、FAILED',
  `disposition` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'NONE' COMMENT 'NONE、EFFECTIVE、DUPLICATE、LATE、STOCK_SHORTAGE',
  `request_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '客户端稳定支付尝试请求号',
  `request_fingerprint` char(64) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL COMMENT '规范化请求SHA-256',
  `payment_params` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '渠道创建结果/支付参数',
  `refund_frozen_amount` bigint(20) NOT NULL DEFAULT 0 COMMENT '该支付单退款冻结金额(分)',
  `refunded_amount` bigint(20) NOT NULL DEFAULT 0 COMMENT '该支付单退款成功金额(分)',
  `expires_at` datetime NULL DEFAULT NULL COMMENT '支付尝试失效时间',
  `paid_at` datetime NULL DEFAULT NULL COMMENT '渠道付款成功时间',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_payment_no` (`payment_no`) USING BTREE,
  UNIQUE KEY `uk_payment_request` (`order_no`, `request_id`) USING BTREE,
  UNIQUE KEY `uk_payment_merchant_no` (`payment_app_id`, `channel_merchant_order_no`) USING BTREE,
  UNIQUE KEY `uk_payment_transaction` (`payment_app_id`, `channel_transaction_id`) USING BTREE,
  KEY `idx_payment_order_status` (`order_no`, `status`) USING BTREE,
  KEY `idx_payment_recovery` (`status`, `expires_at`, `update_time`) USING BTREE,
  CONSTRAINT `fk_payment_order_order` FOREIGN KEY (`order_no`) REFERENCES `t_order_info` (`order_no`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_payment_order_app` FOREIGN KEY (`payment_app_id`) REFERENCES `t_payment_app` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_payment_order_amounts` CHECK (`request_amount` >= 0 AND `paid_amount` >= 0 AND (`payer_cash_amount` IS NULL OR `payer_cash_amount` >= 0) AND (`receipt_amount` IS NULL OR `receipt_amount` >= 0) AND `refund_frozen_amount` >= 0 AND `refunded_amount` >= 0 AND `refund_frozen_amount` + `refunded_amount` <= `paid_amount`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for t_payment_info
-- 支付到账审计流水；支付尝试生命周期在t_payment_order
-- ----------------------------
DROP TABLE IF EXISTS `t_payment_info`;
CREATE TABLE `t_payment_info`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '支付记录id',
  `payment_no` varchar(64) CHARACTER SET ascii COLLATE ascii_general_ci NULL DEFAULT NULL COMMENT '关联本地支付单号；历史可空',
  `payment_app_id` bigint(20) UNSIGNED NULL DEFAULT NULL COMMENT '实际支付应用ID；历史可空',
  `order_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '商户订单编号',
  `transaction_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '支付系统交易编号',
  `payment_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '支付类型',
  `trade_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '交易类型',
  `trade_state` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '交易状态',
  `payer_total` int(11) NULL DEFAULT NULL COMMENT '支付金额(分)',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '通知参数',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_payment_info_payment_no` (`payment_no`) USING BTREE,
  UNIQUE KEY `uk_payment_info_transaction` (`payment_app_id`, `transaction_id`) USING BTREE,
  KEY `idx_order_no` (`order_no`) USING BTREE,
  KEY `idx_payment_info_order_type` (`order_no`, `payment_type`) USING BTREE,
  CONSTRAINT `fk_payment_info_payment` FOREIGN KEY (`payment_no`) REFERENCES `t_payment_order` (`payment_no`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_payment_info_app` FOREIGN KEY (`payment_app_id`) REFERENCES `t_payment_app` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for t_refund_info
-- 退款表：双重幂等控制
-- ----------------------------
DROP TABLE IF EXISTS `t_refund_info`;
CREATE TABLE `t_refund_info`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '退款单id',
  `order_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '商户订单编号',
  `refund_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '商户退款单编号',
  `refund_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '支付系统退款单号',
  `payment_no` varchar(64) CHARACTER SET ascii COLLATE ascii_general_ci NULL DEFAULT NULL COMMENT '原支付单号；历史可空',
  `refund_source` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'USER' COMMENT 'USER、SYSTEM',
  `refund_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '类型化退款场景',
  `application_request_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户退款申请幂等请求号',
  `request_fingerprint` char(64) CHARACTER SET ascii COLLATE ascii_general_ci NULL DEFAULT NULL COMMENT '规范化退款请求SHA-256',
  `system_refund_key` varchar(160) CHARACTER SET ascii COLLATE ascii_general_ci NULL DEFAULT NULL COMMENT '仅系统退款填充：type:paymentNo',
  `total_fee` int(11) NULL DEFAULT NULL COMMENT '原订单金额(分)',
  `refund` int(11) NULL DEFAULT NULL COMMENT '退款金额(分)',
  `goods_refund_amount` bigint(20) NOT NULL DEFAULT 0 COMMENT '商品退款金额(分)',
  `freight_refund_amount` bigint(20) NOT NULL DEFAULT 0 COMMENT '原运费退款金额(分)',
  `refund_amount` bigint(20) NOT NULL DEFAULT 0 COMMENT '本退款单总金额(分)',
  `reason` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '退款原因',
  `approval_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '审核状态',
  `approve_remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '审核备注',
  `approved_time` datetime NULL DEFAULT NULL COMMENT '审核时间',
  `refund_status` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '退款状态',
  `channel_refund_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '渠道退款单号',
  `channel_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '渠道退款状态',
  `requested_at` datetime NULL DEFAULT NULL COMMENT '申请时间',
  `success_time` datetime NULL DEFAULT NULL COMMENT '退款成功时间',
  `content_return` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '申请退款返回参数',
  `content_notify` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '退款结果通知参数',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_refund_no` (`refund_no`) USING BTREE,
  UNIQUE KEY `uk_refund_id` (`refund_id`) USING BTREE,
  UNIQUE KEY `uk_refund_order_request` (`order_no`, `application_request_id`) USING BTREE,
  UNIQUE KEY `uk_refund_system_key` (`system_refund_key`) USING BTREE,
  KEY `idx_order_approval_status` (`order_no`, `approval_status`) USING BTREE,
  KEY `idx_order_status` (`order_no`, `refund_status`) USING BTREE,
  KEY `idx_refund_payment_status` (`payment_no`, `refund_status`) USING BTREE,
  CONSTRAINT `fk_refund_payment` FOREIGN KEY (`payment_no`) REFERENCES `t_payment_order` (`payment_no`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_refund_amounts` CHECK (`goods_refund_amount` >= 0 AND `freight_refund_amount` >= 0 AND `refund_amount` >= 0 AND `goods_refund_amount` + `freight_refund_amount` = `refund_amount`),
  CONSTRAINT `chk_refund_system_key` CHECK ((`refund_source` = 'SYSTEM' AND `system_refund_key` IS NOT NULL) OR (`refund_source` = 'USER' AND `system_refund_key` IS NULL))
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for t_refund_item
-- 退款商品明细：金额由订单快照单价和退款数量计算
-- ----------------------------
CREATE TABLE `t_refund_item`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '退款明细ID',
  `refund_id` bigint(20) UNSIGNED NOT NULL COMMENT '退款单ID',
  `order_item_id` bigint(20) UNSIGNED NOT NULL COMMENT '订单明细ID',
  `product_id` bigint(20) NOT NULL COMMENT '商品ID',
  `sku_id` bigint(20) UNSIGNED NULL DEFAULT NULL COMMENT 'SKU ID；历史可空',
  `product_title` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '商品标题快照',
  `unit_price` int(11) NOT NULL COMMENT '订单单价快照（分）',
  `quantity` int(11) UNSIGNED NOT NULL COMMENT '退款数量',
  `refund_amount` bigint(20) NOT NULL COMMENT '明细退款金额（分）',
  `quota_kind` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'GOODS' COMMENT 'GOODS、COMPENSATION',
  `received_qty` int(11) NOT NULL DEFAULT 0 COMMENT '退货累计验收数量',
  `accepted_qty` int(11) NOT NULL DEFAULT 0 COMMENT '质检可接受数量',
  `restock_authorized_qty` int(11) NOT NULL DEFAULT 0 COMMENT '实物证据授权可回库数量',
  `restocked_qty` int(11) NOT NULL DEFAULT 0 COMMENT '实际回到可售库存数量',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'CREATED' COMMENT '退款明细状态',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_refund_item` (`refund_id`, `order_item_id`) USING BTREE,
  KEY `idx_refund_item_order_item` (`order_item_id`) USING BTREE,
  KEY `idx_refund_item_product` (`product_id`) USING BTREE,
  KEY `idx_refund_item_sku` (`sku_id`) USING BTREE,
  CONSTRAINT `fk_refund_item_refund` FOREIGN KEY (`refund_id`) REFERENCES `t_refund_info` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_refund_item_order_item` FOREIGN KEY (`order_item_id`) REFERENCES `t_order_item` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_refund_item_product` FOREIGN KEY (`product_id`) REFERENCES `t_product` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_refund_item_sku` FOREIGN KEY (`sku_id`) REFERENCES `t_product_sku` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_refund_item_values` CHECK (`quantity` >= 0 AND `refund_amount` >= 0 AND `received_qty` >= 0 AND `accepted_qty` >= 0 AND `restock_authorized_qty` >= 0 AND `restocked_qty` >= 0),
  CONSTRAINT `chk_refund_item_physical_cap` CHECK (`accepted_qty` <= `received_qty` AND `restock_authorized_qty` <= `quantity` AND `restocked_qty` <= `restock_authorized_qty`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for t_return_receipt
-- 分批退货验收证据；可售数量才授权增加available_stock
-- ----------------------------
CREATE TABLE `t_return_receipt`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '退货验收批次ID',
  `refund_id` bigint(20) UNSIGNED NOT NULL COMMENT '退款单ID',
  `refund_item_id` bigint(20) UNSIGNED NOT NULL COMMENT '退款明细ID',
  `receipt_request_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '稳定验收请求号',
  `request_fingerprint` char(64) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL COMMENT '验收请求SHA-256',
  `received_qty` int(11) NOT NULL COMMENT '本批收货数量',
  `saleable_qty` int(11) NOT NULL COMMENT '本批可再次销售数量',
  `rejected_qty` int(11) NOT NULL COMMENT '本批不可售数量',
  `operator_id` bigint(20) UNSIGNED NOT NULL COMMENT '验收管理员ID',
  `operator_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '操作人快照',
  `reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '验收原因/说明',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_return_receipt_request` (`refund_item_id`, `receipt_request_id`) USING BTREE,
  KEY `idx_return_receipt_refund` (`refund_id`, `create_time`) USING BTREE,
  CONSTRAINT `fk_return_receipt_refund` FOREIGN KEY (`refund_id`) REFERENCES `t_refund_info` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_return_receipt_item` FOREIGN KEY (`refund_item_id`) REFERENCES `t_refund_item` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_return_receipt_operator` FOREIGN KEY (`operator_id`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_return_receipt_quantity` CHECK (`received_qty` > 0 AND `saleable_qty` >= 0 AND `rejected_qty` >= 0 AND `saleable_qty` + `rejected_qty` = `received_qty`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for t_fulfillment_operation
-- 发货、收货、取消履约按稳定请求号留痕
-- ----------------------------
CREATE TABLE `t_fulfillment_operation`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '履约操作ID',
  `order_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '业务订单号',
  `order_item_id` bigint(20) UNSIGNED NOT NULL COMMENT '订单明细ID',
  `request_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '稳定操作请求号',
  `request_fingerprint` char(64) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL COMMENT '规范化请求SHA-256',
  `operation_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'SHIP、RECEIVE、CANCEL',
  `quantity` int(11) NOT NULL COMMENT '本次操作数量',
  `before_quantity` int(11) NOT NULL COMMENT '操作前对应累计数量',
  `after_quantity` int(11) NOT NULL COMMENT '操作后对应累计数量',
  `operator_id` bigint(20) UNSIGNED NOT NULL COMMENT '操作用户ID',
  `operator_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '操作人快照',
  `reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '操作原因',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_fulfillment_operation` (`order_no`, `request_id`, `order_item_id`, `operation_type`) USING BTREE,
  KEY `idx_fulfillment_item_time` (`order_item_id`, `create_time`) USING BTREE,
  CONSTRAINT `fk_fulfillment_order` FOREIGN KEY (`order_no`) REFERENCES `t_order_info` (`order_no`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_fulfillment_item` FOREIGN KEY (`order_item_id`) REFERENCES `t_order_item` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_fulfillment_operator` FOREIGN KEY (`operator_id`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_fulfillment_quantity` CHECK (`quantity` > 0 AND `before_quantity` >= 0 AND `after_quantity` >= `before_quantity` AND `after_quantity` = `before_quantity` + `quantity`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for t_inventory_operation
-- 操作类型：ADMIN_ADJUST、ORDER_RESERVE、ORDER_COMMIT、ORDER_RELEASE、REFUND_RESTORE
-- ----------------------------
CREATE TABLE `t_inventory_operation`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '库存流水ID',
  `business_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '库存变化全局幂等键',
  `product_id` bigint(20) NOT NULL COMMENT '商品ID',
  `sku_id` bigint(20) UNSIGNED NULL DEFAULT NULL COMMENT '新模型SKU ID；历史可空',
  `order_item_id` bigint(20) UNSIGNED NULL DEFAULT NULL COMMENT '关联订单明细ID',
  `receipt_request_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '关联退货验收批次请求号',
  `operation_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '库存操作类型：ADMIN_ADJUST、ORDER_RESERVE、ORDER_COMMIT、ORDER_RELEASE、REFUND_RESTORE',
  `order_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '关联订单号',
  `refund_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '关联退款单号',
  `available_delta` int(11) NOT NULL DEFAULT 0 COMMENT '可用库存变化量',
  `locked_delta` int(11) NOT NULL DEFAULT 0 COMMENT '锁定库存变化量',
  `sold_delta` int(11) NOT NULL DEFAULT 0 COMMENT '已售库存变化量',
  `available_before` int(11) UNSIGNED NOT NULL COMMENT '变化前可用库存',
  `available_after` int(11) UNSIGNED NOT NULL COMMENT '变化后可用库存',
  `locked_before` int(11) UNSIGNED NOT NULL COMMENT '变化前锁定库存',
  `locked_after` int(11) UNSIGNED NOT NULL COMMENT '变化后锁定库存',
  `sold_before` int(11) UNSIGNED NOT NULL COMMENT '变化前已售库存',
  `sold_after` int(11) UNSIGNED NOT NULL COMMENT '变化后已售库存',
  `operator_id` bigint(20) UNSIGNED NULL DEFAULT NULL COMMENT '操作用户ID，系统操作可为空',
  `operator_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '操作人快照',
  `reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '操作原因',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_inventory_business_key` (`business_key`) USING BTREE,
  KEY `idx_inventory_product_time` (`product_id`, `create_time`) USING BTREE,
  KEY `idx_inventory_sku_time` (`sku_id`, `create_time`) USING BTREE,
  KEY `idx_inventory_order_no` (`order_no`) USING BTREE,
  KEY `idx_inventory_refund_no` (`refund_no`) USING BTREE,
  CONSTRAINT `fk_inventory_product` FOREIGN KEY (`product_id`) REFERENCES `t_product` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_inventory_sku` FOREIGN KEY (`sku_id`) REFERENCES `t_product_sku` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_inventory_order_item` FOREIGN KEY (`order_item_id`) REFERENCES `t_order_item` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_inventory_operator` FOREIGN KEY (`operator_id`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for t_message_outbox
-- 业务事务内落库，RabbitMQ 发布确认后才标记 SENT
-- ----------------------------
CREATE TABLE `t_message_outbox`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Outbox记录ID',
  `event_id` char(36) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL COMMENT '稳定事件ID',
  `event_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '业务事件唯一键',
  `aggregate_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '聚合类型',
  `aggregate_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '聚合标识',
  `event_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '事件类型',
  `payload` json NOT NULL COMMENT '事件内容JSON',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'NEW' COMMENT 'NEW、SENDING、SENT、FAILED',
  `retry_count` int(11) UNSIGNED NOT NULL DEFAULT 0 COMMENT '发布重试次数',
  `next_retry_time` datetime NULL DEFAULT NULL COMMENT '下次重试时间',
  `locked_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '认领实例',
  `lock_expire_time` datetime NULL DEFAULT NULL COMMENT '认领过期时间',
  `last_error` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '最后发布错误',
  `sent_at` datetime NULL DEFAULT NULL COMMENT '确认发布时间',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_outbox_event_id` (`event_id`) USING BTREE,
  UNIQUE KEY `uk_outbox_event_key` (`event_key`) USING BTREE,
  KEY `idx_outbox_status_retry` (`status`, `next_retry_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for t_message_consume_log
-- Inbox 消费记录与本地业务状态在同一事务提交
-- ----------------------------
CREATE TABLE `t_message_consume_log`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Inbox记录ID',
  `event_id` char(36) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL COMMENT '事件ID',
  `consumer_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '消费者名称',
  `event_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '事件类型',
  `business_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '本地业务幂等键',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'PROCESSING' COMMENT 'PROCESSING、CONSUMED、FAILED',
  `locked_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '消费租约持有者',
  `lock_expire_time` datetime NULL DEFAULT NULL COMMENT '消费租约过期时间',
  `last_error` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '最后消费错误',
  `consumed_at` datetime NULL DEFAULT NULL COMMENT '消费完成时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_consume_event_consumer` (`event_id`, `consumer_name`) USING BTREE,
  KEY `idx_consume_business_key` (`business_key`) USING BTREE,
  KEY `idx_consume_status_lease` (`status`, `lock_expire_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for t_channel_bill
-- 渠道账单表：导入的渠道账单原文，作为对账依据
-- ----------------------------
DROP TABLE IF EXISTS `t_channel_bill`;
CREATE TABLE `t_channel_bill`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '渠道账单ID',
  `bill_date` date NOT NULL COMMENT '账单日期',
  `channel_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '支付渠道编码：WXPAY、ALIPAY',
  `payment_app_id` bigint(20) UNSIGNED NULL DEFAULT NULL COMMENT '支付应用ID',
  `bill_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'ALL' COMMENT '账单子类型：ALL/SUCCESS/REFUND',
  `bill_source` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '账单来源：AUTO_DOWNLOAD（API拉取）/MANUAL_UPLOAD（手动上传）',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'IMPORTED' COMMENT '账单状态：IMPORTED（已导入）',
  `record_count` int(11) NOT NULL DEFAULT 0 COMMENT '账单解析记录数',
  `total_amount` bigint(20) NOT NULL DEFAULT 0 COMMENT '账单总金额（分）',
  `bill_hash` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '账单原文哈希（SHA-256）',
  `file_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '上传文件名（手动上传时记录）',
  `bill_content` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '账单原文（CSV）',
  `error_message` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '错误信息',
  `import_time` datetime NULL DEFAULT NULL COMMENT '导入时间',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_bill_date_channel_app` (`bill_date`, `channel_code`, `payment_app_id`, `bill_type`) USING BTREE,
  KEY `idx_channel_bill_date` (`channel_code`, `bill_date`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC COMMENT = '渠道账单表（对账依据）';

-- ----------------------------
-- Table structure for t_reconciliation
-- 对账主表：记录每一次对账任务的执行情况和统计结果
-- ----------------------------
DROP TABLE IF EXISTS `t_reconciliation_detail`;
DROP TABLE IF EXISTS `t_reconciliation`;
CREATE TABLE `t_reconciliation`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '对账记录ID',
  `bill_date` date NOT NULL COMMENT '对账日期',
  `channel_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '支付渠道编码：WXPAY、ALIPAY',
  `payment_app_id` bigint(20) UNSIGNED NULL DEFAULT NULL COMMENT '支付应用ID',
  `bill_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'ALL' COMMENT '账单子类型：ALL/SUCCESS/REFUND',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'PENDING' COMMENT '对账状态：PENDING/PROCESSING/COMPLETED/FAILED',
  `total_count` int(11) NOT NULL DEFAULT 0 COMMENT '总笔数（渠道账单笔数 + 本地多单笔数）',
  `match_count` int(11) NOT NULL DEFAULT 0 COMMENT '匹配笔数',
  `diff_count` int(11) NOT NULL DEFAULT 0 COMMENT '差异笔数',
  `diff_amount` int(11) NOT NULL DEFAULT 0 COMMENT '差异金额合计（分）',
  `channel_total_amount` bigint(20) NOT NULL DEFAULT 0 COMMENT '渠道账单总金额（分）',
  `local_total_amount` bigint(20) NOT NULL DEFAULT 0 COMMENT '本地订单总金额（分）',
  `bill_hash` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '渠道账单原文哈希（SHA-256），用于审计追溯',
  `bill_id` bigint(20) UNSIGNED NULL DEFAULT NULL COMMENT '关联渠道账单ID（t_channel_bill.id）',
  `error_message` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '错误信息',
  `start_time` datetime NULL DEFAULT NULL COMMENT '对账开始时间',
  `end_time` datetime NULL DEFAULT NULL COMMENT '对账结束时间',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_bill_date_channel_app` (`bill_date`, `channel_code`, `payment_app_id`, `bill_type`) USING BTREE,
  KEY `idx_channel_status` (`channel_code`, `status`) USING BTREE,
  KEY `idx_bill_date` (`bill_date`) USING BTREE,
  KEY `idx_bill_id` (`bill_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC COMMENT = '对账主表';

-- ----------------------------
-- Table structure for t_reconciliation_detail
-- 对账明细表：记录每一笔订单的对账结果
-- ----------------------------
CREATE TABLE `t_reconciliation_detail`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '对账明细ID',
  `reconciliation_id` bigint(20) UNSIGNED NOT NULL COMMENT '对账记录ID',
  `diff_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '差异类型：MATCH/MISSING_LOCAL/MISSING_CHANNEL/AMOUNT_MISMATCH/STATUS_MISMATCH',
  `business_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '业务类型：PAYMENT（进账）/REFUND（退款）',
  `order_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '商户订单号',
  `transaction_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '渠道交易号',
  `refund_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '商户退款单号',
  `refund_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '渠道退款单号',
  `channel_amount` int(11) NULL DEFAULT NULL COMMENT '渠道金额（分）',
  `local_amount` int(11) NULL DEFAULT NULL COMMENT '本地金额（分）',
  `channel_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '渠道状态',
  `local_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '本地状态',
  `diff_amount` int(11) NULL DEFAULT NULL COMMENT '差异金额（分）',
  `channel_trade_time` datetime NULL DEFAULT NULL COMMENT '渠道交易时间',
  `local_trade_time` datetime NULL DEFAULT NULL COMMENT '本地交易时间',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_reconciliation_id` (`reconciliation_id`) USING BTREE,
  KEY `idx_diff_type` (`reconciliation_id`, `diff_type`) USING BTREE,
  KEY `idx_business_type` (`reconciliation_id`, `business_type`) USING BTREE,
  KEY `idx_order_no` (`order_no`) USING BTREE,
  KEY `idx_refund_no` (`refund_no`) USING BTREE,
  CONSTRAINT `fk_recon_detail_recon` FOREIGN KEY (`reconciliation_id`) REFERENCES `t_reconciliation` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC COMMENT = '对账明细表';

-- ===============================================================
-- 并发控制数据库层兜底能力说明：
-- ===============================================================
-- 1. t_order_info.uk_order_no：防止商户订单号重复
-- 2. t_payment_order.uk_payment_request：同一支付意图只对应一个paymentNo
-- 3. t_payment_order.uk_payment_transaction：同一应用的渠道交易事实只落一次
-- 4. t_payment_info不再限制(order_no,payment_type)，允许同单同渠道多次真实付款
-- 5. t_inventory_reservation.uk_reservation_order_item：每个订单明细只预占一次
-- 6. t_refund_info.uk_refund_no / uk_refund_system_key：用户请求和系统补偿分别防重
-- 7. t_return_receipt.uk_return_receipt_request：同一验收批次只回库一次
-- 8. t_order_info.uk_order_user_checkout：防止同一用户重复提交同一次购物车结算
-- 9. t_order_idempotency.uk_order_idempotency_key：防止一次下单意图创建多张订单
-- 10. t_inventory_operation.uk_inventory_business_key：防止同一业务重复变更库存
-- 11. t_message_outbox.uk_outbox_event_key：防止业务事务重复创建事件
-- 12. t_message_consume_log.uk_consume_event_consumer：防止消费者重复处理事件
--
-- 配合代码层的优化：
-- - 通知Inbox租约与稳定事件号（数据库为正确性边界）
-- - Redisson 分布式锁（看门狗自动续期）
-- - 数据库行锁 (select ... for update)
-- - 状态条件更新 (update ... where status = ...)
-- ===============================================================

SET FOREIGN_KEY_CHECKS = 1;
