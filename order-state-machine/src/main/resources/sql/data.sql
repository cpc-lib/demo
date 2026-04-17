USE `test`;

INSERT INTO `t_order_state_transition`
(`business_type`, `current_status`, `event`, `next_status`, `is_enabled`, `rollback_mode`, `remark`)
VALUES
('NORMAL', 'CREATED', 'PAY', 'PAID', 1, 'NONE', '待支付->已支付'),
('NORMAL', 'CREATED', 'CANCEL', 'CANCELLED', 1, 'NONE', '待支付->已取消'),
('NORMAL', 'CREATED', 'TIMEOUT_CLOSE', 'CLOSED', 1, 'NONE', '待支付->超时关闭'),

('NORMAL', 'PAID', 'SHIP', 'SHIPPED', 1, 'NONE', '已支付->已发货'),
('NORMAL', 'PAID', 'APPLY_REFUND', 'REFUNDING', 1, 'NONE', '已支付->退款中'),

('NORMAL', 'SHIPPED', 'CONFIRM_RECEIPT', 'COMPLETED', 1, 'NONE', '已发货->已完成'),
('NORMAL', 'SHIPPED', 'APPLY_REFUND', 'REFUNDING', 1, 'NONE', '已发货->退款中'),

('NORMAL', 'REFUNDING', 'REFUND_SUCCESS', 'REFUNDED', 1, 'NONE', '退款中->已退款'),
('NORMAL', 'REFUNDING', 'REFUND_REJECT', 'REFUNDING', 1, 'PREVIOUS_STATUS', '退款中->恢复原状态');
