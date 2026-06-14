CREATE TABLE `base_photo_type`
(
    `id`           varchar(64) COLLATE utf8mb4_general_ci  NOT NULL COMMENT '主键ID',
    `parent_id`    varchar(64) COLLATE utf8mb4_general_ci  NOT NULL DEFAULT '0' COMMENT '父级ID，0表示根节点',
    `layer`        int                                     NOT NULL COMMENT '层级，从1开始',
    `name`         varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
    `name_en`      varchar(255) COLLATE utf8mb4_general_ci          DEFAULT NULL COMMENT '英文名称',
    `sort`         varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '同级排序，从1开始连续',
    `name_rule`    varchar(255) COLLATE utf8mb4_general_ci          DEFAULT NULL COMMENT '中文命名规则',
    `name_rule_en` varchar(255) COLLATE utf8mb4_general_ci          DEFAULT NULL COMMENT '英文命名规则',
    `is_deleted`   tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-否，1-是',
    `create_time`  datetime                                NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  datetime                                NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `ancestors`    varchar(1000) COLLATE utf8mb4_general_ci         DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_parent_name_deleted` (`parent_id`,`name`,`is_deleted`),
    KEY            `idx_parent_deleted_sort` (`parent_id`,`is_deleted`,`sort`),
    KEY            `idx_parent_id` (`parent_id`),
    KEY            `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='照片类型表';