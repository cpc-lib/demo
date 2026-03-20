CREATE
DATABASE IF NOT EXISTS order_ds_0 DEFAULT CHARACTER SET utf8mb4;
CREATE
DATABASE IF NOT EXISTS order_ds_1 DEFAULT CHARACTER SET utf8mb4;

-- ============ DS0 ============
USE
order_ds_0;

CREATE TABLE IF NOT EXISTS t_user
(
    id
    BIGINT
    PRIMARY
    KEY,
    username
    VARCHAR
(
    64
) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS t_order_0
(
    id
    BIGINT
    PRIMARY
    KEY,
    user_id
    BIGINT
    NOT
    NULL,
    status
    VARCHAR
(
    32
) NOT NULL,
    total_amount BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    KEY idx_user_created
(
    user_id,
    created_at
)
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS t_order_1 LIKE t_order_0;

CREATE TABLE IF NOT EXISTS t_order_item_0
(
    id
    BIGINT
    PRIMARY
    KEY,
    order_id
    BIGINT
    NOT
    NULL,
    user_id
    BIGINT
    NOT
    NULL,
    sku_id
    BIGINT
    NOT
    NULL,
    title
    VARCHAR
(
    128
) NOT NULL,
    price BIGINT NOT NULL,
    quantity INT NOT NULL,
    created_at DATETIME NOT NULL,
    KEY idx_order
(
    order_id
)
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS t_order_item_1 LIKE t_order_item_0;

CREATE TABLE IF NOT EXISTS t_outbox_event_0
(
    id
    BIGINT
    PRIMARY
    KEY,
    aggregate_id
    BIGINT
    NOT
    NULL,
    event_type
    VARCHAR
(
    64
) NOT NULL,
    payload_json TEXT NOT NULL,
    status VARCHAR
(
    32
) NOT NULL,
    next_retry_at DATETIME NOT NULL,
    retry_count INT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    KEY idx_status_next
(
    status,
    next_retry_at
),
    KEY idx_agg
(
    aggregate_id
)
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS t_outbox_event_1 LIKE t_outbox_event_0;

-- ============ DS1 ============
USE
order_ds_1;

CREATE TABLE IF NOT EXISTS t_user LIKE order_ds_0.t_user;

CREATE TABLE IF NOT EXISTS t_order_0 LIKE order_ds_0.t_order_0;
CREATE TABLE IF NOT EXISTS t_order_1 LIKE order_ds_0.t_order_0;

CREATE TABLE IF NOT EXISTS t_order_item_0 LIKE order_ds_0.t_order_item_0;
CREATE TABLE IF NOT EXISTS t_order_item_1 LIKE order_ds_0.t_order_item_0;

CREATE TABLE IF NOT EXISTS t_outbox_event_0 LIKE order_ds_0.t_outbox_event_0;
CREATE TABLE IF NOT EXISTS t_outbox_event_1 LIKE order_ds_0.t_outbox_event_0;

-- 初始化一个用户
INSERT INTO order_ds_0.t_user(id, username)
VALUES (1001, 'alice') ON DUPLICATE KEY
UPDATE username=
VALUES (username);

INSERT INTO order_ds_1.t_user(id, username)
VALUES (1002, 'bob') ON DUPLICATE KEY
UPDATE username=
VALUES (username);
