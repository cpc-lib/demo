CREATE DATABASE IF NOT EXISTS demo;

CREATE TABLE IF NOT EXISTS demo.t_order (
  id UInt64,
  user_id UInt64,
  amount Decimal(18,2),
  status String,
  created_at DateTime
)
ENGINE = MergeTree
ORDER BY (created_at, id);

INSERT INTO demo.t_order (id, user_id, amount, status, created_at)
VALUES (1, 1001, toDecimal64(99.90, 2), 'PAID', now());

INSERT INTO demo.t_order (id, user_id, amount, status, created_at)
VALUES (2, 1002, toDecimal64(19.90, 2), 'NEW', now());

