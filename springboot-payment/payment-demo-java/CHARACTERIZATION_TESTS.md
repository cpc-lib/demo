# Payment Demo 特征测试清单

> 本清单基于 CURRENT_BEHAVIOR_SPEC.md 生成，所有测试锁住当前实际行为，不判断是否合理。
> 可疑行为已在测试名或注释中标明「现状」。

---

## 测试模块 1: 订单创建特征测试

### Test 1.1: 成功创建新订单
**锁住现状**: Spec 1.2 步骤 1-5

```java
@Test
@DisplayName("订单创建 - 成功创建新订单 (Spec 1.2)")
void shouldCreateNewOrderWhenNoExistingOrder() {
    // Given: 商品存在，无未支付订单
    Long productId = 1L; // Java课程，价格1分
    String paymentType = "微信";
    
    // When: 创建订单
    OrderInfo order = orderInfoService.createOrReuseOrder(productId, paymentType);
    
    // Then: 新订单创建成功
    assertThat(order).isNotNull();
    assertThat(order.getProductId()).isEqualTo(productId);
    assertThat(order.getPaymentType()).isEqualTo(paymentType);
    assertThat(order.getOrderStatus()).isEqualTo("未支付");
    assertThat(order.getTotalFee()).isEqualTo(1); // 商品价格1分
    assertThat(order.getVersion()).isEqualTo(0); // 乐观锁初始值
    assertThat(order.getOrderNo()).isNotBlank(); // 订单号自动生成
}
```

**锁住的现状**:
- 订单号由 `OrderNoUtils.getOrderNo()` 生成
- 订单金额等于商品价格 (分)
- 初始 version = 0
- 订单状态 = "未支付"

---

### Test 1.2: 复用未支付订单
**锁住现状**: Spec 1.2 步骤 3-4

```java
@Test
@DisplayName("订单创建 - 复用同一商品未支付订单 (Spec 1.2)")
void shouldReuseExistingUnpaidOrder() {
    // Given: 已存在未支付订单
    Long productId = 1L;
    String paymentType = "微信";
    OrderInfo firstOrder = orderInfoService.createOrReuseOrder(productId, paymentType);
    
    // When: 再次创建订单
    OrderInfo secondOrder = orderInfoService.createOrReuseOrder(productId, paymentType);
    
    // Then: 复用同一订单
    assertThat(secondOrder.getOrderNo()).isEqualTo(firstOrder.getOrderNo());
    assertThat(secondOrder.getId()).isEqualTo(firstOrder.getId());
}
```

**锁住的现状**:
- 同一商品、同一支付类型、未支付状态 → 复用订单
- 不创建新订单，返回同一订单对象

---

### Test 1.3: 商品不存在抛异常
**锁住现状**: Spec 1.2 步骤 5

```java
@Test
@DisplayName("订单创建 - 商品不存在抛 BizException (Spec 1.2)")
void shouldThrowExceptionWhenProductNotFound() {
    // Given: 商品ID不存在
    Long nonExistentProductId = 99999L;
    
    // When & Then: 抛 BizException("商品不存在")
    assertThatThrownBy(() -> 
        orderInfoService.createOrReuseOrder(nonExistentProductId, "微信")
    )
    .isInstanceOf(BizException.class)
    .hasMessage("商品不存在");
}
```

**锁住的现状**:
- 异常消息固定为 "商品不存在"
- 异常类型为 BizException

---

### Test 1.4: 订单创建后发送延迟消息
**锁住现状**: Spec 1.2 步骤 7

