# 测试治理规格

> 本文档定义支付项目的测试分层、编写规范和执行流程。

---

## 1. 测试分层

### 1.1 L0 - 特征测试 (Characterization Tests)

**目标**: 锁住当前实际行为，确保重构不改变行为

**特点**:
- 测试 public API 和返回结构
- 测试错误路径和拒绝路径
- 测试日志输出和事件
- 可疑行为必须标明 `【现状】`

**执行**: 每次提交前

### 1.2 L1 - 单元测试

**目标**: 验证核心业务逻辑的正确性

**特点**:
- 隔离数据库、Redis、RabbitMQ
- Mock 外部服务
- 覆盖边界条件

**执行**: 每次提交前

### 1.3 L2 - 集成测试

**目标**: 验证与数据库、Redis、RabbitMQ 的交互

**特点**:
- 使用测试容器 (Testcontainers)
- 真实数据库交互
- 真实消息队列交互

**执行**: CI 流水线

### 1.4 L3 - 端到端测试

**目标**: 验证完整支付流程

**特点**:
- 完整下单 → 支付 → 通知 → 退款流程
- Mock 外部支付平台

**执行**: 发布前

---

## 2. 测试编写规范

### 2.1 命名规范

```
should{ExpectedBehavior}When{Condition}
```

**示例**:
- `shouldCreateNewOrderWhenNoExistingOrder()`
- `shouldReturnFailureWhenSignatureInvalid()`
- `shouldRejectRefundForUnpaidOrder()`

### 2.2 可疑行为测试命名

```
should{Behavior}For{Condition}_CURRENT_BEHAVIOR()
```

**示例**:
- `shouldReturnPayingMessageForClosedOrder_CURRENT_BEHAVIOR()`

### 2.3 测试结构

```java
@Test
@DisplayName("描述性中文标题")
void shouldXxx() {
    // Given: 准备测试数据
    // When: 执行被测逻辑
    // Then: 验证结果
}
```

### 2.4 测试隔离

```java
@BeforeEach
void resetState() {
    // 1. 清理数据库测试数据
    // 2. 清理 Redis Key
    // 3. 清空消息队列
    // 4. 重置 Mock 对象
}
```

---

## 3. 测试执行流程

### 3.1 本地开发

```bash
# 运行全部特征测试
mvn test -Dtest=CharacterizationTests

# 运行单个测试
mvn test -Dtest=OrderCreationTest
```

### 3.2 CI 流水线

```
Lint → Unit Tests → Integration Tests → E2E Tests
```

### 3.3 测试失败处理

| 失败类型 | 处理方式 |
|---------|---------|
| 特征测试失败 | 停止开发，分析行为变化 |
| 单元测试失败 | 修复实现或测试 |
| 集成测试失败 | 检查测试容器状态 |
| E2E 测试失败 | 检查 Mock 服务 |

---

## 4. 测试覆盖率要求

| 层级 | 覆盖率要求 | 说明 |
|------|-----------|------|
| L0 特征测试 | 100% public API | 所有公开接口必须覆盖 |
| L1 单元测试 | 80% 行覆盖 | 核心业务逻辑 |
| L2 集成测试 | 关键路径 | 数据库、Redis、MQ 交互 |
| L3 E2E 测试 | 主流程 | 支付、退款完整流程 |

---

## 5. 测试数据管理

### 5.1 测试数据前缀

| 数据类型 | 前缀 | 示例 |
|---------|------|------|
| 订单号 | `TEST_` | `TEST_20260614_001` |
| 退款单号 | `TEST_RF_` | `TEST_RF_20260614_001` |
| Redis Key | `test:` | `test:payment:wx:notify:*` |

### 5.2 测试数据清理

```java
@AfterEach
void cleanupTestData() {
    // 清理带 TEST_ 前缀的数据
    // 清理 test: 开头的 Redis Key
}
```
