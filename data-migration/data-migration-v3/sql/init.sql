CREATE DATABASE IF NOT EXISTS migration_demo DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE migration_demo;

DROP TABLE IF EXISTS user_source;
CREATE TABLE user_source (
  id BIGINT PRIMARY KEY,
  user_name VARCHAR(64) NOT NULL,
  age INT NOT NULL,
  phone VARCHAR(32),
  email VARCHAR(128),
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  KEY idx_create_time(create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS user_target;
CREATE TABLE user_target LIKE user_source;

DROP TABLE IF EXISTS migration_task;
CREATE TABLE migration_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_no VARCHAR(64) NOT NULL UNIQUE,
  source_table VARCHAR(128) NOT NULL,
  target_table VARCHAR(128) NOT NULL,
  tmp_table VARCHAR(128) NOT NULL,
  id_column VARCHAR(128) NOT NULL DEFAULT 'id',
  min_id BIGINT NOT NULL,
  max_id BIGINT NOT NULL,
  total_count BIGINT NOT NULL DEFAULT 0,
  shard_size INT NOT NULL,
  batch_size INT NOT NULL,
  thread_size INT NOT NULL,
  status VARCHAR(32) NOT NULL,
  executed_count BIGINT NOT NULL DEFAULT 0,
  success_count BIGINT NOT NULL DEFAULT 0,
  fail_count BIGINT NOT NULL DEFAULT 0,
  source_checksum DECIMAL(38,0) NULL,
  tmp_checksum DECIMAL(38,0) NULL,
  target_checksum DECIMAL(38,0) NULL,
  tps DECIMAL(18,2) NOT NULL DEFAULT 0,
  progress DECIMAL(9,4) NOT NULL DEFAULT 0,
  error_message TEXT NULL,
  start_time DATETIME NULL,
  end_time DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS migration_plan_shard;
CREATE TABLE migration_plan_shard (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_no VARCHAR(64) NOT NULL,
  shard_no INT NOT NULL,
  start_id BIGINT NOT NULL,
  end_id BIGINT NOT NULL,
  planned_count BIGINT NOT NULL DEFAULT 0,
  success_count BIGINT NOT NULL DEFAULT 0,
  fail_count BIGINT NOT NULL DEFAULT 0,
  retry_count INT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL,
  error_message TEXT NULL,
  start_time DATETIME NULL,
  end_time DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_task_shard(task_no, shard_no),
  KEY idx_task_status(task_no, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS migration_batch_log;
CREATE TABLE migration_batch_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_no VARCHAR(64) NOT NULL,
  shard_no INT NOT NULL,
  batch_no INT NOT NULL,
  start_id BIGINT NOT NULL,
  end_id BIGINT NOT NULL,
  affected_rows INT NOT NULL DEFAULT 0,
  cost_ms BIGINT NOT NULL DEFAULT 0,
  tps DECIMAL(18,2) NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL,
  error_message TEXT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_task_shard_batch(task_no, shard_no, batch_no),
  KEY idx_task(task_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS migration_log;
CREATE TABLE migration_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_no VARCHAR(64) NOT NULL,
  level VARCHAR(16) NOT NULL,
  stage VARCHAR(64) NOT NULL,
  message TEXT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_task_created(task_no, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 快速造 100 万测试数据：递归 CTE 生成 0~999，再两表笛卡尔积。
-- 注意：这里故意不使用 1~1000000 连续 ID，而是使用雪花 ID 风格的大整数 + 间隔步长，模拟真实生产主键不连续、起始值很大、有大量空洞的情况。
INSERT INTO user_source(id, user_name, age, phone, email, create_time, update_time)
WITH RECURSIVE seq AS (
  SELECT 0 n
  UNION ALL
  SELECT n + 1 FROM seq WHERE n < 999
), src AS (
  SELECT b.n * 1000 + c.n + 1 AS rn, b.n AS bn, c.n AS cn
  FROM seq b
  JOIN seq c
)
SELECT
  900000000000000000 + rn * 10 + (rn % 7) AS id,
  CONCAT('user_', rn),
  18 + ((bn + cn) % 50),
  CONCAT('13', LPAD(rn % 1000000000, 9, '0')),
  CONCAT('user', rn, '@example.com'),
  NOW() - INTERVAL ((bn + cn) % 365) DAY,
  NOW()
FROM src;
