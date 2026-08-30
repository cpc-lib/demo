# SPEC-006: 渠道账单导入与对账管理

> 渠道账单（微信/支付宝）先导入系统落库作为对账依据，再基于已导入账单执行对账管理。

---

## 元信息

| 字段 | 值 |
|------|------|
| **Spec ID** | SPEC-006 |
| **标题** | 渠道账单导入与对账管理 |
| **状态** | implemented |
| **版本** | 0.2.0 |
| **创建日期** | 2026-08-28 |
| **最后更新** | 2026-08-29 |
| **依赖** | SPEC-005 已实现 |
| **分类** | type: design-change（数据流变更：账单由"对账时临时下载"改为"先导入落库、对账消费已导入账单"） |
| **优先级** | 高 |
| **领域** | bill / reconciliation |
| **影响范围** | backend, database, react-frontend, vue-frontend, schedule, docs, tests |

---

## 1. 背景 / 目标

### 1.1 背景（现状问题）

SPEC-005 已实现对账，但存在以下问题：

1. **账单不落库**：`ReconciliationServiceImpl.doReconcile()` 执行对账时临时调渠道 API 下载账单，解析后只保留 SHA-256 哈希，原文即弃。账单无法作为对账依据进行管理、查看、审计和补录。
2. **T+1 约束与定时任务冲突**：微信账单 T+1 出账（昨日账单次日 10:00 后生成，见 `WxPayBillService.validateBillDate()`），但定时任务配置为凌晨 2:00 执行昨日对账（`application.yml` 中 `payment.reconciliation.cron: 0 0 2 * * ?`），微信自动对账每天必然失败。
3. **cron 配置缺陷**：微信/支付宝两个定时任务共用同一个 `payment.reconciliation.cron` 配置项，无法按各渠道账单生成时间分别调度。
4. **无手动补录入口**：渠道 API 拉取失败或补导历史账单时，没有手动上传账单文件的途径。

### 1.2 目标

- 新增**渠道账单管理模块**：支持**自动拉取**（调渠道 API 下载）和**手动上传**（上传微信商户平台 CSV/TXT/XLSX）两种导入方式，规范化账单文本落库。
- **对账只消费已导入账单**：执行对账时按（账单日期 + 渠道 + 应用 + 账单类型）查找已导入账单；未导入时返回明确的业务错误提示（含 T+1 说明），不再临时下载。
- 对账记录关联账单 ID（`t_reconciliation.bill_id`），对账结果可追溯到具体账单。
- **修复定时任务**：拆分为渠道级 cron；定时任务先导入账单、再执行对账；默认时间调整到渠道账单生成之后（微信 10:30、支付宝 11:00）。
- 前端（Vue + React）在对账页面增加账单管理（导入、列表、查看账单记录）。

---

## 2. 当前现状（变更前必须承认）

### 2.1 现有实现锚点

| 层 | 文件 / 类 / 方法 | 现有职责 | 变更 |
|---|---|---|---|
| Service | `ReconciliationServiceImpl.downloadBill()` | 对账时临时下载渠道账单 | 已删除，改为读取已导入账单 |
| Service | `ReconciliationServiceImpl.doReconcile()` | 下载 -> 解析 -> 匹配 -> 只存哈希 | 从 `t_channel_bill` 读取账单原文 |
| Schedule | `ReconciliationScheduleConfig` | 2:00/2:30 自动对账，共用一个 cron 属性 | 拆分 wx-cron/ali-cron，先导入后对账 |
| Controller | `ReconciliationController` | `/api/reconciliation/**` | 接口不变（行为变更：需先导入账单） |
| Parser | `WxBillParser` / `AliPayBillParser` | 解析账单文本 | 微信兼容官方逗号/Tab/XLSX，导入时即解析校验 |

### 2.2 现有可疑行为

- ~~`application.yml` 的 `payment.reconciliation.cron` 同时被微信、支付宝两个 `@Scheduled` 读取~~ -- 已修复（拆分为 `wx-cron`/`ali-cron`）。
- ~~微信昨日账单 10:00 前查询会抛"微信昨日账单通常在10:00后生成"，凌晨 2:00 的微信自动对账实际每天失败~~ -- 已修复（微信定时默认 10:30）。

---

## 3. 契约（对外行为）

