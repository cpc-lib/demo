# Payment Demo 当前行为 Spec

> 本文档描述系统当前实际行为，不判断是否合理。用于生成特征测试，确保重构前后行为一致。

## 1. 订单创建行为 (OrderInfoServiceImpl.createOrReuseOrder)

### 1.1 输入
- `productId`: 商品ID (Long, 必须 > 0)
- `paymentType`: 支付类型 ("微信" 或 "支付宝")
- `paymentAppId`: 支付应用ID (Long, 可选)
- `paymentChannelCode`: 支付渠道编码 ("WXPAY" 或 "ALIPAY", 可选)

### 1.2 行为
1. **参数校验**: productId 和 paymentType 不能为空，否则抛 BizException
2. **分布式锁**: 获取锁 `payment:create:order:{productId}:{paymentType}:{paymentAppId}`
   - 等待时间: 3000ms
   - 租期: 10000ms (不看门狗续期)
3. **查询未支付订单**: `SELECT ... FOR UPDATE` (行锁)
   - 条件: `product_id = ? AND payment_type = ? AND order_status = '未支付' AND payment_app_id = ?`
   - 如果 paymentAppId 为 NULL，条件为 `payment_app_id IS NULL`
4. **复用订单**: 如果存在未支付订单，直接返回，不创建新订单
5. **创建订单**: 
   - 查询商品信息，商品不存在抛 BizException("商品不存在")
   - 订单号: 调用 `OrderNoUtils.getOrderNo()` 生成
   - 订单金额: 商品价格 (分)
   - 订单状态: "未支付"
   - version: 0 (乐观锁初始值)
6. **唯一约束冲突兜底**: 
   - 如果 INSERT 抛 DuplicateKeyException
   - 重新 `SELECT ... FOR UPDATE` 查询未支付订单
   - 如果找到返回，否则重新抛异常
7. **发送延迟消息**: 
   - 仅在事务提交后发送 (`afterCommit`)
   - 消息内容: `OrderCloseMessage(orderNo)`
   - 延迟时间: 配置 `payment.order.close-delay-ms` (默认60000ms)

### 1.3 输出
- 成功: 返回 OrderInfo 对象
- 失败: 抛出 BizException

### 1.4 数据库影响
- INSERT INTO t_order_info (新订单)
- 或复用已有订单 (无写入)

### 1.5 外部影响
- 发送 RabbitMQ 延迟消息 (afterCommit)

---

## 2. 微信支付V3 Native下单行为 (WxPayOrderService.nativePay)

### 2.1 输入
- `productId`: 商品ID (Long)
- `paymentAppId`: 支付应用ID (Long, 可选)

### 2.2 行为
1. **分布式锁**: 获取锁 `payment:wx:native:v3:{productId}:{paymentAppId}`
   - 等待时间: 3000ms
   - 租期: -1ms (看门狗自动续期，默认30秒)
2. **创建/复用订单**: 调用 `orderInfoService.createOrReuseOrder()`
3. **订单已存在**: 如果 `codeUrl` 已有值，直接返回 `{orderNo, codeUrl}`
4. **调用微信API**: 
   - URL: `{domain}/v3/pay/transactions/native`
   - 方法: POST
   - 参数: appid, mchid, description, out_trade_no, notify_url, amount(total, currency)
   - domain 和 notify_url 从 `PaymentAppConfig` 读取
5. **保存二维码**: 调用 `orderInfoService.saveCodeUrl(orderNo, codeUrl)`
   - 仅当 `code_url` 为 NULL 或空字符串时更新
   - 如果已有值，不覆盖，记录 INFO 日志
6. **返回结果**: `{orderNo, codeUrl}`

### 2.3 输出
- 成功: `R<Map<String, Object>>` with code=0, data={orderNo, codeUrl}
- 失败: 抛出 BizException

### 2.4 外部依赖
- 微信支付API V3 (HTTPS)
- Redis (分布式锁)

---

## 3. 微信支付V3通知处理行为 (WxPayNotifyHandler + WxPayOrderService.processOrder)

### 3.1 输入
- HTTP Request: 微信支付通知报文 (JSON)
- HTTP Response: 用于返回成功/失败

