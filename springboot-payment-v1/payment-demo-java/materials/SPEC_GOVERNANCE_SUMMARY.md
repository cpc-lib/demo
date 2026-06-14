# Spec 治理结构总结

> 本文档总结已建立的最小 spec 状态账本治理结构。

---

## 已创建文件

### 1. 核心规则文件

| 文件 | 用途 | 状态 |
|------|------|------|
| [AGENTS.md](../AGENTS.md) | AI 开发规则，包含 issue 分类、分支命名、测试要求、对账规则、完成定义 | ✅ 已创建 |

### 2. Spec 状态账本

| 文件 | 用途 | 状态 |
|------|------|------|
| [spec/README.md](../spec/README.md) | Spec 状态总览，列出 governance/planned/implemented/archived | ✅ 已创建 |

### 3. 已实现 Spec

| 文件 | Spec ID | 状态 |
|------|---------|------|
| [spec/implemented/current-behavior/PAYMENT_DEMO_CURRENT_BEHAVIOR_SPEC.md](../spec/implemented/current-behavior/PAYMENT_DEMO_CURRENT_BEHAVIOR_SPEC.md) | SPEC-001 | ✅ implemented |

### 4. 已设计 Spec (未实现)

| 文件 | Spec ID | 状态 |
|------|---------|------|
| [spec/planned/V2_NOTIFY_HANDLER_EXTRACTION_SPEC.md](../spec/planned/V2_NOTIFY_HANDLER_EXTRACTION_SPEC.md) | SPEC-002 | 🟡 planned |

### 5. 治理规则

| 文件 | 用途 |
|------|------|
| [spec/governance/TESTING_GOVERNANCE_SPEC.md](../spec/governance/TESTING_GOVERNANCE_SPEC.md) | 测试分层、编写规范、执行流程 |
| [spec/governance/PR_RECONCILIATION_CHECKLIST.md](../spec/governance/PR_RECONCILIATION_CHECKLIST.md) | PR 合并前对账清单 |
| [spec/governance/ISSUE_CLASSIFICATION_GUIDE.md](../spec/governance/ISSUE_CLASSIFICATION_GUIDE.md) | Issue 分类决策树和指南 |

### 6. 归档目录

| 目录 | 用途 |
|------|------|
| `spec/archived/deprecated/` | 被新方案替代的 spec |
| `spec/archived/deferred/` | 暂时搁置的 spec |

---

## 目录结构

```
payment-demo-java/
├── AGENTS.md                              # AI 开发规则
├── CURRENT_BEHAVIOR_SPEC.md               # 当前行为规格 (原始)
├── CHARACTERIZATION_TESTS.md              # 特征测试清单 (原始)
├── TEST_MAPPING.md                        # 测试映射表 (原始)
├── spec/
│   ├── README.md                          # Spec 状态账本
│   ├── governance/
│   │   ├── TESTING_GOVERNANCE_SPEC.md     # 测试治理规则
│   │   ├── PR_RECONCILIATION_CHECKLIST.md # PR 对账清单
│   │   └── ISSUE_CLASSIFICATION_GUIDE.md  # Issue 分类指南
│   ├── implemented/
│   │   └── current-behavior/
│   │       └── PAYMENT_DEMO_CURRENT_BEHAVIOR_SPEC.md  # SPEC-001
│   ├── planned/
│   │   └── V2_NOTIFY_HANDLER_EXTRACTION_SPEC.md       # SPEC-002
│   └── archived/
│       ├── README.md
│       ├── deprecated/
│       └── deferred/
```

---

## Issue 分类示例

基于当前项目的可疑行为，以下是 issue 分类示例：

| 可疑行为 | Issue 分类 | 理由 |
|---------|-----------|------|
| 已关闭订单查询返回"支付中" | `type: bug-local` | 内部逻辑错误，不改变 API |
| 配置修改后不自动刷新 | `type: design-change` | 需要改变架构引入热更新 |
| V2/V3响应格式不一致 | 不建 issue | 微信官方要求，不是 bug |
| V2/V3幂等Key字段不同 | 不建 issue | 微信官方报文差异 |
| 退款计算并发安全 | `type: root-cause-multi` | 可能导致多个超退 issue |
| 分布式锁中断处理 | `type: bug-local` | 内部逻辑问题 |

---

## Spec 状态迁移流程

```
创建 issue
  ↓
分类 issue (bug/design/compatibility/root-cause)
  ↓
编写 spec (planned/)
  ↓
实现 + 测试
  ↓
更新 spec 状态 (implemented/)
  ↓
PR 对账检查
  ↓
合并
```

---

## 下一步

1. **确认治理结构**: 审核 AGENTS.md 和 spec 目录结构
2. **开始第一个重构**: 执行 SPEC-002 (V2通知处理器提取)
3. **建立 issue**: 将可疑行为转为 issue 并分类
4. **定期维护**: 每季度检查 spec 状态

---

## 维护规则

1. **每次 PR 必须更新 spec 状态** (如适用)
2. **实现完成**: spec 从 `planned/` 移到 `implemented/`
3. **设计废弃**: spec 移到 `archived/deprecated/`
4. **实现变更**: 更新 `implemented/` 中的实现锚点
5. **定期清理**: 每季度检查 `planned/` 中超过 3 个月未动的 spec
