-- =====================================================================
-- 订单库：seata_order
-- =====================================================================
CREATE DATABASE IF NOT EXISTS `seata_order` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `seata_order`;

-- 订单主表
CREATE TABLE IF NOT EXISTS `order_info` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `order_no` VARCHAR(64) NOT NULL UNIQUE COMMENT '业务订单号(唯一约束保证业务幂等)',
    `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
    `commodity_code` VARCHAR(64) NOT NULL COMMENT '商品编码',
    `count` INT NOT NULL COMMENT '购买数量',
    `money` DECIMAL(12, 2) NOT NULL COMMENT '订单金额',
    `status` VARCHAR(32) NOT NULL DEFAULT 'INIT' COMMENT '状态: INIT, SUCCESS, FAILED, CANCELLED',
    `mode` VARCHAR(16) NOT NULL COMMENT '事务模式: AT, TCC, SAGA, XA',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 业务防重与接口幂等表
CREATE TABLE IF NOT EXISTS `idempotent_record` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `idempotent_key` VARCHAR(128) NOT NULL UNIQUE COMMENT '幂等唯一标识',
    `biz_type` VARCHAR(64) NOT NULL COMMENT '业务类型',
    `status` VARCHAR(32) NOT NULL COMMENT '状态: PROCESSING, SUCCESS, FAILED',
    `response_data` TEXT COMMENT '成功时的缓存响应数据',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
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

-- Seata SAGA 状态机状态定义表
CREATE TABLE IF NOT EXISTS `seata_state_machine_def` (
    `id` VARCHAR(32) NOT NULL COMMENT 'id',
    `name` VARCHAR(128) NOT NULL COMMENT 'name',
    `tenant_id` VARCHAR(32) NOT NULL COMMENT 'tenant id',
    `app_name` VARCHAR(32) NOT NULL COMMENT 'application name',
    `type` VARCHAR(32) DEFAULT NULL COMMENT 'state language type',
    `comment_` VARCHAR(255) DEFAULT NULL COMMENT 'comment',
    `ver` VARCHAR(16) NOT NULL COMMENT 'version',
    `content` MEDIUMTEXT COMMENT 'content',
    `status` VARCHAR(32) NOT NULL COMMENT 'status',
    `recover_strategy` VARCHAR(16) DEFAULT NULL COMMENT 'transaction recover strategy',
    `gmt_create` DATETIME(6) NOT NULL COMMENT 'create time',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SAGA state machine definition';

-- Seata SAGA 状态机实例表
CREATE TABLE IF NOT EXISTS `seata_state_machine_inst` (
    `id` VARCHAR(128) NOT NULL COMMENT 'id',
    `machine_id` VARCHAR(32) NOT NULL COMMENT 'machine id',
    `tenant_id` VARCHAR(32) NOT NULL COMMENT 'tenant id',
    `parent_id` VARCHAR(128) DEFAULT NULL COMMENT 'parent id',
    `gmt_started` DATETIME(6) NOT NULL COMMENT 'start time',
    `business_key` VARCHAR(48) DEFAULT NULL COMMENT 'business key',
    `start_params` MEDIUMTEXT COMMENT 'start params',
    `gmt_end` DATETIME(6) DEFAULT NULL COMMENT 'end time',
    `excep` BLOB COMMENT 'exception',
    `end_params` MEDIUMTEXT COMMENT 'end params',
    `status` VARCHAR(32) DEFAULT NULL COMMENT 'status',
    `compensation_status` VARCHAR(32) DEFAULT NULL COMMENT 'compensation status',
    `is_running` TINYINT(1) DEFAULT NULL COMMENT 'is running',
    `gmt_updated` DATETIME(6) NOT NULL COMMENT 'update time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `un_ser_inst_biz_key` (`business_key`, `tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SAGA state machine instance';

-- Seata SAGA 状态节点实例表
CREATE TABLE IF NOT EXISTS `seata_state_inst` (
    `id` VARCHAR(48) NOT NULL COMMENT 'id',
    `machine_inst_id` VARCHAR(128) NOT NULL COMMENT 'machine instance id',
    `name` VARCHAR(128) NOT NULL COMMENT 'state name',
    `type` VARCHAR(20) DEFAULT NULL COMMENT 'state type',
    `service_name` VARCHAR(128) DEFAULT NULL COMMENT 'service name',
    `service_method` VARCHAR(128) DEFAULT NULL COMMENT 'method name',
    `service_type` VARCHAR(16) DEFAULT NULL COMMENT 'service type',
    `business_key` VARCHAR(48) DEFAULT NULL COMMENT 'business key',
    `state_id_comp_for` VARCHAR(50) DEFAULT NULL COMMENT 'state id compensation for',
    `status` VARCHAR(32) NOT NULL COMMENT 'status',
    `input_params` MEDIUMTEXT COMMENT 'input params',
    `output_params` MEDIUMTEXT COMMENT 'output params',
    `excep` BLOB COMMENT 'exception',
    `gmt_started` DATETIME(6) NOT NULL COMMENT 'start time',
    `gmt_end` DATETIME(6) DEFAULT NULL COMMENT 'end time',
    PRIMARY KEY (`id`, `machine_inst_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SAGA state instance';
