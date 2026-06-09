# CODE_INTRO.md

更新时间：2026-06-09  
读取范围：当前工作区完整项目，包含 Spring Boot 后端、React/Vue 前端、spec 治理资料、行动卡和近期 RabbitMQ 退款状态同步改动。  
当前分支：`feature/issue-1-refund-rabbitmq-sync`

本文档用于快速理解项目现状、入口调用面、职责拆分、隐式状态、并存规则和测试锚点。它描述当前行为，不把可疑行为直接判定为应修复问题。

## 1. 项目总览

这是一个支付演示系统，核心后端是 `payment-demo-v5`，前端同时保留 React 与 Vue 两套实现。业务围绕商品下单、微信支付 V2/V3、支付宝支付、订单关闭、退款申请、退款审批、退款状态同步、账单查询下载、支付渠道与支付应用配置管理展开。

项目里还包含一套轻量 spec 治理结构：

- `AGENTS.md`：协作规则、issue 分类、分支命名、测试要求、spec 对账规则。
- `spec/`：当前项目的 spec 状态账本。
- `live_spec_template/`：可复用的 spec 模板与样例。
- `materials/`：架构梳理、特征测试、安全重构、治理和 GitHub AI 流程行动卡。
- `PROJECT_ARCHITECTURE.html`：已有架构图。

## 2. 目录地图

```text
.
├── README.md
├── CODE_INTRO.md
├── AGENTS.md
├── PROJECT_ARCHITECTURE.html
├── materials/
├── spec/
├── live_spec_template/
├── payment-demo-v5/
│   ├── pom.xml
│   ├── README.md
│   ├── docs/
│   │   └── RABBITMQ_OPERATIONS.md
│   ├── sql/
│   └── src/
│       ├── main/java/cc/ivera/
│       ├── main/resources/
│       └── test/java/cc/ivera/
├── payment-demo-react/
└── payment-demo-vue/
```

主要模块：

- `payment-demo-v5`：唯一后端服务，默认端口 `8080`。
- `payment-demo-react`：React 18 + Vite + Ant Design 前端，默认开发端口 `3000`。
- `payment-demo-vue`：Vue 2 + Element UI 前端，保留同类业务页面。
- `spec` 与 `live_spec_template`：不参与运行时，只服务于行为锁定、重构和治理。

## 3. 后端技术栈

`payment-demo-v5/pom.xml` 显示后端基于：

- Java 8
- Spring Boot `2.3.7.RELEASE`
- Spring MVC、Validation
- MyBatis-Plus
- MySQL Connector/J
- Spring AMQP / RabbitMQ
- Redis 与 Redisson
- 微信支付 V3 SDK、微信支付 V2 SDK
- 支付宝 SDK
- Swagger 2
- JUnit 5、Mockito、Spring Boot Test

配置入口在 `payment-demo-v5/src/main/resources/application.yml`，包含服务端口、RabbitMQ、Redis、MySQL、MyBatis 日志和支付延迟参数。

## 4. 后端包职责地图

```text
cc.ivera
├── PaymentDemoApplication.java
├── config/         # 支付、RabbitMQ、MyBatis、Swagger、Redis/Redisson 配置
├── controller/     # HTTP API 入口
├── entity/         # MyBatis-Plus 实体
├── enums/          # 订单、退款、审批、支付类型枚举
├── mapper/         # MyBatis Mapper
├── mq/             # RabbitMQ 消息、生产者、消费者
├── service/        # 业务服务接口
├── service/impl/   # 业务服务实现
├── util/           # 支付相关工具类
└── vo/             # 请求/响应对象
```

职责块现状：

