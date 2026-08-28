DROP TABLE IF EXISTS `account_pay`;

CREATE TABLE `account_pay`
(
    `id`         varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
    `account_no` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '账号',
    `pay_amount` double DEFAULT NULL COMMENT '充值余额',
    `result`     varchar(20) COLLATE utf8mb4_general_ci  DEFAULT NULL COMMENT '充值结果:success，fail',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC;