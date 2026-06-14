# SPEC-001: 支付核心流程行为规格

> 本文档定义支付系统核心流程的当前行为，用于特征测试和重构对账。

---

## 元信息

| 字段 | 值 |
|------|------|
| **Spec ID** | SPEC-001 |
| **标题** | 支付核心流程行为规格 |
| **状态** | implemented |
| **版本** | 1.0.0 |
| **创建日期** | 2026-06-14 |
| **最后更新** | 2026-06-14 |
| **作者** | AI + 人工审核 |
| **审核状态** | 已审核 |

---

## 1. 概述

### 1.1 范围

本 spec 覆盖支付系统以下核心模块的**当前实际行为**：

- 订单创建与复用
- 微信支付 V2/V3 Native 下单
- 微信支付 V2/V3 通知处理
- 支付宝支付与通知
- 退款申请与审核
- 订单状态查询
- 分布式锁控制
- 配置加载机制

### 1.2 排除范围

- 前端交互逻辑
- 运维部署流程
- 数据库表结构变更

---

## 2. 契约

### 2.1 公共 API 契约

| API | 方法 | 请求格式 | 响应格式 | 稳定级别 |
|-----|------|---------|---------|---------|
| `/api/wx-pay/native/{productId}` | POST | JSON | `R<Map>` with `{orderNo, codeUrl}` | **稳定** |
| `/api/wx-pay/native/notify` | POST | JSON | `{"code":"SUCCESS/FAIL","message":"..."}` | **稳定** |
| `/api/wx-pay-v2/native/notify` | POST | XML | `<xml><return_code>SUCCESS/FAIL</return_code>...</xml>` | **稳定** |
| `/api/ali-pay/trade/page/pay/{productId}` | POST | JSON | `R<Map>` with `{formStr}` | **稳定** |
| `/api/ali-pay/trade/notify` | POST | Form | `success` 或 `failure` | **稳定** |
| `/api/order-info/query-order-status/{orderNo}` | GET | - | `R<Map>` with code=0/101 | **稳定** |
| `/api/refund-info/apply` | POST | JSON | `R<RefundInfo>` | **稳定** |
| `/api/refund-info/approve/{refundNo}` | POST | JSON | `R` | **稳定** |

### 2.2 数据契约

#### 订单状态流转

```
未支付 → 支付成功 → 退款中 → 部分退款/已退款
   ↓          ↓
已关闭    退款异常
```

#### 退款审核状态

```
待审核 → 审核通过 → 退款执行 → 退款成功
   ↓
审核拒绝
```

### 2.3 并发控制契约

| 层级 | 机制 | 参数 | 作用域 |
|------|------|------|--------|
| L1 | Redis 通知幂等 | SET NX, 24h TTL | 按 requestId/transactionId |
| L2 | Redisson 分布式锁 | 看门狗续期 or 固定租期 | 按 orderNo/productId |
| L3 | 数据库行锁 | SELECT FOR UPDATE | 按 orderNo |
| L4 | 乐观锁 | @Version | 按 order_info 行 |

---

## 3. 验收标准

### 3.1 特征测试覆盖

| 模块 | 测试数 | 覆盖状态 |
|------|--------|---------|
| 订单创建 | 5 | ✅ 已覆盖 |
| 微信V3通知 | 4 | ✅ 已覆盖 |
| 微信V2通知 | 2 | ✅ 已覆盖 |
| 退款申请 | 4 | ✅ 已覆盖 |
| 订单状态查询 | 3 | ✅ 已覆盖 |
| 分布式锁 | 2 | ✅ 已覆盖 |
| 配置加载 | 2 | ✅ 已覆盖 |
| 微信V3下单 | 2 | ✅ 已覆盖 |
| **总计** | **24** | **全绿** |

### 3.2 可疑行为锁住

| # | 行为 | 测试 | 风险等级 | 锁住状态 |
|---|------|------|---------|---------|
| 1 | 已关闭订单查询返回"支付中" | Test 5.3 | 高 | ✅ 已锁住 |
| 2 | 配置修改后不自动刷新 | Test 7.2 | 中 | ✅ 已锁住 |
| 3 | V2/V3响应格式不一致 | Test 3.1 | 低 | ✅ 已锁住 |
| 4 | V2/V3幂等Key字段不同 | Test 3.2 | 低 | ✅ 已锁住 |
| 5 | 退款计算并发安全 | Test 4.3 | 高 | ✅ 已锁住 |
| 6 | 分布式锁中断处理 | Test 6.2 | 中 | ✅ 已锁住 |

---

## 4. 实现锚点

### 4.1 代码文件映射

