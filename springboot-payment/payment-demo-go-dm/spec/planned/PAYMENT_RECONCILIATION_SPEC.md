# 支付对账功能 SPEC

> 适用项目：Payment Demo V5
> 写作目的：把支付系统中的"渠道账单下载、本地订单/支付/退款数据与渠道数据对账、差异识别与处理、对账结果持久化"写成可持续对账的活文档。
> 使用方式：涉及对账流程、对账状态、对账任务、差异处理的设计型改动，都先复制本模板创建一条 spec。

## 0. 元信息

- **状态**：planned
- **领域（feature-domain）**：bill | reconciliation | backend | database | frontend
- **更新于**：2026-07-20
- **负责人**：待确认
- **关联 issue / PR / 任务**：待创建
- **影响范围**：backend | react-frontend | vue-frontend | database | redis | rabbitmq | docs | tests

## 1. 背景 / 目标

当前支付系统已经具备：

- 微信 V3 账单下载能力（`/v3/bill/tradebill`、`/v3/bill/fundflowbill`）
- 支付宝账单下载能力（`alipay.data.dataservice.bill.downloadurl.query`）
- 订单退款状态对账接口（`POST /api/refund-info/reconcile/:orderNo`）
- 订单、支付流水、退款单的本地持久化
- 支付渠道与支付应用的配置管理

**缺口**：

1. 没有统一的对账任务管理，无法按日/按渠道发起对账任务
2. 没有对账结果没有持久化，无法追溯历史对账记录
3. 差异数据没有结构化存储，无法跟踪差异处理状态
4. 没有自动对账调度能力，依赖人工触发
5. 没有对账仪表盘，无法一目了然看到各渠道对账状态
6. 现有退款对账仅针对单个订单，不支持批量和渠道账单级别的对账

**本次 spec 要锁住的行为**：

- 对账任务的创建、执行、状态流转
- 渠道对账单据的下载、解析、持久化
- 本地数据与渠道数据的匹配规则
- 差异类型定义与差异处理流程
- 对账结果的查询与展示

## 2. 当前现状（重构前必须承认）

### 2.1 入口清单

| 入口 | 类型 | 当前调用方 | 备注 |
|---|---|---|---|
| `GET /api/wx-pay/querybill/:billDate/:type` | REST | React/Vue 前端 | 查询微信账单下载 URL，返回 downloadUrl |
| `GET /api/wx-pay/downloadbill/:billDate/:type` | REST | React/Vue 前端 | 下载并返回微信账单原文 |
| `GET /api/ali-pay/bill/downloadurl/query/:billDate/:type` | REST | React/Vue 前端 | 查询支付宝账单下载 URL |
| `POST /api/refund-info/reconcile/:orderNo` | REST | React/Vue 前端 | 单个订单的退款状态对账，刷新订单退款汇总状态 |
| `GET /api/wx-pay/check-order-status/:orderNo` | REST | React/Vue 前端 | 微信订单支付状态查询与本地同步 |
| `GET /api/ali-pay/trade/query/:orderNo` | REST | React/Vue 前端 | 支付宝订单支付状态查询 |

### 2.2 现有实现锚点

| 层 | 文件 / 类 / 函数 | 现有职责 |
|---|---|---|
| Controller | `handler.go::wxQueryBill` | 微信账单查询 URL |
| Controller | `handler.go::wxDownloadBill` | 微信账单下载 |
| Controller | `handler.go::aliQueryBill` | 支付宝账单下载 URL 查询 |
| Controller | `handler.go::refundReconcile` | 订单退款状态对账入口 |
| Service | `wx_pay.go::WxQueryBill` | 调用微信 V3  tradebill/fundflowbill API |
| Service | `wx_pay.go::WxDownloadBill` | 下载微信账单文件 |
| Service | `ali_pay.go::AliQueryBill` | 调用支付宝账单下载 URL API |
| Service | `refund_status.go::RefreshOrderRefundStatus` | 订单退款状态刷新 |
| Service | `refund_info.go::ReconcileOrderRefundStatus` | 订单退款对账实现 |
| Model | `models.go::OrderInfo` | 订单模型 |
| Model | `models.go::PaymentInfo` | 支付流水模型 |
| Model | `models.go::RefundInfo` | 退款单模型 |
| Frontend | `bill.js` | React/Vue 账单 API 封装 |

### 2.3 现有可疑行为

