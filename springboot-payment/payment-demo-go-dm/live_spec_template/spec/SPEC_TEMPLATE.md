# <SPEC 名称> SPEC

> 适用项目：Payment Demo V5  
> 写作目的：把支付系统中的“接口、状态、流程、兼容行为、测试口径”写成可持续对账的活文档。  
> 使用方式：涉及订单、支付、退款、支付配置、通知回调、账单、并发幂等、前端调用面的设计型改动，都先复制本模板创建一条 spec。

## 0. 元信息

- **状态**：planned | implemented | archived | governance
- **领域（feature-domain）**：order | wxpay | alipay | refund | payment-config | bill | frontend | concurrency | governance | other
- **更新于**：<YYYY-MM-DD>
- **负责人**：<who>
- **关联 issue / PR / 任务**：<#N / link / brief>
- **影响范围**：backend | react-frontend | vue-frontend | database | redis | rabbitmq | external-provider | docs | tests

## 1. 背景 / 目标

<说明为什么需要这条 spec。聚焦本次要稳定下来的业务契约，而不是实现方案。>

需要回答：

- 当前业务场景是什么？例如：微信 Native 下单、支付宝回调、退款审核、支付应用配置维护。
- 现在有哪些调用方或下游依赖？例如：React/Vue 前端、微信/支付宝服务端通知、RabbitMQ 延迟关单、数据库历史数据。
- 这条 spec 要锁住哪些行为？哪些只是记录现状，暂不判断对错？

## 2. 当前现状（重构前必须承认）

### 2.1 入口清单

| 入口 | 类型 | 当前调用方 | 备注 |
|---|---|---|---|
| `<HTTP method /api/...>` | REST | `<React/Vue/第三方/人工>` | `<响应格式、兼容路径、是否回调>` |
| `<Rabbit queue>` | Message | `<producer/consumer>` | `<TTL、routing key、幂等键>` |
| `<frontend route>` | UI | `<用户>` | `<对应页面和 API 封装>` |

### 2.2 现有实现锚点

| 层 | 文件 / 类 / 函数 | 现有职责 |
|---|---|---|
| Controller | `<path>::<method>` | `<参数校验、回调响应、适配>` |
| Service | `<path>::<method>` | `<业务编排、状态迁移、外部调用>` |
| Mapper / SQL | `<path>` | `<查询、for update、唯一约束>` |
| Frontend | `<path>` | `<页面、请求封装、展示>` |
| Config / MQ / Cache | `<path>` | `<配置加载、锁、事件>` |

### 2.3 现有可疑行为

> 这里记录“看起来像 bug，但重构前要先原样锁住”的行为。不要在本 spec 里顺手修。

- `<可疑行为 1>`：<实际表现、触发条件、涉及文件>
- `<可疑行为 2>`：<实际表现、触发条件、涉及文件>

## 3. 契约（对外行为，必须稳定）

### 3.1 输入 / 调用

- **HTTP 接口**：`<method> <path>`
- **请求参数**：`<path variable / query / body / header>`
- **消息输入**：`<queue / exchange / routing key / body>`
- **外部回调输入**：`<微信/支付宝通知字段、验签要求、成功响应格式>`
- **前端入口**：`<React/Vue route、API module>`

### 3.2 输出 / 响应

- **成功响应**：`<R<T> / XML / success string / HTML form / codeUrl>`
- **失败响应**：`<错误码、message、HTTP 状态或第三方要求>`
- **数据库变化**：`<插入/更新哪些表，哪些字段变化>`
- **事件输出**：`<RabbitMQ 消息、Redis key、日志>`
- **外部调用**：`<微信/支付宝 API、请求时机、失败语义>`

### 3.3 状态迁移

#### 订单状态

| 起始状态 | 触发 | 目标状态 | 当前规则 / 约束 |
|---|---|---|---|
| `<NOTPAY/中文状态值>` | `<支付通知/查单/取消/延迟关单>` | `<SUCCESS/CLOSED/...>` | `<条件更新、for update、锁 key>` |

#### 退款状态

| 审核状态 | 退款状态 | 触发 | 目标状态 | 当前规则 / 约束 |
|---|---|---|---|---|
| `<PENDING/APPROVED/...>` | `<CREATED/PROCESSING/...>` | `<申请/审核/渠道同步>` | `<...>` | `<金额预占、幂等、补录>` |

### 3.4 不变量 / 边界

- 同一 `<productId + paymentType + paymentAppId>` 下未支付订单是否复用：<是/否/条件>。
- 支付通知重复投递的处理方式：<Redis key / DB 唯一约束 / 状态条件更新>。
- 订单绑定支付应用后，查单、关单、退款、通知校验如何选择配置：<规则>。
- 前端和第三方依赖的路径、响应格式、状态文案：<列出不能随意改变的部分>。

## 4. 隐式输入输出 / 状态地图

