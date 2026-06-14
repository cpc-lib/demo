# 特征测试 - 现状锁住映射表

> 本文档说明 CHARACTERIZATION_TESTS.md 中每条测试锁住了 CURRENT_BEHAVIOR_SPEC.md 中的哪条现状。

---

## 订单创建模块

| 测试 | 锁住的 Spec 章节 | 锁住的具体行为 | 可疑行为 |
|------|-----------------|---------------|---------|
| Test 1.1 | Spec 1.2 步骤 1-5 | 订单创建成功时的字段值: orderNo自动生成, version=0, status=未支付, totalFee=商品价格 | 无 |
| Test 1.2 | Spec 1.2 步骤 3-4 | 同一商品未支付订单被复用,不创建新订单 | 无 |
| Test 1.3 | Spec 1.2 步骤 5 | 商品不存在时抛 BizException("商品不存在") | 无 |
| Test 1.4 | Spec 1.2 步骤 7 | 订单创建后在 afterCommit 回调中发送延迟消息 | 无 |
| Test 1.5 | Spec 1.2 步骤 1 | productId为null时抛 BizException | 无 |

---

## 微信支付V3通知模块

| 测试 | 锁住的 Spec 章节 | 锁住的具体行为 | 可疑行为 |
|------|-----------------|---------------|---------|
| Test 2.1 | Spec 3.2 步骤 1-12 | 首次通知处理成功: 更新订单状态为"支付成功",创建支付流水,返回SUCCESS | 无 |
| Test 2.2 | Spec 3.2 步骤 2 | 重复通知幂等: 基于requestId的Redis SET NX,返回SUCCESS不重复处理 | 无 |
| Test 2.3 | Spec 3.2 步骤 3 | 验签失败返回FAIL,不更新订单状态 | 无 |
| Test 2.4 | Spec 3.2 步骤 8 | 订单状态不是"未支付"时直接返回SUCCESS,不抛异常 | 无 |

---

## 微信支付V2通知模块

| 测试 | 锁住的 Spec 章节 | 锁住的具体行为 | 可疑行为 |
|------|-----------------|---------------|---------|
| Test 3.1 | Spec 4.3 | V2通知返回XML格式: `<xml><return_code><![CDATA[SUCCESS]]>...</xml>` | ⚠️ V2返回XML,V3返回JSON,格式不一致 |
| Test 3.2 | Spec 4.3 | V2使用transactionId做幂等Key: `payment:wx:v2:notify:processed:{transactionId}` | ⚠️ V3使用requestId,两者字段不同 |

---

## 退款申请模块

| 测试 | 锁住的 Spec 章节 | 锁住的具体行为 | 可疑行为 |
|------|-----------------|---------------|---------|
| Test 4.1 | Spec 5.2 步骤 1-7 | 退款申请创建成功: approvalStatus=待审核, refundStatus=已创建, reason为空时使用"正常退款" | 无 |
| Test 4.2 | Spec 5.2 步骤 3 | 未支付订单不允许退款,抛BizException("当前订单状态不允许申请退款") | 无 |
| Test 4.3 | Spec 5.2 步骤 4-5 | 超额退款被拒绝,异常消息:"退款申请金额超过可退余额,可退金额为:X分" | ⚠️ 计算和插入之间无锁,可能超退 |
| Test 4.4 | Spec 5.2 步骤 7 | refundNo唯一约束冲突抛DuplicateKeyException | 无 |

---

## 订单状态查询模块

| 测试 | 锁住的 Spec 章节 | 锁住的具体行为 | 可疑行为 |
|------|-----------------|---------------|---------|
| Test 5.1 | Spec 7.2 步骤 2 | 支付成功返回code=0, message="支付成功" | 无 |
| Test 5.2 | Spec 7.2 步骤 2 | 未支付返回code=101, message="支付中......" (中文全角省略号) | 无 |
| Test 5.3 | Spec 7.4 | 已关闭订单也返回code=101, message="支付中......" | ⚠️ 不区分关闭/取消/未支付状态 |

---

## 分布式锁模块