- 微信账单查询使用全局默认配置，不支持按 `paymentAppId` 选择配置
- 支付宝账单查询使用全局默认配置，不支持按 `paymentAppId` 选择配置
- 账单下载结果不做持久化，每次都重新下载
- 退款对账接口 `refundReconcile` 的实现需要确认是否存在于 service 层

## 3. 契约（对外行为，必须稳定）

### 3.1 输入 / 调用

**HTTP 接口（新增）**：

- `POST /api/reconciliation/task`：创建对账任务
  - 请求参数（body）：
    ```json
    {
      "paymentType": "微信|支付宝",
      "paymentAppId": 1,
      "billDate": "2026-07-19",
      "billType": "trade|refund|all"
    }
    ```
- `GET /api/reconciliation/task/list`：查询对账任务列表
  - 请求参数（query）：paymentType、paymentAppId、billDate、status、page、pageSize
- `GET /api/reconciliation/task/:taskId`：查询对账任务详情
- `POST /api/reconciliation/task/:taskId/execute`：手动触发对账任务执行
- `GET /api/reconciliation/task/:taskId/diff`：查询对账差异列表
  - 请求参数（query）：diffType、handled、page、pageSize
- `POST /api/reconciliation/diff/:diffId/handle`：处理对账差异
  - 请求参数（body）：
    ```json
    {
      "handleType": "补录|标记已退款|忽略|人工处理",
      "remark": "处理备注"
    }
    ```
- `GET /api/reconciliation/summary`：对账总览（仪表盘数据
  - 请求参数（query）：billDate

**消息输入（新增）**：

- RabbitMQ 对账任务调度队列：`payment.reconciliation.task.queue`
  - 消息 JSON：`{"taskId": 1, "trigger": "manual|scheduled"}`

**前端入口**：

- React 路由：`/reconciliation`（对账中心）
- Vue 路由：`/reconciliation`（对账中心）
- API module：`src/api/reconciliation.js`

### 3.2 输出 / 响应

**成功响应**：统一使用 `R<T>` 结构

- 对账任务创建成功：返回任务 ID、状态
- 对账任务列表：分页列表，含任务状态、进度、差异统计
- 对账任务详情：任务基本信息 + 统计数据（总笔数、匹配笔数、差异笔数）
- 对账差异列表：分页的差异明细，含差异类型、本地数据、渠道数据、处理状态
- 对账总览：各渠道当日对账状态、差异笔数、待处理笔数

**失败响应**：

- 参数校验失败：`code != 0`，`message` 说明原因
- 渠道账单下载失败：返回渠道错误信息
- 对账任务执行失败：记录失败原因

**数据库变化（新增表）**：

- `t_reconciliation_task`：对账任务表（插入、更新）
- `t_reconciliation_detail`：对账明细表（插入）
- `t_reconciliation_diff`：对账差异表（插入、更新）

**事件输出**：

- RabbitMQ 对账任务完成事件：`payment.reconciliation.task.complete.exchange`
  - 消息 JSON：`{"taskId": 1, "status": "success|failed", "diffCount": 10}`

**外部调用**：

- 微信 V3 账单 API（tradebill / fundflowbill）
- 支付宝账单下载 URL API
- 微信/支付宝订单查询 API（差异补查用）

### 3.3 状态迁移

#### 对账任务状态

| 起始状态 | 触发 | 目标状态 | 当前规则 / 约束 |
|---|---|---|---|
| `PENDING`（待执行） | 创建任务 | `PENDING` | 初始状态，等待调度或手动触发 |
| `PENDING` | 开始执行 | `PROCESSING` | 加锁防止并发执行 |
| `PROCESSING` | 下载账单成功，开始解析匹配 | `PROCESSING` | 子状态：下载中→解析中→匹配中 |
| `PROCESSING` | 全部处理完成 | `COMPLETED` | 所有数据处理完毕，无错误 |
| `PROCESSING` | 渠道账单下载失败或解析失败 | `FAILED` | 记录失败原因，支持重试 |
| `PROCESSING` | 部分处理完成但有警告 | `COMPLETED_WITH_WARNING` | 例如部分数据缺失但主流程完成 |
| `FAILED` | 手动重试 | `PROCESSING` | 重新执行对账 |
| `COMPLETED` / `COMPLETED_WITH_WARNING` | 重新执行 | `PROCESSING` | 允许重新对账，覆盖旧结果 |

#### 对账差异处理状态

