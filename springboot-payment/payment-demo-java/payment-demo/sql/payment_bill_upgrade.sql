-- ===============================================================
-- SPEC-006: 渠道账单导入与对账管理 - 增量升级脚本
-- 适用：已按旧版 payment-demo.sql 初始化的库
-- 新装直接执行 payment-demo.sql 即可，无需本脚本
-- ===============================================================

SET NAMES = utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 新增渠道账单表（对账依据）
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
-- t_reconciliation 新增账单关联列
-- ----------------------------
ALTER TABLE `t_reconciliation`
    ADD COLUMN `bill_id` bigint(20) UNSIGNED NULL DEFAULT NULL COMMENT '关联渠道账单ID（t_channel_bill.id）' AFTER `bill_hash`,
    ADD INDEX `idx_bill_id` (`bill_id`) USING BTREE;

SET FOREIGN_KEY_CHECKS = 1;
