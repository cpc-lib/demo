-- =====================================================================
-- 账户库：seata_account
-- =====================================================================
CREATE DATABASE IF NOT EXISTS `seata_account` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `seata_account`;

-- 账户表
CREATE TABLE IF NOT EXISTS `account_tbl` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `user_id` VARCHAR(64) NOT NULL UNIQUE COMMENT '用户编码',
    `total_amount` DECIMAL(12, 2) NOT NULL DEFAULT 0.00 COMMENT '总资产',
    `balance` DECIMAL(12, 2) NOT NULL DEFAULT 0.00 COMMENT '可用余额',
    `frozen_amount` DECIMAL(12, 2) NOT NULL DEFAULT 0.00 COMMENT '冻结金额(TCC模式使用)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户账户表';

-- TCC 模式冻结记录表 (用于自定义 TCC 幂等、空回滚、防悬挂记录)
CREATE TABLE IF NOT EXISTS `account_freeze_tbl` (
    `xid` VARCHAR(128) NOT NULL COMMENT '全局事务XID',
    `branch_id` BIGINT NOT NULL COMMENT '分支事务ID',
    `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
    `amount` DECIMAL(12, 2) NOT NULL COMMENT '冻结金额',
    `status` INT NOT NULL COMMENT '状态: 1-TRY(预留), 2-CONFIRM(已扣减), 3-CANCEL(已回滚释放)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`xid`, `branch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账户TCC预留冻结记录表';

-- SAGA 模式正向执行与补偿防重记录表 (保证补偿幂等与防悬挂)
CREATE TABLE IF NOT EXISTS `account_saga_record` (
    `business_key` VARCHAR(128) NOT NULL PRIMARY KEY COMMENT '业务幂等Key(如order_no或xid)',
    `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
    `amount` DECIMAL(12, 2) NOT NULL COMMENT '扣减金额',
    `forward_status` TINYINT NOT NULL DEFAULT 0 COMMENT '正向状态: 0-未执行, 1-成功, 2-失败',
    `compensate_status` TINYINT NOT NULL DEFAULT 0 COMMENT '补偿状态: 0-未补偿, 1-已补偿',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账户SAGA操作与补偿幂等记录表';

-- 通用接口幂等记录表
CREATE TABLE IF NOT EXISTS `idempotent_record` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `idempotent_key` VARCHAR(128) NOT NULL UNIQUE COMMENT '幂等唯一标识',
    `biz_type` VARCHAR(64) NOT NULL COMMENT '业务类型',
    `status` VARCHAR(32) NOT NULL COMMENT '状态: PROCESSING, SUCCESS, FAILED',
    `response_data` TEXT COMMENT '响应数据缓存',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通用接口幂等记录表';

-- Seata AT 模式需要的 undo_log 表
CREATE TABLE IF NOT EXISTS `undo_log` (
    `branch_id` BIGINT NOT NULL COMMENT 'branch transaction id',
    `xid` VARCHAR(128) NOT NULL COMMENT 'global transaction id',
    `context` VARCHAR(128) NOT NULL COMMENT 'undo_log context,such as serialization',
    `rollback_info` LONGBLOB NOT NULL COMMENT 'rollback info',
    `log_status` INT NOT NULL COMMENT '0:NormalStatus,1:DefenseStatus',
    `log_created` DATETIME(6) NOT NULL COMMENT 'create datetime',
    `log_modified` DATETIME(6) NOT NULL COMMENT 'modify datetime',
    UNIQUE KEY `ux_undo_log` (`xid`, `branch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AT transaction mode undo table';

-- Seata TCC 防悬挂与幂等 Fence Log 表
CREATE TABLE IF NOT EXISTS `tcc_fence_log` (
    `xid` VARCHAR(128) NOT NULL COMMENT 'global id',
    `branch_id` BIGINT NOT NULL COMMENT 'branch id',
    `action_name` VARCHAR(64) NOT NULL COMMENT 'action name',
    `status` TINYINT NOT NULL COMMENT 'status(status: 1-tried 2-committed 3-rollbacked)',
    `gmt_create` DATETIME(3) NOT NULL COMMENT 'create time',
    `gmt_modified` DATETIME(3) NOT NULL COMMENT 'update time',
    PRIMARY KEY (`xid`, `branch_id`),
    KEY `idx_gmt_modified` (`gmt_modified`),
    KEY `idx_action_name` (`action_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='TCC fence log';

-- 初始化测试账户数据: 用户U10001初始可用余额10000元
INSERT INTO `account_tbl` (`user_id`, `total_amount`, `balance`, `frozen_amount`)
VALUES ('U10001', 10000.00, 10000.00, 0.00)
ON DUPLICATE KEY UPDATE `balance` = 10000.00, `frozen_amount` = 0.00;
