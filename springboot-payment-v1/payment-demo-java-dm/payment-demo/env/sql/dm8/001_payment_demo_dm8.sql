-- ===============================================================
-- Payment Demo DM8 初始化脚本
-- 目标：替代 MySQL 8 初始化脚本，保留现有业务表、唯一约束、索引和初始化数据。
-- 运行方式：docker compose 启动 DM8 后执行 env/scripts/dm8/init-dm8-sql.sh。
-- 默认连接用户：SYSDBA；默认 schema：SYSDBA。
-- ===============================================================

-- ----------------------------
-- Drop tables in dependency-safe order.
-- Use DM/Oracle-style USER_TABLES checks and standard DDL so initialization
-- works in DM native mode.
-- ----------------------------
DECLARE
  v_table_count NUMBER;
BEGIN
  SELECT COUNT(*) INTO v_table_count FROM USER_TABLES WHERE UPPER(TABLE_NAME) = 'T_REFUND_INFO';
  IF v_table_count > 0 THEN
    EXECUTE IMMEDIATE 'DROP TABLE t_refund_info';
  END IF;
END;
/

DECLARE
  v_table_count NUMBER;
BEGIN
  SELECT COUNT(*) INTO v_table_count FROM USER_TABLES WHERE UPPER(TABLE_NAME) = 'T_PAYMENT_INFO';
  IF v_table_count > 0 THEN
    EXECUTE IMMEDIATE 'DROP TABLE t_payment_info';
  END IF;
END;
/

DECLARE
  v_table_count NUMBER;
BEGIN
  SELECT COUNT(*) INTO v_table_count FROM USER_TABLES WHERE UPPER(TABLE_NAME) = 'T_ORDER_INFO';
  IF v_table_count > 0 THEN
    EXECUTE IMMEDIATE 'DROP TABLE t_order_info';
  END IF;
END;
/

DECLARE
  v_table_count NUMBER;
BEGIN
  SELECT COUNT(*) INTO v_table_count FROM USER_TABLES WHERE UPPER(TABLE_NAME) = 'T_PAYMENT_APP';
  IF v_table_count > 0 THEN
    EXECUTE IMMEDIATE 'DROP TABLE t_payment_app';
  END IF;
END;
/

DECLARE
  v_table_count NUMBER;
BEGIN
  SELECT COUNT(*) INTO v_table_count FROM USER_TABLES WHERE UPPER(TABLE_NAME) = 'T_PRODUCT';
  IF v_table_count > 0 THEN
    EXECUTE IMMEDIATE 'DROP TABLE t_product';
  END IF;
END;
/

DECLARE
  v_table_count NUMBER;
BEGIN
  SELECT COUNT(*) INTO v_table_count FROM USER_TABLES WHERE UPPER(TABLE_NAME) = 'T_PAYMENT_CHANNEL';
  IF v_table_count > 0 THEN
    EXECUTE IMMEDIATE 'DROP TABLE t_payment_channel';
  END IF;
END;
/

-- ----------------------------
-- t_payment_channel 支付渠道配置表
-- ----------------------------
CREATE TABLE t_payment_channel (
  id BIGINT IDENTITY(1, 1) NOT NULL,
  channel_name VARCHAR(64) NOT NULL,
  channel_code VARCHAR(32) NOT NULL,
  channel_status VARCHAR(16) DEFAULT 'ENABLED' NOT NULL,
  channel_desc VARCHAR(255),
  config_params CLOB,
  sort_order INT DEFAULT 0 NOT NULL,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT pk_payment_channel PRIMARY KEY (id),
  CONSTRAINT uk_channel_code UNIQUE (channel_code)
);

COMMENT ON TABLE t_payment_channel IS '支付渠道配置表：保存渠道级公共参数，不保存具体商户应用密钥';
COMMENT ON COLUMN t_payment_channel.id IS '支付渠道ID';
COMMENT ON COLUMN t_payment_channel.channel_name IS '渠道名称，例如微信支付、支付宝';
COMMENT ON COLUMN t_payment_channel.channel_code IS '渠道编码：WXPAY、ALIPAY';
COMMENT ON COLUMN t_payment_channel.channel_status IS '渠道状态：ENABLED-启用，DISABLED-禁用';
COMMENT ON COLUMN t_payment_channel.channel_desc IS '渠道描述';
COMMENT ON COLUMN t_payment_channel.config_params IS '渠道公共参数JSON字符串，例如domain、gatewayUrl、contentKey';
COMMENT ON COLUMN t_payment_channel.sort_order IS '排序号';
COMMENT ON COLUMN t_payment_channel.create_time IS '创建时间';
COMMENT ON COLUMN t_payment_channel.update_time IS '更新时间';

