# SPEC-005: 支付对账功能

> 实现支付渠道账单与本地订单数据的自动对账，生成差异报告，支持财务核销。

---

## 元信息

| 字段 | 值 |
|------|------|
| **Spec ID** | SPEC-005 |
| **标题** | 支付对账功能 |
| **状态** | implemented |
| **版本** | 0.1.0 |
| **创建日期** | 2026-07-20 |
| **最后更新** | 2026-07-20 |
| **依赖** | SPEC-001 特征测试全绿 |
| **优先级** | 高 |
| **领域** | bill |
| **影响范围** | backend, database, react-frontend, vue-frontend, docs, tests |

---

## 1. 背景 / 目标

### 1.1 背景

当前支付系统已具备以下能力：
- 微信支付 V3 账单下载：`WxPayBillService.queryBill()` / `downloadBill()`
- 支付宝账单下载：`AliPayService.queryBill()`
- 前端账单下载页面：React/Vue 的 `Download.jsx` / `Download.vue`

但存在以下缺口：
- 仅能下载渠道账单原始文件，无法与本地订单数据自动比对
- 财务人员需手动导出本地订单并人工核对，效率低下且易出错
- 没有对账差异记录，无法追踪漏单、多单、金额不符等问题
- 没有对账历史记录，无法回溯某一期的对账结果

### 1.2 目标

- 支持微信支付、支付宝两大渠道的自动对账
- 自动下载渠道账单、解析、与本地订单匹配
- 识别四类差异：**漏单**（渠道有本地无）、**多单**（本地有渠道无）、**金额不符**、**状态不符**
- 生成对账报告，支持按差异类型筛选
- 支持手动触发对账和定时自动对账
- 对账结果持久化，支持历史查询

---

## 2. 当前现状（重构前必须承认）

### 2.1 入口清单

| 入口 | 类型 | 当前调用方 | 备注 |
|---|---|---|---|
| `GET /api/wx-pay/querybill/{billDate}/{type}` | REST | React/Vue 前端 | 获取微信账单下载URL |
| `GET /api/wx-pay/downloadbill/{billDate}/{type}` | REST | React/Vue 前端 | 下载微信账单原文 |
| `GET /api/ali-pay/bill/downloadurl/query/{billDate}/{type}` | REST | React/Vue 前端 | 获取支付宝账单下载URL |
| `WxPayBillService.queryBill()` | Service | WxPayController | 调用微信 V3 账单 API |
| `AliPayService.queryBill()` | Service | AliPayController | 调用支付宝账单 API |

### 2.2 现有实现锚点

| 层 | 文件 / 类 / 函数 | 现有职责 |
|---|---|---|
| Controller | `WxPayController.queryTradeBill()` / `downloadBill()` | 微信账单查询和下载入口 |
| Controller | `AliPayController.queryTradeBill()` | 支付宝账单查询入口 |
| Service | `WxPayBillService.queryBill()` / `downloadBill()` | 调用微信 V3 API，返回 downloadUrl 或账单原文 |
| Service | `AliPayService.queryBill()` | 调用支付宝 API，返回 downloadUrl |
| Config | `PaymentConfigLoader` | 支付应用/渠道配置加载 |
| Frontend | `payment-demo-react/src/api/bill.js` | 账单 API 封装 |
| Frontend | `payment-demo-react/src/pages/Download.jsx` | 账单下载页面 |

### 2.3 现有可疑行为

- **账单日期校验**：`WxPayBillService.validateBillDate()` 限制只能查近三个月账单，且昨日账单需 10:00 后才能查询——对账功能需继承此约束。
- **微信账单类型**：仅支持 `tradebill`（交易账单）和 `fundflowbill`（资金账单），对账功能以 `tradebill` 为主。
- **支付宝账单类型**：仅支持 `trade`（交易账单）和 `signcustomer`（业务账单），对账功能以 `trade` 为主。

---

## 3. 契约（对外行为，必须稳定）

