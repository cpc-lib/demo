CREATE
DATABASE IF NOT EXISTS point_reward DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE
point_reward;
CREATE TABLE IF NOT EXISTS user_account
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
    status VARCHAR
(
    16
) NOT NULL DEFAULT 'ACTIVE',
    points BIGINT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_user_status_id
(
    status,
    id
)
    ) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS point_reward_batch
(
    id
    BIGINT
    PRIMARY
    KEY
    AUTO_INCREMENT,
    biz_type
    VARCHAR
(
    64
) NOT NULL,
    reward_date DATE NOT NULL,
    status VARCHAR
(
    16
) NOT NULL,
    shard_total INT NOT NULL DEFAULT 1,
    scanned_count BIGINT NOT NULL DEFAULT 0,
    sent_count BIGINT NOT NULL DEFAULT 0,
    started_at DATETIME NULL,
    finished_at DATETIME NULL,
    remark VARCHAR
(
    1000
) NULL,
    UNIQUE KEY uk_batch_biz_date
(
    biz_type,
    reward_date
)
    ) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS point_reward_batch_shard
(
    id
    BIGINT
    PRIMARY
    KEY
    AUTO_INCREMENT,
    batch_id
    BIGINT
    NOT
    NULL,
    shard_index
    INT
    NOT
    NULL,
    status
    VARCHAR
(
    16
) NOT NULL,
    started_at DATETIME NULL,
    finished_at DATETIME NULL,
    last_error VARCHAR
(
    1000
) NULL,
    UNIQUE KEY uk_batch_shard
(
    batch_id,
    shard_index
),
    KEY idx_shard_status
(
    status
)
    ) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS user_point_ledger
(
    id
    BIGINT
    PRIMARY
    KEY
    AUTO_INCREMENT,
    user_id
    BIGINT
    NOT
    NULL,
    batch_id
    BIGINT
    NOT
    NULL,
    biz_type
    VARCHAR
(
    64
) NOT NULL,
    biz_no VARCHAR
(
    128
) NOT NULL,
    change_point BIGINT NOT NULL,
    reward_date DATE NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_point_biz_no
(
    biz_no
),
    KEY idx_ledger_user_date
(
    user_id,
    reward_date
),
    KEY idx_ledger_batch
(
    batch_id
)
    ) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS point_reward_failure
(
    id
    BIGINT
    PRIMARY
    KEY
    AUTO_INCREMENT,
    batch_id
    BIGINT
    NOT
    NULL,
    user_id
    BIGINT
    NOT
    NULL,
    biz_no
    VARCHAR
(
    128
) NOT NULL,
    biz_type VARCHAR
(
    64
) NOT NULL,
    points BIGINT NOT NULL,
    reward_date DATE NOT NULL,
    status VARCHAR
(
    16
) NOT NULL DEFAULT 'OPEN',
    attempt_count INT NOT NULL DEFAULT 1,
    last_error VARCHAR
(
    2000
) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_failure_biz_no
(
    biz_no
),
    KEY idx_failure_status
(
    status
)
    ) ENGINE=InnoDB;
INSERT INTO user_account(id, username, points)
VALUES (10001, 'alice', 100),
       (10002, 'bob', 200),
       (10003, 'carol', 300),
       (10004, 'dave', 400),
       (10005, 'erin', 500),
       (10006, 'frank', 600),
       (10007, 'grace', 700),
       (10008, 'heidi', 800),
       (10009, 'ivan', 900),
       (10010, 'judy', 1000) ON DUPLICATE KEY
UPDATE username=username;