### 3.1 新增 HTTP 接口：渠道账单管理 `/api/bill/**`

| 方法 | 路径 | 说明 |
|---|---|---|
| `POST` | `/api/bill/auto-fetch` | 自动拉取渠道账单并导入（调微信/支付宝 API） |
| `POST` | `/api/bill/upload` | 手动上传账单文件导入（multipart，微信 CSV/TXT/XLSX） |
| `GET` | `/api/bill/list` | 已导入账单分页列表 |
| `GET` | `/api/bill/{id}` | 账单详情（统计信息） |
| `GET` | `/api/bill/{id}/records` | 账单解析记录分页列表 |
| `DELETE` | `/api/bill/{id}` | 删除账单（被对账记录引用时禁止删除） |

#### 自动拉取请求

```json
{
  "billDate": "2026-08-27",
  "channelCode": "WXPAY",
  "paymentAppId": 1,
  "billType": "ALL",
  "force": false
}
```

| 参数 | 必填 | 说明 |
|---|---|---|
| `billDate` | 是 | 账单日期，必须为历史日期（T+1） |
| `channelCode` | 是 | `WXPAY` / `ALIPAY` |
| `paymentAppId` | 否 | 支付应用，不传为默认应用 |
| `billType` | 否 | 微信账单子类型 `ALL`/`SUCCESS`/`REFUND`，默认 `ALL` |
| `force` | 否 | 已存在同键账单时是否覆盖重新导入，默认 `false` |

#### 手动上传请求（multipart/form-data）

| 参数 | 必填 | 说明 |
|---|---|---|
| `file` | 是 | 微信交易账单 CSV、TXT 或 XLSX 文件 |
| `billDate` | 是 | 账单日期，必须为历史日期（T+1） |
| `channelCode` | 是 | `WXPAY` / `ALIPAY` |
| `billType` | 否 | 默认 `ALL` |
| `force` | 否 | 覆盖重新导入，默认 `false` |

#### 账单实体响应

```json
{
  "code": 0,
  "data": {
    "id": 1,
    "billDate": "2026-08-27",
    "channelCode": "WXPAY",
    "paymentAppId": 1,
    "billType": "ALL",
    "billSource": "AUTO_DOWNLOAD",
    "recordCount": 150,
    "totalAmount": 1500000,
    "billHash": "sha256...",
    "fileName": null,
    "importTime": "2026-08-28T10:30:05"
  }
}
```

注意：列表/详情响应**不返回账单原文**（`billContent` 通过 `@JsonIgnore` 不出参），原文仅通过 `/{id}/records` 以解析后的记录形式查看。

### 3.2 变更行为：对账执行 `POST /api/reconciliation/execute`

请求/响应结构**不变**（见 SPEC-005 3.1/3.2），行为变更：

- 对账前按（`billDate` + `channelCode` + `paymentAppId` + `billType`）查找 `t_channel_bill`；
- **未导入账单**：对账记录置为 `FAILED`，`errorMessage` 为 `"渠道账单未导入，请先导入 {billDate} 的 {channelCode} 账单（渠道账单为T+1出账，当日账单次日可导入）"`，不再临时下载；
- 已导入：使用账单落库原文解析匹配，对账记录写入 `bill_id`、沿用导入时的 `bill_hash`。

### 3.3 账单状态枚举

| 状态 | 说明 |
|---|---|
| `IMPORTED` | 已导入（解析成功） |

导入时解析失败直接抛业务错误、不落库（避免脏数据），不引入 `INVALID` 状态。

账单来源枚举：`AUTO_DOWNLOAD`（API 自动拉取）/ `MANUAL_UPLOAD`（手动上传）。

### 3.4 定时任务变更

| 任务 | 旧配置 | 新配置 | 行为 |
|---|---|---|---|
| 微信自动对账 | `payment.reconciliation.cron`（默认 2:00） | `payment.reconciliation.wx-cron`（默认 `0 30 10 * * ?`） | 先自动拉取导入昨日账单 -> 再对账 |
| 支付宝自动对账 | `payment.reconciliation.cron`（默认 2:30） | `payment.reconciliation.ali-cron`（默认 `0 0 11 * * ?`） | 同上 |

