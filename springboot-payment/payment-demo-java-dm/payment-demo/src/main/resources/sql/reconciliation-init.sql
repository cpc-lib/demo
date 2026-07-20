-- t_reconciliation_batch 对账批次表
CREATE TABLE t_reconciliation_batch (
  id BIGINT IDENTITY(1, 1) NOT NULL,
  batch_no VARCHAR(50) NOT NULL,
  channel_code VARCHAR(32) NOT NULL,
  payment_app_id BIGINT,
  bill_date VARCHAR(10) NOT NULL,
  status VARCHAR(30) NOT NULL,
  channel_total_count INT DEFAULT 0,
  channel_total_amount INT DEFAULT 0,
  local_total_count INT DEFAULT 0,
  local_total_amount INT DEFAULT 0,
  matched_count INT DEFAULT 0,
  matched_amount INT DEFAULT 0,
  discrepancy_count INT DEFAULT 0,
  overpayment_count INT DEFAULT 0,
  underpayment_count INT DEFAULT 0,
  amount_mismatch_count INT DEFAULT 0,
  status_mismatch_count INT DEFAULT 0,
  failure_reason VARCHAR(512),
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT pk_reconciliation_batch PRIMARY KEY (id),
  CONSTRAINT uk_batch_no UNIQUE (batch_no),
  CONSTRAINT uk_batch_channel_date_app UNIQUE (channel_code, bill_date, payment_app_id)
);

CREATE INDEX idx_batch_status ON t_reconciliation_batch(status);
CREATE INDEX idx_batch_channel_date ON t_reconciliation_batch(channel_code, bill_date);

-- t_reconciliation_detail 对账明细表
CREATE TABLE t_reconciliation_detail (
  id BIGINT IDENTITY(1, 1) NOT NULL,
  batch_no VARCHAR(50) NOT NULL,
  order_no VARCHAR(50),
  transaction_id VARCHAR(50),
  trade_type VARCHAR(20),
  channel_amount INT,
  local_amount INT,
  channel_status VARCHAR(50),
  local_status VARCHAR(50),
  match_status VARCHAR(30),
  discrepancy_type VARCHAR(30),
  trade_time TIMESTAMP,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT pk_reconciliation_detail PRIMARY KEY (id)
);

CREATE INDEX idx_detail_batch_no ON t_reconciliation_detail(batch_no);
CREATE INDEX idx_detail_match_status ON t_reconciliation_detail(batch_no, match_status);
CREATE INDEX idx_detail_discrepancy ON t_reconciliation_detail(batch_no, discrepancy_type);

-- t_reconciliation_discrepancy 对账差异单表
CREATE TABLE t_reconciliation_discrepancy (
  id BIGINT IDENTITY(1, 1) NOT NULL,
  batch_no VARCHAR(50) NOT NULL,
  detail_id BIGINT,
  discrepancy_type VARCHAR(30) NOT NULL,
  status VARCHAR(20) DEFAULT 'OPEN' NOT NULL,
  resolve_remark VARCHAR(512),
  resolved_time TIMESTAMP,
  resolved_by VARCHAR(64),
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT pk_reconciliation_discrepancy PRIMARY KEY (id)
);

CREATE INDEX idx_discrepancy_batch ON t_reconciliation_discrepancy(batch_no);
CREATE INDEX idx_discrepancy_status ON t_reconciliation_discrepancy(status);