| 职责块 | 主要文件 | 说明 |
| --- | --- | --- |
| 商品查询 | `ProductController`, `ProductServiceImpl` | 为下单页面提供商品列表与测试入口。 |
| 订单创建与状态 | `OrderInfoController`, `OrderInfoServiceImpl`, `OrderInfoMapper.xml` | 创建订单、复用未支付订单、保存支付二维码、更新订单状态、查询订单状态。 |
| 支付流水 | `PaymentInfoServiceImpl`, `PaymentInfoMapper` | 记录微信 V2/V3 和支付宝支付通知流水，重复流水以日志形式视为幂等。 |
| 微信支付 V3/V2 | `WxPayController`, `WxPayV2Controller`, `WxPayOrderService`, `WxPayHttpClient`, `WxPayNotificationDecoder` | Native 支付、通知验签/解密、查单、关单、退款、账单。 |
| 支付宝支付 | `AliPayController`, `AliPayServiceImpl`, `AlipayClientConfig`, `AlipayProperties` | 电脑网站支付、异步通知、查单、关单、退款、账单 URL 查询。 |
| 退款申请与审批 | `RefundApplicationController`, `RefundApplicationServiceImpl`, `RefundInfoController`, `RefundInfoServiceImpl` | 退款申请创建、审批通过、审批拒绝、查询、人工修复、对账修复。 |
| 订单退款汇总 | `OrderRefundStatusService` | 将退款单状态聚合回订单退款状态。 |
| 支付配置管理 | `PaymentChannelController`, `PaymentAppController`, `PaymentConfigController`, `PaymentConfigLoader` | 管理渠道、应用与运行时支付配置缓存。 |
| 订单超时关闭 MQ | `OrderCloseRabbitConfig`, `OrderCloseMessageServiceImpl`, `OrderCloseConsumer`, `OrderCloseMessage` | 订单创建后发送延迟关闭消息，到期查询并关闭未支付订单。 |
| 退款状态同步 MQ | `RefundStatusSyncRabbitConfig`, `RefundStatusSyncMessageServiceImpl`, `RefundStatusSyncConsumer`, `RefundStatusSyncMessage` | 退款提交到渠道后发送延迟同步消息，到期查询渠道并同步本地退款状态。 |

## 5. HTTP 入口清单

所有 HTTP 入口都通过 `controller` 包暴露，返回值以 `R` 包装或直接返回字符串/SDK 响应。

### 5.1 商品与订单

| Controller | 入口 | 下游调用面 |
| --- | --- | --- |
| `ProductController` | `GET /api/product/test` | 简单字符串探活。 |
| `ProductController` | `GET /api/product/list` | `ProductService.list()`，前端商品页依赖。 |
| `OrderInfoController` | `GET /api/order-info/list` | `OrderInfoService.listOrderByCreateTimeDesc()`。 |
| `OrderInfoController` | `GET /api/order-info/query-order-status/{orderNo}` | 本地订单状态查询，前端轮询支付结果依赖。 |

### 5.2 微信支付

| Controller | 入口 | 下游调用面 |
| --- | --- | --- |
| `WxPayController` | `POST /api/wx-pay/native/{productId}` | 微信 V3 Native 下单，创建订单、调用渠道、保存 codeUrl。 |
| `WxPayController` | `POST /api/wx-pay/native/notify` | 微信 V3 支付通知验签/解密、记录支付流水、更新订单。 |
| `WxPayController` | `POST /api/wx-pay/cancel/{orderNo}` | 用户取消订单，本地状态与渠道关单。 |
| `WxPayController` | `GET /api/wx-pay/query/{orderNo}` | 微信 V3 查单。 |
| `WxPayController` | `GET /api/wx-pay/check-order-status/{orderNo}` | 查渠道并修复本地订单状态。 |
| `WxPayController` | `GET /api/wx-pay/query-refund/{refundNo}` | 微信 V3 查询退款。 |
| `WxPayController` | `POST /api/wx-pay/refunds/notify` | 微信退款通知处理。 |
| `WxPayController` | `GET /api/wx-pay/querybill/{billDate}/{type}` | 查询账单下载地址。 |
| `WxPayController` | `GET /api/wx-pay/downloadbill/{billDate}/{type}` | 下载账单内容。 |
| `WxPayController` | `POST /api/wx-pay/jsapi` | JSAPI 支付入口。 |
| `WxPayController` | `POST /api/wx-pay/jsapi/notify/v1` | JSAPI 通知入口。 |
| `WxPayV2Controller` | `POST /api/wx-pay-v2/native/{productId}` | 微信 V2 Native 下单。 |
| `WxPayV2Controller` | `POST /api/wx-pay-v2/native/notify` | 微信 V2 支付通知，XML 解析和验签路径。 |

