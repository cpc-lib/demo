# Seata 分布式事务全模式微服务实战案例 (AT / TCC / SAGA / XA)

本项目是一个符合实际企业生产规范的分布式事务微服务案例工程，完整覆盖并演示了 Apache Seata 支持的 **4 种分布式事务模式（AT、TCC、SAGA、XA）**。

工程技术栈：
- **微服务框架**：Spring Boot 2.7.x
- **RPC 通信**：Apache Dubbo 3.2.x
- **服务注册与配置中心**：Alibaba Nacos 2.x
- **分布式事务协调器**：Apache Seata 2.0.x
- **持久层与数据库**：MySQL 8.x + MyBatis-Plus 3.5.x (全 XML 映射文件方式，无 SQL 注解) + 阿里巴巴 Druid 连接池
- **全链路幂等保证**：自定义 SpEL 幂等切面 + 唯一键数据库约束 + Seata TCC Fence (防悬挂、空回滚、幂等) + SAGA 幂等补偿表

---

## 一、模块架构与工程划分

```text
seata-demo/
├── pom.xml                       # 顶级父工程，统管版本依赖
├── README.md                     # 本详细架构与实战文档
├── sql/                          # 数据库初始化脚本
│   ├── seata_server.sql          # Seata Server TC 协调器库表 (global/branch/lock)
│   ├── seata_order.sql           # 订单库 (业务表、undo_log、tcc_fence_log、幂等表、SAGA状态机表)
│   ├── seata_storage.sql         # 库存库 (业务表、TCC预留表、SAGA执行表、undo_log、tcc_fence_log)
│   └── seata_account.sql         # 账户库 (业务表、TCC预留表、SAGA执行表、undo_log、tcc_fence_log)
├── seata-common/                 # 公共基础设施模块
│   ├── annotation/Idempotent.java       # 自定义业务与接口防重幂等注解
│   ├── aop/IdempotentAspect.java        # 幂等切面 (状态机: PROCESSING -> SUCCESS/FAILED)
│   ├── domain/                          # 统一DTO、实体对象与返回模型 Result<T>
│   └── exception/GlobalExceptionHandler # 全局异常拦截与友好提示
├── seata-dubbo-api/              # Dubbo RPC 接口契约模块
│   ├── at/                       # AT 模式接口 (StorageDubboService, AccountDubboService)
│   ├── tcc/                      # TCC 模式接口 (@LocalTCC + @TwoPhaseBusinessAction)
│   ├── saga/                     # SAGA 模式正向执行与逆向补偿接口
│   └── xa/                       # XA 模式强一致性接口
├── order-service/                # 订单微服务 (TM 事务发起端，端口: 8081, Dubbo: 20881)
│   ├── controller/               # 暴露 AT / TCC / SAGA / XA 四种模式的 HTTP 下单测试接口
│   ├── service/impl/             # 各模式订单服务业务编排实现
│   └── resources/statemachine/   # Seata SAGA JSON 状态机编排定义文件
├── storage-service/              # 库存微服务 (RM 资源端，端口: 8082, Dubbo: 20882)
│   └── dubbo/                    # 4 种事务模式对应的库存扣减、TCC预留/提交/释放、SAGA扣减/补偿实现
└── account-service/              # 账户微服务 (RM 资源端，端口: 8083, Dubbo: 20883)
    └── dubbo/                    # 4 种事务模式对应的余额扣款、TCC预留/提交/释放、SAGA扣款/补偿实现
```

---

## 二、四大分布式事务模式实现原理与幂等性设计

### 1. AT 模式 (Automatic Transaction)
- **业务场景**：典型的关系型数据库在线业务系统，开发无侵入，改造成本最低。
- **核心机制**：
  - **一阶段**：Seata 数据源代理（`DataSourceProxy`）解析业务 SQL，在本地事务提交前查询“前镜像（Before-Image）”，执行 SQL，查询“后镜像（After-Image）”，并生成 `undo_log` 记录，向 TC 注册分支事务并申请**全局锁**，随本地事务原子提交。
  - **二阶段提交**：异步删除对应的 `undo_log` 记录，迅速释放资源。
  - **二阶段回滚**：校验后镜像是否一致（防止脏写），根据前镜像反向生成补偿 SQL 恢复数据，并清除 `undo_log`。
