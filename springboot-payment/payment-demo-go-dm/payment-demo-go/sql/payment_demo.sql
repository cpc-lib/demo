-- ===============================================================
-- Payment Demo DM8 database initialization script
-- ===============================================================

DECLARE
  schema_count INT;
BEGIN
  SELECT COUNT(*) INTO schema_count
  FROM SYSOBJECTS
  WHERE TYPE$ = 'SCH' AND NAME = 'PAYMENT_DEMO';

  IF schema_count = 0 THEN
    EXECUTE IMMEDIATE 'CREATE SCHEMA PAYMENT_DEMO AUTHORIZATION SYSDBA';
  END IF;
END;
/

SET SCHEMA PAYMENT_DEMO;

DROP TABLE IF EXISTS t_refund_info;
DROP TABLE IF EXISTS t_payment_info;
DROP TABLE IF EXISTS t_order_info;
DROP TABLE IF EXISTS t_payment_app;
DROP TABLE IF EXISTS t_payment_channel;
DROP TABLE IF EXISTS t_product;

-- ----------------------------
-- t_order_info: order master table
-- ----------------------------
CREATE TABLE t_order_info (
  id BIGINT IDENTITY(1,1) NOT NULL,
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
  version INT NOT NULL DEFAULT 0,
  CONSTRAINT pk_t_order_info PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_order_no ON t_order_info(order_no);
CREATE INDEX idx_product_status_pay_type ON t_order_info(product_id, order_status, payment_type);
CREATE INDEX idx_product_payment_status_time ON t_order_info(product_id, payment_type, order_status, create_time);
CREATE INDEX idx_payment_app_status ON t_order_info(payment_app_id, order_status);

COMMENT ON TABLE t_order_info IS '订单表';
COMMENT ON COLUMN t_order_info.payment_app_id IS '支付应用id';

-- ----------------------------
-- t_payment_info: payment audit table
-- ----------------------------
CREATE TABLE t_payment_info (
  id BIGINT IDENTITY(1,1) NOT NULL,
  order_no VARCHAR(50) NOT NULL,
  transaction_id VARCHAR(50),
  payment_type VARCHAR(20) NOT NULL,
  trade_type VARCHAR(20),
  trade_state VARCHAR(50),
  payer_total INT,
  content CLOB,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT pk_t_payment_info PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_order_payment_type ON t_payment_info(order_no, payment_type);
CREATE UNIQUE INDEX uk_transaction_id_payment_type ON t_payment_info(transaction_id, payment_type);
CREATE INDEX idx_order_no ON t_payment_info(order_no);

COMMENT ON TABLE t_payment_info IS '支付流水表';

-- ----------------------------
-- t_product: demo product table
-- ----------------------------
CREATE TABLE t_product (
  id BIGINT IDENTITY(1,1) NOT NULL,
  title VARCHAR(20),
  price INT,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT pk_t_product PRIMARY KEY (id)
);

INSERT INTO t_product (title, price, create_time, update_time) VALUES
('Java课程', 1, TIMESTAMP '2023-02-04 23:34:20', TIMESTAMP '2023-02-04 23:34:20');
INSERT INTO t_product (title, price, create_time, update_time) VALUES
('大数据课程', 1, TIMESTAMP '2023-02-04 23:34:20', TIMESTAMP '2023-02-04 23:34:20');
INSERT INTO t_product (title, price, create_time, update_time) VALUES
('前端课程', 1, TIMESTAMP '2023-02-04 23:34:20', TIMESTAMP '2023-02-04 23:34:20');
INSERT INTO t_product (title, price, create_time, update_time) VALUES
('UI课程', 1, TIMESTAMP '2023-02-04 23:34:20', TIMESTAMP '2023-02-04 23:34:20');

COMMENT ON TABLE t_product IS '商品表';

-- ----------------------------
-- t_payment_channel: channel-level payment config
-- ----------------------------
CREATE TABLE t_payment_channel (
  id BIGINT IDENTITY(1,1) NOT NULL,
  channel_code VARCHAR(50) NOT NULL,
  channel_name VARCHAR(50) NOT NULL,
  payment_type VARCHAR(20) NOT NULL,
  enabled BIT NOT NULL DEFAULT 1,
  config_params CLOB,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT pk_t_payment_channel PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_channel_code ON t_payment_channel(channel_code);
CREATE INDEX idx_payment_type_enabled ON t_payment_channel(payment_type, enabled);

INSERT INTO t_payment_channel (channel_code, channel_name, payment_type, enabled, config_params, create_time, update_time) VALUES
('wxpay', '微信支付', '微信', 1, '{"domain":"https://api.mch.weixin.qq.com","notifyDomain":"https://5834-2409-8a50-3e25-e7f0-ecc6-e42-7b4c-1466.ngrok-free.app"}', TIMESTAMP '2023-02-04 23:34:20', TIMESTAMP '2023-02-04 23:34:20');
INSERT INTO t_payment_channel (channel_code, channel_name, payment_type, enabled, config_params, create_time, update_time) VALUES
('alipay', '支付宝', '支付宝', 1, '{"gatewayUrl":"https://openapi-sandbox.dl.alipaydev.com/gateway.do"}', TIMESTAMP '2023-02-04 23:34:20', TIMESTAMP '2023-02-04 23:34:20');

COMMENT ON TABLE t_payment_channel IS '支付渠道表';

-- ----------------------------
-- t_payment_app: app or merchant-level payment config
-- ----------------------------
CREATE TABLE t_payment_app (
  id BIGINT IDENTITY(1,1) NOT NULL,
  app_code VARCHAR(50) NOT NULL,
  app_name VARCHAR(100) NOT NULL,
  channel_code VARCHAR(50) NOT NULL,
  payment_type VARCHAR(20) NOT NULL,
  enabled BIT NOT NULL DEFAULT 1,
  app_config CLOB,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT pk_t_payment_app PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_app_code ON t_payment_app(app_code);
CREATE INDEX idx_channel_enabled ON t_payment_app(channel_code, enabled);
CREATE INDEX idx_app_payment_type_enabled ON t_payment_app(payment_type, enabled);

INSERT INTO t_payment_app (app_code, app_name, channel_code, payment_type, enabled, app_config, create_time, update_time) VALUES
('wxpay-default', '微信支付默认应用', 'wxpay', '微信', 1, '{"appid":"wx74862e0dfcf69954","mchId":"1558950191","mchSerialNo":"34345964330B66427E0D3D28826C4993C77E631F","domain":"https://api.mch.weixin.qq.com","notifyDomain":"https://5834-2409-8a50-3e25-e7f0-ecc6-e42-7b4c-1466.ngrok-free.app"}', TIMESTAMP '2023-02-04 23:34:20', TIMESTAMP '2023-02-04 23:34:20');
INSERT INTO t_payment_app (app_code, app_name, channel_code, payment_type, enabled, app_config, create_time, update_time) VALUES
('alipay-sandbox', '支付宝沙箱应用', 'alipay', '支付宝', 1, '{"appId":"9021000136667568","sellerId":"2088721034748965","gatewayUrl":"https://openapi-sandbox.dl.alipaydev.com/gateway.do","returnUrl":"http://localhost:8083/#/success","notifyUrl":"http://arhi.nat300.top/api/ali-pay/trade/notify"}', TIMESTAMP '2023-02-04 23:34:20', TIMESTAMP '2023-02-04 23:34:20');

COMMENT ON TABLE t_payment_app IS '支付应用表';

-- ----------------------------
-- t_refund_info: refund table
-- ----------------------------
CREATE TABLE t_refund_info (
  id BIGINT IDENTITY(1,1) NOT NULL,
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
  CONSTRAINT pk_t_refund_info PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_refund_no ON t_refund_info(refund_no);
CREATE UNIQUE INDEX uk_refund_id ON t_refund_info(refund_id);
CREATE INDEX idx_order_approval_status ON t_refund_info(order_no, approval_status);
CREATE INDEX idx_order_status ON t_refund_info(order_no, refund_status);

COMMENT ON TABLE t_refund_info IS '退款表';

-- ===============================================================
-- Idempotency and concurrency notes
-- ===============================================================
-- 1. uk_order_no prevents duplicated merchant order numbers.
-- 2. uk_order_payment_type prevents duplicate payment audit rows per order and payment type.
-- 3. uk_transaction_id_payment_type prevents duplicate provider transaction audit rows.
-- 4. uk_refund_no prevents duplicate merchant refund numbers.
-- 5. uk_refund_id prevents duplicate provider refund numbers when provided.
-- 6. GORM autoUpdateTime is responsible for update_time changes.
