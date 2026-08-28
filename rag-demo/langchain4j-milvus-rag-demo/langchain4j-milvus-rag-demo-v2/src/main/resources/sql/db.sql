CREATE TABLE `work_order`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',

    `ticket_no`   VARCHAR(64)  NOT NULL COMMENT '工单编号（唯一）',
    `title`       VARCHAR(255) NOT NULL COMMENT '工单标题',

    `status`      VARCHAR(32)  NOT NULL COMMENT '工单状态（NEW/PROCESSING/DONE/CLOSED）',
    `priority`    VARCHAR(32)           DEFAULT NULL COMMENT '优先级（LOW/MEDIUM/HIGH/URGENT）',

    `assignee_id` VARCHAR(64)           DEFAULT NULL COMMENT '处理人ID',
    `assignee`    VARCHAR(64)           DEFAULT NULL COMMENT '处理人姓名',

    `creator`     VARCHAR(64)           DEFAULT NULL COMMENT '创建人',

    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),

    UNIQUE KEY `uk_ticket_no` (`ticket_no`),

    KEY           `idx_status` (`status`),
    KEY           `idx_assignee_id` (`assignee_id`),
    KEY           `idx_created_at` (`created_at`)

) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_general_ci
COMMENT='工单表';