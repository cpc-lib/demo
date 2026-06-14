# SPEC-002: V2通知处理器提取重构

> 将 WxPayV2Controller.wxNotify() 内联逻辑提取到独立处理器。

---

## 元信息

| 字段 | 值 |
|------|------|
| **Spec ID** | SPEC-002 |
| **标题** | V2通知处理器提取重构 |
| **状态** | planned |
| **版本** | 0.1.0 |
| **创建日期** | 2026-06-14 |
| **最后更新** | 2026-06-14 |
| **依赖** | SPEC-001 特征测试全绿 |
| **优先级** | 中 |

---

## 1. 概述

### 1.1 背景

- V3 通知已提取到 `WxPayNotifyHandler`
- V2 通知逻辑内联在 `WxPayV2Controller.wxNotify()` 中 (约80行)
- Controller 承担过多职责，难以测试和维护

### 1.2 目标

- 提取 V2 通知处理逻辑到 `WxPayV2NotifyHandler`
- Controller 方法简化为 3-5 行委托调用
- 保持所有外部行为不变

---

## 2. 契约

### 2.1 不变的公共 API

| 约束项 | 现状 | 重构后 |
|--------|------|--------|
| API 路径 | `/api/wx-pay-v2/native/notify` | ✅ 不变 |
| HTTP 方法 | POST | ✅ 不变 |
| 请求格式 | XML | ✅ 不变 |
| 响应格式 | `<xml><return_code>SUCCESS/FAIL</return_code>...</xml>` | ✅ 不变 |
| 幂等 Key | `payment:wx:v2:notify:processed:{transactionId}` | ✅ 不变 |
| 锁 Key | `payment:wx:v2:notify:{transactionId}` | ✅ 不变 |
| 锁参数 | 等待5s, 租期30s | ✅ 不变 |

### 2.2 不变的日志

- 所有 INFO/WARN/ERROR 日志保留
- 日志格式和内容不变

---

## 3. 验收标准

### 3.1 功能验收

- [ ] 特征测试 Test 3.1 通过 (V2通知返回XML格式)
- [ ] 特征测试 Test 3.2 通过 (V2使用transactionId做幂等)
- [ ] 全部 24 个特征测试通过
- [ ] Controller 方法不超过 5 行

### 3.2 结构验收

- [ ] 新增 `WxPayV2NotifyHandler.java`
- [ ] `WxPayV2Controller` 仅保留委托调用
- [ ] 所有依赖通过构造函数注入

---

## 4. 实现锚点

### 4.1 修改文件

| 文件 | 变更类型 | 行数变化 |
|------|---------|---------|
| `WxPayV2Controller.java` | 简化 | -70行 |
| `WxPayV2NotifyHandler.java` | 新增 | +95行 |

### 4.2 提取方法

| 方法 | 职责 | 行数 |
|------|------|------|
| `handle()` | 入口编排 | ~20 |
| `parseAndValidateXml()` | XML解析校验 | ~15 |
| `tryAcquireIdempotentLock()` | 幂等控制 | ~10 |
| `executeInLockAndTransaction()` | 锁+事务 | ~30 |
| `buildSuccessXml()` | 成功响应 | ~5 |
| `buildFailureXml()` | 失败响应 | ~5 |

---

## 5. 兼容影响

### 5.1 影响分析

| 依赖方 | 影响 | 策略 |
|--------|------|------|
| 微信支付平台 | 无 | 响应格式不变 |
| 前端应用 | 无 | API 路径不变 |
| 其他服务 | 无 | 无新增依赖 |

### 5.2 风险评估

🟢 **低风险**: 仅结构变化，不改变业务逻辑

---

## 6. 测试文件

| 文件 | 用途 |
|------|------|
| `CHARACTERIZATION_TESTS.md` | Test 3.1, Test 3.2 |
