CREATE TABLE IF NOT EXISTS gray_rule (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  rule_name VARCHAR(128) NOT NULL,
  service_id VARCHAR(128) NOT NULL,
  target_version VARCHAR(32) NOT NULL,
  rule_type VARCHAR(32) NOT NULL,
  condition_key VARCHAR(128) NULL,
  condition_value VARCHAR(512) NULL,
  traffic_percent INT NOT NULL DEFAULT 0,
  priority INT NOT NULL DEFAULT 100,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  description VARCHAR(512) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_gray_rule_service_enabled_priority(service_id, enabled, priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS release_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_name VARCHAR(128) NOT NULL,
  service_id VARCHAR(128) NOT NULL,
  from_version VARCHAR(32) NOT NULL,
  target_version VARCHAR(32) NOT NULL,
  strategy VARCHAR(32) NOT NULL,
  current_percent INT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL,
  owner VARCHAR(64) NOT NULL,
  stages_json VARCHAR(1024) NULL,
  auto_rollback_enabled TINYINT(1) NOT NULL DEFAULT 1,
  error_rate_threshold DECIMAL(8,4) NOT NULL DEFAULT 0.0500,
  p99_latency_threshold_ms INT NOT NULL DEFAULT 1200,
  latest_error_rate DECIMAL(8,4) NOT NULL DEFAULT 0.0000,
  latest_p99_latency_ms INT NOT NULL DEFAULT 0,
  health_status VARCHAR(32) NOT NULL DEFAULT 'UNKNOWN',
  rollback_reason VARCHAR(512) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_release_task_service_status(service_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS release_approval (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  applicant VARCHAR(64) NOT NULL,
  approver VARCHAR(64) NULL,
  status VARCHAR(32) NOT NULL,
  comment VARCHAR(512) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_release_approval_task_status(task_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS service_policy (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  service_id VARCHAR(128) NOT NULL,
  default_version VARCHAR(32) NOT NULL DEFAULT 'v1',
  blue_version VARCHAR(32) NOT NULL DEFAULT 'v1',
  green_version VARCHAR(32) NOT NULL DEFAULT 'v2',
  active_color VARCHAR(16) NOT NULL DEFAULT 'blue',
  ab_enabled TINYINT(1) NOT NULL DEFAULT 0,
  ab_version_a VARCHAR(32) NOT NULL DEFAULT 'v1',
  ab_version_b VARCHAR(32) NOT NULL DEFAULT 'v2',
  ab_percent_b INT NOT NULL DEFAULT 50,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_service_policy_service(service_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ab_metric (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  service_id VARCHAR(128) NOT NULL,
  experiment_key VARCHAR(128) NOT NULL,
  variant VARCHAR(32) NOT NULL,
  exposures BIGINT NOT NULL DEFAULT 0,
  conversions BIGINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_ab_metric_variant(service_id, experiment_key, variant)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS alert_event (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  level VARCHAR(32) NOT NULL,
  source VARCHAR(128) NOT NULL,
  title VARCHAR(256) NOT NULL,
  content VARCHAR(1024) NOT NULL,
  handled TINYINT(1) NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_alert_event_level_time(level, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS audit_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  operator VARCHAR(64) NOT NULL,
  action VARCHAR(64) NOT NULL,
  resource_type VARCHAR(64) NOT NULL,
  resource_id VARCHAR(64) NULL,
  before_data TEXT NULL,
  after_data TEXT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_audit_log_resource(resource_type, resource_id),
  INDEX idx_audit_log_create_time(create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO gray_rule(rule_name, service_id, target_version, rule_type, condition_key, condition_value, traffic_percent, priority, enabled, description)
SELECT '用户 1001 优先体验 v2', 'demo-order-service', 'v2', 'USER', 'userId', '1001', 0, 1, 1, '精准用户白名单'
WHERE NOT EXISTS (SELECT 1 FROM gray_rule WHERE rule_name = '用户 1001 优先体验 v2');

INSERT INTO gray_rule(rule_name, service_id, target_version, rule_type, condition_key, condition_value, traffic_percent, priority, enabled, description)
SELECT 'Header X-Gray=true 命中 v2', 'demo-order-service', 'v2', 'HEADER', 'X-Gray', 'true', 0, 5, 1, '内部测试 Header'
WHERE NOT EXISTS (SELECT 1 FROM gray_rule WHERE rule_name = 'Header X-Gray=true 命中 v2');

INSERT INTO service_policy(service_id, default_version, blue_version, green_version, active_color, ab_enabled, ab_version_a, ab_version_b, ab_percent_b)
SELECT 'demo-order-service', 'v1', 'v1', 'v2', 'blue', 0, 'v1', 'v2', 50
WHERE NOT EXISTS (SELECT 1 FROM service_policy WHERE service_id = 'demo-order-service');