### 3.1 输入 / 调用

#### HTTP 接口

| 方法 | 路径 | 说明 |
|---|---|---|
| `POST` | `/api/reconciliation/execute` | 手动触发对账 |
| `GET` | `/api/reconciliation/list` | 对账记录列表（分页） |
| `GET` | `/api/reconciliation/{id}` | 对账记录详情 |
| `GET` | `/api/reconciliation/{id}/details` | 对账明细列表（分页，支持按差异类型筛选） |
| `GET` | `/api/reconciliation/{id}/diff` | 仅差异明细（分页） |
| `GET` | `/api/reconciliation/{id}/export` | 导出对账报告（CSV） |

#### 执行对账请求参数

```json
{
  "billDate": "2026-07-19",
  "channelCode": "WXPAY",
  "paymentAppId": 1,
  "billType": "ALL"
}
```

| 参数 | 必填 | 说明 |
|---|---|---|
| `billDate` | 是 | 对账日期，格式 yyyy-MM-dd，必须为历史日期 |
| `channelCode` | 是 | 渠道编码：`WXPAY` / `ALIPAY` |
| `paymentAppId` | 否 | 指定支付应用，不传则使用默认应用 |
| `billType` | 否 | 微信交易账单子类型：`ALL`/`SUCCESS`/`REFUND`，默认 `ALL` |

#### 定时任务

- 每日凌晨 2:00 自动执行昨日对账（可配置）
- 按支付应用维度分别执行微信和支付宝对账

### 3.2 输出 / 响应

#### 执行对账响应

```json
{
  "code": 0,
  "message": "对账任务已提交",
  "data": {
    "reconciliationId": 1001,
    "billDate": "2026-07-19",
    "channelCode": "WXPAY",
    "status": "PROCESSING"
  }
}
```

#### 对账记录详情响应

```json
{
  "code": 0,
  "data": {
    "id": 1001,
    "billDate": "2026-07-19",
    "channelCode": "WXPAY",
    "paymentAppId": 1,
    "status": "COMPLETED",
    "totalCount": 150,
    "matchCount": 145,
    "diffCount": 5,
    "diffAmount": 12800,
    "channelTotalAmount": 1500000,
    "localTotalAmount": 1487200,
    "startTime": "2026-07-20T02:00:00",
    "endTime": "2026-07-20T02:00:35",
    "errorMessage": null
  }
}
```

#### 对账明细响应

```json
{
  "code": 0,
  "data": {
    "list": [
      {
        "id": 1,
        "reconciliationId": 1001,
        "diffType": "AMOUNT_MISMATCH",
        "orderNo": "ORDER_20260719_001",
        "transactionId": "4200001234202607191234567890",
        "channelAmount": 10000,
        "localAmount": 9800,
        "channelStatus": "SUCCESS",
        "localStatus": "SUCCESS",
        "diffAmount": 200,
        "remark": "金额差异2元"
      }
    ],
    "total": 5,
    "pageNum": 1,
    "pageSize": 20
  }
}
```

#### 对账状态枚举

| 状态 | 说明 |
|---|---|
| `PENDING` | 待执行 |
| `PROCESSING` | 执行中 |
| `COMPLETED` | 已完成（有差异或无差异均为完成） |
| `FAILED` | 执行失败 |

#### 差异类型枚举

| 类型 | 说明 |
|---|---|
| `MATCH` | 完全匹配 |
| `MISSING_LOCAL` | 漏单（渠道有，本地无） |
| `MISSING_CHANNEL` | 多单（本地有，渠道无） |
| `AMOUNT_MISMATCH` | 金额不符 |
| `STATUS_MISMATCH` | 状态不符 |

#### 数据库变化

- **新增表**：`t_reconciliation`（对账主表）、`t_reconciliation_detail`（对账明细表）
- **不修改**：现有 `t_order_info`、`t_payment_info`、`t_refund_info` 表结构

#### 事件输出

