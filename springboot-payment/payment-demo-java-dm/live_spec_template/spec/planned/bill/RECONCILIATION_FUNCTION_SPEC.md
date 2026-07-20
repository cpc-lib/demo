# 对账功能 SPEC

> 适用项目：Payment Demo V5
> 写作目的：把支付系统中的"渠道账单下载、解析、本地交易匹配、差异识别、对账结果持久化"写成可持续对账的活文档。
> 使用方式：涉及对账任务、渠道账单解析、差异处理、定时对账的设计型改动，先参考本 spec。

## 0. 元信息

- **状态**：planned
- **领域（feature-domain）**：bill
- **更新于**：2026-07-20
- **负责人**：<待填写>
- **关联 issue / PR / 任务**：新增对账功能
- **影响范围**：backend | database | redis | rabbitmq | external-provider | tests

## 1. 背景 / 目标

### 当前业务场景

当前 Payment Demo V5 已具备以下账单相关能力：

1. **微信支付账单下载**：通过 `WxPayBillService` 调用微信 V3 API 获取交易账单/资金账单的下载 URL，并下载原始账单内容（CSV 格式）。
2. **支付宝账单下载 URL 查询**：通过 `AliPayService.queryBill()` 获取支付宝账单下载 URL。
3. **订单维度退款对账**：通过 `RefundInfoController.reconcileOrderRefundStatus()` 对单个订单下的所有退款单进行状态同步。

但当前系统缺少**完整的对账闭环**：
- 没有对账批次（Reconciliation Batch）概念，无法按日/按渠道组织对账任务
- 没有渠道账单解析能力（下载的原始 CSV 未被结构化为可比对的交易明细）
- 没有本地交易与渠道账单的自动匹配/勾稽逻辑
- 没有差异识别（长款、短款、金额不一致、状态不一致）
- 没有差异处理流程（人工补录、状态修正、二次对账）
- 没有对账结果持久化和历史追溯
- 没有定时对账调度（需人工触发各渠道账单下载）

### 调用方与下游依赖

| 调用方 / 依赖 | 类型 | 说明 |
|---|---|---|
| React/Vue 前端 | UI | 对账列表页、对账详情页、差异处理页 |
| 微信支付 V3 API | 外部 | `tradebill` / `fundflowbill` 下载 |
| 支付宝开放平台 API | 外部 | `alipay.data.dataservice.bill.downloadurl.query` |
| MySQL / DM8 | 数据库 | 对账批次、对账明细、差异单持久化 |
| Redis | 缓存 | 对账任务幂等、分布式锁 |
| RabbitMQ | 消息队列 | 对账任务异步调度、延迟重试 |

### 本 spec 要锁住的行为

- 对账批次的创建、状态流转、幂等规则
- 渠道账单的解析规范（微信 CSV、支付宝 CSV）
- 本地交易与渠道交易的匹配规则
- 差异类型定义与识别逻辑
- 差异处理的状态流转与操作约束
- 定时对账的触发时机与失败重试

## 2. 当前现状（重构前必须承认）

### 2.1 入口清单

| 入口 | 类型 | 当前调用方 | 备注 |
|---|---|---|---|
| `GET /api/wx-pay/querybill/{billDate}/{type}` | REST | 人工/前端测试 | 获取微信账单下载 URL，返回 `R.data.downloadUrl` |
| `GET /api/wx-pay/downloadbill/{billDate}/{type}` | REST | 人工/前端测试 | 下载微信账单原始内容，返回 `R.data.result`（CSV 字符串） |
| `GET /api/ali-pay/bill/downloadurl/query/{billDate}/{type}` | REST | 人工/前端测试 | 获取支付宝账单下载 URL，返回 `R.data.downloadUrl` |
| `POST /api/refund-info/reconcile/{orderNo}` | REST | 管理页 | 单个订单的退款状态对账，不涉及渠道账单 |

### 2.2 现有实现锚点

