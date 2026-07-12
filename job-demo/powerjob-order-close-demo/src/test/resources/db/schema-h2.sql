DROP TABLE IF EXISTS order_close_log;
DROP TABLE IF EXISTS biz_order;

CREATE TABLE biz_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL UNIQUE,
    status TINYINT NOT NULL DEFAULT 0,
    amount DECIMAL(18,2) NOT NULL,
    created_at TIMESTAMP(3) NOT NULL,
    expire_time TIMESTAMP(3) NOT NULL,
    pay_time TIMESTAMP(3),
    close_time TIMESTAMP(3),
    close_reason VARCHAR(64),
    version INT NOT NULL DEFAULT 0,
    created_by VARCHAR(64),
    updated_at TIMESTAMP(3) NOT NULL
);
CREATE INDEX idx_biz_order_timeout_scan ON biz_order(status, expire_time, id);

CREATE TABLE order_close_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE,
    order_no VARCHAR(64) NOT NULL,
    trigger_source VARCHAR(32) NOT NULL,
    scheduler_instance_id VARCHAR(128),
    created_at TIMESTAMP(3) NOT NULL
);