CREATE INDEX idx_channel_status_sort ON t_payment_channel(channel_status, sort_order);

CREATE OR REPLACE TRIGGER trg_payment_channel_uptime
BEFORE UPDATE ON t_payment_channel
FOR EACH ROW
BEGIN
  :NEW.update_time = CURRENT_TIMESTAMP;
END;
/

-- ----------------------------
-- t_payment_app 支付应用配置表
-- ----------------------------
CREATE TABLE t_payment_app (
  id BIGINT IDENTITY(1, 1) NOT NULL,
  app_name VARCHAR(64) NOT NULL,
  app_code VARCHAR(64) NOT NULL,
  app_status VARCHAR(16) DEFAULT 'ENABLED' NOT NULL,
  channel_id BIGINT NOT NULL,
  app_desc VARCHAR(255),
  app_config CLOB,
  sort_order INT DEFAULT 0 NOT NULL,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT pk_payment_app PRIMARY KEY (id),
  CONSTRAINT uk_app_code UNIQUE (app_code),
  CONSTRAINT fk_payment_app_channel FOREIGN KEY (channel_id) REFERENCES t_payment_channel(id)
);

COMMENT ON TABLE t_payment_app IS '支付应用配置表：保存支付渠道下的商户/应用维度参数';
COMMENT ON COLUMN t_payment_app.id IS '支付应用ID';
COMMENT ON COLUMN t_payment_app.app_name IS '应用名称';
COMMENT ON COLUMN t_payment_app.app_code IS '应用编码';
COMMENT ON COLUMN t_payment_app.app_status IS '应用状态：ENABLED-启用，DISABLED-禁用';
COMMENT ON COLUMN t_payment_app.channel_id IS '关联支付渠道ID';
COMMENT ON COLUMN t_payment_app.app_desc IS '应用描述';
COMMENT ON COLUMN t_payment_app.app_config IS '应用参数JSON字符串，例如微信appid/mchId/API密钥，支付宝appId/公私钥/回调地址';
COMMENT ON COLUMN t_payment_app.sort_order IS '排序号';
COMMENT ON COLUMN t_payment_app.create_time IS '创建时间';
COMMENT ON COLUMN t_payment_app.update_time IS '更新时间';

CREATE INDEX idx_app_channel_status_sort ON t_payment_app(channel_id, app_status, sort_order);

CREATE OR REPLACE TRIGGER trg_payment_app_uptime
BEFORE UPDATE ON t_payment_app
FOR EACH ROW
BEGIN
  :NEW.update_time = CURRENT_TIMESTAMP;
END;
/

-- ----------------------------
-- t_order_info 订单表
-- ----------------------------
CREATE TABLE t_order_info (
  id BIGINT IDENTITY(1, 1) NOT NULL,
  title VARCHAR(256),
  order_no VARCHAR(50) NOT NULL,
  user_id BIGINT,
  product_id BIGINT NOT NULL,
  total_fee INT NOT NULL,
  code_url VARCHAR(512),
  order_status VARCHAR(30) NOT NULL,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  payment_type VARCHAR(20) NOT NULL,
  payment_app_id BIGINT,
  payment_channel_code VARCHAR(32),
  version INT DEFAULT 0 NOT NULL,
  CONSTRAINT pk_order_info PRIMARY KEY (id),
  CONSTRAINT uk_order_no UNIQUE (order_no)
);

COMMENT ON TABLE t_order_info IS '订单表：包含乐观锁 version 字段，支持幂等性';
COMMENT ON COLUMN t_order_info.id IS '订单id';
COMMENT ON COLUMN t_order_info.title IS '订单标题';
COMMENT ON COLUMN t_order_info.order_no IS '商户订单编号';
COMMENT ON COLUMN t_order_info.user_id IS '用户id';
COMMENT ON COLUMN t_order_info.product_id IS '支付产品id';
COMMENT ON COLUMN t_order_info.total_fee IS '订单金额(分)';
COMMENT ON COLUMN t_order_info.code_url IS '订单二维码连接';
COMMENT ON COLUMN t_order_info.order_status IS '订单状态';
COMMENT ON COLUMN t_order_info.payment_type IS '支付类型：支付宝、微信';
COMMENT ON COLUMN t_order_info.payment_app_id IS '支付应用ID';
COMMENT ON COLUMN t_order_info.payment_channel_code IS '支付渠道编码：WXPAY、ALIPAY';
COMMENT ON COLUMN t_order_info.version IS '乐观锁版本号';