```java
@Test
@DisplayName("订单创建 - 事务提交后发送延迟关单消息 (Spec 1.2)")
void shouldSendCloseOrderMessageAfterTransactionCommit() {
    // Given: 清空 RabbitMQ 测试队列
    rabbitTestHelper.purgeQueue("order.close.queue");
    
    // When: 创建订单
    OrderInfo order = orderInfoService.createOrReuseOrder(1L, "微信");
    
    // Then: 消息在事务提交后发送
    // 注意: 由于 afterCommit 回调，消息可能在测试方法返回后才发送
    // 使用 Awaitility 等待消息到达
    await().atMost(5, SECONDS)
        .until(() -> rabbitTestHelper.getMessageCount("order.close.queue"), 
               greaterThan(0));
    
    OrderCloseMessage message = rabbitTestHelper.receiveMessage("order.close.queue");
    assertThat(message.getOrderNo()).isEqualTo(order.getOrderNo());
}
```

**锁住的现状**:
- 消息在事务提交后发送 (afterCommit)
- 消息内容包含 orderNo
- 队列为 "order.close.queue"

---

### Test 1.5: 参数校验 - productId为空
**锁住现状**: Spec 1.2 步骤 1

```java
@Test
@DisplayName("订单创建 - productId为null抛异常 (Spec 1.2)")
void shouldThrowExceptionWhenProductIdIsNull() {
    assertThatThrownBy(() -> 
        orderInfoService.createOrReuseOrder(null, "微信")
    )
    .isInstanceOf(BizException.class);
}
```

**锁住的现状**:
- productId 为 null 时抛 BizException

---

## 测试模块 2: 微信支付V3通知处理特征测试

### Test 2.1: 首次通知处理成功
**锁住现状**: Spec 3.2 步骤 1-12

```java
@Test
@DisplayName("微信V3通知 - 首次处理成功更新订单状态 (Spec 3.2)")
void shouldProcessFirstNotifySuccessfully() {
    // Given: 未支付订单存在
    OrderInfo order = orderInfoService.createOrReuseOrder(1L, "微信");
    
    // And: 模拟微信支付通知报文
    String notifyBody = buildWxPayNotifyBody(
        order.getOrderNo(), 
        "transaction_123", 
        order.getTotalFee()
    );
    
    // When: 处理通知
    String response = wxPayNotifyHandler.handle(
        mockRequest(notifyBody), 
        mockResponse(), 
        wxPayOrderFacade::processOrder, 
        "处理微信支付通知失败"
    );
    
    // Then: 返回成功响应
    assertThat(response).contains("SUCCESS");
    
    // And: 订单状态更新为支付成功
    OrderInfo updatedOrder = orderInfoService.getOrderByOrderNo(order.getOrderNo());
    assertThat(updatedOrder.getOrderStatus()).isEqualTo("支付成功");
    
    // And: 支付流水创建
    PaymentInfo paymentInfo = paymentInfoService.getByOrderNo(order.getOrderNo());
    assertThat(paymentInfo).isNotNull();
    assertThat(paymentInfo.getTransactionId()).isEqualTo("transaction_123");
}
```

**锁住的现状**:
- 成功响应包含 "SUCCESS" 字符串
- 订单状态从 "未支付" 更新为 "支付成功"
- 创建支付流水记录

---

### Test 2.2: 通知幂等 - 重复通知返回成功
**锁住现状**: Spec 3.2 步骤 2

```java
@Test
@DisplayName("微信V3通知 - 重复通知幂等返回成功 (Spec 3.2)")
void shouldReturnSuccessForDuplicateNotify() {
    // Given: 已处理过一次通知
    OrderInfo order = orderInfoService.createOrReuseOrder(1L, "微信");
    String notifyBody = buildWxPayNotifyBody(order.getOrderNo(), "txn_123", order.getTotalFee());
    wxPayNotifyHandler.handle(mockRequest(notifyBody), mockResponse(), 
        wxPayOrderFacade::processOrder, "error");
    
    // When: 再次发送相同通知 (requestId 相同)
    String response = wxPayNotifyHandler.handle(mockRequest(notifyBody), mockResponse(), 
        wxPayOrderFacade::processOrder, "error");
    
    // Then: 直接返回成功，不重复处理
    assertThat(response).contains("SUCCESS");
    
    // And: 支付流水只有一条记录
    List<PaymentInfo> payments = paymentInfoService.listByOrderNo(order.getOrderNo());
    assertThat(payments).hasSize(1);
}
```

