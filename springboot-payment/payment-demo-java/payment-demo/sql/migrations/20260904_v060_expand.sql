-- v0.6.0 expansion phase for an existing payment_demo database.
-- Execute exactly once in a tested maintenance window. New columns on legacy
-- tables stay nullable until evidence-based backfill and cutover validation.

CREATE TABLE IF NOT EXISTS `t_product_sku` (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `product_id` bigint(20) NOT NULL,
  `sku_code` varchar(64) NOT NULL,
  `sku_name` varchar(255) NULL,
  `sku_spec` json NULL,
  `status` varchar(16) NOT NULL DEFAULT 'ACTIVE',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_sku_product` (`product_id`),
  UNIQUE KEY `uk_product_sku_code` (`sku_code`),
  CONSTRAINT `fk_v060_product_sku_product` FOREIGN KEY (`product_id`) REFERENCES `t_product` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `t_sku_stock` (
  `sku_id` bigint(20) UNSIGNED NOT NULL,
  `available_stock` int(11) NOT NULL DEFAULT 0,
  `locked_stock` int(11) NOT NULL DEFAULT 0,
  `version` bigint(20) NOT NULL DEFAULT 0,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`sku_id`),
  CONSTRAINT `fk_v060_sku_stock_sku` FOREIGN KEY (`sku_id`) REFERENCES `t_product_sku` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_v060_sku_available` CHECK (`available_stock` >= 0),
  CONSTRAINT `chk_v060_sku_locked` CHECK (`locked_stock` >= 0),
  CONSTRAINT `chk_v060_sku_version` CHECK (`version` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE `t_order_info`
  ADD COLUMN `goods_amount` bigint(20) NULL AFTER `total_fee`,
  ADD COLUMN `discount_amount` bigint(20) NULL AFTER `goods_amount`,
  ADD COLUMN `freight_amount` bigint(20) NULL AFTER `discount_amount`,
  ADD COLUMN `payable_amount` bigint(20) NULL AFTER `freight_amount`,
  ADD COLUMN `paid_amount` bigint(20) NULL AFTER `payable_amount`,
  ADD COLUMN `refund_frozen_amount` bigint(20) NULL AFTER `paid_amount`,
  ADD COLUMN `refunded_amount` bigint(20) NULL AFTER `refund_frozen_amount`,
  ADD COLUMN `freight_refund_frozen_amount` bigint(20) NULL AFTER `refunded_amount`,
  ADD COLUMN `freight_refunded_amount` bigint(20) NULL AFTER `freight_refund_frozen_amount`,
  ADD COLUMN `effective_payment_no` varchar(64) NULL AFTER `freight_refunded_amount`,
  ADD COLUMN `pay_status` varchar(32) NULL AFTER `order_status`,
  ADD COLUMN `fulfillment_status` varchar(32) NULL AFTER `pay_status`,
  ADD COLUMN `refund_status` varchar(32) NULL AFTER `fulfillment_status`,
  ADD COLUMN `close_reason` varchar(32) NULL AFTER `refund_status`,
  ADD COLUMN `expires_at` datetime NULL AFTER `close_reason`,
  ADD COLUMN `close_requested_at` datetime NULL AFTER `expires_at`,
  ADD COLUMN `paid_at` datetime NULL AFTER `close_requested_at`,
  ADD UNIQUE KEY `uk_order_effective_payment` (`effective_payment_no`),
  ADD KEY `idx_order_close_scan` (`order_status`, `expires_at`, `close_requested_at`);

ALTER TABLE `t_order_item`
  ADD COLUMN `sku_id` bigint(20) UNSIGNED NULL AFTER `product_id`,
  ADD COLUMN `product_name_snapshot` varchar(255) NULL AFTER `subtotal`,
  ADD COLUMN `sku_name_snapshot` varchar(255) NULL AFTER `product_name_snapshot`,
  ADD COLUMN `sku_spec_snapshot` json NULL AFTER `sku_name_snapshot`,
  ADD COLUMN `original_unit_amount` bigint(20) NULL AFTER `sku_spec_snapshot`,
  ADD COLUMN `deal_unit_amount` bigint(20) NULL AFTER `original_unit_amount`,
  ADD COLUMN `original_total_amount` bigint(20) NULL AFTER `deal_unit_amount`,
  ADD COLUMN `discount_amount` bigint(20) NULL AFTER `original_total_amount`,
  ADD COLUMN `pay_amount` bigint(20) NULL AFTER `discount_amount`,
  ADD COLUMN `refund_frozen_qty` int(11) NULL AFTER `pay_amount`,
  ADD COLUMN `refunded_qty` int(11) NULL AFTER `refund_frozen_qty`,
  ADD COLUMN `refund_frozen_amount` bigint(20) NULL AFTER `refunded_qty`,
  ADD COLUMN `refunded_amount` bigint(20) NULL AFTER `refund_frozen_amount`,
  ADD COLUMN `shipped_qty` int(11) NULL AFTER `refunded_amount`,
  ADD COLUMN `cancel_confirmed_qty` int(11) NULL AFTER `shipped_qty`,
  ADD COLUMN `received_qty` int(11) NULL AFTER `cancel_confirmed_qty`,
  ADD COLUMN `restocked_qty` int(11) NULL AFTER `received_qty`,
  ADD KEY `idx_order_item_sku` (`sku_id`),
  ADD CONSTRAINT `fk_v060_order_item_sku` FOREIGN KEY (`sku_id`) REFERENCES `t_product_sku` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE;

CREATE TABLE IF NOT EXISTS `t_inventory_reservation` (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `reservation_no` varchar(64) NOT NULL,
  `order_no` varchar(64) NOT NULL,
  `order_item_id` bigint(20) UNSIGNED NOT NULL,
  `sku_id` bigint(20) UNSIGNED NOT NULL,
  `quantity` int(11) NOT NULL,
  `status` varchar(32) NOT NULL,
  `expires_at` datetime NOT NULL,
  `committed_at` datetime NULL,
  `released_at` datetime NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_reservation_no` (`reservation_no`),
  UNIQUE KEY `uk_reservation_order_item` (`order_item_id`),
  KEY `idx_reservation_order_status` (`order_no`, `status`),
  KEY `idx_reservation_sku_status` (`sku_id`, `status`),
  CONSTRAINT `fk_v060_reservation_item` FOREIGN KEY (`order_item_id`) REFERENCES `t_order_item` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_v060_reservation_sku` FOREIGN KEY (`sku_id`) REFERENCES `t_product_sku` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_v060_reservation_quantity` CHECK (`quantity` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `t_payment_order` (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `payment_no` varchar(64) NOT NULL,
  `order_no` varchar(64) NOT NULL,
  `payment_app_id` bigint(20) UNSIGNED NOT NULL,
  `channel` varchar(32) NOT NULL,
  `protocol` varchar(32) NOT NULL,
  `channel_merchant_order_no` varchar(128) NOT NULL,
  `channel_transaction_id` varchar(128) NULL,
  `request_amount` bigint(20) NOT NULL,
  `paid_amount` bigint(20) NOT NULL DEFAULT 0,
  `payer_cash_amount` bigint(20) NULL,
  `receipt_amount` bigint(20) NULL,
  `currency` varchar(8) NOT NULL DEFAULT 'CNY',
  `status` varchar(32) NOT NULL,
  `disposition` varchar(32) NOT NULL DEFAULT 'NONE',
  `request_id` varchar(64) NOT NULL,
  `request_fingerprint` char(64) NOT NULL,
  `payment_params` text NULL,
  `refund_frozen_amount` bigint(20) NOT NULL DEFAULT 0,
  `refunded_amount` bigint(20) NOT NULL DEFAULT 0,
  `expires_at` datetime NULL,
  `paid_at` datetime NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_no` (`payment_no`),
  UNIQUE KEY `uk_payment_request` (`order_no`, `request_id`),
  UNIQUE KEY `uk_payment_merchant_no` (`payment_app_id`, `channel_merchant_order_no`),
  UNIQUE KEY `uk_payment_transaction` (`payment_app_id`, `channel_transaction_id`),
  KEY `idx_payment_order_status` (`order_no`, `status`),
  KEY `idx_payment_recovery` (`status`, `expires_at`, `update_time`),
  CONSTRAINT `fk_v060_payment_order` FOREIGN KEY (`order_no`) REFERENCES `t_order_info` (`order_no`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_v060_payment_app` FOREIGN KEY (`payment_app_id`) REFERENCES `t_payment_app` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_v060_payment_amounts` CHECK (`request_amount` >= 0 AND `paid_amount` >= 0 AND (`payer_cash_amount` IS NULL OR `payer_cash_amount` >= 0) AND (`receipt_amount` IS NULL OR `receipt_amount` >= 0) AND `refund_frozen_amount` >= 0 AND `refunded_amount` >= 0 AND `refund_frozen_amount` + `refunded_amount` <= `paid_amount`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE `t_payment_info`
  ADD COLUMN `payment_no` varchar(64) NULL AFTER `id`,
  ADD COLUMN `payment_app_id` bigint(20) UNSIGNED NULL AFTER `payment_no`,
  MODIFY COLUMN `transaction_id` varchar(128) NULL,
  ADD UNIQUE KEY `uk_payment_info_payment_no` (`payment_no`),
  ADD UNIQUE KEY `uk_payment_info_transaction` (`payment_app_id`, `transaction_id`),
  ADD CONSTRAINT `fk_v060_payment_info_payment` FOREIGN KEY (`payment_no`) REFERENCES `t_payment_order` (`payment_no`) ON DELETE RESTRICT ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_v060_payment_info_app` FOREIGN KEY (`payment_app_id`) REFERENCES `t_payment_app` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE `t_refund_info`
  ADD COLUMN `payment_no` varchar(64) NULL AFTER `refund_id`,
  ADD COLUMN `refund_source` varchar(16) NULL AFTER `payment_no`,
  ADD COLUMN `refund_type` varchar(32) NULL AFTER `refund_source`,
  ADD COLUMN `request_fingerprint` char(64) NULL AFTER `application_request_id`,
  ADD COLUMN `system_refund_key` varchar(160) NULL AFTER `request_fingerprint`,
  ADD COLUMN `goods_refund_amount` bigint(20) NULL AFTER `refund`,
  ADD COLUMN `freight_refund_amount` bigint(20) NULL AFTER `goods_refund_amount`,
  ADD COLUMN `refund_amount` bigint(20) NULL AFTER `freight_refund_amount`,
  ADD COLUMN `channel_refund_id` varchar(128) NULL AFTER `refund_status`,
  ADD COLUMN `channel_status` varchar(32) NULL AFTER `channel_refund_id`,
  ADD COLUMN `requested_at` datetime NULL AFTER `channel_status`,
  ADD COLUMN `success_time` datetime NULL AFTER `requested_at`,
  ADD UNIQUE KEY `uk_refund_system_key` (`system_refund_key`),
  ADD KEY `idx_refund_payment_status` (`payment_no`, `refund_status`),
  ADD CONSTRAINT `fk_v060_refund_payment` FOREIGN KEY (`payment_no`) REFERENCES `t_payment_order` (`payment_no`) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE `t_refund_item`
  ADD COLUMN `sku_id` bigint(20) UNSIGNED NULL AFTER `product_id`,
  MODIFY COLUMN `refund_amount` bigint(20) NOT NULL,
  ADD COLUMN `quota_kind` varchar(16) NULL AFTER `refund_amount`,
  ADD COLUMN `received_qty` int(11) NULL AFTER `quota_kind`,
  ADD COLUMN `accepted_qty` int(11) NULL AFTER `received_qty`,
  ADD COLUMN `restock_authorized_qty` int(11) NULL AFTER `accepted_qty`,
  ADD COLUMN `restocked_qty` int(11) NULL AFTER `restock_authorized_qty`,
  ADD COLUMN `status` varchar(32) NULL AFTER `restocked_qty`,
  ADD COLUMN `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER `create_time`,
  ADD KEY `idx_refund_item_sku` (`sku_id`),
  ADD CONSTRAINT `fk_v060_refund_item_sku` FOREIGN KEY (`sku_id`) REFERENCES `t_product_sku` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE;

CREATE TABLE IF NOT EXISTS `t_return_receipt` (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `refund_id` bigint(20) UNSIGNED NOT NULL,
  `refund_item_id` bigint(20) UNSIGNED NOT NULL,
  `receipt_request_id` varchar(64) NOT NULL,
  `request_fingerprint` char(64) NOT NULL,
  `received_qty` int(11) NOT NULL,
  `saleable_qty` int(11) NOT NULL,
  `rejected_qty` int(11) NOT NULL,
  `operator_id` bigint(20) UNSIGNED NOT NULL,
  `operator_name` varchar(64) NOT NULL,
  `reason` varchar(255) NOT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_return_receipt_request` (`refund_item_id`, `receipt_request_id`),
  KEY `idx_return_receipt_refund` (`refund_id`, `create_time`),
  CONSTRAINT `fk_v060_receipt_refund` FOREIGN KEY (`refund_id`) REFERENCES `t_refund_info` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_v060_receipt_item` FOREIGN KEY (`refund_item_id`) REFERENCES `t_refund_item` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_v060_receipt_operator` FOREIGN KEY (`operator_id`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_v060_receipt_quantity` CHECK (`received_qty` > 0 AND `saleable_qty` >= 0 AND `rejected_qty` >= 0 AND `saleable_qty` + `rejected_qty` = `received_qty`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `t_fulfillment_operation` (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `order_no` varchar(64) NOT NULL,
  `order_item_id` bigint(20) UNSIGNED NOT NULL,
  `request_id` varchar(64) NOT NULL,
  `request_fingerprint` char(64) NOT NULL,
  `operation_type` varchar(16) NOT NULL,
  `quantity` int(11) NOT NULL,
  `before_quantity` int(11) NOT NULL,
  `after_quantity` int(11) NOT NULL,
  `operator_id` bigint(20) UNSIGNED NOT NULL,
  `operator_name` varchar(64) NOT NULL,
  `reason` varchar(255) NOT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_fulfillment_operation` (`order_no`, `request_id`, `order_item_id`, `operation_type`),
  KEY `idx_fulfillment_item_time` (`order_item_id`, `create_time`),
  CONSTRAINT `fk_v060_fulfillment_order` FOREIGN KEY (`order_no`) REFERENCES `t_order_info` (`order_no`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_v060_fulfillment_item` FOREIGN KEY (`order_item_id`) REFERENCES `t_order_item` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_v060_fulfillment_operator` FOREIGN KEY (`operator_id`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_v060_fulfillment_quantity` CHECK (`quantity` > 0 AND `before_quantity` >= 0 AND `after_quantity` >= `before_quantity` AND `after_quantity` = `before_quantity` + `quantity`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE `t_inventory_operation`
  ADD COLUMN `sku_id` bigint(20) UNSIGNED NULL AFTER `product_id`,
  ADD COLUMN `order_item_id` bigint(20) UNSIGNED NULL AFTER `sku_id`,
  ADD COLUMN `receipt_request_id` varchar(64) NULL AFTER `order_item_id`,
  ADD KEY `idx_inventory_sku_time` (`sku_id`, `create_time`),
  ADD CONSTRAINT `fk_v060_inventory_sku` FOREIGN KEY (`sku_id`) REFERENCES `t_product_sku` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_v060_inventory_item` FOREIGN KEY (`order_item_id`) REFERENCES `t_order_item` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE;

