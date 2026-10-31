CREATE
DATABASE IF NOT EXISTS scan_login CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE
scan_login;
CREATE TABLE IF NOT EXISTS app_user
(
    id
    BIGINT
    NOT
    NULL
    AUTO_INCREMENT
    PRIMARY
    KEY,
    username
    VARCHAR
(
    40
) NOT NULL,
    password_hash VARCHAR
(
    100
) NOT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME
(
    3
) NOT NULL DEFAULT CURRENT_TIMESTAMP
(
    3
),
    UNIQUE KEY uk_user_name
(
    username
)
    ) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS login_audit
(
    id
    BIGINT
    NOT
    NULL
    AUTO_INCREMENT
    PRIMARY
    KEY,
    user_id
    BIGINT
    NOT
    NULL,
    event_type
    VARCHAR
(
    30
) NOT NULL,
    ip VARCHAR
(
    45
),
    user_agent VARCHAR
(
    255
),
    created_at DATETIME
(
    3
) NOT NULL DEFAULT CURRENT_TIMESTAMP
(
    3
),
    KEY idx_audit_user_created
(
    user_id,
    created_at
)
    ) ENGINE=InnoDB;