| 层 | 文件 / 类 / 函数 | 现有职责 |
|---|---|---|
| Controller | `WxPayController.queryTradeBill()` / `downloadBill()` | 微信账单查询/下载入口，参数校验（billDate、type、billType、accountType、tarType） |
| Controller | `AliPayController.queryTradeBill()` | 支付宝账单下载 URL 查询入口，参数校验（billDate、type） |
| Controller | `RefundInfoController.reconcileOrderRefundStatus()` | 单订单退款状态对账入口 |
| Service | `WxPayBillService.queryBill()` / `downloadBill()` | 微信 V3 账单 API 调用：校验 billDate（近3个月、昨日10点后）、构造请求、获取 download_url 或直接下载 |
| Service | `AliPayServiceImpl.queryBill()` | 支付宝账单下载 URL API 调用：`AlipayDataDataserviceBillDownloadurlQueryRequest` |
| Service | `RefundApplicationServiceImpl.reconcileOrderRefundStatus()` | 遍历订单下所有退款单，逐个调用 `queryRefundStatus()` 同步渠道状态 |
| Facade | `WxPayBillFacade` | 微信账单服务门面接口 |
| Config | `PaymentConfigLoader` | 支付应用配置加载，对账时需按应用维度获取渠道凭证 |
| Entity | `OrderInfo` / `PaymentInfo` / `RefundInfo` | 本地交易数据来源（订单、支付流水、退款单） |

### 2.3 现有可疑行为

- **「现状」** 微信账单下载后原始 CSV 内容直接返回给前端，未做解析或持久化，对账完全依赖人工。
- **「现状」** `WxPayBillService.validateBillDate()` 限制只能申请近 3 个月内的账单，且昨日账单需 10:00 后生成；此限制仅在微信侧存在，支付宝侧无对应校验。
- **「现状」** 微信账单 `resolveWxPayConfig()` 使用默认微信应用配置，不支持按 `paymentAppId` 维度拉取多应用账单。
- **「现状」** 支付宝账单 `queryBill()` 同样使用默认配置，不支持多应用维度。
- **「现状」** 对账只存在于"单订单退款状态同步"层面，没有批次化对账，无法保证某一渠道/某一日所有交易都已对齐。
- **「现状」** 前端 `bill.js` 只封装了下载接口，没有对账列表、详情、差异处理的 API 封装。

## 3. 契约（对外行为，必须稳定）

### 3.1 输入 / 调用

#### HTTP 接口（新增）

| 方法 | 路径 | 说明 |
|---|---|---|
| `POST` | `/api/reconciliation/batch/create` | 创建对账批次（指定渠道、日期范围、支付应用） |
| `GET` | `/api/reconciliation/batch/list` | 对账批次列表（分页、按状态/渠道/日期筛选） |
| `GET` | `/api/reconciliation/batch/{batchNo}` | 对账批次详情（含统计汇总） |
| `POST` | `/api/reconciliation/batch/{batchNo}/execute` | 手动触发对账批次执行 |
| `GET` | `/api/reconciliation/detail/list/{batchNo}` | 对账明细列表（支持按匹配状态/差异类型筛选） |
| `GET` | `/api/reconciliation/discrepancy/list/{batchNo}` | 差异单列表（长款/短款/金额不一致等） |
| `POST` | `/api/reconciliation/discrepancy/{discrepancyId}/resolve` | 处理差异单（标记已解决、备注、关联操作） |

#### 定时任务输入

- **触发时机**：每日 10:30（Asia/Shanghai）自动发起昨日全渠道对账
- **幂等键**：`reconciliation:batch:{channelCode}:{billDate}:{paymentAppId 或 "all"}`
- **失败重试**：RabbitMQ 延迟队列，间隔 5min / 15min / 1h，最多 3 次

#### 外部回调输入

- 本功能为主动拉取模式，不依赖渠道回调
- 账单下载通过微信 V3 API 和支付宝开放平台 API 主动调用

### 3.2 输出 / 响应