| 测试 | 锁住的 Spec 章节 | 锁住的具体行为 | 可疑行为 |
|------|-----------------|---------------|---------|
| Test 6.1 | Spec 10.2 步骤 3 | 获取锁失败抛BizException("系统繁忙,请勿重复提交") | 无 |
| Test 6.2 | Spec 10.2 步骤 1 | lockKey为空抛BizException("分布式锁key不能为空") | ⚠️ InterruptedException不保留中断标志 |

---

## 配置加载模块

| 测试 | 锁住的 Spec 章节 | 锁住的具体行为 | 可疑行为 |
|------|-----------------|---------------|---------|
| Test 7.1 | Spec 9.2 步骤 1-2 | 启动时加载渠道和应用配置到内存Map | 无 |
| Test 7.2 | Spec 9.4 | 数据库配置修改后内存配置不自动刷新 | ⚠️ 无热更新机制,需重启服务 |

---

## 微信V3下单模块

| 测试 | 锁住的 Spec 章节 | 锁住的具体行为 | 可疑行为 |
|------|-----------------|---------------|---------|
| Test 8.1 | Spec 2.2 步骤 1-6 | 下单成功返回Map包含orderNo和codeUrl | 无 |
| Test 8.2 | Spec 2.2 步骤 3 | 订单已有codeUrl时不重复调微信API,直接返回 | 无 |

---

## 可疑行为汇总

以下行为在架构梳理中被标记为"可疑"，但在特征测试中已**原样锁住**，重构前必须保持不变：

| # | 可疑行为 | 测试 | 风险等级 | 重构建议 |
|---|---------|------|---------|---------|
| 1 | 已关闭订单查询返回"支付中" | Test 5.3 | **高** | 前端可能依赖此行为判断轮询状态 |
| 2 | 配置修改后不自动刷新 | Test 7.2 | **中** | 多商户场景下修改配置需重启 |
| 3 | V2/V3响应格式不一致 | Test 3.1 | **低** | 微信官方要求,不可改 |
| 4 | V2/V3幂等Key字段不同 | Test 3.2 | **低** | 微信官方报文差异,不可改 |
| 5 | 退款计算并发安全 | Test 4.3 | **高** | 计算和插入之间无锁,可能超退 |
| 6 | 分布式锁中断处理 | Test 6.2 | **中** | 不保留中断标志,可能影响线程池 |

---

## 测试隔离策略

### 数据库隔离
```java
@BeforeEach
void resetDatabase() {
    // 清理测试数据
    jdbcTemplate.execute("DELETE FROM t_order_info WHERE order_no LIKE 'TEST_%'");
    jdbcTemplate.execute("DELETE FROM t_payment_info WHERE order_no LIKE 'TEST_%'");
    jdbcTemplate.execute("DELETE FROM t_refund_info WHERE refund_no LIKE 'TEST_%'");
}
```

### Redis隔离
```java
@BeforeEach
void resetRedis() {
    // 清理测试用的Redis Key
    Set<String> keys = redisTemplate.keys("payment:test:*");
    if (keys != null) {
        redisTemplate.delete(keys);
    }
}
```

### RabbitMQ隔离
```java
@BeforeEach
void purgeTestQueues() {
    // 清空测试队列
    rabbitTemplate.purgeQueue("order.close.queue", false);
    rabbitTemplate.purgeQueue("refund.status.sync.queue", false);
}
```

### 外部服务Mock
```java
@BeforeEach
void setupMocks() {
    // Mock 微信支付API
    when(mockWxPayHttpClient.postJson(any(), any(), any(), any()))
        .thenReturn(buildMockWxPayResponse());
    
    // Mock 验签器
    when(mockVerifier.validate(any()))
        .thenReturn(true);
}
```

---

## 完成标准检查

- [x] 特征测试覆盖所有 public API
- [x] 每条可疑行为至少有一条测试
- [x] 测试使用临时隔离 (数据库/Redis/RabbitMQ)
- [x] 测试覆盖 happy path 和 error path
- [x] 测试覆盖返回结构 (code, message, data)
- [x] 测试覆盖日志行为 (INFO/WARN/ERROR)
- [x] 测试名称或注释标明「现状」
- [x] 测试可重复跑，结果稳定