### 5.3 支付宝

| Controller | 入口 | 下游调用面 |
| --- | --- | --- |
| `AliPayController` | `GET /api/ali-pay/trade/page/pay/{productId}` | 电脑网站支付表单生成，浏览器直接渲染依赖。 |
| `AliPayController` | `POST /api/ali-pay/trade/notify` | 支付宝异步通知验签、记录流水、更新订单。 |
| `AliPayController` | `POST /api/ali-pay/trade/close/{orderNo}` | 支付宝关单。 |
| `AliPayController` | `GET /api/ali-pay/trade/query/{orderNo}` | 支付宝查单。 |
| `AliPayController` | `POST /api/ali-pay/trade/fastpay/refund/{refundNo}` | 支付宝退款。 |
| `AliPayController` | `GET /api/ali-pay/bill/downloadurl/query/{billDate}/{type}` | 支付宝账单下载地址查询。 |

### 5.4 退款

| Controller | 入口 | 下游调用面 |
| --- | --- | --- |
| `RefundApplicationController` | `POST /api/refund-info/apply` | JSON 退款申请统一入口。 |
| `RefundApplicationController` | `POST /api/wx-pay/refunds` | JSON 微信退款申请兼容入口。 |
| `RefundApplicationController` | `POST /api/ali-pay/trade/refund` | JSON 支付宝退款申请兼容入口。 |
| `RefundApplicationController` | `POST /api/refund-info/apply/{orderNo}/{reason}` | 旧路径退款申请入口，前端或外部调用可能仍依赖。 |
| `RefundApplicationController` | `POST /api/wx-pay/refunds/{orderNo}/{reason}` | 旧路径微信退款申请入口。 |
| `RefundApplicationController` | `POST /api/ali-pay/trade/refund/{orderNo}/{reason}` | 旧路径支付宝退款申请入口。 |
| `RefundInfoController` | `GET /api/refund-info/list` | 全量退款列表。 |
| `RefundInfoController` | `GET /api/refund-info/list/{orderNo}` | 指定订单退款列表。 |
| `RefundInfoController` | `POST /api/refund-info/approve/{refundNo}` | 审批通过并提交渠道退款。 |
| `RefundInfoController` | `POST /api/refund-info/reject/{refundNo}` | 审批拒绝。 |
| `RefundInfoController` | `GET /api/refund-info/query/{refundNo}` | 查询单笔退款。 |
| `RefundInfoController` | `POST /api/refund-info/reconcile/{orderNo}` | 订单退款对账/修复入口。 |

### 5.5 支付配置

| Controller | 入口 | 下游调用面 |
| --- | --- | --- |
| `PaymentChannelController` | `/api/payment-channel/list-all`, `/list`, `/{id}`, `POST`, `PUT`, `PATCH /{id}/status`, `DELETE /{id}` | 支付渠道 CRUD 和状态切换。 |
| `PaymentAppController` | `/api/payment-app/list`, `/list-all`, `/list-by-channel/{channelId}`, `/{id}`, `POST`, `PUT`, `PATCH /{id}/status`, `DELETE /{id}`, `/channels` | 支付应用 CRUD、按渠道过滤、前端配置页依赖。 |
| `PaymentConfigController` | `POST /api/payment-config/reload` | 清空并重载运行时支付配置缓存。 |
| `PaymentConfigController` | `GET /api/payment-config/apps` | 当前可用支付应用查询。 |
| `TestController` | `GET /api/test` | 简单测试入口。 |

## 6. 关键业务流

### 6.1 下单与支付

