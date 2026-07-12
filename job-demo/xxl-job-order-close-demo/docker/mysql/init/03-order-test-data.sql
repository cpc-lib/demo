USE order_demo;

SET @current_time = NOW(3);

INSERT INTO biz_order (
    order_no, status, amount, created_at, expire_time,
    pay_time, close_time, close_reason, version, created_by, updated_at
) VALUES
('ORDER202607120001', 0, 99.00,
 DATE_SUB(@current_time, INTERVAL 2 HOUR),
 DATE_SUB(@current_time, INTERVAL 90 MINUTE),
 NULL, NULL, NULL, 0, 'test-user', DATE_SUB(@current_time, INTERVAL 2 HOUR)),

('ORDER202607120002', 0, 199.99,
 DATE_SUB(@current_time, INTERVAL 1 HOUR),
 DATE_SUB(@current_time, INTERVAL 30 MINUTE),
 NULL, NULL, NULL, 0, 'test-user', DATE_SUB(@current_time, INTERVAL 1 HOUR)),

('ORDER202607120003', 0, 29.90,
 DATE_SUB(@current_time, INTERVAL 50 MINUTE),
 DATE_SUB(@current_time, INTERVAL 20 MINUTE),
 NULL, NULL, NULL, 0, 'test-user', DATE_SUB(@current_time, INTERVAL 50 MINUTE)),

('ORDER202607120004', 0, 888.88,
 DATE_SUB(@current_time, INTERVAL 3 DAY),
 DATE_SUB(@current_time, INTERVAL 2 DAY),
 NULL, NULL, NULL, 0, 'test-user', DATE_SUB(@current_time, INTERVAL 3 DAY)),

('ORDER202607120005', 0, 59.90,
 @current_time,
 DATE_ADD(@current_time, INTERVAL 30 MINUTE),
 NULL, NULL, NULL, 0, 'test-user', @current_time),

('ORDER202607120006', 0, 128.00,
 DATE_SUB(@current_time, INTERVAL 5 MINUTE),
 DATE_ADD(@current_time, INTERVAL 25 MINUTE),
 NULL, NULL, NULL, 0, 'test-user', DATE_SUB(@current_time, INTERVAL 5 MINUTE)),

('ORDER202607120007', 1, 399.00,
 DATE_SUB(@current_time, INTERVAL 2 HOUR),
 DATE_SUB(@current_time, INTERVAL 90 MINUTE),
 DATE_SUB(@current_time, INTERVAL 100 MINUTE),
 NULL, NULL, 1, 'test-user', DATE_SUB(@current_time, INTERVAL 100 MINUTE)),

('ORDER202607120008', 1, 66.66,
 DATE_SUB(@current_time, INTERVAL 1 DAY),
 DATE_SUB(@current_time, INTERVAL 23 HOUR),
 DATE_SUB(DATE_SUB(@current_time, INTERVAL 23 HOUR), INTERVAL 30 MINUTE),
 NULL, NULL, 1, 'test-user',
 DATE_SUB(DATE_SUB(@current_time, INTERVAL 23 HOUR), INTERVAL 30 MINUTE)),

('ORDER202607120009', 2, 500.00,
 DATE_SUB(@current_time, INTERVAL 5 HOUR),
 DATE_SUB(@current_time, INTERVAL 4 HOUR),
 NULL, DATE_SUB(@current_time, INTERVAL 4 HOUR),
 'PAY_TIMEOUT', 1, 'powerjob', DATE_SUB(@current_time, INTERVAL 4 HOUR)),

('ORDER202607120010', 2, 18.80,
 DATE_SUB(@current_time, INTERVAL 2 DAY),
 DATE_SUB(@current_time, INTERVAL 47 HOUR),
 NULL, DATE_SUB(@current_time, INTERVAL 47 HOUR),
 'MANUAL_CLOSE', 1, 'admin', DATE_SUB(@current_time, INTERVAL 47 HOUR));
