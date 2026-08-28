CREATE DATABASE `bank2` CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci';

CREATE TABLE `account_info`
(
    `id`               bigint(20) NOT NULL AUTO_INCREMENT,
    `account_name`     varchar(100) CHARACTER SET utf8 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '户
主姓名',
    `account_no`       varchar(100) CHARACTER SET utf8 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '银行
卡号',
    `account_password` varchar(100) CHARACTER SET utf8 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT
        '帐户密码',
    `account_balance` double NULL DEFAULT NULL COMMENT '帐户余额',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8 COLLATE = utf8mb4_general_ci ROW_FORMAT =
Dynamic;
INSERT INTO `account_info`
VALUES (3, '李四的账户', '2', NULL, 0);


CREATE TABLE `undo_log`
(
    `id`            bigint(20) NOT NULL AUTO_INCREMENT,
    `branch_id`     bigint(20) NOT NULL,
    `xid`           varchar(100) NOT NULL,
    `context`       varchar(128) NOT NULL,
    `rollback_info` longblob     NOT NULL,
    `log_status`    int(11) NOT NULL,
    `log_created`   datetime     NOT NULL,
    `log_modified`  datetime     NOT NULL,
    `ext`           varchar(100) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `ux_undo_log` (`xid`,`branch_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4;