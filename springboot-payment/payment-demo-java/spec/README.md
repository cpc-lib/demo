# Spec 状态账本

> 本文档跟踪项目中所有 spec 的状态，确保 spec 不退化为过期文档。

---

## 状态说明

| 状态 | 目录 | 含义 |
|------|------|------|
| 🟢 **Implemented** | `implemented/` | 已实现，有测试，有实现锚点 |
| 🟡 **Planned** | `planned/` | 已设计，未实现 |
| ⚫ **Archived** | `archived/` | 废弃或搁置 |
| 📋 **Governance** | `governance/` | 长期治理规则，不迁移 |

---

## 📋 Governance (治理规则)

| Spec | 描述 | 状态 |
|------|------|------|
| [TESTING_GOVERNANCE_SPEC.md](governance/TESTING_GOVERNANCE_SPEC.md) | 测试治理规则：特征测试、单元测试、集成测试分层 | ✅ 活跃 |

---

## 🟢 Implemented (已实现)

| Spec ID | 标题 | 状态 | 实现锚点 | 测试覆盖 | 最后更新 |
|---------|------|------|---------|---------|---------|
| SPEC-001 | [支付核心流程行为规格](implemented/current-behavior/PAYMENT_DEMO_CURRENT_BEHAVIOR_SPEC.md) | ✅ 已实现 | 见锚点 | 24个特征测试 | 2026-06-14 |
| SPEC-005 | [支付对账功能](implemented/RECONCILIATION_FUNCTION_SPEC.md) | ✅ 已实现 | 见锚点 | 待补充 | 2026-07-20 |
| SPEC-006 | [渠道账单导入与对账管理](implemented/CHANNEL_BILL_IMPORT_SPEC.md) | ✅ 已实现 | 见锚点 | 15个特征测试 | 2026-08-28 |
| SPEC-007 | [微信进账与退款逐笔对账兼容扩展](implemented/WXPAY_PAYMENT_REFUND_RECONCILIATION_COMPAT_SPEC.md) | ✅ 已实现 | 见锚点 | 36个相关测试 | 2026-08-30 |
| SPEC-008 | [登录、服务端购物车与多课程合并下单兼容扩展](implemented/AUTH_CART_MULTI_ITEM_ORDER_COMPAT_SPEC.md) | ✅ 已实现 | 见锚点 | 48个相关测试 | 2026-08-31 |
| SPEC-009 | [管理员运营边界与主动状态核对](implemented/ADMIN_OPERATIONS_AND_PURCHASE_BOUNDARY_SPEC.md) | ✅ 已实现 | 见下方锚点 | 14个专用测试，后端全量135个测试 | 2026-08-31 |

### SPEC-001 详情

**标题**: 支付核心流程行为规格

**状态**: implemented

**描述**: 支付系统核心流程的当前行为，包括订单创建、微信支付V2/V3、支付宝、退款、通知处理、分布式锁、配置加载等模块。

**实现锚点**:
| 模块 | 文件 | 关键方法 |
|------|------|---------|
| 订单创建 | `OrderInfoServiceImpl.java` | `createOrReuseOrder()` |
| 微信V3下单 | `WxPayOrderService.java` | `nativePay()`, `doNativePay()` |
| 微信V3通知 | `WxPayNotifyHandler.java`, `WxPayOrderService.java` | `handle()`, `processOrder()` |
| 微信V2通知 | `WxPayV2Controller.java` | `wxNotify()` |
| 退款申请 | `RefundInfoServiceImpl.java` | `createRefundApplication()` |
| 退款审核 | `RefundInfoServiceImpl.java` | `approveRefund()` |
| 订单状态查询 | `OrderInfoController.java` | `queryOrderStatus()` |
| 分布式锁 | `RedissonDistributedLockTemplate.java` | `execute()` |
| 配置加载 | `PaymentConfigLoader.java` | `listAppConfigsByChannelCode()` |

**测试覆盖**: 24个特征测试 (见 `CHARACTERIZATION_TESTS.md`)

**可疑行为** (已锁住):
1. 已关闭订单查询返回"支付中" (Test 5.3)
2. 配置修改后不自动刷新 (Test 7.2)
3. V2/V3响应格式不一致 (Test 3.1)
4. V2/V3幂等Key字段不同 (Test 3.2)
5. 退款计算并发安全 (Test 4.3)
6. 分布式锁中断处理 (Test 6.2)

### SPEC-005 详情

**标题**: 支付对账功能

**状态**: implemented

**描述**: 实现支付渠道账单与本地订单数据的自动对账，生成差异报告，支持财务核销。涵盖微信支付、支付宝两大渠道，支持手动触发和定时自动对账。