#### 对账批次创建响应

```json
{
  "code": 0,
  "message": "成功",
  "data": {
    "batchNo": "RC20260719WXPAY001",
    "channelCode": "WXPAY",
    "paymentAppId": 1,
    "billDate": "2026-07-19",
    "status": "CREATED",
    "createTime": "2026-07-20T10:30:00"
  }
}
```

#### 对账批次统计汇总

```json
{
  "code": 0,
  "message": "成功",
  "data": {
    "batchNo": "RC20260719WXPAY001",
    "status": "COMPLETED",
    "channelTotalCount": 150,
    "channelTotalAmount": 15000,
    "localTotalCount": 148,
    "localTotalAmount": 14800,
    "matchedCount": 145,
    "matchedAmount": 14500,
    "discrepancyCount": 8,
    "overpaymentCount": 2,
    "underpaymentCount": 1,
    "amountMismatchCount": 3,
    "statusMismatchCount": 2
  }
}
```

#### 对账明细结构

| 字段 | 类型 | 说明 |
|---|---|---|
| `detailId` | Long | 对账明细 ID |
| `batchNo` | String | 对账批次号 |
| `orderNo` | String | 本地订单号（可空，纯渠道侧无匹配时） |
| `transactionId` | String | 渠道交易号（可空，纯本地侧无匹配时） |
| `tradeType` | String | 交易类型（支付/退款） |
| `channelAmount` | Integer | 渠道金额（分） |
| `localAmount` | Integer | 本地金额（分） |
| `channelStatus` | String | 渠道交易状态 |
| `localStatus` | String | 本地交易状态 |
| `matchStatus` | String | 匹配状态：MATCHED / MISMATCH / CHANNEL_ONLY / LOCAL_ONLY |
| `discrepancyType` | String | 差异类型：OVERPAYMENT / UNDERPAYMENT / AMOUNT_MISMATCH / STATUS_MISMATCH |
| `tradeTime` | LocalDateTime | 交易时间 |

#### 数据库变化（新增表）

- `t_reconciliation_batch`：对账批次表
- `t_reconciliation_detail`：对账明细表
- `t_reconciliation_discrepancy`：对账差异单表

### 3.3 状态迁移

#### 对账批次状态

| 起始状态 | 触发 | 目标状态 | 约束 |
|---|---|---|---|
| `CREATED` | 账单下载成功 | `BILL_DOWNLOADED` | 账单文件已解析并持久化到渠道侧明细 |
| `BILL_DOWNLOADED` | 本地交易归集完成 | `LOCAL_COLLECTED` | 本地订单/支付/退款数据已提取 |
| `LOCAL_COLLECTED` | 匹配算法执行完成 | `MATCHED` | 所有明细已打标 matchStatus |
| `MATCHED` | 有未处理差异 | `DISCREPANCY_PENDING` | discrepancyCount > 0 且未全部处理 |
| `DISCREPANCY_PENDING` | 所有差异处理完成 | `RESOLVED` | 所有差异单 status = RESOLVED |
| `MATCHED` / `RESOLVED` | 批次结束 | `COMPLETED` | 终态 |
| 任意非终态 | 执行失败且重试耗尽 | `FAILED` | 记录失败原因，支持人工重跑 |
| `FAILED` | 人工重跑 | `CREATED` | 重置状态，保留历史明细需归档 |

#### 差异单状态

| 起始状态 | 触发 | 目标状态 | 约束 |
|---|---|---|---|
| `OPEN` | 人工处理并填写处理说明 | `RESOLVED` | 必填 resolveRemark |
| `OPEN` | 二次对账后差异消失 | `AUTO_RESOLVED` | 系统自动标记 |
| `OPEN` / `RESOLVED` | 人工驳回，要求重新处理 | `OPEN` | 仅管理员可操作 |

### 3.4 不变量 / 边界

