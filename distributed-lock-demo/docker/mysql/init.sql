USE distributed_lock_demo;

CREATE TABLE IF NOT EXISTS distributed_lock (
    id BIGINT NOT NULL AUTO_INCREMENT,
    lock_key VARCHAR(200) NOT NULL,
    owner VARCHAR(200) NOT NULL,
    expire_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_distributed_lock_key (lock_key),
    KEY idx_distributed_lock_expire_at (expire_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS demo_inventory (
    product_id BIGINT NOT NULL,
    stock INT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO demo_inventory(product_id, stock, version)
VALUES (1001, 20, 0)
ON DUPLICATE KEY UPDATE stock = VALUES(stock), version = 0;
