# Spec 状态账本

`spec/` 记录 Payment Demo 的架构、接口、状态机、兼容行为和治理规则。任何设计型改动都要让代码、测试、文档和 spec 讲同一个故事。

## 状态目录

| 状态 | 目录 | 用途 |
|---|---|---|
| governance | `spec/governance/` | 项目治理规则、issue 分类、PR 对账清单、测试治理等长期规则 |
| planned | `spec/planned/` | 已确认方向但尚未完整实现的设计、功能或兼容变更 |
| implemented | `spec/implemented/` | 当前代码已经实现，并由测试或明确验收标准锁住的行为 |
| archived | `spec/archived/` | 已废弃、推迟、被替代或不再适用的 spec |

## 当前索引

### governance

- [Issue 分类与 PR 对账](governance/ISSUE_TRIAGE_AND_PR_CHECKLIST.md)

### planned

- 暂无。

### implemented

- [Payment Demo Go 当前行为](implemented/current-behavior/PAYMENT_DEMO_GO_CURRENT_BEHAVIOR_SPEC.md)
- [Issue 2 支付渠道与支付应用配置入库](implemented/payment-config/ISSUE_2_PAYMENT_APP_CONFIG_SPEC.md)
- [支付参数管理与支付应用管理](implemented/payment-config/PAYMENT_CONFIG_MANAGEMENT_SPEC.md)
- [支付应用 ID 必填与订单绑定配置](implemented/payment-config/REQUIRED_PAYMENT_APP_ID_ORDER_BINDING_SPEC.md)

### archived

- 暂无。

## 使用规则

- 新增或改变 HTTP API、状态值、DB schema、MQ 名称、配置字段、第三方回调响应、前端调用契约时，先新增或更新 spec。
- 重构不改变行为时，应引用 `implemented` spec 和特征测试，证明外部行为未变。
- spec 落地后，从 `planned` 移到 `implemented`，并更新本索引。
- 不确定是否需要 spec 时，按需要 spec 处理。
