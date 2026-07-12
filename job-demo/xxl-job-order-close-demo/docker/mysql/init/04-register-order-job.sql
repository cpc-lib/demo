USE xxl_job;

INSERT INTO xxl_job_group (
    app_name, title, address_type, address_list, update_time
)
SELECT
    'xxl-order-executor', '订单关单执行器', 0, NULL, NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM xxl_job_group WHERE app_name = 'xxl-order-executor'
);

SET @order_executor_group_id = (
    SELECT id
    FROM xxl_job_group
    WHERE app_name = 'xxl-order-executor'
    ORDER BY id
    LIMIT 1
);

INSERT INTO xxl_job_info (
    job_group,
    job_desc,
    add_time,
    update_time,
    author,
    alarm_email,
    schedule_type,
    schedule_conf,
    misfire_strategy,
    executor_route_strategy,
    executor_handler,
    executor_param,
    executor_block_strategy,
    executor_timeout,
    executor_fail_retry_count,
    glue_type,
    glue_source,
    glue_remark,
    glue_updatetime,
    child_jobid,
    trigger_status,
    trigger_last_time,
    trigger_next_time
)
SELECT
    @order_executor_group_id,
    '关闭超时未支付订单',
    NOW(),
    NOW(),
    'system',
    '',
    'CRON',
    '0 0/1 * * * ? *',
    'DO_NOTHING',
    'FIRST',
    'closeTimeoutOrderJobHandler',
    'batchSize=200,maxRounds=20',
    'SERIAL_EXECUTION',
    60,
    1,
    'BEAN',
    '',
    'GLUE代码初始化',
    NOW(),
    '',
    0,
    0,
    0
WHERE NOT EXISTS (
    SELECT 1
    FROM xxl_job_info
    WHERE job_group = @order_executor_group_id
      AND executor_handler = 'closeTimeoutOrderJobHandler'
);