CREATE INDEX idx_product_status_pay_type ON t_order_info(product_id, order_status, payment_type);
CREATE INDEX idx_product_payment_status_time ON t_order_info(product_id, payment_type, order_status, create_time);
CREATE INDEX idx_payment_app ON t_order_info(payment_app_id, create_time);

CREATE OR REPLACE TRIGGER trg_order_info_uptime
BEFORE UPDATE ON t_order_info
FOR EACH ROW
BEGIN
  :NEW.update_time = CURRENT_TIMESTAMP;
END;
/

-- ----------------------------
-- t_payment_info 支付流水表
-- ----------------------------
CREATE TABLE t_payment_info (
  id BIGINT IDENTITY(1, 1) NOT NULL,
  order_no VARCHAR(50) NOT NULL,
  transaction_id VARCHAR(50),
  payment_type VARCHAR(20) NOT NULL,
  trade_type VARCHAR(20),
  trade_state VARCHAR(50),
  payer_total INT,
  content CLOB,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT pk_payment_info PRIMARY KEY (id),
  CONSTRAINT uk_order_payment_type UNIQUE (order_no, payment_type),
  CONSTRAINT uk_transaction_id_payment_type UNIQUE (transaction_id, payment_type)
);

COMMENT ON TABLE t_payment_info IS '支付流水表：双重幂等控制';
COMMENT ON COLUMN t_payment_info.id IS '支付记录id';
COMMENT ON COLUMN t_payment_info.order_no IS '商户订单编号';
COMMENT ON COLUMN t_payment_info.transaction_id IS '支付系统交易编号';
COMMENT ON COLUMN t_payment_info.payment_type IS '支付类型';
COMMENT ON COLUMN t_payment_info.trade_type IS '交易类型';
COMMENT ON COLUMN t_payment_info.trade_state IS '交易状态';
COMMENT ON COLUMN t_payment_info.payer_total IS '支付金额(分)';
COMMENT ON COLUMN t_payment_info.content IS '通知参数';

CREATE INDEX idx_payment_order_no ON t_payment_info(order_no);

CREATE OR REPLACE TRIGGER trg_payment_info_uptime
BEFORE UPDATE ON t_payment_info
FOR EACH ROW
BEGIN
  :NEW.update_time = CURRENT_TIMESTAMP;
END;
/

-- ----------------------------
-- t_product 商品表
-- ----------------------------
CREATE TABLE t_product (
  id BIGINT IDENTITY(1, 1) NOT NULL,
  title VARCHAR(20),
  price INT,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT pk_product PRIMARY KEY (id)
);

COMMENT ON TABLE t_product IS '商品表';
COMMENT ON COLUMN t_product.id IS '商品id';
COMMENT ON COLUMN t_product.title IS '商品名称';
COMMENT ON COLUMN t_product.price IS '价格（分）';

CREATE OR REPLACE TRIGGER trg_product_uptime
BEFORE UPDATE ON t_product
FOR EACH ROW
BEGIN
  :NEW.update_time = CURRENT_TIMESTAMP;
END;
/

-- ----------------------------
-- t_refund_info 退款表
-- ----------------------------
CREATE TABLE t_refund_info (
  id BIGINT IDENTITY(1, 1) NOT NULL,
  order_no VARCHAR(50) NOT NULL,
  refund_no VARCHAR(50) NOT NULL,
  refund_id VARCHAR(50),
  total_fee INT,
  refund INT,
  reason VARCHAR(50),
  approval_status VARCHAR(20),
  approve_remark VARCHAR(255),
  approved_time TIMESTAMP,
  refund_status VARCHAR(30),
  content_return CLOB,
  content_notify CLOB,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT pk_refund_info PRIMARY KEY (id),
  CONSTRAINT uk_refund_no UNIQUE (refund_no),
  CONSTRAINT uk_refund_id UNIQUE (refund_id)
);

