package main

import (
	"fmt"
	"log"
	"strings"

	"payment-demo-go/internal/config"
	"payment-demo-go/internal/db"

	"gorm.io/gorm"
)

func main() {
	cfg, err := config.Load()
	if err != nil {
		log.Fatalf("加载配置失败: %v", err)
	}

	// 先用默认 schema 连接（不指定 schema）
	defaultDSN := strings.Replace(cfg.Database.DSN, "schema=PAYMENT_DEMO", "schema=SYSDBA", -1)
	defaultCfg := cfg
	defaultCfg.Database.DSN = defaultDSN

	gormDB, err := gorm.Open(db.Dialector(defaultCfg.Database), &gorm.Config{})
	if err != nil {
		log.Fatalf("连接达梦数据库失败: %v", err)
	}

	log.Println("开始数据库初始化...")

	// 步骤1：创建 schema（如果不存在）
	log.Println("步骤1：创建 PAYMENT_DEMO schema...")
	if err := gormDB.Exec("CREATE SCHEMA PAYMENT_DEMO AUTHORIZATION SYSDBA").Error; err != nil {
		log.Printf("[INFO] schema 可能已存在: %v", err)
	}

	// 步骤2：切换到 PAYMENT_DEMO schema
	log.Println("步骤2：切换到 PAYMENT_DEMO schema...")
	gormDB.Exec("SET SCHEMA PAYMENT_DEMO")

	// 步骤3：删除表（按依赖顺序）
	log.Println("步骤3：清理旧表...")
	dropTables := []string{
		"t_reconciliation_diff",
		"t_reconciliation_detail",
		"t_reconciliation_task",
		"t_refund_info",
		"t_payment_info",
		"t_order_info",
		"t_payment_app",
		"t_payment_channel",
		"t_product",
	}
	for _, table := range dropTables {
		gormDB.Exec(fmt.Sprintf("DROP TABLE IF EXISTS %s", table))
	}

	// 步骤4：创建表
	log.Println("步骤4：创建表...")
	createStatements := getCreateTableStatements()
	executed := 0
	failed := 0
	for _, stmt := range createStatements {
		stmt = strings.TrimSpace(stmt)
		if stmt == "" {
			continue
		}
		if err := gormDB.Exec(stmt).Error; err != nil {
			log.Printf("[WARN] 创建失败: %v", err)
			failed++
			continue
		}
		executed++
	}

	// 步骤5：创建索引
	log.Println("步骤5：创建索引...")
	indexStatements := getIndexStatements()
	for _, stmt := range indexStatements {
		stmt = strings.TrimSpace(stmt)
		if stmt == "" {
			continue
		}
		if err := gormDB.Exec(stmt).Error; err != nil {
			log.Printf("[WARN] 索引创建失败（可能已存在）: %v", err)
			failed++
			continue
		}
		executed++
	}

	// 步骤6：插入种子数据
	log.Println("步骤6：插入种子数据...")
	insertStatements := getInsertStatements()
	for _, stmt := range insertStatements {
		stmt = strings.TrimSpace(stmt)
		if stmt == "" {
			continue
		}
		if err := gormDB.Exec(stmt).Error; err != nil {
			log.Printf("[WARN] 插入失败: %v", err)
			failed++
			continue
		}
		executed++
	}

	// 步骤7：添加注释
	log.Println("步骤7：添加注释...")
	commentStatements := getCommentStatements()
	for _, stmt := range commentStatements {
		stmt = strings.TrimSpace(stmt)
		if stmt == "" {
			continue
		}
		if err := gormDB.Exec(stmt).Error; err != nil {
			log.Printf("[WARN] 注释失败: %v", err)
			failed++
			continue
		}
		executed++
	}

	log.Printf("SQL 初始化完成: 成功 %d 条, 失败 %d 条", executed, failed)

	// 验证
	var count int64
	gormDB.Raw("SELECT COUNT(*) FROM PAYMENT_DEMO.t_product").Scan(&count)
	fmt.Printf("验证: t_product 表中有 %d 条记录\n", count)
	gormDB.Raw("SELECT COUNT(*) FROM PAYMENT_DEMO.t_payment_channel").Scan(&count)
	fmt.Printf("验证: t_payment_channel 表中有 %d 条记录\n", count)
	gormDB.Raw("SELECT COUNT(*) FROM PAYMENT_DEMO.t_payment_app").Scan(&count)
	fmt.Printf("验证: t_payment_app 表中有 %d 条记录\n", count)

	log.Println("数据库初始化完成！")
}