- 同一 `channelCode + billDate + paymentAppId` 只能有一个非终态对账批次（`CREATED` ~ `DISCREPANCY_PENDING`）。
- 对账批次创建后，渠道账单下载和本地交易归集必须在**同一日期维度**内完成，不允许跨日混合。
- 差异单一旦 `RESOLVED`，对账统计中的 `discrepancyCount` 仍保留（用于审计），但批次状态可推进至 `COMPLETED`。
- 对账明细中的金额单位统一为**分**，与现有 `OrderInfo.totalFee` / `PaymentInfo.payer_total` 保持一致。
- 定时对账仅处理**昨日及之前**的日期，不处理当日（与微信账单 10:00 后生成的约束一致）。
- 对账操作的审计日志必须包含操作人、操作时间、变更前后状态。

## 4. 隐式输入输出 / 状态地图

| 类型 | 名称 / 位置 | 读 | 写 | TTL / 生命周期 | 备注 |
|---|---|---|---|---|---|
| DB | `t_reconciliation_batch` | 列表查询、详情查询 | 创建、状态更新 | 持久 | 唯一约束 `uk_batch_channel_date_app` |
| DB | `t_reconciliation_detail` | 明细列表、差异筛选 | 批量插入、更新 matchStatus | 持久 | 索引 `idx_batch_match_status` |
| DB | `t_reconciliation_discrepancy` | 差异列表 | 创建、状态更新、备注写入 | 持久 | 关联 detailId |
| DB | `t_order_info` | 本地交易归集 | 不写 | 持久 | 按 payment_channel_code + create_time 范围查询 |
| DB | `t_payment_info` | 本地支付流水归集 | 不写 | 持久 | 按 payment_type + create_time 范围查询 |
| DB | `t_refund_info` | 本地退款单归集 | 不写 | 持久 | 关联订单范围查询 |
| DB | `t_payment_app` | 获取支付应用配置 | 不写 | 持久 | 对账凭证来源 |
| Redis | `reconciliation:batch:lock:{batchNo}` | 分布式锁 | 分布式锁 | 锁生命周期 | 防止并发执行同一批次 |
| Redis | `reconciliation:batch:idempotent:{key}` | 幂等检查 | 幂等标记 | 24h | 防止重复创建同一批次 |
| RabbitMQ | `reconciliation.execute.queue` | 消费者执行对账 | 生产者投递任务 | 立即消费 | 异步对账执行 |
| RabbitMQ | `reconciliation.retry.delay.queue` | 消费者重试 | 失败投递 | TTL 递增 | 失败重试延迟队列 |
| Config | `application.yml` | 对账调度 cron、重试次数 | 人工 | 启动期 | `reconciliation.schedule.cron` |
| Log | `reconciliation.logger` | 审计/排障 | 对账全过程 | 日志保留策略 | 含批次号、差异统计 |

## 5. 多套实现 / 多套规则并存

- **微信支付 V2 与 V3 并存**：V2 账单 API 与 V3 不同，本期仅支持 V3 账单下载，V2 账单留待后续扩展；接口层通过 `channelCode=WXPAY` 统一入口，内部根据 `paymentApp` 配置的 API 版本分发。
- **微信支付与支付宝并存**：两者账单格式不同（微信为 CSV 表头 `交易时间,公众账号ID,商户号,...`，支付宝为 CSV 含多行表头和汇总行），需分别实现解析器。
- **多支付应用并存**：对账支持按单个 `paymentAppId` 执行，也支持按渠道全量应用执行（逐个应用拉取账单后合并）。
- **手动触发与定时触发并存**：手动触发用于补跑历史日期或重试失败批次，定时触发用于日常自动化对账。
- **现有账单下载接口保留**：`/api/wx-pay/querybill`、`/api/wx-pay/downloadbill`、`/api/ali-pay/bill/downloadurl/query` 作为底层能力保留，对账功能内部复用这些能力。

## 6. 验收标准

