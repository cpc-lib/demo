-- Seckill: 高并发秒杀场景，每人限购 1 件，不能超卖。
-- 复用 Redis 分布式锁 + MySQL Fencing Token 机制。
-- 关键不变量：
--   1. stock >= 0 （不超卖）
--   2. UNIQUE(product_id, user_id) （一人一单，MySQL 强制）
--   3. fence_token 单调递增，业务写带条件防止 stale token 穿透

CREATE TABLE seckill_product (
  product_id    VARCHAR(64)  NOT NULL,
  name          VARCHAR(128) NOT NULL,
  stock         BIGINT       NOT NULL DEFAULT 0,
  sold          BIGINT       NOT NULL DEFAULT 0,
  fence_token   BIGINT       NOT NULL DEFAULT 0,
  updated_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                              ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (product_id),
  CONSTRAINT chk_seckill_stock  CHECK (stock >= 0),
  CONSTRAINT chk_seckill_fence  CHECK (fence_token >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 示例商品：product_id = 'SKU-001'，库存 10
INSERT INTO seckill_product(product_id, name, stock)
  VALUES ('SKU-001', '秒杀旗舰款', 10);

-- 一人一单的最终防线：唯一索引
CREATE TABLE seckill_order (
  order_id     BIGINT       NOT NULL AUTO_INCREMENT,
  product_id   VARCHAR(64)  NOT NULL,
  user_id      VARCHAR(64)  NOT NULL,
  fence_token  BIGINT       NOT NULL,
  created_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (order_id),
  UNIQUE KEY uk_product_user (product_id, user_id),
  KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 幂等记录表：(product_id, request_id) 唯一，防止前端重试重复下单
CREATE TABLE seckill_operation (
  product_id   VARCHAR(64)  NOT NULL,
  request_id   VARCHAR(64)  NOT NULL,
  user_id      VARCHAR(64)  NOT NULL,
  fence_token  BIGINT       NOT NULL,
  order_id     BIGINT       NOT NULL,
  created_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (product_id, request_id),
  KEY idx_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
