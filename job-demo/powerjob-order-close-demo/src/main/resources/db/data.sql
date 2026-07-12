-- 避免重复执行时触发 order_no 唯一索引冲突
DELETE FROM biz_order
WHERE order_no BETWEEN 'ORDER202607120001' AND 'ORDER202607120010';

-- 统一测试数据的时间基准，避免同一条 SQL 中 NOW(3) 存在微小时间差
SET @current_time = NOW(3);

INSERT INTO biz_order (
    order_no,
    status,
    amount,
    created_at,
    expire_time,
    pay_time,
    close_time,
    close_reason,
    version,
    created_by,
    updated_at
) VALUES
-- 1. 已超时未支付：应该被任务关闭
(
    'ORDER202607120001',
    0,
    99.00,
    DATE_SUB(@current_time, INTERVAL 2 HOUR),
    DATE_SUB(@current_time, INTERVAL 90 MINUTE),
    NULL,
    NULL,
    NULL,
    0,
    'test-user',
    DATE_SUB(@current_time, INTERVAL 2 HOUR)
),

-- 2. 已超时未支付
(
    'ORDER202607120002',
    0,
    199.99,
    DATE_SUB(@current_time, INTERVAL 1 HOUR),
    DATE_SUB(@current_time, INTERVAL 30 MINUTE),
    NULL,
    NULL,
    NULL,
    0,
    'test-user',
    DATE_SUB(@current_time, INTERVAL 1 HOUR)
),

-- 3. 已超时未支付
(
    'ORDER202607120003',
    0,
    29.90,
    DATE_SUB(@current_time, INTERVAL 50 MINUTE),
    DATE_SUB(@current_time, INTERVAL 20 MINUTE),
    NULL,
    NULL,
    NULL,
    0,
    'test-user',
    DATE_SUB(@current_time, INTERVAL 50 MINUTE)
),

-- 4. 已超时未支付
(
    'ORDER202607120004',
    0,
    888.88,
    DATE_SUB(@current_time, INTERVAL 3 DAY),
    DATE_SUB(@current_time, INTERVAL 2 DAY),
    NULL,
    NULL,
    NULL,
    0,
    'test-user',
    DATE_SUB(@current_time, INTERVAL 3 DAY)
),

-- 5. 未超时未支付：不应该被关闭
(
    'ORDER202607120005',
    0,
    59.90,
    @current_time,
    DATE_ADD(@current_time, INTERVAL 30 MINUTE),
    NULL,
    NULL,
    NULL,
    0,
    'test-user',
    @current_time
),

-- 6. 未超时未支付
(
    'ORDER202607120006',
    0,
    128.00,
    DATE_SUB(@current_time, INTERVAL 5 MINUTE),
    DATE_ADD(@current_time, INTERVAL 25 MINUTE),
    NULL,
    NULL,
    NULL,
    0,
    'test-user',
    DATE_SUB(@current_time, INTERVAL 5 MINUTE)
),

-- 7. 已支付：即使过期也不能关闭
(
    'ORDER202607120007',
    1,
    399.00,
    DATE_SUB(@current_time, INTERVAL 2 HOUR),
    DATE_SUB(@current_time, INTERVAL 90 MINUTE),
    DATE_SUB(@current_time, INTERVAL 100 MINUTE),
    NULL,
    NULL,
    1,
    'test-user',
    DATE_SUB(@current_time, INTERVAL 100 MINUTE)
),

-- 8. 已支付
(
    'ORDER202607120008',
    1,
    66.66,
    DATE_SUB(@current_time, INTERVAL 1 DAY),
    DATE_SUB(@current_time, INTERVAL 23 HOUR),

    -- 原来的 INTERVAL 23 HOUR 30 MINUTE 语法错误
    DATE_SUB(
            DATE_SUB(@current_time, INTERVAL 23 HOUR),
            INTERVAL 30 MINUTE
    ),

    NULL,
    NULL,
    1,
    'test-user',

    DATE_SUB(
            DATE_SUB(@current_time, INTERVAL 23 HOUR),
            INTERVAL 30 MINUTE
    )
),

-- 9. 已关闭：不应该重复关闭
(
    'ORDER202607120009',
    2,
    500.00,
    DATE_SUB(@current_time, INTERVAL 5 HOUR),
    DATE_SUB(@current_time, INTERVAL 4 HOUR),
    NULL,
    DATE_SUB(@current_time, INTERVAL 4 HOUR),
    'PAY_TIMEOUT',
    1,
    'powerjob',
    DATE_SUB(@current_time, INTERVAL 4 HOUR)
),

-- 10. 已关闭
(
    'ORDER202607120010',
    2,
    18.80,
    DATE_SUB(@current_time, INTERVAL 2 DAY),
    DATE_SUB(@current_time, INTERVAL 47 HOUR),
    NULL,
    DATE_SUB(@current_time, INTERVAL 47 HOUR),
    'MANUAL_CLOSE',
    1,
    'admin',
    DATE_SUB(@current_time, INTERVAL 47 HOUR)
);