- [ ] 创建对账批次时，同一 `channelCode + billDate + paymentAppId` 的重复创建被幂等拒绝，返回已存在批次号。
- [ ] 微信 V3 交易账单（tradebill）CSV 解析后，每笔交易能正确提取 `transaction_id`、`out_trade_no`、`amount`、`trade_state`、`trade_time`。
- [ ] 支付宝账单 CSV 解析后，能跳过表头和汇总行，正确提取每笔交易的 `商户订单号`、`支付宝交易号`、`金额`、`交易状态`、`交易创建时间`。
- [ ] 本地交易归机能按 `billDate`（基于 `create_time` 或交易时间）正确筛选当日的订单、支付流水、退款单。
- [ ] 匹配算法：
  - `orderNo + transactionId + amount` 完全一致 → `MATCHED`
  - 渠道有、本地无 → `CHANNEL_ONLY`（长款）
  - 本地有、渠道无 → `LOCAL_ONLY`（短款）
  - `orderNo/transactionId` 匹配但 `amount` 不一致 → `AMOUNT_MISMATCH`
  - `orderNo/transactionId` 匹配但 `status` 不一致 → `STATUS_MISMATCH`
- [ ] 对账批次完成后，统计汇总数据（各 count/amount）与明细数据一致。
- [ ] 差异单处理流程：标记 `RESOLVED` 后不可再被普通用户重新打开，仅管理员可驳回。
- [ ] 定时任务在每日 10:30 自动创建昨日所有启用支付应用的对账批次并执行。
- [ ] 对账执行失败时，通过 RabbitMQ 延迟重试最多 3 次（5min / 15min / 1h），仍失败则批次状态置为 `FAILED` 并记录失败原因。
- [ ] 所有新接口返回 `R<T>` 结构，与现有公共 API 契约一致。
- [ ] React 和 Vue 前端的 `bill.js` API 封装同步扩展对账接口。

## 7. 特征测试清单

### 7.1 第一批必须锁住

- [ ] 对账批次创建：重复创建幂等、参数校验（billDate 格式、渠道有效性、支付应用存在性）。
- [ ] 微信 V3 账单 CSV 解析：表头识别、字段提取、空值/异常行处理、汇总行跳过。
- [ ] 支付宝账单 CSV 解析：多行表头跳过、汇总行识别、交易明细提取。
- [ ] 匹配算法：5 种 matchStatus 分支各至少 1 条用例。
- [ ] 对账批次状态流转：正常路径（CREATED → ... → COMPLETED）与失败路径（FAILED → CREATED 重跑）。
- [ ] 差异单处理：RESOLVED / AUTO_RESOLVED / 管理员驳回。
- [ ] 定时任务幂等：同一日期重复触发不创建重复批次。
- [ ] 分布式锁：并发执行同一批次时只有一个线程实际执行。

### 7.2 测试锚点（implemented 时填写）

| 测试类型 | 文件 | 覆盖行为 |
|---|---|---|
| 单元测试 | `<待创建>` | 账单解析器、匹配算法 |
| 单元测试 | `<待创建>` | 批次状态机、差异状态机 |
| Web/API 测试 | `<待创建>` | 对账批次 CRUD、差异处理接口契约 |
| 集成测试 | `<待创建>` | 完整对账流程（mock 渠道账单 + 真实 DB） |
| 集成测试 | `<待创建>` | 定时任务调度 + 幂等 + 重试 |

## 8. 兼容性影响

- **HTTP 路径**：全部为新增接口（`/api/reconciliation/**`），不修改现有路径；现有账单下载接口完全保留。
- **响应结构**：统一使用 `R<T>`，与现有公共 API 契约一致。
- **数据库结构**：新增 3 张表（`t_reconciliation_batch`、`t_reconciliation_detail`、`t_reconciliation_discrepancy`），不修改现有表结构；需同时提供完整初始化脚本和增量升级脚本。
- **状态值**：新增对账批次状态枚举、差异类型枚举，不修改现有 `OrderStatus`、`RefundStatus` 等已有状态。
- **配置字段**：新增 `reconciliation.schedule.cron`、`reconciliation.retry.max-attempts` 等配置项，不修改现有配置。
- **前端影响**：React 和 Vue 均需新增对账页面和 API 封装（`bill.js` 扩展）。
- **第三方平台配置**：不新增对微信/支付宝的回调依赖，仅使用已有账单下载 API 权限。