- 对账完成后发送 RabbitMQ 消息（可选，用于通知或后续处理）
- Redis 缓存最近一次对账结果摘要

### 3.3 状态迁移

#### 对账任务状态

```
PENDING → PROCESSING → COMPLETED
              ↓
            FAILED
```

| 起始状态 | 触发 | 目标状态 | 规则 |
|---|---|---|---|
| `PENDING` | 开始执行 | `PROCESSING` | 记录开始时间 |
| `PROCESSING` | 对账成功完成 | `COMPLETED` | 记录结束时间、统计数据 |
| `PROCESSING` | 对账异常 | `FAILED` | 记录错误信息 |

### 3.4 不变量 / 边界

- **账单日期约束**：继承 `WxPayBillService.validateBillDate()` 规则——仅支持历史日期，近三个月内，昨日账单 10:00 后生成。
- **同一日期同一应用同一渠道的对账幂等**：若已存在完成的对账记录，重复触发返回已有记录，不重新执行（除非显式指定 `force=true`）。
- **对账范围**：仅比对 `billDate` 当天成功的支付订单（含退款），不包含未支付或已关闭订单。
- **金额单位**：统一使用"分"为单位，与现有订单/支付流水表保持一致。
- **渠道账单原文**：每次对账保存渠道账单原文的哈希值，用于后续审计追溯（原文本身不落库，避免占用空间）。

---

## 4. 隐式输入输出 / 状态地图

| 类型 | 名称 / 位置 | 读 | 写 | TTL / 生命周期 | 备注 |
|---|---|---|---|---|---|
| DB | `t_reconciliation` | 列表查询、详情查询 | 对账执行时插入/更新 | 持久 | 对账主记录 |
| DB | `t_reconciliation_detail` | 明细查询、差异查询 | 对账执行时批量插入 | 持久 | 对账明细，按 reconciliation_id 索引 |
| DB | `t_order_info` | 对账时按日期+渠道读取 | 不写 | 持久 | 仅读取，不改写 |
| DB | `t_payment_info` | 对账时按日期+渠道读取 | 不写 | 持久 | 仅读取，不改写 |
| DB | `t_payment_app` | 对账时读取配置 | 不写 | 持久 | 获取渠道参数 |
| Redis | `recon:lock:{billDate}:{channelCode}:{appId}` | 对账前检查 | 对账开始时加锁 | 30分钟 | 防止同一对账任务并发执行 |
| Redis | `recon:latest:{channelCode}:{appId}` | 查询最新状态 | 对账完成后写入 | 24小时 | 最近一次对账摘要 |
| RabbitMQ | `reconciliation.completed`（可选） | consumer | 对账完成后发布 | 消息TTL 1小时 | 通知下游（如财务系统） |
| Config | `application.yml` | 读取定时任务cron | 不写 | 启动期 | `reconciliation.cron=0 0 2 * * ?` |
| Log | `cc.ivera.reconciliation` | n/a | 对账全流程日志 | 日志保留策略 | 含执行耗时、差异统计 |

---

## 5. 多套实现 / 多套规则并存

- **微信 vs 支付宝账单格式**：微信返回 CSV 格式，支付宝返回 CSV 格式（GZIP 压缩），需分别实现解析器：
  - `WxBillParser`：解析微信交易账单 CSV
  - `AliPayBillParser`：解析支付宝交易账单 CSV
- **多支付应用**：支持按 `paymentAppId` 对账，不传则使用默认应用
- **手动 vs 定时**：两种触发方式共用同一对账核心逻辑，仅触发入口不同
- **前端兼容**：React 和 Vue 前端均需新增对账页面，复用同一后端 API

---

## 6. 验收标准

