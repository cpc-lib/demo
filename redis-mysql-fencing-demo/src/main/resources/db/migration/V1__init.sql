-- MySQL 8.0+ InnoDB; production rule: do not DELETE/TRUNCATE or reset fence_token.
CREATE TABLE fenced_counter (
  resource_id VARCHAR(64) NOT NULL,
  counter_value BIGINT NOT NULL DEFAULT 0,
  fence_token BIGINT NOT NULL DEFAULT 0,
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
    ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (resource_id),
  CONSTRAINT chk_fenced_counter_value CHECK (counter_value >= 0),
  CONSTRAINT chk_fenced_token CHECK (fence_token >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE fenced_operation (
  resource_id VARCHAR(64) NOT NULL,
  request_id VARCHAR(64) NOT NULL,
  delta_amount BIGINT NOT NULL,
  applied_value BIGINT NOT NULL,
  fence_token BIGINT NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (resource_id, request_id),
  KEY idx_fenced_operation_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