| 起始状态 | 触发 | 目标状态 | 当前规则 / 约束 |
|---|---|---|---|
| `PENDING`（待处理） | 标记忽略 | `IGNORED` | 记录操作人和备注 |
| `PENDING` | 补录本地数据 | `HANDLED` | 触发补录流程后自动标记 |
| `PENDING` | 人工处理 | `HANDLED` | 标记为人工已处理 |
| `PENDING` | 下次对账自动匹配 | `RESOLVED` | 差异被后续对账自动消除 |

### 3.4 不变量 / 边界

- 同一 `paymentType + paymentAppId + billDate + billType` 唯一约束，重复创建返回已有任务
- 对账任务按 `paymentAppId` 选择对应的渠道配置，不再使用全局默认配置
- 对账任务执行期间加分布式锁，防止同一任务并发执行
- 本地订单金额单位为分，渠道账单金额单位可能为元或分，解析时统一转换
- 差异处理时必须区分：金额不一致、状态不一致、本地有渠道无、渠道有本地无
- 对账结果至少保留 90 天

## 4. 隐式输入输出 / 状态地图

| 类型 | 名称 / 位置 | 读 | 写 | TTL / 生命周期 | 备注 |
|---|---|---|---|---|---|
| DB | `t_reconciliation_task` | 前端查询 | 任务创建/执行 | 持久 | 对账任务主表 |
| DB | `t_reconciliation_detail` | 差异追溯 | 对账执行 | 持久 | 对账明细匹配结果 |
| DB | `t_reconciliation_diff` | 前端处理 | 对账执行/人工处理 | 持久 | 差异记录 |
| DB | `t_order_info` | 对账匹配 | 差异补录 | 持久 | 本地订单 |
| DB | `t_payment_info` | 对账匹配 | 差异补录 | 持久 | 本地支付流水 |
| DB | `t_refund_info` | 对账匹配 | 差异补录 | 持久 | 本地退款单 |
| DB | `t_payment_app` | 选配置 | - | 持久 | 支付应用配置 |
| Redis | `payment:reconciliation:lock:{taskId}` | 锁检查 | 加锁 | 30min | 任务执行锁 |
| Redis | `payment:reconciliation:progress:{taskId}` | 进度查询 | 进度更新 | 24h | 任务执行进度 |
| RabbitMQ | `payment.reconciliation.task.queue` | 消费者 | 调度器/手动触发 | 无 TTL | 对账任务执行队列 |
| RabbitMQ | `payment.reconciliation.task.complete.exchange` | 监听者 | 任务完成 | 无 TTL | 对账完成事件 |
| Config | `application.yml` 对账调度配置 | 调度器 | manual | 启动期 | 调度开关、cron |
| Log | `reconciliation.*` | n/a | 对账服务 | 日志保留策略 | 对账执行日志 |

## 5. 多套实现 / 多套规则并存

