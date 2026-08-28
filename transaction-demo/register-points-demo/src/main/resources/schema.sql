CREATE DATABASE IF NOT EXISTS points_demo_3 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE points_demo_3;

DROP TABLE IF EXISTS user;
CREATE TABLE user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL,
  create_time DATETIME NOT NULL
);

DROP TABLE IF EXISTS user_points;
CREATE TABLE user_points (
  user_id BIGINT PRIMARY KEY,
  points INT NOT NULL
);

DROP TABLE IF EXISTS points_log;
CREATE TABLE points_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  business_id VARCHAR(64) NOT NULL,
  points INT NOT NULL,
  status TINYINT NOT NULL,
  retry_count INT NOT NULL DEFAULT 0,
  err_msg VARCHAR(255) NULL,
  create_time DATETIME NOT NULL,
  send_time DATETIME NULL,
  update_time DATETIME NULL,
  UNIQUE KEY uk_business_id (business_id)
);