### 3.2 行为
1. **解析通知**: 读取 request body，解析为 JSON Map
   - 如果 body 为空，抛 IllegalArgumentException("微信通知报文不能为空")
2. **通知幂等**: 
   - Key: `payment:wx:notify:processed:{requestId}` (requestId 来自通知的 `id` 字段)
   - 如果 SET NX 失败 (已存在)，直接返回成功响应 `{"code":"SUCCESS","message":"成功"}`
   - TTL: 无 (24小时后由其他逻辑清理？**现状**: 实际代码中未设置TTL)
3. **验签**: 
   - 尝试所有候选验签器 (defaultVerifier + 数据库配置构建的验签器)
   - 如果所有验签器都失败，释放幂等锁，返回 `{"code":"FAIL","message":"失败"}`
   - 验签器缓存: `ConcurrentHashMap<String, Verifier>` (内存缓存，无失效机制)
4. **解密通知**: 
   - 调用 `wxPayNotificationDecoder.decryptResource(bodyMap)`
   - 如果解密失败，抛 BizException("微信支付通知解密失败")
5. **通知处理锁**: 
   - Key: `payment:wx:notify:pay:{orderNo}`
   - 等待时间: 5000ms
   - 租期: -1ms (看门狗续期)
6. **查询订单**: `SELECT ... FOR UPDATE` (行锁)
   - 如果订单不存在，抛 BizException("微信支付通知对应订单不存在")
7. **金额校验**: 
   - 校验通知中的金额与订单金额是否一致
   - 不一致抛 BizException
8. **状态幂等**: 
   - 如果订单状态不是 "未支付"，记录 INFO 日志，直接返回 (不抛异常)
9. **状态更新**: 
   - `UPDATE t_order_info SET order_status='支付成功', version=version+1 WHERE order_no=? AND order_status='未支付'`
   - 如果更新失败 (已被其他事务处理)，记录 INFO 日志，直接返回
10. **创建支付流水**: 
    - `INSERT INTO t_payment_info`
    - 唯一约束: `(order_no, payment_type)`, `(transaction_id, payment_type)`
    - 如果重复，抛 DuplicateKeyException (被全局异常处理捕获)
11. **标记已处理**: 
    - `SET payment:wx:notify:processed:{requestId}` (24小时过期)
12. **返回响应**: 
    - 成功: `{"code":"SUCCESS","message":"成功"}`
    - 失败: `{"code":"FAIL","message":"失败"}`

### 3.3 输出
- HTTP Response Body: JSON 字符串

### 3.4 数据库影响
- UPDATE t_order_info (状态更新)
- INSERT t_payment_info (支付流水)

### 3.5 Redis影响
- SET payment:wx:notify:processed:{requestId} (幂等标记)

---

## 4. 微信支付V2通知处理行为 (WxPayV2Controller.wxNotify)

### 4.1 输入
- HTTP Request: 微信支付V2通知报文 (XML)

### 4.2 行为
1. **解析通知**: 
   - 调用 `WXPayUtil.xmlToMap()` 解析 XML
   - 如果 return_code != "SUCCESS"，记录 ERROR 日志，返回失败
2. **通知幂等**: 
   - Key: `payment:wx:v2:notify:processed:{transactionId}`
   - TTL: 24小时
   - 如果已存在，直接返回成功
3. **分布式锁**: 
   - Key: `payment:wx:v2:notify:{transactionId}`
   - 等待时间: 5000ms
   - 租期: 30000ms (固定30秒，不看门狗)
4. **事务处理**: 
   - 查询订单 FOR UPDATE
   - 如果订单不存在或状态不是 "未支付"，记录日志，返回成功
   - 更新订单状态为 "支付成功" (乐观锁)
   - 创建支付流水
5. **返回响应**: 
   - 成功: `<xml><return_code><![CDATA[SUCCESS]]></return_code><return_msg><![CDATA[OK]]></return_msg></xml>`
   - 失败: `<xml><return_code><![CDATA[FAIL]]></return_code><return_msg><![CDATA[失败]]></return_msg></xml>`

### 4.3 与V3的差异 (现状)
- V2 在 Controller 内直接处理，没有使用 WxPayNotifyHandler
- V2 返回 XML，V3 返回 JSON
- V2 使用固定30秒锁租期，V3 使用看门狗续期
- V2 幂等 Key 使用 transactionId，V3 使用 requestId

