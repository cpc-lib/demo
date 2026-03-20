-- 建议：MySQL 5.7 / 8.0
CREATE DATABASE IF NOT EXISTS order_demo DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE order_demo;

DROP TABLE IF EXISTS t_order_item;
DROP TABLE IF EXISTS t_order;
DROP TABLE IF EXISTS t_user;

CREATE TABLE t_user (
                        id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
                        username      VARCHAR(64)  NOT NULL COMMENT '用户名',
                        phone         VARCHAR(32)  NULL COMMENT '手机号',
                        email         VARCHAR(128) NULL COMMENT '邮箱',
                        status        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
                        created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                        updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                        PRIMARY KEY (id),
                        UNIQUE KEY uk_user_username (username),
                        KEY idx_user_phone (phone),
                        KEY idx_user_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE t_order (
                         id              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '订单ID',
                         user_id         BIGINT        NOT NULL COMMENT '用户ID',
                         order_no        VARCHAR(32)   NOT NULL COMMENT '业务订单号',
                         total_amount    DECIMAL(18,2) NOT NULL COMMENT '订单总金额',
                         status          VARCHAR(16)   NOT NULL COMMENT '订单状态：CREATED/PAID/CANCELLED/SHIPPED/COMPLETED',
                         remark          VARCHAR(255)  NULL COMMENT '备注',
                         created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                         updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                         PRIMARY KEY (id),
                         UNIQUE KEY uk_order_no (order_no),
                         KEY idx_order_user_time (user_id, created_at),
                         CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES t_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

CREATE TABLE t_order_item (
                              id            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '明细ID',
                              order_id      BIGINT        NOT NULL COMMENT '订单ID',
                              product_id    BIGINT        NOT NULL COMMENT '商品ID',
                              product_name  VARCHAR(128)  NOT NULL COMMENT '商品名',
                              unit_price    DECIMAL(18,2) NOT NULL COMMENT '单价',
                              quantity      INT           NOT NULL COMMENT '数量',
                              line_amount   DECIMAL(18,2) NOT NULL COMMENT '行金额=单价*数量',
                              created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                              PRIMARY KEY (id),
                              KEY idx_item_order (order_id),
                              CONSTRAINT fk_item_order FOREIGN KEY (order_id) REFERENCES t_order(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';
