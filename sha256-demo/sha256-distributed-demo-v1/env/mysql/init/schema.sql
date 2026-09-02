CREATE DATABASE IF NOT EXISTS sha256_demo CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE sha256_demo;

CREATE TABLE IF NOT EXISTS sha256_task (
    task_id            CHAR(32)      NOT NULL,
    original_filename  VARCHAR(512)  NOT NULL,
    storage_key        VARCHAR(512)  NOT NULL,
    storage_bucket     VARCHAR(128)  NOT NULL,
    total_bytes        BIGINT        NOT NULL,
    broker             VARCHAR(32)   NOT NULL,
    status             VARCHAR(32)   NOT NULL,
    created_at         DATETIME(3)   NOT NULL,
    updated_at         DATETIME(3)   NOT NULL,
    PRIMARY KEY (task_id),
    UNIQUE KEY uk_sha256_task_storage_key (storage_key),
    KEY idx_sha256_task_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS outbox_event (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    event_id         CHAR(32)      NOT NULL,
    aggregate_id     CHAR(32)      NOT NULL,
    event_type       VARCHAR(64)   NOT NULL,
    broker           VARCHAR(32)   NOT NULL,
    payload          LONGTEXT      NOT NULL,
    status           VARCHAR(32)   NOT NULL,
    retry_count      INT           NOT NULL DEFAULT 0,
    next_retry_at    DATETIME(3)   NOT NULL,
    last_error       VARCHAR(2000) NULL,
    sent_at          DATETIME(3)   NULL,
    created_at       DATETIME(3)   NOT NULL,
    updated_at       DATETIME(3)   NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_outbox_event_event_id (event_id),
    KEY idx_outbox_dispatch (status, next_retry_at, id),
    KEY idx_outbox_aggregate (aggregate_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
