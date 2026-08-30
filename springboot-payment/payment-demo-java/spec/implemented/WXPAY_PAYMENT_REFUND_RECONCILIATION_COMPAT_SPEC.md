# SPEC-007: 微信进账与退款逐笔对账兼容扩展

> 在不改变现有对账接口路径的前提下，让微信官方交易账单中的进账与退款分别和本地支付流水、退款流水完整匹配。

---

## 元信息

| 字段 | 值 |
|---|---|
| **Spec ID** | SPEC-007 |
| **分类** | `type: compatibility` |
| **状态** | implemented |
| **版本** | 0.2.0 |
| **创建日期** | 2026-08-29 |
| **依赖** | SPEC-005、SPEC-006 |
| **影响范围** | backend、database、react-frontend、vue-frontend、docs、tests |

## 1. 需求理解与边界

### 1.1 本次必须完成

微信账单导入后只处理两种业务流水：

1. **进账（PAYMENT）**：微信成功交易明细与本地 `t_payment_info` 微信支付流水逐笔对账。
2. **退款（REFUND）**：微信退款明细与本地 `t_refund_info` 退款流水逐笔对账。

一笔业务流水同时满足标识、金额和状态规则时为 `MATCH`。渠道有本地无、本地有渠道无、金额不同、状态不兼容均生成现有差异类型。对账任务执行完成且 `diff_count = 0` 表示本次进账与退款全部对账成功。

### 1.2 明确不做

- 不处理 `REVOKED` 撤销交易。
- 不处理资金账单 `fundflowbill`。
- 不扩展支付宝的解析和匹配规则；支付宝保持现状。
- 不新增原始二进制账单归档、审计工作流或自动修账。
- 不把退款合并回原支付订单后做净额对账；支付和每笔退款是独立对账单元。

## 2. 微信账单输入兼容契约

依据微信支付交易账单说明：