---

## 5. 退款申请行为 (RefundInfoServiceImpl.createRefundApplication)

### 5.1 输入
- `orderNo`: 订单号 (String, 不能为空)
- `refundAmount`: 退款金额 (Integer, 分, 可选)
- `reason`: 退款原因 (String, 可选)

### 5.2 行为
1. **参数校验**: 
   - orderNo 不能为空，否则抛 BizException("订单号不能为空")
2. **查询订单**: `SELECT ... FOR UPDATE` (行锁)
   - 如果订单不存在，抛 BizException("订单不存在")
3. **订单状态校验**: 
   - 允许退款的状态: "支付成功", "部分退款", "退款中"
   - 其他状态抛 BizException("当前订单状态不允许申请退款：{orderStatus}")
4. **计算可退金额**: 
   - `reservedRefundAmount` = 查询该订单所有 `approvalStatus='审核通过'` 的退款总额
   - `remainRefundAmount` = 订单总额 - reservedRefundAmount
   - 如果 remainRefundAmount <= 0，抛 BizException("金额已经全部退还处理")
5. **退款金额确定**: 
   - 如果 refundAmount 为 NULL，使用 remainRefundAmount (全额退款)
   - 如果 refundAmount <= 0，抛 BizException("退款金额必须大于0")
   - 如果 refundAmount > remainRefundAmount，抛 BizException("退款申请金额超过可退余额，可退金额为：{remainRefundAmount}分")
6. **创建退款申请**: 
   - refundNo: 调用 `OrderNoUtils.getRefundNo()` 生成
   - approvalStatus: "待审核"
   - refundStatus: "已创建"
   - reason: 如果为空，使用 "正常退款"
7. **唯一约束冲突**: 
   - 如果 refundNo 重复，抛 BizException("退款申请单重复提交，请勿重复操作")

### 5.3 输出
- 成功: 返回 RefundInfo 对象
- 失败: 抛出 BizException

### 5.4 数据库影响
- INSERT INTO t_refund_info

### 5.5 现状可疑行为 (必须锁住)
- **并发安全**: 计算可退金额和插入退款申请之间没有加锁，可能超退
- **事务包裹**: 使用 `@Transactional`，但行锁只在查询订单时获取

---

## 6. 退款审核通过行为 (RefundInfoServiceImpl.approveRefund)

### 6.1 输入
- `refundNo`: 退款单号 (String)
- `remark`: 审核备注 (String, 可选)

### 6.2 行为
1. **查询退款单**: 
   - 如果不存在，抛 BizException("退款单不存在")
   - 如果已审核，抛 BizException("退款单已审核，请勿重复操作")
2. **更新退款单**: 
   - approvalStatus: "审核通过"
   - approveRemark: remark
   - approvedTime: 当前时间
3. **同步订单退款状态**: 
   - 查询该订单所有退款记录
   - 计算已退款总额
   - 如果 已退款总额 < 订单总额，更新订单状态为 "部分退款"
   - 如果 已退款总额 = 订单总额，更新订单状态为 "已退款"
   - 如果 已退款总额 > 订单总额，更新订单状态为 "退款异常"
4. **发送延迟同步消息**: 
   - 在事务提交后发送 `RefundStatusSyncMessage`
   - 延迟时间: 配置 `payment.refund.status-sync-delay-ms` (默认60000ms)

### 6.3 输出
- 成功: 无返回值
- 失败: 抛出 BizException

### 6.4 数据库影响
- UPDATE t_refund_info (审核状态)
- UPDATE t_order_info (订单退款状态)

### 6.5 外部影响
- 发送 RabbitMQ 延迟消息 (afterCommit)

---

## 7. 订单状态查询行为 (OrderInfoController.queryOrderStatus)

### 7.1 输入
- `orderNo`: 订单号 (String, 不能为空, 最大50字符)

### 7.2 行为
1. **查询订单状态**: 
   - 调用 `orderInfoService.getOrderStatus(orderNo)`
   - 返回 order_status 字段值
2. **返回结果**: 
   - 如果状态为 "支付成功": `R.ok().setMessage("支付成功")` (code=0)
   - 如果状态为 "未支付": `R.ok().setCode(101).setMessage("支付中......")`
   - 其他状态: `R.ok().setCode(101).setMessage("支付中......")`