func getCreateTableStatements() []string {
	return []string{
		`CREATE TABLE t_order_info (
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
)`,
		`CREATE TABLE t_payment_info (
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
)`,
		`CREATE TABLE t_product (
  id BIGINT IDENTITY(1,1) NOT NULL,
  title VARCHAR(20),
  price INT,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT pk_t_product PRIMARY KEY (id)
)`,
		`CREATE TABLE t_payment_channel (
  id BIGINT IDENTITY(1,1) NOT NULL,
  channel_code VARCHAR(50) NOT NULL,
  channel_name VARCHAR(50) NOT NULL,
  payment_type VARCHAR(20) NOT NULL,
  enabled BIT NOT NULL DEFAULT 1,
  config_params CLOB,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT pk_t_payment_channel PRIMARY KEY (id)
)`,
		`CREATE TABLE t_payment_app (
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
)`,
		`CREATE TABLE t_refund_info (
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
)`,
		`CREATE TABLE t_reconciliation_task (
  id BIGINT IDENTITY(1,1) NOT NULL,
  task_no VARCHAR(50) NOT NULL,
  payment_type VARCHAR(20) NOT NULL,
  payment_app_id BIGINT,
  bill_date VARCHAR(10) NOT NULL,
  bill_type VARCHAR(20) NOT NULL,
  task_status VARCHAR(20) NOT NULL,
  total_count INT DEFAULT 0,
  matched_count INT DEFAULT 0,
  diff_count INT DEFAULT 0,
  error_msg VARCHAR(1000),
  trigger_source VARCHAR(20),
  remark VARCHAR(255),
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT pk_t_reconciliation_task PRIMARY KEY (id)
)`,
		`CREATE TABLE t_reconciliation_detail (
  id BIGINT IDENTITY(1,1) NOT NULL,
  task_id BIGINT NOT NULL,
  detail_type VARCHAR(20) NOT NULL,
  order_no VARCHAR(50),
  refund_no VARCHAR(50),
  transaction_id VARCHAR(50),
  channel_trade_no VARCHAR(50),
  local_amount INT,
  channel_amount INT,
  local_status VARCHAR(30),
  channel_status VARCHAR(30),
  match_status VARCHAR(20) NOT NULL,
  diff_type VARCHAR(30),
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT pk_t_reconciliation_detail PRIMARY KEY (id)
)`,
		`CREATE TABLE t_reconciliation_diff (
  id BIGINT IDENTITY(1,1) NOT NULL,
  task_id BIGINT NOT NULL,
  detail_id BIGINT NOT NULL,
  diff_type VARCHAR(30) NOT NULL,
  detail_type VARCHAR(20) NOT NULL,
  order_no VARCHAR(50),
  refund_no VARCHAR(50),
  local_data CLOB,
  channel_data CLOB,
  handle_status VARCHAR(20) NOT NULL,
  handle_type VARCHAR(30),
  handle_remark VARCHAR(500),
  handled_by VARCHAR(50),
  handled_time TIMESTAMP,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT pk_t_reconciliation_diff PRIMARY KEY (id)
)`,
	}
}

func getIndexStatements() []string {
	return []string{
		`CREATE UNIQUE INDEX uk_order_no ON t_order_info(order_no)`,
		`CREATE INDEX idx_product_status_pay_type ON t_order_info(product_id, order_status, payment_type)`,
		`CREATE INDEX idx_product_payment_status_time ON t_order_info(product_id, payment_type, order_status, create_time)`,
		`CREATE INDEX idx_payment_app_status ON t_order_info(payment_app_id, order_status)`,
		`CREATE UNIQUE INDEX uk_order_payment_type ON t_payment_info(order_no, payment_type)`,
		`CREATE UNIQUE INDEX uk_transaction_id_payment_type ON t_payment_info(transaction_id, payment_type)`,
		`CREATE INDEX idx_order_no ON t_payment_info(order_no)`,
		`CREATE UNIQUE INDEX uk_channel_code ON t_payment_channel(channel_code)`,
		`CREATE INDEX idx_payment_type_enabled ON t_payment_channel(payment_type, enabled)`,
		`CREATE UNIQUE INDEX uk_app_code ON t_payment_app(app_code)`,
		`CREATE INDEX idx_channel_enabled ON t_payment_app(channel_code, enabled)`,
		`CREATE INDEX idx_app_payment_type_enabled ON t_payment_app(payment_type, enabled)`,
		`CREATE UNIQUE INDEX uk_refund_no ON t_refund_info(refund_no)`,
		`CREATE UNIQUE INDEX uk_refund_id ON t_refund_info(refund_id)`,
		`CREATE INDEX idx_order_approval_status ON t_refund_info(order_no, approval_status)`,
		`CREATE INDEX idx_order_status ON t_refund_info(order_no, refund_status)`,
		`CREATE UNIQUE INDEX uk_task_unique ON t_reconciliation_task(payment_type, payment_app_id, bill_date, bill_type)`,
		`CREATE UNIQUE INDEX uk_task_no ON t_reconciliation_task(task_no)`,
		`CREATE INDEX idx_task_status ON t_reconciliation_task(task_status)`,
		`CREATE INDEX idx_task_bill_date ON t_reconciliation_task(bill_date)`,
		`CREATE INDEX idx_detail_task_id ON t_reconciliation_detail(task_id)`,
		`CREATE INDEX idx_detail_match_status ON t_reconciliation_detail(match_status)`,
		`CREATE INDEX idx_detail_order_no ON t_reconciliation_detail(order_no)`,
		`CREATE INDEX idx_detail_refund_no ON t_reconciliation_detail(refund_no)`,
		`CREATE INDEX idx_diff_task_id ON t_reconciliation_diff(task_id)`,
		`CREATE INDEX idx_diff_handle_status ON t_reconciliation_diff(handle_status)`,
		`CREATE INDEX idx_diff_diff_type ON t_reconciliation_diff(diff_type)`,
		`CREATE INDEX idx_diff_order_no ON t_reconciliation_diff(order_no)`,
		`CREATE INDEX idx_diff_refund_no ON t_reconciliation_diff(refund_no)`,
	}
}

