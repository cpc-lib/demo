-- Read-only pre-cutover report. Any returned blocker row must be explained or
-- resolved before the v0.6.0 writer is enabled.

SELECT required.table_name AS missing_table
FROM (
  SELECT 't_product_sku' AS table_name
  UNION ALL SELECT 't_sku_stock'
  UNION ALL SELECT 't_inventory_reservation'
  UNION ALL SELECT 't_payment_order'
  UNION ALL SELECT 't_return_receipt'
  UNION ALL SELECT 't_fulfillment_operation'
) required
LEFT JOIN information_schema.tables actual
  ON actual.table_schema = DATABASE()
 AND actual.table_name = required.table_name
WHERE actual.table_name IS NULL;

SELECT table_name, index_name AS legacy_unique_index
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 't_payment_info'
  AND index_name = 'uk_order_payment_type';

SELECT p.id AS product_id, p.title, ps.id AS sku_id, ss.sku_id AS stock_sku_id
FROM t_product p
LEFT JOIN t_product_sku ps ON ps.product_id = p.id
LEFT JOIN t_sku_stock ss ON ss.sku_id = ps.id
WHERE ps.id IS NULL OR ss.sku_id IS NULL;

SELECT oi.id AS order_item_id, oi.order_id, oi.product_id, oi.sku_id
FROM t_order_item oi
LEFT JOIN t_product_sku ps ON ps.id = oi.sku_id
WHERE oi.sku_id IS NULL OR ps.id IS NULL OR ps.product_id <> oi.product_id;

SELECT oi.id AS order_item_id, oi.quantity, oi.pay_amount,
       oi.refund_frozen_qty, oi.refunded_qty,
       oi.refund_frozen_amount, oi.refunded_amount,
       oi.shipped_qty, oi.cancel_confirmed_qty, oi.received_qty, oi.restocked_qty
FROM t_order_item oi
WHERE oi.quantity <= 0
   OR oi.pay_amount < 0
   OR oi.refund_frozen_qty < 0
   OR oi.refunded_qty < 0
   OR oi.refund_frozen_qty + oi.refunded_qty > oi.quantity
   OR oi.refund_frozen_amount < 0
   OR oi.refunded_amount < 0
   OR oi.refund_frozen_amount + oi.refunded_amount > oi.pay_amount
   OR oi.shipped_qty < 0
   OR oi.cancel_confirmed_qty < 0
   OR oi.shipped_qty + oi.cancel_confirmed_qty > oi.quantity
   OR oi.received_qty < 0
   OR oi.received_qty > oi.shipped_qty
   OR oi.restocked_qty < 0;

SELECT o.order_no, o.payable_amount, o.paid_amount,
       o.refund_frozen_amount, o.refunded_amount, o.effective_payment_no
FROM t_order_info o
WHERE o.goods_amount IS NULL
   OR o.payable_amount IS NULL
   OR o.paid_amount IS NULL
   OR o.refund_frozen_amount IS NULL
   OR o.refunded_amount IS NULL
   OR o.paid_amount < 0
   OR o.refund_frozen_amount < 0
   OR o.refunded_amount < 0
   OR o.refund_frozen_amount + o.refunded_amount > o.paid_amount;

SELECT po.order_no, po.channel, po.channel_transaction_id, COUNT(*) AS fact_count
FROM t_payment_order po
WHERE po.channel_transaction_id IS NOT NULL
GROUP BY po.order_no, po.channel, po.channel_transaction_id
HAVING COUNT(*) > 1;

SELECT r.refund_no, r.order_no, r.refund_status
FROM t_refund_info r
LEFT JOIN t_refund_item ri ON ri.refund_id = r.id
WHERE r.refund_source = 'USER'
  AND ri.id IS NULL;

SELECT ir.order_no, ir.order_item_id, ir.sku_id, ir.quantity, ir.status
FROM t_inventory_reservation ir
LEFT JOIN t_order_item oi ON oi.id = ir.order_item_id
WHERE oi.id IS NULL
   OR oi.sku_id <> ir.sku_id
   OR oi.quantity <> ir.quantity;

