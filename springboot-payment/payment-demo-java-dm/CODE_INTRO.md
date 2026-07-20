# CODE_INTRO - Payment Demo V5 代码导览

## 项目概述

Payment Demo 是一个基于 Spring Boot 的支付集成演示项目，集成**微信支付 V2/V3** 和**支付宝**真实支付通道，实现企业级并发控制、幂等性保障机制，以及完整的**对账（Reconciliation）**功能。

- **技术栈**: Spring Boot 2.3.7 + Java 1.8 + MyBatis-Plus 3.3.1
- **数据库**: 达梦 DM8 (DmJdbcDriver18)
- **中间件**: Redis (Redisson), RabbitMQ
- **前端**: Vue.js (payment-demo-vue) + React (payment-demo-react)
- **API 文档**: Swagger 2.7.0

## 项目结构

```
payment-demo-java-dm/
├── payment-demo/              # 后端 Spring Boot 应用
│   ├── src/main/java/cc/ivera/
│   │   ├── config/            # 配置类
│   │   ├── controller/        # REST 控制器
│   │   ├── dto/               # 数据传输对象
│   │   │   └── reconciliation/   # 对账 DTO
│   │   ├── entity/            # 数据库实体
│   │   │   └── reconciliation/   # 对账实体
│   │   ├── enums/             # 枚举定义
│   │   │   └── reconciliation/   # 对账枚举
│   │   ├── exception/         # 异常定义
│   │   ├── handler/           # 全局异常处理
│   │   ├── job/               # 定时任务 (对账调度)
│   │   ├── lock/              # 分布式锁模板
│   │   ├── mapper/            # MyBatis-Plus Mapper
│   │   │   └── reconciliation/   # 对账 Mapper
│   │   ├── mq/                # RabbitMQ 消费者/生产者
│   │   ├── service/           # 业务接口与实现
│   │   │   └── reconciliation/   # 对账服务
│   │   │       ├── bill/      # 账单解析
│   │   │       └── channel/   # 渠道对账策略
│   │   └── util/              # 工具类
│   ├── src/main/resources/
│   │   ├── mapper/            # MyBatis XML
│   │   │   └── reconciliation/
│   │   ├── sql/               # SQL 脚本
│   │   └── application.yml    # 应用配置
│   └── env/sql/dm8/           # DM8 数据库初始化脚本
├── payment-demo-vue/          # Vue 前端 (主前端)
├── payment-demo-react/        # React 前端
├── live_spec_template/        # Spec 模板与规划
└── AGENTS.md                  # Agent 规则
```

## 核心架构

### 三层并发控制

| 层级 | 机制 | 说明 |
|------|------|------|
| 第一层 | 通知幂等检查 (Redis + notifyId) | 防止重复处理支付/退款通知 |
| 第二层 | Redisson 分布式锁 (看门狗) | 防止并发操作同一订单 |
| 第三层 | 数据库行锁 + CAS 状态更新 | 最终一致性保障 |

### 支付配置体系

支付参数从数据库动态加载，支持前端配置维护：

- `t_payment_channel` — 支付渠道公共配置 (WXPAY/ALIPAY)
- `t_payment_app` — 渠道下商户/应用配置 (appid/mchId/密钥等)
- 订单绑定 `payment_app_id` + `payment_channel_code`，不再硬编码

### 支付流程

1. **创建订单** → 从 `/api/payment-app/list` 加载启用的支付应用
2. **发起支付** → 根据 `paymentAppId` 动态构造真实客户端
3. **异步通知** → 按订单绑定配置校验金额/商户号/appId
4. **订单关闭** → RabbitMQ 延迟消息 (可配置 `payment.order.close-delay-ms`)
5. **退款** → 申请 → 审核 → 执行退款 → 延迟同步退款状态

### 对账架构（策略模式）

对账模块采用**策略模式**实现渠道解耦：

- `ChannelReconciliationStrategy` — 渠道对账策略接口
- `WxPayReconciliationStrategy` — 微信支付对账实现
- `AliPayReconciliationStrategy` — 支付宝对账实现
- `ChannelReconciliationStrategyFactory` — 策略工厂

对账流程：创建批次 → 下载账单 → 解析账单 → 本地匹配 → 差异识别 → 差异处理

## 数据库实体

| 实体 | 表名 | 说明 |
|------|------|------|
| `Product` | `t_product` | 支付产品 |
| `OrderInfo` | `t_order_info` | 订单 (含 version 乐观锁) |
| `PaymentInfo` | `t_payment_info` | 支付记录 |
| `PaymentChannel` | `t_payment_channel` | 支付渠道配置 |
| `PaymentApp` | `t_payment_app` | 支付应用配置 |
| `RefundInfo` | `t_refund_info` | 退款记录 |
| `ReconciliationBatch` | `t_reconciliation_batch` | 对账批次 |
| `ReconciliationDetail` | `t_reconciliation_detail` | 对账明细 |
| `ReconciliationDiscrepancy` | `t_reconciliation_discrepancy` | 对账差异 |
| `BaseEntity` | — | 公共基类 (id/createTime/updateTime) |

## 枚举定义