1. 前端选择商品，调用微信或支付宝支付入口。
2. `OrderInfoServiceImpl` 根据商品、支付类型、支付应用等信息创建或复用订单。
3. 微信 Native 支付会调用渠道拿到 `codeUrl`，再保存到订单。
4. 支付通知到达后，通知处理器记录 `t_payment_info`，再更新 `t_order_info`。
5. 前端可通过本地订单状态查询或渠道查单入口轮询结果。

### 6.2 订单超时关闭

1. 订单创建后，`OrderInfoServiceImpl` 在事务提交后发送订单关闭延迟消息。
2. `OrderCloseConsumer` 消费消息。
3. 消费者查询本地订单和渠道状态。
4. 对仍未支付的订单执行关闭或修复。

### 6.3 退款申请、审批与同步

1. 退款入口创建本地退款申请和退款单。
2. 审批通过时，`RefundApplicationServiceImpl` 提交微信或支付宝渠道退款。
3. 渠道提交成功后，服务发送退款状态同步延迟消息。
4. `RefundStatusSyncConsumer` 到期查询退款渠道状态。
5. `RefundInfoServiceImpl` 同步本地退款状态，并通过 `OrderRefundStatusService` 聚合订单退款状态。

### 6.4 支付配置

1. 支付渠道与支付应用存储在数据库中。
2. `PaymentConfigLoader` 启动后加载配置到内存缓存。
3. 配置 CRUD 会影响数据库记录，运行时是否立即生效依赖重载入口和具体服务调用路径。

## 7. 数据库状态地图

SQL 初始化与升级脚本位于 `payment-demo-v5/sql/`。核心表：

| 表 | 主要含义 | 关键约束或行为 |
| --- | --- | --- |
| `t_product` | 演示商品 | 商品列表、下单金额来源。 |
| `t_order_info` | 订单 | `uk_order_no`；包含支付类型、订单状态、支付渠道、支付应用、二维码链接、退款汇总状态。 |
| `t_payment_info` | 支付流水 | `uk_order_payment_type`、`uk_transaction_id_payment_type`；重复流水被当作幂等路径处理。 |
| `t_refund_info` | 退款单 | `uk_refund_no`、`uk_refund_id`；包含审批状态、渠道退款状态、本地退款状态。 |
| `t_payment_channel` | 支付渠道 | `uk_channel_code`；如微信、支付宝。 |
| `t_payment_app` | 支付应用 | `uk_app_code`；绑定渠道与商户/应用配置。 |

Mapper XML 中存在兼容查询，例如部分订单查询会匹配 `payment_app_id = #{paymentAppId}` 或 `payment_app_id is null`，说明历史数据可能没有支付应用维度。

## 8. 隐式输入输出与运行时状态

| 类型 | 位置 | 现状 |
| --- | --- | --- |
| 数据库 | `application.yml`, `sql/`, `mapper/` | MySQL 是订单、支付、退款、配置的主状态源。 |
| RabbitMQ | `OrderCloseRabbitConfig`, `RefundStatusSyncRabbitConfig` | 使用延迟队列/死信交换机承载订单关闭和退款状态同步。 |
| Redis/Redisson | `application.yml`, `RedissonConfig` | 项目引入 Redis 与 Redisson，运行时配置存在，但并非所有业务路径都强依赖。 |
| 内存缓存 | `PaymentConfigLoader` | 使用 `ConcurrentHashMap` 缓存支付应用、渠道与渠道能力配置。 |
| 第三方配置 | `application.yml`, `PaymentConfigLoader`, `AlipayProperties`, `WxPayConfig` | 包含微信、支付宝商户号、应用号、证书、密钥、回调地址等敏感配置入口。 |
| 事务后事件 | `OrderInfoServiceImpl`, `RefundApplicationServiceImpl` | 订单关闭消息和退款状态同步消息倾向于在事务提交后发送。 |
| 日志 | 各 service/controller | 支付通知、退款、消息消费、重复流水、异常拒绝路径大量依赖日志记录。 |
| 前端 baseURL | `payment-demo-react/src/utils/request.js`, `payment-demo-vue/src/utils/request.js` | 两套前端均直接请求 `http://localhost:8080`。 |

