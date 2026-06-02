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
  `version` int(11) NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_order_no` (`order_no`) USING BTREE,
  KEY `idx_product_status_pay_type` (`product_id`, `order_status`, `payment_type`) USING BTREE,
  KEY `idx_product_payment_status_time` (`product_id`, `payment_type`, `order_status`, `create_time`) USING BTREE
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