**实现锚点**:
| 模块 | 文件 | 关键方法 |
|------|------|---------|
| 对账控制器 | `ReconciliationController.java` | `executeReconciliation()`, `listReconciliation()`, `listDetails()`, `exportReconciliation()` |
| 对账服务 | `ReconciliationServiceImpl.java` | `executeReconciliation()`, `matchOrders()`, `saveDetails()` |
| 微信账单解析 | `WxBillParser.java` | `parse()` |
| 支付宝账单解析 | `AliPayBillParser.java` | `parse()` |
| 定时任务 | `ReconciliationScheduleConfig.java` | `autoReconcileWxPay()`, `autoReconcileAliPay()` |

**验收进度**: 16/17 项已完成（仅特征测试待补充）

### SPEC-006 详情

**标题**: 渠道账单导入与对账管理

**状态**: implemented

**分类**: type: design-change（数据流变更）

**描述**: 渠道账单（微信/支付宝）先导入系统落库（自动拉取 + 手动上传）作为对账依据，对账只消费已导入账单；修复定时任务与微信 T+1 出账（次日 10:00 生成）的冲突，拆分渠道级 cron（微信默认 10:30、支付宝默认 11:00）；对账记录通过 `bill_id` 关联账单。

**实现锚点**:
| 模块 | 文件 | 关键方法 |
|------|------|---------|
| 账单控制器 | `ChannelBillController.java` | `importFromChannel()`, `uploadBill()`, `listBills()`, `listRecords()`, `deleteBill()` |
| 账单服务 | `ChannelBillServiceImpl.java` | `importFromChannel()`, `uploadBill()`, `saveImportedBill()`, `deleteBill()` |
| 对账服务（变更） | `ReconciliationServiceImpl.java` | `doReconcile()` 消费已导入账单 |
| 定时任务（变更） | `ReconciliationScheduleConfig.java` | `autoImportAndReconcileWxPay()`, `autoImportAndReconcileAliPay()` |

**测试覆盖**: 15个特征测试（`ChannelBillServiceTest` 12个 + `ReconciliationBillDependencyTest` 3个）

### SPEC-007 详情

**标题**: 微信进账与退款逐笔对账兼容扩展

**状态**: implemented

**分类**: type: compatibility（微信账单输入协议和明细响应字段扩展）

**描述**: 兼容微信官方 CSV、27 列无表头 Tab、商户平台 XLSX 和旧版 CSV；将 PAYMENT 与本地支付流水、REFUND 与本地退款流水分别做标识、金额和状态双向匹配。

**实现锚点**:
| 模块 | 文件 | 关键方法 |
|------|------|---------|
| 微信账单解析 | `WxBillParser.java` | `normalize()`, `parse()` |
| 进账/退款匹配 | `WxPaymentRefundMatcher.java` | `match()` |
| 多微信应用账单下载 | `WxPayBillService.java` | `downloadBill(Long, ...)` |
| 对账编排 | `ReconciliationServiceImpl.java` | `doReconcile()`, `queryLocalWxPayments()`, `queryLocalWxRefunds()` |
| 手动导入 | `ChannelBillServiceImpl.java` | `uploadBill()` |
| 数据库初始化 | `payment-demo/sql/payment-demo.sql` | 完整表结构，包含明细类型与退款标识列 |

**测试覆盖**: 36个相关测试（`WxBillParserTest` 10个 + `ChannelBillServiceTest` 14个 + `WxPayBillServiceTest` 1个 + `WxPaymentRefundMatcherTest` 7个 + `ReconciliationBillDependencyTest` 4个），并用真实 XLSX 验证 31 条 PAYMENT 与 14 条 REFUND。

---

## 🟡 Planned (已设计未实现)

| Spec ID | 标题 | 优先级 | 依赖 | 计划版本 |
|---------|------|--------|------|---------|
| SPEC-002 | V2通知处理器提取重构 | 中 | SPEC-001测试全绿 | v0.0.2 |
| SPEC-003 | 配置热更新机制 | 低 | 无 | v0.1.0 |
| SPEC-004 | 退款并发安全加固 | 高 | SPEC-001测试全绿 | v0.0.3 |

### SPEC-008 详情

**标题**: 登录、服务端购物车与多课程合并下单兼容扩展

**状态**: implemented

**分类**: type: compatibility（现有购买和管理接口新增登录/角色要求，同时扩展订单数据模型和结算数据流）

**描述**: 新增 USER/ADMIN 登录鉴权、Access/Refresh Token 轮换、按用户隔离的服务端购物车和订单明细；支持多课程、多份数一次下单，并保留微信 V3、微信 V2、支付宝以及现有支付/退款/对账协议。

**实现锚点**: `implemented/AUTH_CART_MULTI_ITEM_ORDER_COMPAT_SPEC.md`；认证、购物车、结算与支付入口见 `payment-demo/src/main/java/cc/ivera/`，React/Vue 入口见各自 `src/`。

**测试覆盖**: 48个相关测试，包含 `CorsPreflightCharacterizationTest` 对 React 购物车写请求的真实 CORS 预检链路。

### SPEC-002 详情

**标题**: V2通知处理器提取重构

**状态**: planned