### 7.3 输出
- code: 0 (支付成功) 或 101 (支付中/其他)
- message: "支付成功" 或 "支付中......"

### 7.4 现状可疑行为 (必须锁住)
- **非成功状态统一返回 "支付中"**: 即使订单已关闭或已取消，也返回 "支付中......"
- **code=101**: 使用自定义响应码，非标准 HTTP 状态码

---

## 8. 订单关闭消费者行为 (OrderCloseConsumer)

### 8.1 输入
- RabbitMQ 消息: `OrderCloseMessage(orderNo)`

### 8.2 行为
1. **查询订单**: 
   - 调用 `orderInfoService.getOrderByOrderNo(orderNo)`
   - 如果订单不存在，记录 WARN 日志，直接返回
2. **状态检查**: 
   - 如果订单状态不是 "未支付"，记录 INFO 日志，直接返回
3. **关闭订单**: 
   - 调用微信/支付宝取消订单 API (如果已调用过支付接口)
   - 更新订单状态为 "超时已关闭"
   - **现状**: 仅更新本地状态，不主动调用外部取消接口

### 8.3 输出
- 无返回值
- 异常: 记录 ERROR 日志，不抛异常 (避免消息重试风暴)

### 8.4 数据库影响
- UPDATE t_order_info SET order_status='超时已关闭'

---

## 9. 配置加载行为 (PaymentConfigLoader)

### 9.1 输入
- 无 (启动时自动加载)

### 9.2 行为
1. **加载渠道配置**: 
   - 查询 t_payment_channel，构建 `Map<String, PaymentChannel>`
2. **加载应用配置**: 
   - 查询 t_payment_app，构建 `Map<String, List<PaymentAppConfig>>`
   - Key: channelCode (例如 "WXPAY", "ALIPAY")
3. **内存缓存**: 
   - 配置加载到内存后不再更新
   - **现状**: 无热更新机制，修改数据库需重启服务

### 9.3 输出
- 内存 Map 结构

### 9.4 现状可疑行为 (必须锁住)
- **启动后不可变**: 配置修改后不会自动刷新
- **无配置校验**: 启动时不校验配置完整性，运行时才发现缺失

---

## 10. 分布式锁行为 (RedissonDistributedLockTemplate.execute)

### 10.1 输入
- `lockKey`: 锁 key (String, 不能为空)
- `waitTimeMillis`: 等待时间 (毫秒)
- `leaseTimeMillis`: 租期 (毫秒, -1表示看门狗续期)
- `supplier`: 业务逻辑

### 10.2 行为
1. **参数校验**: 
   - lockKey 不能为空，否则抛 BizException("分布式锁key不能为空")
   - supplier 不能为空，否则抛 BizException("分布式锁业务逻辑不能为空")
2. **获取锁**: 
   - 如果 leaseTime > 0: `tryLock(waitTime, leaseTime, MILLISECONDS)`
   - 如果 leaseTime <= 0: `tryLock(waitTime, -1, MILLISECONDS)` (看门狗续期)
3. **获取失败**: 
   - 等待超时后返回 false
   - 记录 WARN 日志
   - 抛 BizException("系统繁忙，请勿重复提交")
4. **业务执行**: 
   - 执行 supplier.get()
   - 如果抛 InterruptedException，记录 ERROR 日志，抛 BizException("获取分布式锁被中断")
5. **释放锁**: 
   - finally 块中检查 `lock.isHeldByCurrentThread()`
   - 如果当前线程持有锁，调用 `lock.unlock()`

### 10.3 输出
- 成功: 返回 supplier 的执行结果
- 失败: 抛出 BizException

### 10.4 Redis影响
- 获取锁: SET NX (Redisson内部实现)
- 释放锁: DEL (Redisson内部实现)
- 看门狗续期: 每 10 秒续期一次 (默认，不可配置)

### 10.5 现状可疑行为 (必须锁住)
- **中断处理**: InterruptedException 被捕获后抛 BizException，不保留中断标志
- **锁释放条件**: 仅当前线程持有锁时才释放，不处理锁过期后被其他线程获取的情况