## 9. 并存实现和并存规则

当前系统保留了多套实现或规则，重构前需要按现状锁住：

- 微信支付存在 V3 与 V2 两套 Native 支付、通知和工具链。
- 退款申请存在 JSON body 新入口，也保留路径参数旧入口。
- 前端存在 React 与 Vue 两套实现，接口意图相近但页面组织不同。
- 支付配置一部分来自数据库与缓存，一部分仍可从配置文件属性进入。
- 订单状态既可由支付通知更新，也可由主动查单/关闭/修复路径更新。
- 退款状态既可由渠道退款通知、延迟同步消息、手动查询和对账修复路径更新。
- 本地订单退款状态是聚合状态，和单笔退款单状态不是同一层规则。
- 部分接口返回 `R`，部分支付入口直接返回字符串、表单或渠道响应。

## 10. 可疑但需先锁定的现状

这些行为看起来可能需要治理，但在重构前应先以特征测试锁住：

- 全量 `mvn test` 在没有真实 MySQL/支付配置环境时可能因 SpringBoot 上下文加载失败，不代表特征测试失败。
- 配置文件与 SQL 示例包含真实形态的敏感字段，文档和测试应避免复写密钥值。
- `PaymentConfigLoader` 启动时会访问数据库，导致部分上下文测试天然依赖外部环境。
- 旧退款路径入口仍存在，可能被前端或外部脚本依赖。
- 支付通知重复流水通过唯一键和日志体现幂等，不能在重构时直接改成异常外抛。
- 部分修复/对账入口会主动查询渠道并改写本地状态，属于有副作用的查询型路径。
- 支付宝金额在部分路径存在元/分转换和字符串格式处理，需以现有测试锁住。
- 微信 V2 通知 XML 解析错误路径已有当前行为，不能因工具替换改变返回结构。
- 订单和退款 MQ 消息的空编号、缺失记录、渠道失败等拒绝路径需要原样保持日志和不崩溃特征。

## 11. 前端项目

### 11.1 React 前端

路径：`payment-demo-react/`

技术栈：

- React 18
- Vite
- Ant Design
- axios
- qrcode.react

关键位置：

- `src/utils/request.js`：axios 实例，baseURL 为 `http://localhost:8080`。
- `src/api/`：后端 API 包装，包含商品、订单、微信支付、支付宝、退款、账单、支付配置。
- 页面层围绕商品下单、订单列表、退款审批、支付配置等业务组织。

### 11.2 Vue 前端

路径：`payment-demo-vue/`

技术栈：

- Vue 2
- Vue CLI
- Element UI
- axios
- vue-router

关键位置：

- `src/utils/request.js`：axios 实例，baseURL 为 `http://localhost:8080`。
- `src/api/`：与 React 前端类似的 API 包装。
- 页面层保留旧版支付演示与管理能力。

## 12. 规格与治理资料

| 文件或目录 | 用途 |
| --- | --- |
| `AGENTS.md` | 当前仓库协作规则，包含 issue 分类、分支命名、测试要求、spec 对账和完成定义。 |
| `spec/README.md` | 最小 spec 状态账本，划分 `governance`、`planned`、`implemented`、`archived`。 |
| `spec/implemented/current-behavior/PAYMENT_DEMO_CURRENT_BEHAVIOR_SPEC.md` | 当前行为 spec，覆盖 public API、返回结构、拒绝路径、日志/MQ 等现状。 |
| `live_spec_template/spec/SPEC_TEMPLATE.md` | 可复用 spec 模板。 |
| `materials/*.md` | 任务行动卡，覆盖架构梳理、特征测试、安全重构、治理、GitHub issue 流程、质量飞轮。 |
| `PROJECT_ARCHITECTURE.html` | 架构图 HTML。 |