**描述**: 将 `WxPayV2Controller.wxNotify()` 内联逻辑提取到 `WxPayV2NotifyHandler`，与V3保持一致的处理器架构。

**目标**:
- Controller方法简化为 3-5 行委托调用
- 新增 `WxPayV2NotifyHandler` 类
- 公共 API、响应格式、幂等Key、锁参数全部不变

**风险**: 低 (仅结构变化，特征测试已覆盖)

**验收标准**:
- [ ] 特征测试全绿 (24个测试)
- [ ] V2通知响应格式不变 (XML)
- [ ] 幂等Key不变 (`payment:wx:v2:notify:processed:{transactionId}`)

### SPEC-003 详情

**标题**: 配置热更新机制

**状态**: planned

**描述**: 实现数据库配置修改后自动刷新内存缓存，无需重启服务。

**目标**:
- 监听 `t_payment_app` 和 `t_payment_channel` 表变更
- 自动刷新 `PaymentConfigLoader` 内存缓存
- 刷新 `WxPayNotifyHandler.verifierCache` 验签器缓存

**风险**: 中 (影响多商户配置加载)

**验收标准**:
- [ ] 配置修改后 5 秒内生效
- [ ] 刷新过程不中断服务
- [ ] 刷新失败时回滚到旧配置

### SPEC-004 详情

**标题**: 退款并发安全加固

**状态**: planned

**描述**: 修复退款申请计算可退金额和插入记录之间的并发安全漏洞。

**目标**:
- 在退款申请创建时获取订单行锁
- 计算和插入在同一事务和锁范围内
- 防止超退

**风险**: 高 (改变退款流程锁范围)

**验收标准**:
- [ ] 并发退款申请不会超退
- [ ] 特征测试全绿
- [ ] 性能影响 < 10%

### SPEC-009 详情

**标题**: 管理员运营边界与主动状态核对

**状态**: implemented

**分类**: type: design-change（调整 ADMIN 购买边界并新增管理端运维流程）

**描述**: ADMIN 禁止购物车、结算、商品支付、已有订单支付和退款申请；React/Vue 管理端提供全部订单、退款审核、支付查单和退款状态核对，优先复用已有接口。

**设计文档**: `docs/superpowers/specs/2026-08-31-admin-operations-design.md`

**实现锚点**: `implemented/ADMIN_OPERATIONS_AND_PURCHASE_BOUNDARY_SPEC.md`；后端购买边界见 `AuthContext.requireShoppingUser()`，支付宝主动查单见 `AliPayController.checkOrderStatus()`，React/Vue 退款审批见各自 `Refunds` 页面。

**测试覆盖**: 14 个专用测试；后端全量 135 个测试通过；React 构建、Vue 构建与 lint 通过。

---

## ⚫ Archived (废弃)

| Spec ID | 标题 | 废弃原因 | 废弃日期 |
|---------|------|---------|---------|
| (暂无) | - | - | - |

---

## Spec 状态迁移记录

| 日期 | Spec ID | 旧状态 | 新状态 | 原因 |
|------|---------|--------|--------|------|
| 2026-06-14 | SPEC-001 | - | implemented | 初始创建，覆盖核心流程 |
| 2026-07-20 | SPEC-005 | - | planned | 新增支付对账功能设计方案 |
| 2026-07-20 | SPEC-005 | planned | implemented | 完成全部开发：后端（Java）+ React 前端 + Vue 前端 |
| 2026-08-28 | SPEC-006 | - | planned | 新增渠道账单导入与对账管理设计（type: design-change） |
| 2026-08-28 | SPEC-006 | planned | implemented | 完成全部开发：数据库 + 后端 + Vue/React 前端 + 15 个特征测试 |
| 2026-08-29 | SPEC-007 | - | planned | 兼容微信官方账单格式，按支付流水和退款流水逐笔双向对账 |
| 2026-08-30 | SPEC-007 | planned | implemented | 完成解析、逐笔匹配、持久化、双前端展示、真实账单验证与兼容测试 |
| 2026-08-30 | SPEC-008 | - | planned | 新增登录、两级角色、服务端购物车和多课程合并下单设计 |
| 2026-08-30 | SPEC-008 | planned | implemented | 完成认证、服务端购物车、多课程合单、三渠道按订单支付、权限隔离、双前端与47个相关测试 |
| 2026-08-31 | SPEC-009 | - | planned | 新增管理员购买边界、退款审核、支付查单和退款状态核对设计 |
| 2026-08-31 | SPEC-009 | planned | implemented | 完成 ADMIN 购买边界、双前端退款审批与支付/退款状态核对；后端全量135个测试及前端构建通过 |

---

## 维护规则

1. **每次 PR 必须更新 spec 状态** (如适用)
2. **实现完成**: spec 从 `planned` 移到 `implemented`
3. **设计废弃**: spec 移到 `archived/deprecated/`
4. **实现变更**: 更新 `implemented/` 中的实现锚点
5. **定期清理**: 每季度检查 `planned/` 中超过 3 个月未动的 spec