- **幂等性与防重设计**：
  - **业务层幂等**：TM 端添加 `@Idempotent(key = "#request.orderNo", bizType = "ORDER_CREATE_AT")`，防止由于网络抖动引发网关或前端重复提交。
  - **数据库约束**：订单表设置 `order_no` 为 `UNIQUE KEY`，从存储层杜绝并发重试脏写入。
  - **RPC 层幂等**：Dubbo 消费端配置 `retries: 0`，防止因 RPC 执行慢导致超时误重试；资源端采用原子更新条件 `available_count >= #{count}` 防止超卖。

---

### 2. TCC 模式 (Try - Confirm - Cancel)
- **业务场景**：高并发核心链路、跨异构系统、或非关系型数据库资源（如 Redis、第三方支付通道）。
- **核心机制**：
  - **Try 阶段**：完成业务检查，预留/冻结业务资源（如扣减 `available_count`，增加 `frozen_count`）。
  - **Confirm 阶段**：真正扣减预留资源，执行实际业务确认。
  - **Cancel 阶段**：释放 Try 阶段预留的资源，退回可用资源。
- **TCC 三大核心痛点与本工程防线**：
  1. **幂等性问题**：网络重试导致 Confirm 或 Cancel 多次触发。
     - **解决方案**：二阶段执行前查询冻结记录表 `storage_freeze_tbl` / `account_freeze_tbl`，若状态已处于 `STATUS_CONFIRM` 或 `STATUS_CANCEL`，直接返回 `true` 放行。
  2. **空回滚问题 (Empty Rollback)**：一阶段 Try 因网络丢包或超时未到达 RM，事务管理器发起全局回滚，Cancel 被触发。
     - **解决方案**：Cancel 阶段若发现无 Try 阶段记录，判定为空回滚，插入一条状态为 `STATUS_CANCEL` 且冻结数量为 0 的记录（标记已回滚），不执行任何余额/库存退还操作。
  3. **防悬挂问题 (Anti-Hanging)**：Cancel 先于延迟到达的 Try 执行完成后，迟到的 Try 到达 RM。若盲目执行 Try，预留的资源将永远不会被 Confirm 或 Cancel，造成资源永久悬挂冻结。
     - **解决方案**：Try 阶段执行前，首先检查是否已存在 Cancel 插入的回滚标记，若已存在则直接拒绝执行 Try。
  - **双重保障**：本工程同时启用了 Seata 原生注解 `@TwoPhaseBusinessAction(..., useTCCFence = true)`（基于 `tcc_fence_log`）和自定义状态机表双重防护，兼具框架保障与业务透明性。

---

### 3. SAGA 模式 (长事务状态机 / 补偿模式)
- **业务场景**：跨企业长流程、银行电汇转账、流程耗时很长且不能长时间持有数据库行锁的场景。
- **核心机制**：
  - 核心由**正向操作（Forward Action）**和**逆向补偿（Compensating Action）**成对组成。
  - 正向操作直接修改真实业务数据（无预留资源），一旦某一链路失败，逆向按执行相反顺序逐个调用补偿方法回退数据。
  - 本工程提供两种实现示范：
    1. **Seata JSON 状态机编排**：`order-service/src/main/resources/statemachine/order_saga.json`。
    2. **企业级程序化补偿编排**：`OrderSagaServiceImpl`，直观呈现每一步执行状态跟踪与精准回滚。
- **幂等性与防重设计**：
  - 引入 `storage_saga_record` 和 `account_saga_record` 独立追踪表：
    - `forward_status`：0-未执行，1-成功，2-失败。
    - `compensate_status`：0-未补偿，1-已补偿。
  - **补偿幂等**：补偿方法执行前检查 `compensate_status == 1`，若已补偿直接返回成功，防止网络重试造成多退金额或多加库存。
  - **空补偿防悬挂**：若正向操作未成功（`forward_status != 1`），补偿操作仅写入已补偿占位标记，绝不上调库存或余额。

---

### 4. XA 模式 (eXtended Architecture 规范)
- **业务场景**：金融级强一致性要求系统，严格遵循 ACID，不接受最终一致性过程中的脏读或数据不一致。
- **核心机制**：
  - 关系型数据库底层（MySQL 5.7+ InnoDB）原生支持 XA 事务规范。
  - Seata 通过 `DataSourceProxyXA` 对数据源进行代理，将分支事务纳入 RM 纳管，由数据库底层的 XA PREPARE、XA COMMIT、XA ROLLBACK 保证严格两阶段提交。
  - 优点：零业务侵入，数据强隔离；缺点：数据库行锁持续持有到二阶段提交，并发吞吐量低于 AT 和 TCC。
