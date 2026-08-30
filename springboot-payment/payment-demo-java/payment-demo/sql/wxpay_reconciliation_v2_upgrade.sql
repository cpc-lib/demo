-- ===============================================================
-- SPEC-007: 微信进账与退款逐笔对账兼容扩展 - v0.2.0
-- 适用：已存在 t_reconciliation_detail 的数据库
-- 新装直接执行 payment-demo.sql，无需本脚本
-- ===============================================================

SET NAMES = utf8mb4;

ALTER TABLE `t_reconciliation_detail`
    ADD COLUMN `business_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '业务类型：PAYMENT（进账）/REFUND（退款）' AFTER `diff_type`,
    ADD COLUMN `refund_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '商户退款单号' AFTER `transaction_id`,
    ADD COLUMN `refund_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '渠道退款单号' AFTER `refund_no`,
    ADD INDEX `idx_business_type` (`reconciliation_id`, `business_type`) USING BTREE,
    ADD INDEX `idx_refund_no` (`refund_no`) USING BTREE;