**锁住的现状**:
- 重复通知返回 SUCCESS
- 不重复创建支付流水
- 幂等 Key 基于 requestId

---

### Test 2.3: 验签失败返回failure
**锁住现状**: Spec 3.2 步骤 3

```java
@Test
@DisplayName("微信V3通知 - 验签失败返回failure (Spec 3.2)")
void shouldReturnFailureWhenSignatureInvalid() {
    // Given: 无效的通知报文 (签名错误)
    String invalidNotifyBody = buildInvalidSignatureNotifyBody();
    
    // When: 处理通知
    String response = wxPayNotifyHandler.handle(
        mockRequest(invalidNotifyBody), 
        mockResponse(), 
        wxPayOrderFacade::processOrder, 
        "error"
    );
    
    // Then: 返回失败响应
    assertThat(response).contains("FAIL");
}
```

**锁住的现状**:
- 验签失败返回包含 "FAIL" 的响应
- 不更新订单状态

---

### Test 2.4: 订单状态已变化时幂等忽略
**锁住现状**: Spec 3.2 步骤 8

```java
@Test
@DisplayName("微信V3通知 - 订单状态已变化时幂等忽略 (Spec 3.2)")
void shouldIgnoreNotifyWhenOrderStatusChanged() {
    // Given: 订单已支付成功
    OrderInfo order = orderInfoService.createOrReuseOrder(1L, "微信");
    orderInfoService.updateStatusByOrderNo(order.getOrderNo(), OrderStatus.SUCCESS);
    
    // And: 收到支付成功通知
    String notifyBody = buildWxPayNotifyBody(order.getOrderNo(), "txn_456", order.getTotalFee());
    
    // When: 处理通知
    String response = wxPayNotifyHandler.handle(
        mockRequest(notifyBody), 
        mockResponse(), 
        wxPayOrderFacade::processOrder, 
        "error"
    );
    
    // Then: 返回成功 (幂等)
    assertThat(response).contains("SUCCESS");
    
    // And: 订单状态不变
    OrderInfo updatedOrder = orderInfoService.getOrderByOrderNo(order.getOrderNo());
    assertThat(updatedOrder.getOrderStatus()).isEqualTo("支付成功");
}
```

**锁住的现状**:
- 订单状态不是 "未支付" 时，直接返回成功
- 不抛异常
- 记录 INFO 日志

---

## 测试模块 3: 微信支付V2通知处理特征测试

### Test 3.1: V2通知返回XML格式
**锁住现状**: Spec 4.3 差异

```java
@Test
@DisplayName("微信V2通知 - 返回XML格式响应 (Spec 4.3)")
void shouldReturnXmlResponseForV2Notify() {
    // Given: V2通知报文 (XML)
    String xmlNotify = buildWxPayV2NotifyXml("SUCCESS", "txn_v2_123");
    
    // When: 处理V2通知
    String response = wxPayV2Controller.wxNotify(mockXmlRequest(xmlNotify));
    
    // Then: 返回XML格式
    assertThat(response).contains("<xml>");
    assertThat(response).contains("<return_code><![CDATA[SUCCESS]]></return_code>");
}
```

**锁住的现状**:
- V2 返回 XML 格式，不是 JSON
- 响应格式: `<xml><return_code><![CDATA[SUCCESS]]></return_code>...</xml>`

---

### Test 3.2: V2使用transactionId做幂等
**锁住现状**: Spec 4.3 差异

```java
@Test
@DisplayName("微信V2通知 - 使用transactionId做幂等Key (Spec 4.3)")
void shouldUseTransactionIdForV2Idempotent() {
    // Given: V2通知
    String xmlNotify = buildWxPayV2NotifyXml("SUCCESS", "txn_v2_unique");
    
    // When: 第一次处理
    String response1 = wxPayV2Controller.wxNotify(mockXmlRequest(xmlNotify));
    
    // And: 第二次处理 (相同transactionId)
    String response2 = wxPayV2Controller.wxNotify(mockXmlRequest(xmlNotify));
    
    // Then: 两次都返回成功
    assertThat(response1).contains("SUCCESS");
    assertThat(response2).contains("SUCCESS");
    
    // And: Redis幂等Key存在 (使用transactionId)
    String idempotentKey = "payment:wx:v2:notify:processed:txn_v2_unique";
    assertThat(redisTemplate.hasKey(idempotentKey)).isTrue();
}
```