| 枚举 | 说明 |
|------|------|
| `PayType` | WXPAY / ALIPAY |
| `OrderStatus` | NOTPAY / SUCCESS / CLOSED / CANCEL / REFUNDING / REFUND |
| `RefundStatus` | 退款状态 |
| `RefundApprovalStatus` | 退款审核状态 |
| `alipay/AliPayTradeState` | 支付宝交易状态 |
| `wxpay/WxTradeState` | 微信交易状态 |
| `wxpay/WxRefundStatus` | 微信退款状态 |
| `wxpay/WxApiType` | 微信 API 类型 |
| `wxpay/WxNotifyType` | 微信通知类型 |
| `reconciliation/BatchStatus` | 对账批次状态 (PENDING/PROCESSING/COMPLETED/FAILED) |
| `reconciliation/MatchStatus` | 匹配状态 (MATCHED/MISMATCHED/LOCAL_ONLY/CHANNEL_ONLY) |
| `reconciliation/DiscrepancyType` | 差异类型 (OVERPAYMENT/UNDERPAYMENT/AMOUNT_MISMATCH等) |
| `reconciliation/DiscrepancyStatus` | 差异状态 (OPEN/RESOLVED) |

## Controller 路由

| Controller | 路径前缀 | 说明 |
|------------|----------|------|
| `ProductController` | `/api/product` | 产品管理 |
| `OrderInfoController` | `/api/order-info` | 订单管理 |
| `AliPayController` | `/api/alipay` | 支付宝支付 |
| `WxPayController` | `/api/wxpay` | 微信支付 |
| `WxPayV2Controller` | `/api/wxpay-v2` | 微信支付 V2 |
| `PaymentChannelController` | `/api/payment-channel` | 支付渠道配置 |
| `PaymentAppController` | `/api/payment-app` | 支付应用配置 |
| `PaymentConfigController` | `/api/payment-config` | 配置缓存管理 |
| `RefundApplicationController` | `/api/refund-application` | 退款申请/审核 |
| `RefundInfoController` | `/api/refund-info` | 退款查询 |
| `ReconciliationController` | `/api/reconciliation` | 对账管理 (批次/明细/差异/进度/汇总) |
| `TestController` | `/api/test` | 测试接口 |

## Service 层

| Service | 说明 |
|---------|------|
| `ProductService` | 产品 CRUD |
| `OrderInfoService` | 订单创建/查询/关闭/状态管理 |
| `PaymentInfoService` | 支付记录管理 |
| `AliPayService` | 支付宝支付/退款/查单/关单 |
| `wxpay/WxPayOrderFacade` | 微信订单门面 (V3) |
| `wxpay/WxPayRefundFacade` | 微信退款门面 (V3) |
| `wxpay/WxPayBillFacade` | 微信对账单门面 |
| `PaymentChannelService` | 支付渠道 CRUD |
| `PaymentAppService` | 支付应用 CRUD |
| `RefundInfoService` | 退款信息管理 |
| `RefundApplicationService` | 退款申请与审核 |
| `OrderCloseMessageService` | 发送订单关闭延迟消息 |
| `RefundStatusSyncMessageService` | 发送退款状态同步延迟消息 |
| `ReconciliationBatchService` | 对账批次管理 (创建/查询/执行/进度/汇总) |
| `ReconciliationMatchService` | 对账匹配服务 (账单与本地订单匹配) |

## MQ 消费者/生产者

| 组件 | 队列 | 说明 |
|------|------|------|
| `OrderCloseConsumer` | 订单关闭队列 | 处理超时未支付订单自动关闭 |
| `RefundStatusSyncConsumer` | 退款状态同步队列 | 延迟同步退款结果 |
| `ReconciliationExecuteProducer` | 对账执行队列 | 发送对账执行消息 |
| `ReconciliationExecuteConsumer` | 对账执行队列 | 异步执行对账任务 |

## 分布式锁

- `DistributedLockTemplate` — 锁模板接口
- `RedissonDistributedLockTemplate` — 基于 Redisson 实现，支持看门狗自动续期

## 关键工具类

| 工具类 | 说明 |
|--------|------|
| `HttpClientUtils` | HTTP 请求工具 |
| `HttpUtils` | HTTP 工具 |
| `JsonUtils` | JSON 序列化/反序列化 |
| `MoneyUtils` | 金额计算 (元/分转换) |
| `OrderNoUtils` | 订单号生成 |

## 前端项目

### payment-demo-vue (主前端)
- 技术: Vue.js + Vue Router + Axios + Element UI
- 页面: Home(支付页), Orders(订单), PaymentConfig(支付配置), Reconciliation(对账管理), Success(成功页), Download(下载)

### payment-demo-react
- 技术: React + Vite + Ant Design
- 页面与功能与 Vue 版本对等，含 Reconciliation(对账管理) 页面

## 环境要求

| 组件 | 版本 |
|------|------|
| JDK | 1.8+ |
| Maven | 3.6+ |
| 达梦 DM8 | 8.x |
| Redis | 5.0+ |
| RabbitMQ | 3.7+ |

## 快速启动

```powershell
# 1. 配置 application.yml 中的 Redis/RabbitMQ/DM 连接

# 2. 启动后端 (首次启动自动创建对账相关表)
cd payment-demo
mvn spring-boot:run

# 3. 启动前端 (Vue)
cd payment-demo-vue
npm install
npm run serve

# 4. 启动前端 (React, 可选)
cd payment-demo-react
npm install
npm run dev
```

## Spec 治理

项目使用 `live_spec_template/spec/` 作为行为状态台账：
- `spec/planned/` — 规划中的变更（含对账功能规格说明）
- `spec/implemented/` — 已实现的行为
- `spec/governance/` — 治理规范
- `spec/archived/` — 废弃/归档决策

详见 [AGENTS.md](AGENTS.md)。