| 类型 | 名称 / 位置 | 读 | 写 | TTL / 生命周期 | 备注 |
|---|---|---|---|---|---|
| DB | `t_order_info` | `<who>` | `<who>` | 持久 | `<唯一约束/索引/状态字段>` |
| DB | `t_payment_info` | `<who>` | `<who>` | 持久 | `<流水幂等>` |
| DB | `t_refund_info` | `<who>` | `<who>` | 持久 | `<退款状态>` |
| DB | `t_payment_channel/t_payment_app` | `<who>` | `<who>` | 持久 + 缓存 | `<JSON 配置>` |
| Redis | `<key prefix>` | `<who>` | `<who>` | `<TTL>` | `<锁/幂等>` |
| RabbitMQ | `<exchange/queue/routing key>` | `<consumer>` | `<producer>` | `<TTL>` | `<延迟关单>` |
| Config | `<application.yml/properties/pem>` | `<who>` | `<manual>` | 启动期 | `<敏感信息>` |
| Log | `<logger>` | n/a | `<who>` | 日志保留策略 | `<是否含回调原文>` |

## 5. 多套实现 / 多套规则并存

> 记录历史包袱和兼容面。这里不是合并方案，只说明现状。

- 微信支付：`V2` 与 `V3` 是否并存：<入口、验签、通知、配置差异>。
- 支付配置：数据库动态配置与 properties fallback 是否并存：<规则>。
- 前端：React 与 Vue 是否都需要保持兼容：<页面/API 封装>。
- 退款入口：新 JSON body 与旧 path variable 兼容入口：<路径>。
- 幂等规则：Redis、Redisson、DB 行锁、唯一约束、状态条件更新分别覆盖什么。

## 6. 验收标准

> 验收标准必须可测试。不要写“优化体验”“保证稳定”这类不可判定句子。

- [ ] `<接口/流程>` 在 `<输入>` 下返回 `<明确输出>`。
- [ ] `<数据库表/字段>` 在 `<触发>` 后变为 `<明确状态>`。
- [ ] 重复调用 / 重复通知 / 并发场景保持当前幂等行为。
- [ ] 第三方回调成功/失败响应格式保持不变。
- [ ] React 和 Vue 仍能调用对应 API，或已明确某一端不适用。
- [ ] 文档、示例、测试、spec 状态与实现一致。

## 7. 特征测试清单

> planned 阶段写“应该补哪些特征测试”；implemented 阶段把测试文件锚点补上。  
> 老代码先补特征测试锁现状，再谈重构。

### 7.1 第一批必须锁住

- [ ] API 契约：`<method path>` 的成功响应、失败响应、字段结构。
- [ ] 状态迁移：`<起始状态 + 触发>` 后的订单/退款状态。
- [ ] 幂等：重复通知、重复支付流水、重复退款审核。
- [ ] 配置选择：默认配置、指定 `paymentAppId`、禁用配置、fallback。
- [ ] 外部回调：微信 XML/JSON、支付宝 form 参数验签后的响应文本。
- [ ] 异步消息：延迟关单消息发送和消费行为。

### 7.2 测试锚点

| 测试类型 | 文件 | 覆盖行为 |
|---|---|---|
| 单元测试 | `<path>` | `<service/util/state rule>` |
| Web/API 测试 | `<path>` | `<controller contract>` |
| 集成测试 | `<path>` | `<DB/Redis/RabbitMQ interaction>` |
| 前端测试 | `<path>` | `<route/API wrapper behavior>` |

## 8. 兼容性影响

- **HTTP 路径**：<是否新增/变更/废弃；旧路径如何处理>
- **响应结构**：<是否改变 `R<T>`、XML、HTML form、字符串响应>
- **数据库结构**：<是否需要完整初始化脚本和增量升级脚本同步>
- **状态值**：<是否改变中文状态文案、枚举值、退款状态字符串>
- **配置字段**：<是否改变 `payment_channel.config_params` / `payment_app.app_config` JSON>
- **前端影响**：<React/Vue 是否都要同步>
- **第三方平台配置**：<微信/支付宝回调地址、证书、公钥、密钥是否受影响>

## 9. 实现锚点（implemented 时必须填写）

- Controller：`<path>::<method>`
- Service：`<path>::<method>`
- Mapper / SQL：`<path>`
- Entity / DTO / Enum：`<path>`
- Frontend：`<path>`
- Config / MQ / Cache：`<path>`
- Tests：`<path>`
- Docs / Examples：`<path>`

## 10. 回滚 / 观测

- **回滚方式**：<代码回滚、配置回滚、DB 回滚或兼容开关>
- **需要观察的日志**：<关键 logger / message>
- **需要观察的数据**：<订单状态、支付流水、退款状态、Redis key、RabbitMQ 队列>
- **失败时对外表现**：<前端提示、第三方是否重试、是否会重复通知>

## 11. 变更记录

| 日期 | 状态 | 改了什么 | 关联 issue / PR |
|---|---|---|---|
| <YYYY-MM-DD> | planned | 初稿 | <#N> |

