-- ===============================================================
-- Payment Demo V6 完整数据库初始化脚本
-- 合并了所有版本的改进，包括：
-- 1. 基础表结构
-- 2. V3 幂等性增强 (乐观锁、唯一约束、索引优化)
-- 3. V4/V5/V6 并发控制增强 (数据库层兜底能力)
-- ===============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for t_order_info
-- 订单表：包含乐观锁 version 字段，支持幂等性
-- 索引优化：
--   - uk_order_no：防止商户订单号重复
--   - idx_product_payment_status_time：支持查询场景优化
-- ----------------------------
DROP TABLE IF EXISTS `t_order_info`;
CREATE TABLE `t_order_info`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '订单id',
  `title` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '订单标题',
  `order_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '商户订单编号',
  `user_id` bigint(20) NULL DEFAULT NULL COMMENT '用户id',
  `product_id` bigint(20) NOT NULL COMMENT '支付产品id',
  `total_fee` int(11) NOT NULL COMMENT '订单金额(分)',
  `code_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '订单二维码连接',
  `order_status` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '订单状态',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `payment_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '支付类型：支付宝，微信',
  `payment_app_id` bigint(20) NULL DEFAULT NULL COMMENT '支付应用id',
  `version` int(11) NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_order_no` (`order_no`) USING BTREE,
  KEY `idx_product_status_pay_type` (`product_id`, `order_status`, `payment_type`) USING BTREE,
  KEY `idx_product_payment_status_time` (`product_id`, `payment_type`, `order_status`, `create_time`) USING BTREE,
  KEY `idx_payment_app_status` (`payment_app_id`, `order_status`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for t_payment_info
-- 支付流水表：双重幂等控制
-- 索引优化：
--   - uk_order_payment_type：防止同一订单同一支付方式重复写支付流水
--   - uk_transaction_id_payment_type：防止同一渠道交易号重复写支付流水
-- ----------------------------
DROP TABLE IF EXISTS `t_payment_info`;
CREATE TABLE `t_payment_info`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '支付记录id',
  `order_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '商户订单编号',
  `transaction_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '支付系统交易编号',
  `payment_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '支付类型',
  `trade_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '交易类型',
  `trade_state` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '交易状态',
  `payer_total` int(11) NULL DEFAULT NULL COMMENT '支付金额(分)',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '通知参数',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_order_payment_type` (`order_no`, `payment_type`) USING BTREE,
  UNIQUE KEY `uk_transaction_id_payment_type` (`transaction_id`, `payment_type`) USING BTREE,
  KEY `idx_order_no` (`order_no`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for t_product
-- 商品表
-- ----------------------------
DROP TABLE IF EXISTS `t_product`;
CREATE TABLE `t_product`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '商品id',
  `title` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '商品名称',
  `price` int(11) NULL DEFAULT NULL COMMENT '价格（分）',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_product
-- ----------------------------
INSERT INTO `t_product` VALUES (1, 'Java课程', 1, '2023-02-04 23:34:20', '2023-02-04 23:34:20');
INSERT INTO `t_product` VALUES (2, '大数据课程', 1, '2023-02-04 23:34:20', '2023-02-04 23:34:20');
INSERT INTO `t_product` VALUES (3, '前端课程', 1, '2023-02-04 23:34:20', '2023-02-04 23:34:20');
INSERT INTO `t_product` VALUES (4, 'UI课程', 1, '2023-02-04 23:34:20', '2023-02-04 23:34:20');

-- ----------------------------
-- Table structure for t_payment_channel
-- 支付渠道表：保存微信、支付宝等渠道级配置
-- ----------------------------
DROP TABLE IF EXISTS `t_payment_channel`;
CREATE TABLE `t_payment_channel`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '支付渠道id',
  `channel_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '支付渠道编码',
  `channel_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '支付渠道名称',
  `payment_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '支付类型：支付宝，微信',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  `config_params` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '渠道级配置参数JSON',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_channel_code` (`channel_code`) USING BTREE,
  KEY `idx_payment_type_enabled` (`payment_type`, `enabled`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_payment_channel
-- ----------------------------
INSERT INTO `t_payment_channel` (`id`, `channel_code`, `channel_name`, `payment_type`, `enabled`, `config_params`, `create_time`, `update_time`) VALUES
(1, 'wxpay', '微信支付', '微信', 1, '{"domain":"https://api.mch.weixin.qq.com","notifyDomain":"https://5834-2409-8a50-3e25-e7f0-ecc6-e42-7b4c-1466.ngrok-free.app"}', '2023-02-04 23:34:20', '2023-02-04 23:34:20'),
(2, 'alipay', '支付宝', '支付宝', 1, '{"gatewayUrl":"https://openapi-sandbox.dl.alipaydev.com/gateway.do"}', '2023-02-04 23:34:20', '2023-02-04 23:34:20');

-- ----------------------------
-- Table structure for t_payment_app
-- 支付应用表：保存每个支付渠道下的应用/商户配置
-- ----------------------------
DROP TABLE IF EXISTS `t_payment_app`;
CREATE TABLE `t_payment_app`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '支付应用id',
  `app_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '支付应用编码',
  `app_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '支付应用名称',
  `channel_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '支付渠道编码',
  `payment_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '支付类型：支付宝，微信',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  `app_config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '应用/商户配置JSON',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_app_code` (`app_code`) USING BTREE,
  KEY `idx_channel_enabled` (`channel_code`, `enabled`) USING BTREE,
  KEY `idx_payment_type_enabled` (`payment_type`, `enabled`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_payment_app
-- ----------------------------
INSERT INTO `t_payment_app` (`id`, `app_code`, `app_name`, `channel_code`, `payment_type`, `enabled`, `app_config`, `create_time`, `update_time`) VALUES
(1, 'wxpay-default', '微信支付默认应用', 'wxpay', '微信', 1, '{"appid":"wx74862e0dfcf69954","mchId":"1558950191","mchSerialNo":"34345964330B66427E0D3D28826C4993C77E631F","privateKeyPath":"apiclient_key.pem","apiV3Key":"UDuLFDcmy5Eb6o0nTNZdu6ek4DDh4K8B","partnerKey":"T6m9iK73b0kn9g5v426MKfHQH7X8rKwb","domain":"https://api.mch.weixin.qq.com","notifyDomain":"https://5834-2409-8a50-3e25-e7f0-ecc6-e42-7b4c-1466.ngrok-free.app"}', '2023-02-04 23:34:20', '2023-02-04 23:34:20'),
(2, 'alipay-sandbox', '支付宝沙箱应用', 'alipay', '支付宝', 1, '{"appId":"9021000136667568","sellerId":"2088721034748965","gatewayUrl":"https://openapi-sandbox.dl.alipaydev.com/gateway.do","merchantPrivateKey":"MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCCDhLjo3lEFNcWdMnehWhHPams3KbmxpVJTxmIglvMLt6j207pSlWxSvacT3wFebXW4herg9RWhjTTh7KFwkxRWwzBVfp63YvEUbNsF09X1K6NqT2Kqz/w77l38lhQxpVau8odV3nHR+KhtDD2YJ9z8C2AzJLzKCD5CdK5coP7k+mRx3/mlOEj07oA7H6CrDkm4j4oasqEeLfbyJrXYcpPMi2hIHMXkR21fX5905v9BxwPhkVFsxHOABPYMWu5+uPpjRH1e/HKZpo5sinud05oWI0b1waMRZ9AGUEJnNZ3Yg9b9b2k1ycnCkApzy8cgQOgGhr6Rr9V7ieAsOn0YahLAgMBAAECggEAeImcvjj0GsKJ+zkxJDlXRbgD+7/iPL/O+0wBqUDQ3fSOyyVnBNetho2o9YTBuL1uaIPSVlfvxGXMrkT1k/1aCIkv0Dzk011kvgbPGZ6dHhVz1r4F2PERaTh2GJKXgf4bzSWBlSJPLwEULrU4MBGrl6QCOH7ir9UAgnC1SsW1R8QkCbgXchZkh9s3LddJ0Skb9Qu144yhT3tSxIMDL/ZJqANIR0Smv9tUuP2+DehqBH8OEapW4c00mt7OWT0jQ8H/8BaFV3j0q8SCn3cmKBFeUzh9H2UlHjiVUeF9mRmiEJUK8/N/li/WZbiWwRXOcRlno54+aRbvlwo+A8gWZF2n+QKBgQC6EIONzns/n0wSB87X0ZWUb7Y3Q6aReu/JvFwNuUA1wbS4bw7PTAijvB0daLXke142ujt6tnghyQHQuQU+5xzA5q8Q3Ooy8OZo9YgCRzNmU20CltEWcWijOt7ndCxpEVDeB7dY55yavVI8hgWX5LFI0t4Zwa8u+c+QL9C48DUVNQKBgQCy8DbydcZdRx2XSYZGtd0p+yR6lFucxpIFIYt/CFXcKGLKasF7Yhds5RKderp/I/1WZl0kwpbTv17HuvJqdoDX4qLXhvlh30P9Pbvvk7YhIv9oR9NqRtTTr0E40jALTouAuEaWs1f49C6AfrWJC26jgNHWpdeEkUQ6NN6DaP73fwKBgAsDRTYUfZkDbbY3fheqEQdrIUbeGzLLKvwuyOgLCfDkmTS9ZgwA/RXr4XFHLFTstGPa3ABkYnHletUGznetqDcGsF/4I2iGd6zIs5cm7bTlxTL9CD0i00WuC1l5t9M0MiwiGskJVGyYPhDVAem+oHul931gyGSoZo+rNNhtZ0btAoGAU2gM9K9ZKxl+/YnUARm8YVkjA9Arc8RLRAEC2M+11c0tX1Sroytx59xO9QDD9Yd9CszkFcJuM308XLUTUfSy0e5eIUBU9f3v3xbrhxy/BGsfyifQr/UcNx+1sxqmMl8GP5WlsZEfLHgFRPfK/npJtATTys26y5w6xTbnkTFbx1kCgYB+2wDnTWavXJkQErniXh3ifmC7qLcBNBpSUrI9eAtI4AwqKfBnp7d+wGuh05epecb1gzE76bWPqgIX7N5zmpZaQ/qZL+CgcGpU/7PN/qKGrqa1JEu8ZMaDhFys6MkIpQIGMElgnbM0cXvNrY8ReKvcGGysdtLnIBrp9BXcm+uWSg==","alipayPublicKey":"MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAh9ivuRV+2eNbAzxXQ3bmx3wdhBHVaNQlhzv6wDXBMNhKG/AJDOAtYNbzPYJJPfySOrxyMlcTCvTGMS//Zn4yJHXx8IjJ4LSIguyy2BjDMwvDlF+TP7Xj6F/qtHWzuhztDnkLdAOwUx70Zyq1ajjzU5b2ku3J9WX5bwmnDpFGCVCwwG51mJYTd7Fwr4nE1qRZeMgGMhR7xR5Qdu1Nwx8Z+l1FCs47eZiirearT83/pwCRPD364SHq2uBJLtse9ozO7meBb8mzt6CDZKK6imEX1MKeVILbfE7GZAbtIAV4TLbvhB4VZuVxQrYmppPGhMpEiFDkpk0nFyNb7tz3HcHIQwIDAQAB","contentKey":"GD8AS9VQy0hZROhMzOARQw==","returnUrl":"http://localhost:8083/#/success","notifyUrl":"http://arhi.nat300.top/api/ali-pay/trade/notify"}', '2023-02-04 23:34:20', '2023-02-04 23:34:20');

-- ----------------------------
-- Table structure for t_refund_info
-- 退款表：双重幂等控制
-- 索引优化：
--   - uk_refund_no：防止商户退款单号重复
--   - uk_refund_id：防止渠道退款单号重复
--   - idx_order_approval_status：优化审核查询场景
--   - idx_order_status：支持退款状态同步查询优化
-- ----------------------------
DROP TABLE IF EXISTS `t_refund_info`;
CREATE TABLE `t_refund_info`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '退款单id',
  `order_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '商户订单编号',
  `refund_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '商户退款单编号',
  `refund_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '支付系统退款单号',
  `total_fee` int(11) NULL DEFAULT NULL COMMENT '原订单金额(分)',
  `refund` int(11) NULL DEFAULT NULL COMMENT '退款金额(分)',
  `reason` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '退款原因',
  `approval_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '审核状态',
  `approve_remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '审核备注',
  `approved_time` datetime NULL DEFAULT NULL COMMENT '审核时间',
  `refund_status` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '退款状态',
  `content_return` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '申请退款返回参数',
  `content_notify` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '退款结果通知参数',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_refund_no` (`refund_no`) USING BTREE,
  UNIQUE KEY `uk_refund_id` (`refund_id`) USING BTREE,
  KEY `idx_order_approval_status` (`order_no`, `approval_status`) USING BTREE,
  KEY `idx_order_status` (`order_no`, `refund_status`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ===============================================================
-- V6 并发控制数据库层兜底能力说明：
-- ===============================================================
-- 1. t_order_info.uk_order_no：防止商户订单号重复
-- 2. t_payment_info.uk_order_payment_type：防止同一订单同一支付方式重复写支付流水
-- 3. t_payment_info.uk_transaction_id_payment_type：防止同一渠道交易号重复写支付流水
-- 4. t_refund_info.uk_refund_no：防止商户退款单号重复
-- 5. t_refund_info.uk_refund_id：防止渠道退款单号重复
--
-- 配合代码层的优化：
-- - 通知幂等检查（Redis + notifyId）
-- - Redisson 分布式锁（看门狗自动续期）
-- - 数据库行锁 (select ... for update)
-- - 状态条件更新 (update ... where status = ...)
-- ===============================================================

SET FOREIGN_KEY_CHECKS = 1;