- [x] 调用 `POST /api/reconciliation/execute` 能成功触发对账，返回对账任务 ID 和状态
- [x] 微信对账：能正确下载并解析微信交易账单，与本地订单逐一匹配
- [x] 支付宝对账：能正确下载并解析支付宝交易账单，与本地订单逐一匹配
- [x] 漏单识别：渠道账单中有但本地无的订单，标记为 `MISSING_LOCAL`
- [x] 多单识别：本地有但渠道账单中无的成功订单，标记为 `MISSING_CHANNEL`
- [x] 金额不符识别：渠道金额与本地金额不一致的订单，标记为 `AMOUNT_MISMATCH`
- [x] 状态不符识别：渠道状态与本地状态不一致的订单，标记为 `STATUS_MISMATCH`
- [x] 对账统计：`totalCount = matchCount + diffCount`，金额汇总准确
- [x] 幂等性：同一日期同一渠道同一应用重复触发对账，不重复生成对账记录（`force=true` 除外）
- [x] 并发安全：同一对账任务并发触发时，通过 Redis 锁保证只有一个执行
- [x] 历史查询：`GET /api/reconciliation/list` 支持按日期、渠道、状态筛选，分页正确
- [x] 明细查询：`GET /api/reconciliation/{id}/details` 支持按差异类型筛选
- [x] 导出功能：`GET /api/reconciliation/{id}/export` 能导出 CSV 格式对账报告
- [x] 定时任务：每日凌晨自动执行昨日对账，可通过配置关闭
- [x] 失败处理：对账失败时状态标记为 `FAILED`，错误信息可读，不影响历史数据
- [ ] 特征测试：新增对账相关特征测试不少于 8 个，全部通过
- [x] 不破坏现有账单下载功能：原有 `/api/wx-pay/querybill`、`/api/ali-pay/bill/downloadurl/query` 等接口行为不变

---

## 7. 特征测试清单

### 7.1 第一批必须锁住

- [ ] 对账执行 API 契约：`POST /api/reconciliation/execute` 的成功/失败响应、字段结构
- [ ] 对账记录列表 API：`GET /api/reconciliation/list` 分页和筛选
- [ ] 对账详情 API：`GET /api/reconciliation/{id}/details` 差异类型筛选
- [ ] 微信对账匹配逻辑：模拟微信账单 CSV，验证匹配、漏单、多单、金额差异识别
- [ ] 支付宝对账匹配逻辑：模拟支付宝账单 CSV，验证匹配、漏单、多单、金额差异识别
- [ ] 对账幂等性：同一日期重复触发，验证不重复生成记录
- [ ] 对账并发安全：并发触发同一对账任务，验证 Redis 锁生效
- [ ] 对账统计准确性：验证 totalCount、matchCount、diffCount、金额汇总正确
- [ ] 定时任务调度：验证定时任务在配置时间触发（用 Mock）
- [ ] 对账失败场景：模拟渠道账单下载失败，验证 FAILED 状态和错误信息

### 7.2 测试锚点（implemented 时填写）

| 测试类型 | 文件 | 覆盖行为 |
|---|---|---|
| 单元测试 | `cc.ivera.service.reconciliation.WxBillParserTest` | 微信账单解析 |
| 单元测试 | `cc.ivera.service.reconciliation.AliPayBillParserTest` | 支付宝账单解析 |
| 单元测试 | `cc.ivera.service.reconciliation.ReconciliationMatcherTest` | 匹配逻辑、差异识别 |
| Web/API 测试 | `cc.ivera.controller.ReconciliationControllerTest` | API 契约、幂等、并发 |
| 集成测试 | `cc.ivera.reconciliation.ReconciliationIntegrationTest` | 全流程对账（Mock 外部 API） |

---

## 8. 兼容性影响

- **HTTP 路径**：新增 `/api/reconciliation/**` 系列接口，不修改现有接口路径
- **响应结构**：所有新接口遵循现有 `R<T>` 统一响应格式
- **数据库结构**：新增 2 张表（`t_reconciliation`、`t_reconciliation_detail`），不修改现有表结构；需提供增量升级脚本 `payment_reconciliation_upgrade.sql`
- **状态值**：新增对账状态枚举（`PENDING`/`PROCESSING`/`COMPLETED`/`FAILED`）和差异类型枚举，不影响现有订单/退款状态
- **配置字段**：新增 `reconciliation.cron`、`reconciliation.enabled` 配置项，默认开启
- **前端影响**：React 和 Vue 均需新增对账页面（对账列表、对账详情、执行对账），不修改现有页面
- **第三方平台配置**：不改变微信/支付宝回调地址、证书等配置，仅复用已有账单下载能力

