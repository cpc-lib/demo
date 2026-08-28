-- demo schema
CREATE DATABASE IF NOT EXISTS user_demo DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE user_demo;

-- 分表：user_base_00..03
DROP TABLE IF EXISTS user_base_00;
DROP TABLE IF EXISTS user_base_01;
DROP TABLE IF EXISTS user_base_02;
DROP TABLE IF EXISTS user_base_03;

CREATE TABLE user_base_00 (
  uid BIGINT NOT NULL,
  tenant_id VARCHAR(32) NOT NULL,
  phone VARCHAR(32) NOT NULL,
  email VARCHAR(128) NOT NULL,
  nickname VARCHAR(64) NOT NULL,
  gender CHAR(1) NOT NULL,
  status VARCHAR(16) NOT NULL,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  version BIGINT NOT NULL DEFAULT 1,
  password_hash VARCHAR(100) NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  PRIMARY KEY (tenant_id, uid),
  KEY idx_updated (tenant_id, updated_at)
) ENGINE=InnoDB;

CREATE TABLE user_base_01 LIKE user_base_00;
CREATE TABLE user_base_02 LIKE user_base_00;
CREATE TABLE user_base_03 LIKE user_base_00;

-- phone index sharding
DROP TABLE IF EXISTS user_phone_index_00;
DROP TABLE IF EXISTS user_phone_index_01;
DROP TABLE IF EXISTS user_phone_index_02;
DROP TABLE IF EXISTS user_phone_index_03;

CREATE TABLE user_phone_index_00 (
  tenant_id VARCHAR(32) NOT NULL,
  phone VARCHAR(32) NOT NULL,
  uid BIGINT NOT NULL,
  updated_at DATETIME NOT NULL,
  PRIMARY KEY (tenant_id, phone),
  KEY idx_uid (tenant_id, uid)
) ENGINE=InnoDB;

CREATE TABLE user_phone_index_01 LIKE user_phone_index_00;
CREATE TABLE user_phone_index_02 LIKE user_phone_index_00;
CREATE TABLE user_phone_index_03 LIKE user_phone_index_00;

-- email index sharding
DROP TABLE IF EXISTS user_email_index_00;
DROP TABLE IF EXISTS user_email_index_01;
DROP TABLE IF EXISTS user_email_index_02;
DROP TABLE IF EXISTS user_email_index_03;

CREATE TABLE user_email_index_00 (
  tenant_id VARCHAR(32) NOT NULL,
  email_lower VARCHAR(128) NOT NULL,
  uid BIGINT NOT NULL,
  updated_at DATETIME NOT NULL,
  PRIMARY KEY (tenant_id, email_lower),
  KEY idx_uid (tenant_id, uid)
) ENGINE=InnoDB;

CREATE TABLE user_email_index_01 LIKE user_email_index_00;
CREATE TABLE user_email_index_02 LIKE user_email_index_00;
CREATE TABLE user_email_index_03 LIKE user_email_index_00;

-- seed user：把 uid 固定成可控值，确保落到 user_base_0x 分表（按 uid % 4）
-- uid=170000000000000 -> %4 = 0
SET @uid := 170000000000000;
SET @tenant := 't1';
SET @phone := '13800000000';
SET @email := 'demo@ivera.cc';
SET @nick := 'DemoUser';
SET @gender := 'F';
SET @status := 'ACTIVE';
SET @deleted := 0;
SET @version := 1;
-- bcrypt(Passw0rd!) 下面这个 hash 是 Spring BCrypt 标准格式
SET @pwd := '$2a$10$Wm9jEJKmC6jYp3tGmHhL0O3Z1g7A6GZfN3nXQ5Yf3f5WmW3lYc0rC';

INSERT INTO user_base_00(uid, tenant_id, phone, email, nickname, gender, status, deleted, version, password_hash, created_at, updated_at)
VALUES(@uid, @tenant, @phone, @email, @nick, @gender, @status, @deleted, @version, @pwd, NOW(), NOW());

-- index tables：按 hash 分表（应用也按同规则路由）
-- 这里简单插入到 _00，真实需按 hash(phone/email) 放到正确分表，本 demo 因为会先 Redis miss 后查对分表，所以我们也写到正确分表：
-- 为了确定分表，我们在 SQL 里用 CRC32 模拟 Java hashCode 不同，这里不可靠；所以我们在 demo 中会通过 create API 预热正确分表。
-- 但登录 demo 为了开箱即用，我们直接把 phone/email 同时写入 4 张分表各一条（只在 demo 使用，生产不要这么做）。
INSERT INTO user_phone_index_00(tenant_id, phone, uid, updated_at) VALUES(@tenant, @phone, @uid, NOW());
INSERT INTO user_phone_index_01(tenant_id, phone, uid, updated_at) VALUES(@tenant, @phone, @uid, NOW());
INSERT INTO user_phone_index_02(tenant_id, phone, uid, updated_at) VALUES(@tenant, @phone, @uid, NOW());
INSERT INTO user_phone_index_03(tenant_id, phone, uid, updated_at) VALUES(@tenant, @phone, @uid, NOW());

INSERT INTO user_email_index_00(tenant_id, email_lower, uid, updated_at) VALUES(@tenant, LOWER(@email), @uid, NOW());
INSERT INTO user_email_index_01(tenant_id, email_lower, uid, updated_at) VALUES(@tenant, LOWER(@email), @uid, NOW());
INSERT INTO user_email_index_02(tenant_id, email_lower, uid, updated_at) VALUES(@tenant, LOWER(@email), @uid, NOW());
INSERT INTO user_email_index_03(tenant_id, email_lower, uid, updated_at) VALUES(@tenant, LOWER(@email), @uid, NOW());