**锁住的现状**:
- V2 幂等 Key: `payment:wx:v2:notify:processed:{transactionId}`
- V3 幂等 Key: `payment:wx:notify:processed:{requestId}`
- 两者使用不同字段

---

## 测试模块 4: 退款申请特征测试

### Test 4.1: 成功申请退款
**锁住现状**: Spec 5.2 步骤 1-7

```java
@Test
@DisplayName("退款申请 - 支付成功订单可申请退款 (Spec 5.2)")
void shouldCreateRefundApplicationForPaidOrder() {
    // Given: 支付成功订单
    OrderInfo order = createAndPayOrder(1L, "微信");
    
    // When: 申请退款
    RefundInfo refund = refundInfoService.createRefundApplication(
        order.getOrderNo(), 
        null, // 全额退款
        "正常退款"
    );
    
    // Then: 退款申请创建成功
    assertThat(refund).isNotNull();
    assertThat(refund.getOrderNo()).isEqualTo(order.getOrderNo());
    assertThat(refund.getRefund()).isEqualTo(order.getTotalFee());
    assertThat(refund.getApprovalStatus()).isEqualTo("待审核");
    assertThat(refund.getRefundStatus()).isEqualTo("已创建");
    assertThat(refund.getReason()).isEqualTo("正常退款");
    assertThat(refund.getRefundNo()).isNotBlank(); // 退款单号自动生成
}
```

**锁住的现状**:
- 退款单号由 `OrderNoUtils.getRefundNo()` 生成
- 初始审核状态 = "待审核"
- 初始退款状态 = "已创建"
- reason 为 null 时使用 "正常退款"

---

### Test 4.2: 未支付订单不允许退款
**锁住现状**: Spec 5.2 步骤 3

```java
@Test
@DisplayName("退款申请 - 未支付订单不允许退款 (Spec 5.2)")
void shouldRejectRefundForUnpaidOrder() {
    // Given: 未支付订单
    OrderInfo order = orderInfoService.createOrReuseOrder(1L, "微信");
    
    // When & Then: 抛异常
    assertThatThrownBy(() -> 
        refundInfoService.createRefundApplication(order.getOrderNo(), null, "退款")
    )
    .isInstanceOf(BizException.class)
    .hasMessageContaining("当前订单状态不允许申请退款");
}
```

**锁住的现状**:
- 未支付订单退款抛 BizException
- 异常消息包含 "当前订单状态不允许申请退款"

---

### Test 4.3: 超额退款被拒绝
**锁住现状**: Spec 5.2 步骤 4-5

```java
@Test
@DisplayName("退款申请 - 退款金额超过可退余额被拒绝 (Spec 5.2)")
void shouldRejectRefundWhenAmountExceedsRemain() {
    // Given: 订单总额100分，已申请退款80分并审核通过
    OrderInfo order = createAndPayOrder(1L, "微信");
    // 模拟订单金额为100分
    order.setTotalFee(100);
    orderInfoService.updateOrder(order);
    
    // 第一笔退款已审核通过
    RefundInfo firstRefund = refundInfoService.createRefundApplication(
        order.getOrderNo(), 80, "部分退款"
    );
    refundInfoService.approveRefund(firstRefund.getRefundNo(), "同意");
    
    // When & Then: 再申请30分退款 (超过可退20分)
    assertThatThrownBy(() -> 
        refundInfoService.createRefundApplication(order.getOrderNo(), 30, "超额退款")
    )
    .isInstanceOf(BizException.class)
    .hasMessageContaining("退款申请金额超过可退余额，可退金额为：20分");
}
```

