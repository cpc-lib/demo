-- ===============================================================
-- Payment Demo 完整数据库初始化脚本（一键执行）
-- 合并了所有版本的改进，包括：
-- 1. 基础表结构（商品、订单、支付、退款）
-- 2. 支付渠道/应用配置表
-- 3. V3 幂等性增强（乐观锁、唯一约束、索引优化）
-- 4. V4/V5/V6 并发控制增强（数据库层兜底能力）
-- 5. 对账功能（对账主表 + 对账明细表）
-- ===============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

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
-- Table structure for t_order_info
-- 订单表：包含乐观锁 version 字段，支持幂等性
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
  `payment_app_id` bigint(20) UNSIGNED NULL DEFAULT NULL COMMENT '支付应用ID',
  `payment_channel_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '支付渠道编码：WXPAY、ALIPAY',
  `version` int(11) NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_order_no` (`order_no`) USING BTREE,
  KEY `idx_product_status_pay_type` (`product_id`, `order_status`, `payment_type`) USING BTREE,
  KEY `idx_product_payment_status_time` (`product_id`, `payment_type`, `order_status`, `create_time`) USING BTREE,
  KEY `idx_payment_app` (`payment_app_id`, `create_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for t_payment_info
-- 支付流水表：双重幂等控制
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
-- Table structure for t_refund_info
-- 退款表：双重幂等控制
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
-- 2. t_payment_info.uk_order_payment_type：防止同一订单同一支付方式重复写支付流水
-- 3. t_payment_info.uk_transaction_id_payment_type：防止同一渠道交易号重复写支付流水
-- 4. t_refund_info.uk_refund_no：防止商户退款单号重复
-- 5. t_refund_info.uk_refund_id：防止渠道退款单号重复
-- 6. t_reconciliation.uk_bill_date_channel_app：防止同一日期同一渠道同一应用重复对账
--
-- 配合代码层的优化：
-- - 通知幂等检查（Redis + notifyId）
-- - Redisson 分布式锁（看门狗自动续期）
-- - 数据库行锁 (select ... for update)
-- - 状态条件更新 (update ... where status = ...)
-- ===============================================================

SET FOREIGN_KEY_CHECKS = 1;