- **幂等性设计**：
  - 借助数据库行级排他锁 + 上层全局 `@Idempotent` 切面，防止由于 RPC 连接超时引发的重复下单。

---

## 三、快速开始与实战运行

### 1. 准备环境
- **JDK**：17 或 21
- **Maven**：3.8+
- **MySQL**：8.0+
- **Nacos**：2.x+
- **Seata Server**：2.0.x

### 2. 一键启动基础设施 (Docker Compose)
项目根目录下提供了完整的 [`docker-compose.yml`](file:///d:/release/temp/seata-demo/docker-compose.yml)，内置了 **MySQL 8.0**（带健康检查与自动初始化脚本）、**Nacos 2.3.0**、**Apache Seata Server 2.0.0**：

```bash
# 启动 MySQL + Nacos + Seata Server 中间件
docker compose up -d

# 检查各容器健康状态
docker compose ps
```

> **提示**：如果想将微服务也一并在 Docker 中运行，可先执行 `mvn clean package -DskipTests`，然后执行 `docker compose --profile apps up -d` 即可。

### 3. 数据库自动化版本迁移 (Flyway)
各微服务已深度整合 **Flyway 8.5+** 自动迁移工具：
- `order-service`：启动时自动扫描 `classpath:db/migration/V1.0__init_order_schema.sql`，自动建立订单主表、幂等表、undo_log、tcc_fence_log 和 saga 表。
- `storage-service`：启动时自动执行 `V1.0__init_storage_schema.sql`，自动建表并灌入商品 `C10001` (库存100件)。
- `account-service`：启动时自动执行 `V1.0__init_account_schema.sql`，自动建表并灌入测试用户 `U10001` (余额10000元)。
- 数据库连接字符串配置了 `createDatabaseIfNotExist=true`，应用启动时即使数据库尚不存在，MySQL 与 Flyway 也会全自动建库并迁移建表！

### 4. 编译与打包
在根目录 `d:\release\temp\seata-demo` 下运行：
```bash
mvn clean package -DskipTests
```

### 5. 启动微服务（本地开发建议按依赖顺序启动）
1. 启动库存微服务：`StorageApplication` (端口: 8082)
2. 启动账户微服务：`AccountApplication` (端口: 8083)
3. 启动订单微服务：`OrderApplication` (端口: 8081)

---

## 四、接口测试用例 (Curl 示例)

### 场景一：AT 模式测试

#### 1.1 正常下单提交 (事务全链路 Commit)
```bash
curl -X POST http://localhost:8081/order/at/create \
  -H "Content-Type: application/json" \
  -d '{
    "orderNo": "ORDER_AT_1001",
    "userId": "U10001",
    "commodityCode": "C10001",
    "count": 2,
    "money": 200.00,
    "mockException": false
  }'
```
- **预期结果**：返回 HTTP 200，`order_info` 插入成功，`storage_tbl` 库存减 2 (变为 98)，`account_tbl` 余额减 200 (变为 9800)。

#### 1.2 模拟异常回滚 (事务全链路 Rollback)
```bash
curl -X POST http://localhost:8081/order/at/create \
  -H "Content-Type: application/json" \
  -d '{
    "orderNo": "ORDER_AT_1002",
    "userId": "U10001",
    "commodityCode": "C10001",
    "count": 2,
    "money": 200.00,
    "mockException": true
  }'
```
- **预期结果**：抛出模拟异常，Seata AT 自动回滚，`order_info` 无此记录，库存与余额自动还原。

#### 1.3 幂等性验证 (防重提交)
使用相同的 `orderNo` 重复调用 1.1 接口：
```bash
# 再次执行 ORDER_AT_1001
curl -X POST http://localhost:8081/order/at/create \
  -H "Content-Type: application/json" \
  -d '{
    "orderNo": "ORDER_AT_1001",
    "userId": "U10001",
    "commodityCode": "C10001",
    "count": 2,
    "money": 200.00,
    "mockException": false
  }'
```
- **预期结果**：触发 `@Idempotent` 幂等切面拦截，直接命中缓存返回历史订单数据，库存与余额**绝不发生重复扣减**！

---

### 场景二：TCC 模式测试

#### 2.1 正常 Try -> Confirm 流程
```bash
curl -X POST http://localhost:8081/order/tcc/create \
  -H "Content-Type: application/json" \
  -d '{
    "orderNo": "ORDER_TCC_2001",
    "userId": "U10001",
    "commodityCode": "C10001",
    "count": 1,
    "money": 100.00,
    "mockException": false
  }'
```
- **预期过程**：
  1. Try 阶段：预留冻结库存 1，冻结金额 100。
  2. Confirm 阶段：扣除冻结库存，扣除冻结金额。
  3. 冻结记录状态更新为 `2` (CONFIRM)。

#### 2.2 异常 Try -> Cancel 回滚流程
```bash
curl -X POST http://localhost:8081/order/tcc/create \
  -H "Content-Type: application/json" \
  -d '{
    "orderNo": "ORDER_TCC_2002",
    "userId": "U10001",
    "commodityCode": "C10001",
    "count": 1,
    "money": 100.00,
    "mockException": true
  }'
```
- **预期过程**：抛出异常，TC 协调器自动调用 Cancel 方法，释放冻结库存与金额回可用状态，冻结记录状态更新为 `3` (CANCEL)。

---

### 场景三：SAGA 模式测试 (长事务与补偿)

#### 3.1 正常正向执行
```bash
curl -X POST http://localhost:8081/order/saga/create \
  -H "Content-Type: application/json" \
  -d '{
    "orderNo": "ORDER_SAGA_3001",
    "userId": "U10001",
    "commodityCode": "C10001",
    "count": 3,
    "money": 300.00,
    "mockException": false
  }'
```
- **预期结果**：依次扣减库存与余额，生成成功订单。

#### 3.2 逆向幂等补偿触发
```bash
curl -X POST http://localhost:8081/order/saga/create \
  -H "Content-Type: application/json" \
  -d '{
    "orderNo": "ORDER_SAGA_3002",
    "userId": "U10001",
    "commodityCode": "C10001",
    "count": 3,
    "money": 300.00,
    "mockException": true
  }'
```
- **预期结果**：检测到异常，逆向补偿自动触发：反向退还余额 -> 反向补回库存 -> 订单状态更新为 `CANCELLED`。SAGA 追踪表标记为已补偿，多次调用具备严格幂等性。

---

### 场景四：XA 模式测试 (强一致性 2PC)

#### 4.1 XA 提交
```bash
curl -X POST http://localhost:8081/order/xa/create \
  -H "Content-Type: application/json" \
  -d '{
    "orderNo": "ORDER_XA_4001",
    "userId": "U10001",
    "commodityCode": "C10001",
    "count": 1,
    "money": 500.00,
    "mockException": false
  }'
```

#### 4.2 XA 回滚
```bash
curl -X POST http://localhost:8081/order/xa/create \
  -H "Content-Type: application/json" \
  -d '{
    "orderNo": "ORDER_XA_4002",
    "userId": "U10001",
    "commodityCode": "C10001",
    "count": 1,
    "money": 500.00,
    "mockException": true
  }'
```
- **预期结果**：底层 MySQL 参与 XA 2PC 协调回滚，保持强一致性隔离。

---

## 五、企业实战核心注意事项总结

1. **RPC 超时与重试**：
   - 在分布式事务环境下，**强烈建议在 Dubbo 消费端将 `retries` 设置为 `0`**。若发生网络抖动引起超时，让全局事务管理器统一判定失败并触发二阶段回滚，避免 Dubbo 自动重试引发并发重复调用。
2. **幂等设计贯穿全流程**：
   - 接口入口：Token / 业务单号唯一性拦截（`@Idempotent` 切面）。
   - 存储层：数据库 `UNIQUE KEY` 唯一索引做最终底线防御。
   - 二阶段提交：TCC 与 SAGA 的 Cancel/Compensate 必须支持被多次调用而无副作用。
3. **空回滚与防悬挂**：
   - TCC 与 SAGA 必须记录状态，明确识别“未执行 Try 的 Cancel”以及“Cancel 之后迟到的 Try”，否则必然发生资金或库存遗失。