---

## 9. 实现锚点（implemented 时必须填写）

- Controller：`payment-demo/src/main/java/cc/ivera/controller/ReconciliationController.java`
- Service：
  - `payment-demo/src/main/java/cc/ivera/service/reconciliation/ReconciliationService.java`（接口）
  - `payment-demo/src/main/java/cc/ivera/service/impl/reconciliation/ReconciliationServiceImpl.java`（实现）
  - `payment-demo/src/main/java/cc/ivera/service/impl/reconciliation/WxBillParser.java`
  - `payment-demo/src/main/java/cc/ivera/service/impl/reconciliation/AliPayBillParser.java`
- Mapper / SQL：
  - `payment-demo/src/main/java/cc/ivera/mapper/ReconciliationMapper.java`
  - `payment-demo/src/main/java/cc/ivera/mapper/ReconciliationDetailMapper.java`
  - `payment-demo/src/main/resources/mapper/ReconciliationMapper.xml`
  - `payment-demo/sql/payment_reconciliation_upgrade.sql`
- Entity / DTO / Enum：
  - `payment-demo/src/main/java/cc/ivera/entity/Reconciliation.java`
  - `payment-demo/src/main/java/cc/ivera/entity/ReconciliationDetail.java`
  - `payment-demo/src/main/java/cc/ivera/dto/ReconciliationExecuteRequest.java`
  - `payment-demo/src/main/java/cc/ivera/enums/ReconciliationStatus.java`
  - `payment-demo/src/main/java/cc/ivera/enums/DiffType.java`
- Frontend：
  - `payment-demo-react/src/api/reconciliation.js`
  - `payment-demo-react/src/pages/Reconciliation.jsx`
  - `payment-demo-vue/src/api/reconciliation.js`
  - `payment-demo-vue/src/views/Reconciliation.vue`
- Config / MQ / Cache：
  - `application.yml`（新增 reconciliation 配置段）
  - `ReconciliationScheduleConfig.java`（定时任务配置）
- Tests：见 7.2 测试锚点
- Docs / Examples：
  - `spec/implemented/RECONCILIATION_FUNCTION_SPEC.md`（实现后迁移至此）

---

## 10. 回滚 / 观测

- **回滚方式**：
  - 代码回滚：`git revert` 对账功能相关提交
  - 配置回滚：将 `reconciliation.enabled` 设为 `false` 可关闭对账功能
  - DB 回滚：对账表为新增表，不影响现有业务，可直接丢弃（若需回滚）
- **需要观察的日志**：
  - `cc.ivera.reconciliation` logger 下的 INFO/WARN/ERROR 日志
  - 关键字：`对账开始`、`账单下载完成`、`账单解析完成`、`匹配完成`、`对账完成`、`对账失败`
- **需要观察的数据**：
  - `t_reconciliation` 表：最新记录的状态、diffCount、diffAmount
  - Redis `recon:lock:*`：是否存在未释放的锁
  - Redis `recon:latest:*`：最近对账摘要是否更新
- **失败时对外表现**：
  - 手动触发对账：API 返回错误信息，前端展示失败原因
  - 定时对账：日志记录失败，不影响其他功能，次日自动重试

---

## 11. 变更记录

| 日期 | 状态 | 改了什么 | 关联 issue / PR |
|---|---|---|---|
| 2026-07-20 | planned | 初稿，设计对账功能完整方案 | - |
| 2026-07-20 | implemented | 完成全部开发：后端（Java）+ React 前端 + Vue 前端 | - |
