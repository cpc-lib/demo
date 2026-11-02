-- 商品列表数据 + 支持重置：initial_stock 记录每个商品的初始库存
ALTER TABLE seckill_product
  ADD COLUMN initial_stock BIGINT NOT NULL DEFAULT 0 AFTER name;

-- 已有商品：初始库存 = 剩余库存 + 已售
UPDATE seckill_product SET initial_stock = stock + sold;

INSERT INTO seckill_product(product_id, name, initial_stock, stock) VALUES
  ('SKU-002', '秒杀轻薄本 14 寸', 5, 5),
  ('SKU-003', '秒杀降噪耳机', 20, 20),
  ('SKU-004', '秒杀智能手表', 8, 8);
