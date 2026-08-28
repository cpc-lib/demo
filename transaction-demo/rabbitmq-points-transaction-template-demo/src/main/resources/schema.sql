CREATE DATABASE IF NOT EXISTS points_demo_2 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE points_demo_2;

DROP TABLE IF EXISTS user;
CREATE TABLE user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50),
    mobile VARCHAR(20),
    create_time DATETIME
);

DROP TABLE IF EXISTS user_points;
CREATE TABLE user_points (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    points INT NOT NULL,
    reason VARCHAR(50),
    create_time DATETIME,
    UNIQUE KEY uk_user_reason(user_id, reason)
);

DROP TABLE IF EXISTS msg_idempotent;
CREATE TABLE msg_idempotent (
    msg_id VARCHAR(64) PRIMARY KEY,
    status TINYINT NOT NULL,
    create_time DATETIME
);

DROP TABLE IF EXISTS dead_message;
CREATE TABLE dead_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    msg_id VARCHAR(64),
    user_id BIGINT,
    payload TEXT,
    reason VARCHAR(255),
    create_time DATETIME
);
