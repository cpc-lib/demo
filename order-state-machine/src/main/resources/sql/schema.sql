CREATE DATABASE IF NOT EXISTS `test` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `test`;

DROP TABLE IF EXISTS `t_order_state_log`;
DROP TABLE IF EXISTS `t_order_state_transition`;
DROP TABLE IF EXISTS `t_order`;

CREATE TABLE `t_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_no` VARCHAR(64) NOT NULL COMMENT '订单号',
  `business_type` VARCHAR(32) NOT NULL COMMENT '业务类型',
  `status` VARCHAR(32) NOT NULL COMMENT '订单状态',
  `before_refund_status` VARCHAR(32) DEFAULT NULL COMMENT '退款前状态',
  `amount` DECIMAL(16,2) NOT NULL DEFAULT 0 COMMENT '订单金额',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

CREATE TABLE `t_order_state_transition` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `business_type` VARCHAR(32) NOT NULL COMMENT '业务类型',
  `current_status` VARCHAR(32) NOT NULL COMMENT '当前状态',
  `event` VARCHAR(32) NOT NULL COMMENT '触发事件',
  `next_status` VARCHAR(32) NOT NULL COMMENT '目标状态',
  `is_enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
  `rollback_mode` VARCHAR(32) NOT NULL DEFAULT 'NONE' COMMENT '回退模式:NONE/PREVIOUS_STATUS',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_status_event` (`business_type`, `current_status`, `event`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单状态流转规则表';

CREATE TABLE `t_order_state_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id` BIGINT NOT NULL COMMENT '订单ID',
  `order_no` VARCHAR(64) NOT NULL COMMENT '订单号',
  `business_type` VARCHAR(32) NOT NULL COMMENT '业务类型',
  `from_status` VARCHAR(32) NOT NULL COMMENT '原状态',
  `event` VARCHAR(32) NOT NULL COMMENT '触发事件',
  `to_status` VARCHAR(32) NOT NULL COMMENT '目标状态',
  `operator` VARCHAR(64) DEFAULT NULL COMMENT '操作人',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单状态流转日志表';