COMMENT ON TABLE t_refund_info IS '退款表：双重幂等控制';
COMMENT ON COLUMN t_refund_info.id IS '退款单id';
COMMENT ON COLUMN t_refund_info.order_no IS '商户订单编号';
COMMENT ON COLUMN t_refund_info.refund_no IS '商户退款单编号';
COMMENT ON COLUMN t_refund_info.refund_id IS '支付系统退款单号';
COMMENT ON COLUMN t_refund_info.total_fee IS '原订单金额(分)';
COMMENT ON COLUMN t_refund_info.refund IS '退款金额(分)';
COMMENT ON COLUMN t_refund_info.reason IS '退款原因';
COMMENT ON COLUMN t_refund_info.approval_status IS '审核状态';
COMMENT ON COLUMN t_refund_info.approve_remark IS '审核备注';
COMMENT ON COLUMN t_refund_info.approved_time IS '审核时间';
COMMENT ON COLUMN t_refund_info.refund_status IS '退款状态';
COMMENT ON COLUMN t_refund_info.content_return IS '申请退款返回参数';
COMMENT ON COLUMN t_refund_info.content_notify IS '退款结果通知参数';

CREATE INDEX idx_refund_order_approval_status ON t_refund_info(order_no, approval_status);
CREATE INDEX idx_refund_order_status ON t_refund_info(order_no, refund_status);

CREATE OR REPLACE TRIGGER trg_refund_info_uptime
BEFORE UPDATE ON t_refund_info
FOR EACH ROW
BEGIN
  :NEW.update_time = CURRENT_TIMESTAMP;
END;
/

-- ----------------------------
-- Initial data：来源于现有 wxpay.properties / alipay-sandbox.properties，不使用 mock 数据
-- ----------------------------
INSERT INTO t_payment_channel (channel_name, channel_code, channel_status, channel_desc, config_params, sort_order) VALUES
('微信支付', 'WXPAY', 'ENABLED', '微信支付渠道公共配置', '{"domain":"https://api.mch.weixin.qq.com","notifyUrl":"https://5834-2409-8a50-3e25-e7f0-ecc6-e42-7b4c-1466.ngrok-free.app"}', 10);

INSERT INTO t_payment_channel (channel_name, channel_code, channel_status, channel_desc, config_params, sort_order) VALUES
('支付宝', 'ALIPAY', 'ENABLED', '支付宝渠道公共配置', '{"gatewayUrl":"https://openapi-sandbox.dl.alipaydev.com/gateway.do","contentKey":"GD8AS9VQy0hZROhMzOARQw=="}', 20);

INSERT INTO t_payment_app (app_name, app_code, app_status, channel_id, app_desc, app_config, sort_order) VALUES
('微信支付默认应用', 'WXPAY_DEFAULT', 'ENABLED', 1, '从现有微信支付配置文件迁移的默认应用', '{"appid":"wx74862e0dfcf69954","mchId":"1558950191","mchSerialNo":"34345964330B66427E0D3D28826C4993C77E631F","privateKeyPath":"apiclient_key.pem","apiV3Key":"UDuLFDcmy5Eb6o0nTNZdu6ek4DDh4K8B","partnerKey":"T6m9iK73b0kn9g5v426MKfHQH7X8rKwb","notifyUrl":"https://5834-2409-8a50-3e25-e7f0-ecc6-e42-7b4c-1466.ngrok-free.app"}', 10);

