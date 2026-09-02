CREATE DATABASE IF NOT EXISTS idem_demo
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;
USE idem_demo;

-- Backend-issued token table. Raw requestId is NEVER stored, only SHA-256(token).
CREATE TABLE IF NOT EXISTS idempotency_token (
  id BIGINT NOT NULL AUTO_INCREMENT,
  token_hash CHAR(64) NOT NULL,
  scope VARCHAR(160) NOT NULL,
  request_hash CHAR(64) NULL,
  status VARCHAR(16) NOT NULL COMMENT 'ISSUED/PROCESSING/SUCCESS',
  response_json LONGTEXT NULL,
  business_ref VARCHAR(128) NULL,
  expires_at DATETIME(3) NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
    ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_idem_token_hash (token_hash),
  KEY idx_idem_token_cleanup (status, expires_at),
  KEY idx_idem_token_scope_created (scope, created_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS biz_order (
  id BIGINT NOT NULL AUTO_INCREMENT,
  order_no VARCHAR(64) NOT NULL,
  tenant_id VARCHAR(64) NOT NULL,
  user_id BIGINT NOT NULL,
  item_name VARCHAR(128) NOT NULL,
  amount DECIMAL(18,2) NOT NULL,
  idempotency_key_hash CHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_order_no (order_no),
  UNIQUE KEY uk_order_idem (tenant_id, idempotency_key_hash)
) ENGINE=InnoDB;
