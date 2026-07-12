USE order_demo;

CREATE TABLE IF NOT EXISTS biz_order (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_no VARCHAR(64) NOT NULL,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0=UNPAID,1=PAID,2=CLOSED',
    amount DECIMAL(18,2) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    expire_time DATETIME(3) NOT NULL,
    pay_time DATETIME(3) NULL,
    close_time DATETIME(3) NULL,
    close_reason VARCHAR(64) NULL,
    version INT NOT NULL DEFAULT 0,
    created_by VARCHAR(64) NULL,
    updated_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_biz_order_order_no (order_no),
    KEY idx_biz_order_timeout_scan (status, expire_time, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