- `payment.reconciliation.enabled` 保留（总开关）；
- 旧 `payment.reconciliation.cron` 配置项已删除（demo 项目直接删除，不留兼容）；
- 导入失败（如账单未生成）时跳过当日对账并记录日志，不影响另个渠道。

### 3.5 不变量 / 边界

- **T+1 约束**：账单日期必须为历史日期（`billDate < 今天`，Asia/Shanghai）；自动拉取微信昨日账单在 10:00 前会被渠道拒绝（继承 `WxPayBillService.validateBillDate()`）；手动上传不做 10:00 校验（商户平台下载的文件即已生成）。
- **账单唯一性**：`(bill_date, channel_code, payment_app_id, bill_type)` 唯一；已存在时重复导入幂等返回已有账单，`force=true` 覆盖（更新原文/哈希/统计/来源，保留 id）。
- **导入即校验**：导入时同步解析账单，解析失败抛错不落库；解析记录数为 0 同样拒绝。
- **对账依赖**：对账必须有已导入账单；新产生的对账记录 `bill_id` 非空。
- **删除保护**：账单被任意对账记录引用（`t_reconciliation.bill_id`）时禁止删除。
- **金额单位**：分。

---

## 4. 隐式输入输出 / 状态地图

| 类型 | 名称 / 位置 | 读 | 写 | 备注 |
|---|---|---|---|---|
| DB | `t_channel_bill`（新增） | 对账时读取、列表/记录查询 | 导入时插入/覆盖更新、删除 | 账单原文 `bill_content` MEDIUMTEXT |
| DB | `t_reconciliation`（变更） | - | 新增 `bill_id` 列 | 对账关联账单 |
| Redis | `recon:lock:*` | 不变 | 不变 | 对账并发锁沿用 |
| Config | `payment.reconciliation.wx-cron` / `ali-cron` / `enabled` | 定时任务读取 | - | 新增渠道级 cron |
| Config | `spring.servlet.multipart.max-file-size`（10MB） | 上传接口 | - | 手动上传账单文件限制 |

---

## 5. 多套实现 / 多套规则并存

- **微信 vs 支付宝**：账单格式与拉取 API 不同，复用 `WxBillParser` / `AliPayBillParser` 与 `WxPayBillFacade` / `AliPayService`；手动上传当前仅支持微信交易账单格式（支付宝账单为 GZIP，后续扩展）。
- **自动拉取 vs 手动上传**：来源字段 `billSource` 区分，导入校验与唯一键规则一致。

---

## 6. 验收标准

- [x] 自动拉取导入：`POST /api/bill/auto-fetch` 能下载微信/支付宝账单并落库，返回统计信息
- [x] 手动上传导入：`POST /api/bill/upload` 能导入微信 CSV/TXT/XLSX 账单文件并解析
- [x] T+1 约束：当天/未来日期导入被拒绝；微信昨日账单 10:00 前自动拉取被拒绝（继承渠道校验）
- [x] 唯一性：同键账单重复导入默认幂等返回已存在，`force=true` 覆盖导入
- [x] 解析失败：导入失败抛业务错误，不落库
- [x] 对账依赖：未导入账单时 `POST /api/reconciliation/execute` 记录 FAILED 并返回明确错误提示
- [x] 对账关联：对账记录 `billId` 正确指向消费的账单，`billHash` 与账单一致
- [x] 删除保护：被对账引用的账单无法删除
- [x] 定时任务：拆分渠道 cron，默认微信 10:30、支付宝 11:00，先导入后对账，失败互不影响
- [x] 账单记录查询：`GET /api/bill/{id}/records` 分页返回解析后记录
- [x] 前端：Vue/React 对账页新增账单管理（自动拉取、上传、列表、查看记录）
- [x] 不破坏现有接口：`/api/wx-pay/**`、`/api/ali-pay/**`、`/api/reconciliation/**` 路径与响应结构不变

---

## 7. 特征测试清单

- [x] 手动上传导入：合法微信 CSV/XLSX 落库、recordCount/totalAmount 统计正确
- [x] 手动上传导入：当天日期被拒绝（T+1）
- [x] 手动上传导入：非微信渠道被拒绝
- [x] 重复导入：默认幂等返回已存在，force=true 覆盖
- [x] 被对账引用的账单禁止删除
- [x] 账单记录分页解析正确
- [x] 对账依赖：无已导入账单时执行对账记录 FAILED 且错误信息包含日期与渠道
- [x] 对账关联：已导入账单时对账关联 billId 并沿用账单哈希