## 9. 实现锚点（implemented 时必须填写）

- Controller：[ReconciliationController.java](file:///d:/code/demo/springboot-payment/payment-demo-java-dm/payment-demo/src/main/java/cc/ivera/controller/ReconciliationController.java)
- Service：[ReconciliationBatchService.java](file:///d:/code/demo/springboot-payment/payment-demo-java-dm/payment-demo/src/main/java/cc/ivera/service/reconciliation/ReconciliationBatchService.java) / [ReconciliationBatchServiceImpl.java](file:///d:/code/demo/springboot-payment/payment-demo-java-dm/payment-demo/src/main/java/cc/ivera/service/reconciliation/ReconciliationBatchServiceImpl.java)、[ReconciliationMatchService.java](file:///d:/code/demo/springboot-payment/payment-demo-java-dm/payment-demo/src/main/java/cc/ivera/service/reconciliation/ReconciliationMatchService.java) / [ReconciliationMatchServiceImpl.java](file:///d:/code/demo/springboot-payment/payment-demo-java-dm/payment-demo/src/main/java/cc/ivera/service/reconciliation/ReconciliationMatchServiceImpl.java)
- 账单解析器：[WxPayBillParser.java](file:///d:/code/demo/springboot-payment/payment-demo-java-dm/payment-demo/src/main/java/cc/ivera/service/reconciliation/bill/WxPayBillParser.java)、[AliPayBillParser.java](file:///d:/code/demo/springboot-payment/payment-demo-java-dm/payment-demo/src/main/java/cc/ivera/service/reconciliation/bill/AliPayBillParser.java)
- 调度器：[ReconciliationScheduler.java](file:///d:/code/demo/springboot-payment/payment-demo-java-dm/payment-demo/src/main/java/cc/ivera/job/ReconciliationScheduler.java)
- Mapper / SQL：[ReconciliationBatchMapper.xml](file:///d:/code/demo/springboot-payment/payment-demo-java-dm/payment-demo/src/main/resources/mapper/reconciliation/ReconciliationBatchMapper.xml)、[ReconciliationDetailMapper.xml](file:///d:/code/demo/springboot-payment/payment-demo-java-dm/payment-demo/src/main/resources/mapper/reconciliation/ReconciliationDetailMapper.xml)、[ReconciliationDiscrepancyMapper.xml](file:///d:/code/demo/springboot-payment/payment-demo-java-dm/payment-demo/src/main/resources/mapper/reconciliation/ReconciliationDiscrepancyMapper.xml)
- DDL：[002_reconciliation_tables_dm8.sql](file:///d:/code/demo/springboot-payment/payment-demo-java-dm/payment-demo/env/sql/dm8/002_reconciliation_tables_dm8.sql)
- Entity / DTO / Enum：
  - Entity: [ReconciliationBatch.java](file:///d:/code/demo/springboot-payment/payment-demo-java-dm/payment-demo/src/main/java/cc/ivera/entity/reconciliation/ReconciliationBatch.java)、[ReconciliationDetail.java](file:///d:/code/demo/springboot-payment/payment-demo-java-dm/payment-demo/src/main/java/cc/ivera/entity/reconciliation/ReconciliationDetail.java)、[ReconciliationDiscrepancy.java](file:///d:/code/demo/springboot-payment/payment-demo-java-dm/payment-demo/src/main/java/cc/ivera/entity/reconciliation/ReconciliationDiscrepancy.java)
  - Enum: [BatchStatus.java](file:///d:/code/demo/springboot-payment/payment-demo-java-dm/payment-demo/src/main/java/cc/ivera/enums/reconciliation/BatchStatus.java)、[MatchStatus.java](file:///d:/code/demo/springboot-payment/payment-demo-java-dm/payment-demo/src/main/java/cc/ivera/enums/reconciliation/MatchStatus.java)、[DiscrepancyType.java](file:///d:/code/demo/springboot-payment/payment-demo-java-dm/payment-demo/src/main/java/cc/ivera/enums/reconciliation/DiscrepancyType.java)、[DiscrepancyStatus.java](file:///d:/code/demo/springboot-payment/payment-demo-java-dm/payment-demo/src/main/java/cc/ivera/enums/reconciliation/DiscrepancyStatus.java)
- Frontend：
  - React: [bill.js](file:///d:/code/demo/springboot-payment/payment-demo-java-dm/payment-demo-react/src/api/bill.js)
  - Vue: [bill.js](file:///d:/code/demo/springboot-payment/payment-demo-java-dm/payment-demo-vue/src/api/bill.js)
- Config / MQ / Cache：
  - application.yml 对账配置：[application.yml](file:///d:/code/demo/springboot-payment/payment-demo-java-dm/payment-demo/src/main/resources/application.yml)
  - RabbitMQ：[ReconciliationRabbitConfig.java](file:///d:/code/demo/springboot-payment/payment-demo-java-dm/payment-demo/src/main/java/cc/ivera/config/ReconciliationRabbitConfig.java)
  - MQ 消息：[ReconciliationExecuteMessage.java](file:///d:/code/demo/springboot-payment/payment-demo-java-dm/payment-demo/src/main/java/cc/ivera/mq/ReconciliationExecuteMessage.java)
  - MQ 生产者：[ReconciliationExecuteProducer.java](file:///d:/code/demo/springboot-payment/payment-demo-java-dm/payment-demo/src/main/java/cc/ivera/mq/ReconciliationExecuteProducer.java)
  - MQ 消费者：[ReconciliationExecuteConsumer.java](file:///d:/code/demo/springboot-payment/payment-demo-java-dm/payment-demo/src/main/java/cc/ivera/mq/ReconciliationExecuteConsumer.java)
- Tests：`<待创建>`
- Docs / Examples：`<待创建>`

## 10. 回滚 / 观测

- **回滚方式**：
  - 代码回滚：功能开关 `reconciliation.feature.enabled` 控制，关闭后新接口返回 503 并提示"功能暂未开放"，旧接口不受影响。
  - 数据库回滚：新增表可直接 DROP，不影响现有业务数据。
  - 配置回滚：删除 `reconciliation.*` 配置项即可。
- **需要观察的日志**：
  - `reconciliation.batch.created`：批次创建日志（含批次号、渠道、日期）
  - `reconciliation.bill.downloaded`：账单下载成功/失败日志
  - `reconciliation.match.completed`：匹配完成日志（含统计汇总）
  - `reconciliation.discrepancy.resolved`：差异处理日志（含操作人、差异 ID）
- **需要观察的数据**：
  - 每日各渠道对账批次状态分布（CREATED / COMPLETED / FAILED）
  - 差异数量趋势（长款、短款、金额不一致）
  - 对账执行耗时（P50 / P95）
- **失败时对外表现**：
  - 手动触发：接口返回 `R.error()` 并携带失败原因
  - 定时任务：静默失败 + 重试 + 告警日志，不影响前端用户
  - 渠道 API 不可用：批次状态置为 `FAILED`，记录原因，支持人工重跑

## 11. 变更记录

| 日期 | 状态 | 改了什么 | 关联 issue / PR |
|---|---|---|---|
| 2026-07-20 | planned | 初稿，定义对账功能的完整契约、状态机、数据模型、验收标准 | 新增对账功能任务 |
| 2026-07-20 | planned(partially implemented) | 完成后端核心实现：DDL、枚举、Entity、Mapper、账单解析器、Service、RabbitMQ、调度器、Controller、前端 API 扩展；编译通过，待补充测试 | 新增对账功能任务 |