## 13. 测试现状

当前已有特征测试集中在 `payment-demo-v5/src/test/java/cc/ivera/characterization/` 以及退款状态同步相关测试。

重点测试：

- `PublicApiCharacterizationTest`：锁住公开 API 的返回结构、兼容入口、拒绝路径等。
- `InfrastructureBehaviorCharacterizationTest`：锁住支付通知解析、重复流水、MQ、退款聚合等基础设施行为。
- `RefundStatusSyncMessagingTest`：锁住退款状态同步消息发送、拒绝和消费行为。
- `RefundApplicationRabbitSyncTest`：锁住审批通过后发送同步消息，以及渠道提交失败不发送消息。

推荐的本地等价验证命令：

```powershell
cd payment-demo-v5
mvn "-Dtest=RefundStatusSyncMessagingTest,RefundApplicationRabbitSyncTest,PublicApiCharacterizationTest,InfrastructureBehaviorCharacterizationTest" test
```

单独运行特征测试：

```powershell
cd payment-demo-v5
mvn "-Dtest=PublicApiCharacterizationTest,InfrastructureBehaviorCharacterizationTest" test
```

注意：完整 `mvn test` 会运行 `AlipayTests`、`PaymentDemoApplicationTests` 等 SpringBoot 上下文测试。当前环境如果没有真实 MySQL、支付配置和网络依赖，可能失败。做业务重构前应优先运行上面的特征测试集合，并在具备完整环境时再跑全量。

## 14. RabbitMQ 运维补充

近期新增退款状态同步队列后，RabbitMQ 拓扑由两类延迟流程组成：

- 订单关闭：订单创建后延迟检查未支付订单。
- 退款状态同步：退款提交渠道成功后延迟查询渠道退款状态。

部署和运维清单位于：

- `payment-demo-v5/docs/RABBITMQ_OPERATIONS.md`

配置项位于：

```yaml
payment:
  order:
    close-delay-ms: 60000
  refund:
    status-sync-delay-ms: 60000
```

## 15. 新人阅读顺序

建议按以下顺序理解项目：

1. 读 `CODE_INTRO.md`，建立整体地图。
2. 读 `payment-demo-v5/README.md` 和根目录 `README.md`，理解支付配置改造背景。
3. 读 `spec/implemented/current-behavior/PAYMENT_DEMO_CURRENT_BEHAVIOR_SPEC.md`，确认当前行为契约。
4. 读 `PaymentDemoApplication.java`、`controller/`、`service/impl/`，串起运行时入口和业务主线。
5. 读 `config/` 与 `mq/`，理解隐式状态、缓存和消息流。
6. 读 `payment-demo-react/src/api/` 或 `payment-demo-vue/src/api/`，确认前端实际调用面。
7. 改代码前先读对应特征测试，并补充缺失行为锁定。

## 16. 改动高风险区

以下区域对外部行为影响较大，小步重构时应先补或复用特征测试：

- `RefundApplicationServiceImpl`
- `RefundInfoServiceImpl`
- `OrderInfoServiceImpl`
- `PaymentInfoServiceImpl`
- `PaymentConfigLoader`
- 微信/支付宝通知处理入口
- RabbitMQ consumer 与 message service
- `R` 返回包装结构
- 旧路径兼容入口
- `OrderInfoMapper.xml` 中兼容历史数据的查询条件

## 17. 当前未决问题

需要人工裁决或后续 issue 承接的点：

- 是否继续保留 React 与 Vue 两套前端，还是指定一套作为主维护面。
- 支付密钥、证书、回调域名是否迁移到环境变量或密钥管理。
- 全量 SpringBoot 上下文测试是否要改造为可在无外部 MySQL 的环境下稳定运行。
- 支付配置缓存与数据库配置的刷新边界是否要形成明确契约。
- 微信 V2 是否仍作为长期支持入口。
- 查询型接口中带副作用的修复/对账行为是否需要在未来 spec 中单独命名。