### 7.2 测试锚点

| 测试类型 | 文件 | 覆盖行为 |
|---|---|---|
| 单元测试 | `cc.ivera.service.impl.reconciliation.ChannelBillServiceTest` | CSV/XLSX 上传、拉取导入、T+1 校验、幂等与覆盖、删除保护、记录分页（13 个用例） |
| 单元测试 | `cc.ivera.service.impl.reconciliation.ReconciliationBillDependencyTest` | 对账依赖已导入账单、billId/billHash 关联、对账幂等、跨日退款（4 个用例） |

---

## 8. 兼容性影响

- **新增 API**：`/api/bill/**`，遵循现有 `R<T>` 响应格式。
- **行为变更**：`/api/reconciliation/execute` 不再自动下载账单，需先导入（请求/响应结构不变，未导入账单时记录 FAILED）。
- **数据库**：新增 `t_channel_bill` 表；`t_reconciliation` 新增 `bill_id` 列（可空，存量记录为 null）；完整结构统一维护在 `payment-demo/sql/payment-demo.sql`。
- **配置**：删除 `payment.reconciliation.cron`，新增 `wx-cron` / `ali-cron`，`enabled` 保留；新增 `spring.servlet.multipart`（10MB）。
- **前端**：Vue/React 对账页新增账单管理区块，不修改其他页面。

---

## 9. 实现锚点

- Entity / Mapper：
  - `payment-demo/src/main/java/cc/ivera/entity/ChannelBill.java`
  - `payment-demo/src/main/java/cc/ivera/mapper/ChannelBillMapper.java`
  - `payment-demo/src/main/resources/mapper/ChannelBillMapper.xml`
- Service / Controller：
  - `payment-demo/src/main/java/cc/ivera/service/reconciliation/ChannelBillService.java`
  - `payment-demo/src/main/java/cc/ivera/service/impl/reconciliation/ChannelBillServiceImpl.java`
  - `payment-demo/src/main/java/cc/ivera/controller/ChannelBillController.java`
- 变更：
  - `payment-demo/src/main/java/cc/ivera/service/impl/reconciliation/ReconciliationServiceImpl.java`（消费已导入账单）
  - `payment-demo/src/main/java/cc/ivera/entity/Reconciliation.java`（billId）
  - `payment-demo/src/main/resources/mapper/ReconciliationMapper.xml`（bill_id 映射）
  - `payment-demo/src/main/java/cc/ivera/config/ReconciliationScheduleConfig.java`（先导入后对账）
  - `payment-demo/src/main/resources/application.yml`（wx-cron/ali-cron、multipart）
- SQL：`payment-demo/sql/payment-demo.sql`（唯一完整初始化脚本）
- 前端：
  - `payment-demo-vue/src/api/bill.js`、`payment-demo-vue/src/views/Reconciliation.vue`
  - `payment-demo-react/src/api/bill.js`、`payment-demo-react/src/pages/Reconciliation.jsx`
- 测试：见 7.2 测试锚点
- 文档：`payment-demo/README.md`（核心特性）

---

## 10. 回滚 / 观测

- **回滚**：`git revert` 相关提交；`t_channel_bill` 为新增表可直接丢弃；`bill_id` 列可空不影响存量对账数据。
- **观测**：账单导入日志（`渠道账单下载完成`、`渠道账单导入完成`、`渠道账单已导入，直接返回`）、对账日志含 `billId`；定时任务日志关注"自动拉取账单失败"关键字。
- **失败时对外表现**：
  - 手动导入：API 返回错误信息，前端弹窗展示失败原因
  - 定时任务：导入失败跳过当日对账并记录日志，次日自动重试

---

## 11. 变更记录

| 日期 | 状态 | 改了什么 | 关联 issue / PR |
|---|---|---|---|
| 2026-08-28 | planned | 初稿：渠道账单导入与对账管理设计 | - |
| 2026-08-28 | implemented | 完成全部开发：数据库 + 后端 + Vue/React 前端 + 15 个特征测试 | - |
| 2026-08-29 | implemented | 兼容微信官方反引号/Tab/XLSX 账单，进账与退款拆分为独立业务记录 | SPEC-007 |