| 模块 | 文件路径 | 关键方法 | 行数 |
|------|---------|---------|------|
| 订单创建 | `payment-demo/src/main/java/cc/ivera/service/impl/OrderInfoServiceImpl.java` | `createOrReuseOrder()` | ~100 |
| 微信V3下单 | `payment-demo/src/main/java/cc/ivera/service/impl/wxpay/WxPayOrderService.java` | `nativePay()`, `doNativePay()` | ~80 |
| 微信V3通知 | `payment-demo/src/main/java/cc/ivera/controller/support/WxPayNotifyHandler.java` | `handle()` | ~60 |
| 微信V3通知处理 | `payment-demo/src/main/java/cc/ivera/service/impl/wxpay/WxPayOrderService.java` | `processOrder()` | ~40 |
| 微信V2通知 | `payment-demo/src/main/java/cc/ivera/controller/WxPayV2Controller.java` | `wxNotify()` | ~80 |
| 退款申请 | `payment-demo/src/main/java/cc/ivera/service/impl/RefundInfoServiceImpl.java` | `createRefundApplication()` | ~60 |
| 退款审核 | `payment-demo/src/main/java/cc/ivera/service/impl/RefundInfoServiceImpl.java` | `approveRefund()` | ~40 |
| 订单状态查询 | `payment-demo/src/main/java/cc/ivera/controller/OrderInfoController.java` | `queryOrderStatus()` | ~15 |
| 分布式锁 | `payment-demo/src/main/java/cc/ivera/lock/RedissonDistributedLockTemplate.java` | `execute()` | ~40 |
| 配置加载 | `payment-demo/src/main/java/cc/ivera/config/PaymentConfigLoader.java` | `listAppConfigsByChannelCode()` | ~30 |

### 4.2 数据库表映射

| 表名 | 用途 | 关键字段 | 约束 |
|------|------|---------|------|
| `t_order_info` | 订单 | order_no (UK), version | 乐观锁 |
| `t_payment_info` | 支付流水 | order_no+payment_type (UK) | 幂等 |
| `t_refund_info` | 退款 | refund_no (UK), refund_id (UK) | 幂等 |
| `t_payment_app` | 支付应用 | app_code (UK) | JSON配置 |
| `t_payment_channel` | 支付渠道 | channel_code (UK) | JSON配置 |
| `t_product` | 商品 | id (PK) | - |

### 4.3 外部服务依赖

| 服务 | URL | 用途 | 协议 |
|------|-----|------|------|
| 微信支付 V3 | `https://api.mch.weixin.qq.com` | 下单、退款、查单 | HTTPS + JSON |
| 支付宝沙箱 | `https://openapi-sandbox.dl.alipaydev.com` | 下单、退款 | HTTPS + Form |
| Redis | `192.168.220.200:6379` | 分布式锁、幂等 | RESP |
| RabbitMQ | `192.168.220.200:5672` | 延迟消息 | AMQP |

---

## 5. 兼容影响

### 5.1 下游依赖

| 依赖方 | 依赖内容 | 影响等级 | 变更策略 |
|--------|---------|---------|---------|
| React 前端 | `/api/wx-pay/native/**` | 高 | 保持 API 不变 |
| Vue 前端 | `/api/wx-pay-v2/native/**` | 高 | 保持 API 不变 |
| 微信支付平台 | `/api/wx-pay/native/notify` | 高 | 保持响应格式不变 |
| 支付宝平台 | `/api/ali-pay/trade/notify` | 高 | 保持响应格式不变 |

### 5.2 破坏性变更历史

| 日期 | 变更 | 影响 | 迁移方案 |
|------|------|------|---------|
| (暂无) | - | - | - |

### 5.3 向后兼容承诺

- ✅ 所有 public API 路径保持不变
- ✅ 响应结构 (code, message, data) 保持不变
- ✅ 微信支付 V2 响应格式保持 XML
- ✅ 微信支付 V3 响应格式保持 JSON
- ✅ 支付宝响应格式保持纯文本
- ✅ 幂等 Key 格式保持不变

---

## 6. 测试文件

| 文件 | 用途 |
|------|------|
| `CHARACTERIZATION_TESTS.md` | 24个特征测试清单 |
| `TEST_MAPPING.md` | 测试与现状映射表 |

---

## 7. 相关 Spec

| Spec ID | 关系 | 描述 |
|---------|------|------|
| SPEC-002 | 衍生 | V2通知处理器提取重构 (planned) |
| SPEC-003 | 衍生 | 配置热更新机制 (planned) |
| SPEC-004 | 衍生 | 退款并发安全加固 (planned) |

---

## 8. 变更日志

| 版本 | 日期 | 变更 | 作者 |
|------|------|------|------|
| 1.0.0 | 2026-06-14 | 初始创建，覆盖核心流程 | AI + 审核 |