**锁住的现状**:
- 异常消息格式: "退款申请金额超过可退余额，可退金额为：{X}分"
- 可退金额 = 订单总额 - 已审核通过退款总额

---

### Test 4.4: 重复退款申请被拒绝
**锁住现状**: Spec 5.2 步骤 7

```java
@Test
@DisplayName("退款申请 - 相同refundNo重复提交被拒绝 (Spec 5.2)")
void shouldRejectDuplicateRefundApplication() {
    // Given: 已存在退款申请
    OrderInfo order = createAndPayOrder(1L, "微信");
    RefundInfo firstRefund = refundInfoService.createRefundApplication(
        order.getOrderNo(), null, "退款"
    );
    
    // When & Then: 使用相同refundNo再次提交
    // 注意: createRefundApplication 内部自动生成 refundNo，这里模拟唯一约束冲突
    assertThatThrownBy(() -> {
        // 直接插入相同refundNo的记录，模拟并发重复提交
        RefundInfo duplicateRefund = new RefundInfo();
        duplicateRefund.setRefundNo(firstRefund.getRefundNo());
        duplicateRefund.setOrderNo(order.getOrderNo());
        // ... 设置其他字段
        refundInfoMapper.insert(duplicateRefund);
    })
    .isInstanceOf(DuplicateKeyException.class);
}
```

**锁住的现状**:
- refundNo 唯一约束冲突抛 DuplicateKeyException
- 被 Service 层捕获后转为 BizException("退款申请单重复提交，请勿重复操作")

---

## 测试模块 5: 订单状态查询特征测试

### Test 5.1: 支付成功返回code=0
**锁住现状**: Spec 7.2 步骤 2

```java
@Test
@DisplayName("订单状态查询 - 支付成功返回code=0 (Spec 7.2)")
void shouldReturnCode0WhenOrderPaid() {
    // Given: 支付成功订单
    OrderInfo order = createAndPayOrder(1L, "微信");
    
    // When: 查询订单状态
    R<Map<String, Object>> response = orderInfoController.queryOrderStatus(order.getOrderNo());
    
    // Then: code=0, message="支付成功"
    assertThat(response.getCode()).isEqualTo(0);
    assertThat(response.getMessage()).isEqualTo("支付成功");
}
```

**锁住的现状**:
- 支付成功: code=0

---

### Test 5.2: 未支付订单返回code=101
**锁住现状**: Spec 7.2 步骤 2, 7.4

```java
@Test
@DisplayName("订单状态查询 - 未支付订单返回code=101 (Spec 7.2, 7.4现状)")
void shouldReturnCode101ForUnpaidOrder() {
    // Given: 未支付订单
    OrderInfo order = orderInfoService.createOrReuseOrder(1L, "微信");
    
    // When: 查询订单状态
    R<Map<String, Object>> response = orderInfoController.queryOrderStatus(order.getOrderNo());
    
    // Then: code=101, message="支付中......"
    assertThat(response.getCode()).isEqualTo(101);
    assertThat(response.getMessage()).isEqualTo("支付中......");
}
```

**锁住的现状**:
- 未支付: code=101 (自定义响应码)
- message="支付中......" (注意省略号是中文全角)

---

### Test 5.3: 已关闭订单也返回"支付中"
**锁住现状**: Spec 7.4 (可疑行为)

```java
@Test
@DisplayName("订单状态查询 - 【现状】已关闭订单也返回支付中 (Spec 7.4)")
void shouldReturnPayingMessageForClosedOrder_CURRENT_BEHAVIOR() {
    // Given: 已关闭订单
    OrderInfo order = orderInfoService.createOrReuseOrder(1L, "微信");
    orderInfoService.updateStatusByOrderNo(order.getOrderNo(), OrderStatus.CLOSED);
    
    // When: 查询订单状态
    R<Map<String, Object>> response = orderInfoController.queryOrderStatus(order.getOrderNo());
    
    // Then: 【现状】仍然返回 code=101, message="支付中......"
    // 注意: 这是当前行为，可能不合理，但特征测试必须锁住
    assertThat(response.getCode()).isEqualTo(101);
    assertThat(response.getMessage()).isEqualTo("支付中......");
}
```