INSERT INTO t_payment_app (app_name, app_code, app_status, channel_id, app_desc, app_config, sort_order) VALUES
('支付宝沙箱默认应用', 'ALIPAY_SANDBOX_DEFAULT', 'ENABLED', 2, '从现有支付宝沙箱配置文件迁移的默认应用', '{"appId":"9021000136667568","sellerId":"2088721034748965","merchantPrivateKey":"MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCCDhLjo3lEFNcWdMnehWhHPams3KbmxpVJTxmIglvMLt6j207pSlWxSvacT3wFebXW4herg9RWhjTTh7KFwkxRWwzBVfp63YvEUbNsF09X1K6NqT2Kqz/w77l38lhQxpVau8odV3nHR+KhtDD2YJ9z8C2AzJLzKCD5CdK5coP7k+mRx3/mlOEj07oA7H6CrDkm4j4oasqEeLfbyJrXYcpPMi2hIHMXkR21fX5905v9BxwPhkVFsxHOABPYMWu5+uPpjRH1e/HKZpo5sinud05oWI0b1waMRZ9AGUEJnNZ3Yg9b9b2k1ycnCkApzy8cgQOgGhr6Rr9V7ieAsOn0YahLAgMBAAECggEAeImcvjj0GsKJ+zkxJDlXRbgD+7/iPL/O+0wBqUDQ3fSOyyVnBNetho2o9YTBuL1uaIPSVlfvxGXMrkT1k/1aCIkv0Dzk011kvgbPGZ6dHhVz1r4F2PERaTh2GJKXgf4bzSWBlSJPLwEULrU4MBGrl6QCOH7ir9UAgnC1SsW1R8QkCbgXchZkh9s3LddJ0Skb9Qu144yhT3tSxIMDL/ZJqANIR0Smv9tUuP2+DehqBH8OEapW4c00mt7OWT0jQ8H/8BaFV3j0q8SCn3cmKBFeUzh9H2UlHjiVUeF9mRmiEJUK8/N/li/WZbiWwRXOcRlno54+aRbvlwo+A8gWZF2n+QKBgQC6EIONzns/n0wSB87X0ZWUb7Y3Q6aReu/JvFwNuUA1wbS4bw7PTAijvB0daLXke142ujt6tnghyQHQuQU+5xzA5q8Q3Ooy8OZo9YgCRzNmU20CltEWcWijOt7ndCxpEVDeB7dY55yavVI8hgWX5LFI0t4Zwa8u+c+QL9C48DUVNQKBgQCy8DbydcZdRx2XSYZGtd0p+yR6lFucxpIFIYt/CFXcKGLKasF7Yhds5RKderp/I/1WZl0kwpbTv17HuvJqdoDX4qLXhvlh30P9Pbvvk7YhIv9oR9NqRtTTr0E40jALTouAuEaWs1f49C6AfrWJC26jgNHWpdeEkUQ6NN6DaP73fwKBgAsDRTYUfZkDbbY3fheqEQdrIUbeGzLLKvwuyOgLCfDkmTS9ZgwA/RXr4XFHLFTstGPa3ABkYnHletUGznetqDcGsF/4I2iGd6zIs5cm7bTlxTL9CD0i00WuC1l5t9M0MiwiGskJVGyYPhDVAem+oHul931gyGSoZo+rNNhtZ0btAoGAU2gM9K9ZKxl+/YnUARm8YVkjA9Arc8RLRAEC2M+11c0tX1Sroytx59xO9QDD9Yd9CszkFcJuM308XLUTUfSy0e5eIUBU9f3v3xbrhxy/BGsfyifQr/UcNx+1sxqmMl8GP5WlsZEfLHgFRPfK/npJtATTys26y5w6xTbnkTFbx1kCgYB+2wDnTWavXJkQErniXh3ifmC7qLcBNBpSUrI9eAtI4AwqKfBnp7d+wGuh05epecb1gzE76bWPqgIX7N5zmpZaQ/qZL+CgcGpU/7PN/qKGrqa1JEu8ZMaDhFys6MkIpQIGMElgnbM0cXvNrY8ReKvcGGysdtLnIBrp9BXcm+uWSg==","alipayPublicKey":"MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAh9ivuRV+2eNbAzxXQ3bmx3wdhBHVaNQlhzv6wDXBMNhKG/AJDOAtYNbzPYJJPfySOrxyMlcTCvTGMS//Zn4yJHXx8IjJ4LSIguyy2BjDMwvDlF+TP7Xj6F/qtHWzuhztDnkLdAOwUx70Zyq1ajjzU5b2ku3J9WX5bwmnDpFGCVCwwG51mJYTd7Fwr4nE1qRZeMgGMhR7xR5Qdu1Nwx8Z+l1FCs47eZiirearT83/pwCRPD364SHq2uBJLtse9ozO7meBb8mzt6CDZKK6imEX1MKeVILbfE7GZAbtIAV4TLbvhB4VZuVxQrYmppPGhMpEiFDkpk0nFyNb7tz3HcHIQwIDAQAB","returnUrl":"http://localhost:8083/#/success","notifyUrl":"http://arhi.nat300.top/api/ali-pay/trade/notify"}', 20);

INSERT INTO t_product (title, price, create_time, update_time) VALUES ('Java课程', 1, TIMESTAMP '2023-02-04 23:34:20', TIMESTAMP '2023-02-04 23:34:20');
INSERT INTO t_product (title, price, create_time, update_time) VALUES ('大数据课程', 1, TIMESTAMP '2023-02-04 23:34:20', TIMESTAMP '2023-02-04 23:34:20');
INSERT INTO t_product (title, price, create_time, update_time) VALUES ('前端课程', 1, TIMESTAMP '2023-02-04 23:34:20', TIMESTAMP '2023-02-04 23:34:20');
INSERT INTO t_product (title, price, create_time, update_time) VALUES ('UI课程', 1, TIMESTAMP '2023-02-04 23:34:20', TIMESTAMP '2023-02-04 23:34:20');

COMMIT;