func getInsertStatements() []string {
	return []string{
		`INSERT INTO t_product (title, price, create_time, update_time) VALUES ('Java课程', 1, TIMESTAMP '2023-02-04 23:34:20', TIMESTAMP '2023-02-04 23:34:20')`,
		`INSERT INTO t_product (title, price, create_time, update_time) VALUES ('大数据课程', 1, TIMESTAMP '2023-02-04 23:34:20', TIMESTAMP '2023-02-04 23:34:20')`,
		`INSERT INTO t_product (title, price, create_time, update_time) VALUES ('前端课程', 1, TIMESTAMP '2023-02-04 23:34:20', TIMESTAMP '2023-02-04 23:34:20')`,
		`INSERT INTO t_product (title, price, create_time, update_time) VALUES ('UI课程', 1, TIMESTAMP '2023-02-04 23:34:20', TIMESTAMP '2023-02-04 23:34:20')`,
		`INSERT INTO t_payment_channel (channel_code, channel_name, payment_type, enabled, config_params, create_time, update_time) VALUES ('wxpay', '微信支付', '微信', 1, '{"domain":"https://api.mch.weixin.qq.com","notifyDomain":"https://5834-2409-8a50-3e25-e7f0-ecc6-e42-7b4c-1466.ngrok-free.app"}', TIMESTAMP '2023-02-04 23:34:20', TIMESTAMP '2023-02-04 23:34:20')`,
		`INSERT INTO t_payment_channel (channel_code, channel_name, payment_type, enabled, config_params, create_time, update_time) VALUES ('alipay', '支付宝', '支付宝', 1, '{"gatewayUrl":"https://openapi-sandbox.dl.alipaydev.com/gateway.do"}', TIMESTAMP '2023-02-04 23:34:20', TIMESTAMP '2023-02-04 23:34:20')`,
		`INSERT INTO t_payment_app (app_code, app_name, channel_code, payment_type, enabled, app_config, create_time, update_time) VALUES ('wxpay-default', '微信支付默认应用', 'wxpay', '微信', 1, '{"appid":"wx74862e0dfcf69954","mchId":"1558950191","mchSerialNo":"34345964330B66427E0D3D28826C4993C77E631F","domain":"https://api.mch.weixin.qq.com","notifyDomain":"https://5834-2409-8a50-3e25-e7f0-ecc6-e42-7b4c-1466.ngrok-free.app"}', TIMESTAMP '2023-02-04 23:34:20', TIMESTAMP '2023-02-04 23:34:20')`,
		`INSERT INTO t_payment_app (app_code, app_name, channel_code, payment_type, enabled, app_config, create_time, update_time) VALUES ('alipay-sandbox', '支付宝沙箱应用', 'alipay', '支付宝', 1, '{"appId":"9021000136667568","sellerId":"2088721034748965","gatewayUrl":"https://openapi-sandbox.dl.alipaydev.com/gateway.do","returnUrl":"http://localhost:8083/#/success","notifyUrl":"http://arhi.nat300.top/api/ali-pay/trade/notify"}', TIMESTAMP '2023-02-04 23:34:20', TIMESTAMP '2023-02-04 23:34:20')`,
	}
}

func getCommentStatements() []string {
	return []string{
		`COMMENT ON TABLE t_order_info IS '订单表'`,
		`COMMENT ON COLUMN t_order_info.payment_app_id IS '支付应用id'`,
		`COMMENT ON TABLE t_payment_info IS '支付流水表'`,
		`COMMENT ON TABLE t_product IS '商品表'`,
		`COMMENT ON TABLE t_payment_channel IS '支付渠道表'`,
		`COMMENT ON TABLE t_payment_app IS '支付应用表'`,
		`COMMENT ON TABLE t_refund_info IS '退款表'`,
		`COMMENT ON TABLE t_reconciliation_task IS '对账任务表'`,
		`COMMENT ON TABLE t_reconciliation_detail IS '对账明细表'`,
		`COMMENT ON TABLE t_reconciliation_diff IS '对账差异表'`,
	}
}