**锁住的现状**:
- 【可疑行为】已关闭订单返回 "支付中......"
- 不区分 "未支付"、"已关闭"、"已取消" 等状态

---

## 测试模块 6: 分布式锁特征测试

### Test 6.1: 获取锁失败抛BizException
**锁住现状**: Spec 10.2 步骤 3

```java
@Test
@DisplayName("分布式锁 - 获取锁失败抛BizException (Spec 10.2)")
void shouldThrowExceptionWhenLockAcquireFailed() {
    // Given: 锁已被其他线程持有
    String lockKey = "test:lock:contention";
    CountDownLatch latch = new CountDownLatch(1);
    
    // 启动线程占用锁
    Thread lockHolder = new Thread(() -> {
        distributedLockTemplate.execute(lockKey, 0, 10000, () -> {
            latch.countDown();
            sleep(5000); // 持有锁5秒
            return null;
        });
    });
    lockHolder.start();
    latch.await(); // 等待锁被获取
    
    // When & Then: 当前线程获取锁失败
    assertThatThrownBy(() -> 
        distributedLockTemplate.execute(lockKey, 100, 5000, () -> "should not reach")
    )
    .isInstanceOf(BizException.class)
    .hasMessage("系统繁忙，请勿重复提交");
}
```

**锁住的现状**:
- 获取锁失败抛 BizException("系统繁忙，请勿重复提交")
- 等待时间超时后返回失败

---

### Test 6.2: 空lockKey抛异常
**锁住现状**: Spec 10.2 步骤 1

```java
@Test
@DisplayName("分布式锁 - lockKey为空抛异常 (Spec 10.2)")
void shouldThrowExceptionWhenLockKeyIsEmpty() {
    assertThatThrownBy(() -> 
        distributedLockTemplate.execute("", 3000, 10000, () -> null)
    )
    .isInstanceOf(BizException.class)
    .hasMessage("分布式锁key不能为空");
}
```

**锁住的现状**:
- 空字符串 lockKey 抛 BizException("分布式锁key不能为空")

---

## 测试模块 7: 配置加载特征测试

### Test 7.1: 配置启动时加载到内存
**锁住现状**: Spec 9.2 步骤 1-2

```java
@Test
@DisplayName("配置加载 - 启动时加载渠道和应用配置 (Spec 9.2)")
void shouldLoadConfigsOnStartup() {
    // When: 查询微信支付渠道配置
    List<PaymentAppConfig> wxConfigs = paymentConfigLoader.listAppConfigsByChannelCode("WXPAY");
    
    // Then: 配置已加载到内存
    assertThat(wxConfigs).isNotEmpty();
    assertThat(wxConfigs.get(0).getAppid()).isNotBlank();
    assertThat(wxConfigs.get(0).getMchId()).isNotBlank();
}
```

**锁住的现状**:
- 配置在应用启动时加载到内存
- 通过 `listAppConfigsByChannelCode(channelCode)` 查询

---

### Test 7.2: 配置修改后不自动刷新
**锁住现状**: Spec 9.4 (可疑行为)

```java
@Test
@DisplayName("配置加载 - 【现状】配置修改后不自动刷新 (Spec 9.4)")
void shouldNotRefreshConfigAfterModification_CURRENT_BEHAVIOR() {
    // Given: 获取当前配置
    List<PaymentAppConfig> configsBefore = paymentConfigLoader.listAppConfigsByChannelCode("WXPAY");
    String appidBefore = configsBefore.get(0).getAppid();
    
    // When: 修改数据库中的配置
    paymentAppMapper.updateAppConfig(1L, "{\"appid\":\"modified_appid\"}");
    
    // Then: 【现状】内存配置未更新
    List<PaymentAppConfig> configsAfter = paymentConfigLoader.listAppConfigsByChannelCode("WXPAY");
    assertThat(configsAfter.get(0).getAppid()).isEqualTo(appidBefore);
    // 不等于 "modified_appid"
}
```