- 微信支付：V3 账单 API（tradebill / fundflowbill）；V2 无账单 API 暂不支持
- 支付宝：`alipay.data.dataservice.bill.downloadurl.query`
- 账单类型：交易账单（trade）、退款账单（refund），综合对账需要同时下载两种账单
- 前端：React 与 Vue 都需要保持兼容，新增 `reconciliation.js API module
- 幂等规则：任务唯一索引 + Redis 分布式锁 + 状态条件更新
- 配置选择：按 `paymentAppId` 选择配置，不存在时回退到渠道默认配置

## 6. 验收标准

- [ ] 对账任务创建：同一 `paymentType + paymentAppId + billDate + billType` 重复创建返回已有任务
- [ ] 对账任务执行：微信/支付宝账单下载成功，解析后与本地数据匹配
- [ ] 差异识别：正确识别 4 类差异（金额不一致、状态不一致、本地有渠道无、渠道有本地无
- [ ] 差异处理：补录、忽略、人工处理三种方式正确更新差异状态
- [ ] 对账总览：按日期返回各渠道对账状态和差异统计
- [ ] 幂等：同一任务并发执行被 Redis 锁保护
- [ ] React 和 Vue 前端都能调用对账 API
- [ ] `cd payment-demo-go && go test ./...` 通过

## 7. 特征测试清单

### 7.1 第一批必须锁住

- [x] API 契约：对账任务创建、列表、详情、差异查询、差异处理的成功/失败响应
- [x] 状态迁移：对账任务 PENDING -> PROCESSING -> COMPLETED/FAILED
- [x] 幂等：重复创建同一条件的对账任务、并发执行同一任务
- [x] 配置选择：指定 paymentAppId、默认配置、禁用配置
- [x] 账单解析：微信 CSV 账单、支付宝 CSV 账单解析
- [x] 差异匹配：订单号匹配、金额比对、状态比对
- [ ] 差异处理：补录触发本地数据修正、忽略标记、人工处理标记（需集成测试）

### 7.2 测试锚点

| 测试类型 | 文件 | 覆盖行为 |
|---|---|---|
| 单元测试 | `internal/service/reconciliation_characterization_test.go` | 对账匹配规则、状态机、金额转换、账单解析、请求校验 |
| Web/API 测试 | `internal/handler/reconciliation_characterization_test.go` | 对账 API 路由注册、参数校验失败响应 |
| 集成测试 | 待补充 | 账单下载、解析、匹配全流程 |
| 前端测试 | 待补充 | React/Vue reconciliation API wrapper |

## 8. 兼容性影响

- **HTTP 路径**：新增 `/api/reconciliation/*` 路径，不修改现有路径
- **响应结构**：新增对账相关响应结构，不修改现有 `R<T>` 格式
- **数据库结构**：新增 3 张表（t_reconciliation_task / t_reconciliation_detail / t_reconciliation_diff），不修改现有表结构
- **状态值**：新增对账任务状态和差异处理状态枚举，不修改现有订单/退款状态
- **配置字段**：新增对账调度配置项（reconciliation.scheduler.enabled、reconciliation.scheduler.cron）
- **前端影响**：React/Vue 都要同步新增对账页面和 API 封装
- **第三方平台配置**：不影响微信/支付宝回调地址、证书配置

## 9. 实现锚点（implemented 时必须填写）

- Controller：`internal/handler/handler.go`（Register 方法中 `/api/reconciliation/*` 路由组）
- Service：
  - `internal/service/reconciliation_task.go`：对账任务管理（创建、查询、执行、总览、MQ 消费）
  - `internal/service/reconciliation_parser.go`：微信/支付宝账单下载与解析
  - `internal/service/reconciliation_matcher.go`：本地与渠道数据匹配、差异识别
  - `internal/service/reconciliation.go`：差异处理（补录、忽略、人工处理）
- Mapper / SQL：`sql/payment_demo.sql`（新增 3 张表 DDL + 索引）
- Entity / DTO / Enum：
  - `internal/model/models.go`（ReconciliationTask / ReconciliationDetail / ReconciliationDiff + DTO）
  - `internal/constant/enums.go`（对账状态、差异类型、处理类型枚举）
- Frontend：
  - `payment-demo-react/src/api/reconciliation.js`
  - `payment-demo-vue/src/api/reconciliation.js`
- Config / MQ / Cache：
  - `internal/config/config.go`（ReconciliationConfig）
  - `internal/mq/order_close.go`（对账任务队列声明、发送、消费）
  - `cmd/server/main.go`（注册对账任务消费者）
- Pay Client：`internal/pay/alipay.go`（新增 DownloadBill 方法）
- Tests：待补充特征测试
- Docs / Examples：待补充

## 10. 回滚 / 观测

- **回滚方式**：
  - 代码回滚：对账功能为新增，回滚不影响现有支付/退款流程
  - 配置回滚：关闭对账调度开关（reconciliation.scheduler.enabled=false）
  - DB 回滚：新增表不影响现有表，可直接删除
- **需要观察的日志**：
  - `reconciliation.task.*`：任务执行日志
  - `reconciliation.parser.*`：账单解析日志
  - `reconciliation.diff.*`：差异处理日志
- **需要观察的数据**：
  - `t_reconciliation_task` 任务状态分布
  - `t_reconciliation_diff` 待处理差异数量
  - Redis 对账任务锁 key
  - RabbitMQ 对账任务队列堆积
- **失败时对外表现**：
  - 对账任务标记为 FAILED，前端展示失败原因
  - 不影响现有支付、退款、订单等核心流程
  - 渠道账单下载失败不影响本地数据

## 11. 变更记录

| 日期 | 状态 | 改了什么 | 关联 issue / PR |
|---|---|---|---|
| 2026-07-20 | planned | 初稿，对账功能整体设计 | 待创建 |
| 2026-07-20 | planned | 完成对账功能后端实现：3 张表 DDL、模型、常量、配置、MQ 队列、4 个 Service 文件、7 个 API、支付宝 DownloadBill 方法 | 待创建 |
| 2026-07-20 | planned | 修复 spec 第 2.3 节现有可疑行为：微信/支付宝账单查询支持 paymentAppId 参数选择配置 | 待创建 |
| 2026-07-20 | planned | 完成前端 API 封装（React + Vue reconciliation.js） | 待创建 |