- [申请交易账单](https://pay.weixin.qq.com/doc/v3/merchant/4013071227)
- [下载账单](https://pay.weixin.qq.com/doc/v3/merchant/4013071238)
- [交易账单字段说明](https://pay.weixin.qq.com/doc/v3/merchant/4013071246)
- [商户平台手动下载账单](https://pay.weixin.qq.com/doc/v3/merchant/4013071252)

手动上传兼容以下输入：

| 输入 | 识别规则 | 处理方式 |
|---|---|---|
| 微信 API 交易账单文本 | UTF-8、英文逗号分隔、字段可能带一个前导反引号 | 去除字段的一个保护反引号后解析 |
| 商户平台复制文本 | Tab 分隔，可无表头，ALL 明细固定 27 列 | 按官方 ALL 字段顺序补充表头后解析 |
| 商户平台 Excel | `.xlsx`，第一张工作表含表头、明细和汇总 | 转为规范化 Tab 文本后复用同一解析逻辑 |
| 旧版示例 CSV | 已有中文表头和 22 列格式 | 保持兼容 |

以下内容不进入业务明细：空行、账单说明行、汇总表头、汇总值。文件存在有效内容但没有 PAYMENT/REFUND 明细时导入失败。

## 3. 统一业务记录

`ChannelBillRecord` 增加以下兼容字段：

| 字段 | PAYMENT | REFUND |
|---|---|---|
| `businessType` | `PAYMENT` | `REFUND` |
| `orderNo` | 商户订单号 | 原商户订单号 |
| `transactionId` | 微信订单号 | 原微信订单号 |
| `refundNo` | 空 | 商户退款单号 |
| `refundId` | 空 | 微信退款单号 |
| `amount` | 优先订单金额，兼容回退应结订单金额 | 优先申请退款金额，兼容回退退款金额 |
| `status` | 交易状态 | 优先退款状态，空时回退 `REFUND` |

金额从元精确转换为分。订单号、交易号、退款号始终按字符串处理，不做数值转换。

## 4. 匹配规则

### 4.1 PAYMENT

- 本地来源：账单日内创建的 `t_payment_info`，`payment_type = 微信`，并且其订单属于 `WXPAY` 和本次 `paymentAppId`。
- 主匹配键：`商户订单号 = PaymentInfo.orderNo`。
- 回退键：主键缺失或未命中时，仅在微信订单号唯一时使用 `微信订单号 = PaymentInfo.transactionId`。
- 金额：`ChannelBillRecord.amount = PaymentInfo.payerTotal`。
- 状态：渠道和本地均为支付成功状态。

### 4.2 REFUND

- 本地来源：账单日内审核通过的 `t_refund_info`，其原订单属于 `WXPAY` 和本次 `paymentAppId`。原订单创建日期不限制，因此支持跨日退款。
- 主匹配键：`商户退款单号 = RefundInfo.refundNo`。
- 回退键：主键缺失或未命中时，仅在微信退款单号唯一时使用 `微信退款单号 = RefundInfo.refundId`。
- 金额：`ChannelBillRecord.amount = RefundInfo.refund`。
- 状态：相同状态匹配；账单快照为 `PROCESSING`/`ABNORMAL` 而本地已推进到 `SUCCESS` 时也视为兼容；只有交易状态 `REFUND`、缺少退款状态时，本地已审核且不是 `FAILED`/`CLOSED` 视为兼容。

### 4.3 双向完整性

- 每一条本地 PAYMENT/REFUND 流水无渠道记录：`MISSING_CHANNEL`。
- 每一条渠道 PAYMENT/REFUND 明细无本地记录：`MISSING_LOCAL`。
- 重复交易号不得覆盖前一条记录；无法用唯一回退键判断时保留为差异。
- `total_count = match_count + diff_count`。

## 5. 数据库与 API 兼容

现有 HTTP 路径和必填参数不变。兼容扩展如下：

- `POST /api/bill/upload` 接受 `.csv`、`.txt`、`.xlsx`。
- 自动拉取传入 `paymentAppId` 时，账单申请、下载凭证、账单唯一键和本地流水过滤使用同一支付应用。
- 账单记录响应新增 `businessType`、`refundNo`、`refundId`。
- 对账明细响应和导出新增 `businessType`、`refundNo`、`refundId`。
- `t_reconciliation_detail` 新增三个可空列；旧数据和旧调用方继续可用。
- `.xlsx` 在入库前规范化为 Tab 文本；`bill_content` 仍为文本字段，不保存二进制文件。

## 6. 验收标准

- [x] 官方 27 列、带反引号的 ALL 文本能解析 PAYMENT 与 REFUND。
- [x] 无表头 Tab 文本能按官方 ALL 顺序解析。
- [x] 商户平台 `.xlsx` 能手动上传、解析、入库和查看记录。
- [x] PAYMENT 使用支付流水的金额、状态和标识匹配。
- [x] REFUND 使用独立退款流水的金额、状态和退款标识匹配。
- [x] 退款不受原订单创建日期限制。
- [x] 渠道缺失、本地缺失、金额不符、状态不符均有明细。
- [x] 重复渠道交易号不覆盖记录。
- [x] 旧版 CSV、支付宝现有逻辑和公共接口路径保持兼容。
- [x] 新增单元测试、可隔离特征测试和 Maven 打包通过；依赖本机 MySQL 的 2 个既有 Spring 上下文测试因环境连接超时未通过。

## 7. 实现与测试锚点

| 模块 | 实现锚点 |
|---|---|
| 账单规范化与解析 | `WxBillParser.normalize()`、`WxBillParser.parse()` |
| 账单导入 | `ChannelBillServiceImpl.uploadBill()` |
| 多支付应用账单下载 | `WxPayBillService.downloadBill(Long, ...)` |
| 进账/退款匹配 | `WxPaymentRefundMatcher.match()` |
| 本地流水查询与编排 | `ReconciliationServiceImpl.doReconcile()` |
| 明细持久化 | `ReconciliationDetailMapper.xml`、`wxpay_reconciliation_v2_upgrade.sql` |
| 前端展示 | React/Vue `Reconciliation` 页面 |

测试锚点：`WxBillParserTest`、`ChannelBillServiceTest`、`WxPayBillServiceTest`、`WxPaymentRefundMatcherTest`、`ReconciliationBillDependencyTest`。真实 `29082026_ALL.xlsx` 验证解析 45 条有效明细，其中 PAYMENT 31 条、REFUND 14 条。

## 8. 回滚与观测

- 回滚代码后，新列为可空，不影响旧版本读取。
- 账单解析日志分别报告 PAYMENT/REFUND 数量；导入失败返回可读错误。
- 财务判断成功条件固定为：任务 `COMPLETED` 且 `diffCount = 0`。

## 9. 变更记录

| 日期 | 状态 | 说明 |
|---|---|---|
| 2026-08-29 | planned | 根据真实微信 XLSX、官方 ALL 文本和用户限定范围完成兼容设计 |
| 2026-08-30 | implemented | 完成微信进账与退款独立逐笔对账、真实账单验证和兼容测试 |