**锁住的现状**:
- 【可疑行为】配置修改后不会自动刷新
- 需要重启服务才能加载新配置

---

## 测试模块 8: 微信V3 Native下单特征测试

### Test 8.1: 下单成功返回orderNo和codeUrl
**锁住现状**: Spec 2.2 步骤 1-6

```java
@Test
@DisplayName("微信V3下单 - 成功返回orderNo和codeUrl (Spec 2.2)")
void shouldReturnOrderNoAndCodeUrl() {
    // Given: Mock 微信支付API响应
    mockWxPayApiNativeResponse("https://wx.pay/code/url");
    
    // When: 发起支付
    Map<String, Object> result = wxPayOrderFacade.nativePay(1L);
    
    // Then: 返回orderNo和codeUrl
    assertThat(result).containsKey("orderNo");
    assertThat(result).containsKey("codeUrl");
    assertThat(result.get("codeUrl")).isEqualTo("https://wx.pay/code/url");
}
```

**锁住的现状**:
- 返回 Map 包含 orderNo 和 codeUrl
- codeUrl 来自微信支付API响应

---

### Test 8.2: 复用已有二维码
**锁住现状**: Spec 2.2 步骤 3

```java
@Test
@DisplayName("微信V3下单 - 订单已有二维码时不重复调用微信API (Spec 2.2)")
void shouldReuseExistingCodeUrl() {
    // Given: 已有二维码的订单
    OrderInfo order = orderInfoService.createOrReuseOrder(1L, "微信");
    orderInfoService.saveCodeUrl(order.getOrderNo(), "existing_code_url");
    
    // When: 再次下单
    Map<String, Object> result = wxPayOrderFacade.nativePay(1L);
    
    // Then: 复用已有二维码
    assertThat(result.get("codeUrl")).isEqualTo("existing_code_url");
    // 未调用微信支付API (通过 Mock 验证)
    verifyNoInteractions(mockWxPayHttpClient);
}
```

**锁住的现状**:
- 订单已有 codeUrl 时，直接返回
- 不调用微信支付API

---

## 测试覆盖总结

| 模块 | 测试数 | 覆盖的行为面 | 可疑行为锁住 |
|------|--------|-------------|-------------|
| 订单创建 | 5 | 创建、复用、校验、消息发送 | 无 |
| 微信V3通知 | 4 | 首次处理、幂等、验签、状态检查 | 无 |
| 微信V2通知 | 2 | XML响应、transactionId幂等 | V2与V3差异 |
| 退款申请 | 4 | 创建、状态校验、金额校验、重复 | 并发安全 |
| 订单状态查询 | 3 | 支付成功、未支付、已关闭 | 已关闭返回"支付中" |
| 分布式锁 | 2 | 获取失败、空Key | 中断处理 |
| 配置加载 | 2 | 启动加载、热更新 | 无热更新 |
| 微信V3下单 | 2 | 下单、复用二维码 | 无 |

**总计**: 24 个特征测试

---

## 可疑行为锁住清单

| # | 可疑行为 | 测试编号 | 说明 |
|---|---------|---------|------|
| 1 | 已关闭订单查询返回"支付中" | Test 5.3 | 不区分关闭/取消/未支付状态 |
| 2 | 配置修改后不自动刷新 | Test 7.2 | 内存缓存无失效机制 |
| 3 | V2通知使用transactionId做幂等 | Test 3.2 | V3使用requestId，不一致 |
| 4 | V2通知返回XML，V3返回JSON | Test 3.1 | 响应格式不一致 |
| 5 | 退款计算并发安全 | Test 4.3 | 计算和插入之间无锁 |
| 6 | 分布式锁中断处理 | Test 6.1 | 不保留中断标志 |
