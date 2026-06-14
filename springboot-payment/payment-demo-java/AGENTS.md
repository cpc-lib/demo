# AGENTS.md - AI 开发规则

> 本文档定义 AI 助手在 payment-demo 项目中必须遵守的开发规则和流程。

---

## 1. Issue 分类规则

所有 issue 必须按以下分类打标签：

| 分类 | 标签 | 定义 | 处理流程 |
|------|------|------|---------|
| **局部 Bug** | `type: bug-local` | 不影响公共 API，不影响外部调用方，仅内部逻辑错误 | 直接修复 + 特征测试 |
| **设计变更** | `type: design-change` | 改变架构、模块职责、数据流 | 先更新 spec → 再实现 |
| **兼容变更** | `type: compatibility` | 改变公共 API、返回结构、协议、回调格式 | 必须有兼容方案 + 版本标记 |
| **同根因问题** | `type: root-cause-multi` | 多个 issue 源自同一设计缺陷 | 合并处理，不逐个打补丁 |

### 分类判断流程

```
收到 issue
  ↓
是否改变 public API / 返回结构 / 协议格式？
  ├─ 是 → type: compatibility
  └─ 否 → 是否改变模块职责、数据流、架构？
           ├─ 是 → type: design-change
           └─ 否 → 是否有多个 issue 指向同一原因？
                    ├─ 是 → type: root-cause-multi
                    └─ 否 → type: bug-local
```

---

## 2. 分支命名规则

| 类型 | 分支命名格式 | 示例 |
|------|------------|------|
| Bug 修复 | `fix/{issue-number}-{short-desc}` | `fix/123-refund-idempotent` |
| 重构 | `refactor/{module}-{desc}` | `refactor/v2-notify-handler` |
| 设计变更 | `feature/{spec-name}-{desc}` | `feature/payment-config-hot-reload` |
| 兼容变更 | `compat/{api-name}-{version}` | `compat/wxpay-v2-notify-v2` |

### 分支规则

- 必须从 `main` 分支创建
- 合并前必须通过全部特征测试
- 兼容变更分支必须包含 `version` 标记
- 重构分支不得改变业务逻辑（仅结构变化）

---

## 3. 测试要求

### 3.1 测试分层

| 层级 | 测试类型 | 覆盖要求 | 执行时机 |
|------|---------|---------|---------|
| **L0** | 特征测试 (Characterization Tests) | 所有 public API、可疑行为锁住 | 每次提交前 |
| **L1** | 单元测试 | 核心业务逻辑、边界条件 | 每次提交前 |
| **L2** | 集成测试 | 数据库、Redis、RabbitMQ 交互 | CI 流水线 |
| **L3** | 端到端测试 | 完整支付流程 | 发布前 |

### 3.2 特征测试规则

- 特征测试必须**全绿**才能开始重构
- 特征测试失败时，**先分析行为变化**，不得直接修改测试
- 可疑行为必须在测试名或注释中标明 `【现状】`
- 测试必须隔离数据库、Redis、RabbitMQ 状态

### 3.3 测试隔离要求

```java
@BeforeEach
void resetState() {
    // 1. 清理测试数据
    // 2. 清理 Redis Key
    // 3. 清空消息队列
    // 4. 重置 Mock 对象
}
```

---

## 4. Spec 对账规则

### 4.1 Spec 状态机

```
planned → implemented → archived
   ↓          ↓
   └────── deprecated
```

| 状态 | 目录 | 含义 | 迁移条件 |
|------|------|------|---------|
| `planned` | `spec/planned/` | 已设计、未实现 | 实现完成 → `implemented` |
| `implemented` | `spec/implemented/` | 已实现、有测试 | 废弃 → `archived/deprecated/` |
| `archived` | `spec/archived/` | 废弃或搁置 | - |
| `governance` | `spec/governance/` | 长期治理规则 | 不迁移 |

### 4.2 PR 合并前对账清单

每个 PR 必须确认：

- [ ] **实现与 spec 一致**: 代码行为符合 spec 描述的契约
- [ ] **测试已覆盖**: 新增/修改的逻辑有对应特征测试
- [ ] **Spec 状态已更新**: 如果实现完成，spec 从 `planned` 移到 `implemented`
- [ ] **公共 API 未变**: 如果未标记 `type: compatibility`，API 不得改变
- [ ] **文档已更新**: README、API 文档、配置说明同步更新
- [ ] **兼容影响已标注**: 如果有破坏性变更，必须有迁移指南

### 4.3 对账失败处理

| 失败类型 | 处理方式 |
|---------|---------|
| 实现与 spec 不一致 | 暂停合并，更新 spec 或修正实现 |
| 测试未覆盖 | 补充特征测试，不得跳过 |
| Spec 状态未更新 | 更新 spec 目录状态 |
| 公共 API 意外改变 | 回退变更或升级为 `type: compatibility` |

---

## 5. 完成定义 (Definition of Done)

一个 issue 或 PR 标记为"完成"，必须满足：

### 5.1 代码层面
- [ ] 代码已提交并通过代码审查
- [ ] 全部特征测试通过
- [ ] 无新增编译警告（或已标注可接受）
- [ ] 无安全漏洞扫描告警

### 5.2 测试层面
- [ ] 特征测试已覆盖新增/修改的逻辑
- [ ] 可疑行为已锁住（如有）
- [ ] 测试可重复执行，结果稳定

### 5.3 文档层面
- [ ] Spec 文件已创建或更新
- [ ] Spec 状态已迁移（如适用）
- [ ] README 或 API 文档已更新（如适用）

### 5.4 兼容层面
- [ ] 公共 API 变更已标注版本号（如适用）
- [ ] 破坏性变更有迁移指南（如适用）
- [ ] 下游依赖影响已评估（如适用）

---

## 6. AI 开工前检查清单

AI 助手在开始任何开发任务前，必须确认：

1. **已读取规则**: 本文档 (`AGENTS.md`) 已加载到上下文
2. **已读取 spec**: `spec/README.md` 已读取，了解当前 spec 状态
3. **已分类 issue**: 明确 issue 类型（bug/design/compatibility/root-cause）
4. **已检查测试**: 特征测试是否存在，是否全绿
5. **已评估影响**: 公共 API、返回结构、协议格式是否受影响

---

## 7. 禁止事项

- ❌ 禁止把重要规则只留在聊天记录里
- ❌ 禁止实现改了但 spec 不改
- ❌ 禁止多个同根因 issue 逐个打补丁
- ❌ 禁止分不清影响面时直接开工
- ❌ 禁止重构和修 bug 混在一起
- ❌ 禁止特征测试红时直接改测试代码
- ❌ 禁止随手修改业务逻辑（仅结构重构时）

---

## 8. 文件索引

| 文件 | 用途 |
|------|------|
| `AGENTS.md` | AI 开发规则（本文件） |
| `spec/README.md` | Spec 状态账本 |
| `spec/governance/` | 长期治理规则 |
| `spec/planned/` | 已设计未实现 spec |
| `spec/implemented/` | 已实现 spec |
| `spec/archived/` | 废弃 spec |
| `CURRENT_BEHAVIOR_SPEC.md` | 当前行为规格 |
| `CHARACTERIZATION_TESTS.md` | 特征测试清单 |
| `TEST_MAPPING.md` | 测试与现状映射表 |
