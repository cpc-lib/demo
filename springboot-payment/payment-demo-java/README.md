# Payment Demo · 支付集成与企业级并发控制演示（Monorepo）

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.3.7.RELEASE-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/JDK-21%20(target%201.8)-orange.svg)](https://www.oracle.com/java/technologies/downloads/)
[![MyBatis-Plus](https://img.shields.io/badge/MyBatis--Plus-3.3.1-blue.svg)](https://baomidou.com/)
[![React](https://img.shields.io/badge/React-18.3-61dafb.svg)](https://react.dev/)
[![Vue](https://img.shields.io/badge/Vue-2.6-42b883.svg)](https://v2.vuejs.org/)

> 一个基于 Spring Boot 的企业级支付集成演示项目，打通 **微信支付 V2/V3** 与 **支付宝沙箱** 两大渠道，覆盖
> **登录鉴权 → 商品/库存 → 购物车 → 合并下单 → 支付 → 退款 → 账单导入 → 对账** 的完整链路，
> 并用 **分布式锁 + 数据库行锁/CAS + 幂等键 + Outbox/Inbox 可靠消息** 保障并发一致性。
> 同时提供 **React** 与 **Vue** 两套前端，方便对比不同框架的接入方式。

---

## 目录

- [1. 仓库结构](#1-仓库结构)
- [2. 子项目一览](#2-子项目一览)
- [3. 技术栈](#3-技术栈)
- [4. 核心功能模块](#4-核心功能模块)
- [5. 关键架构特性](#5-关键架构特性)
- [6. 快速开始](#6-快速开始)
- [7. 配置说明](#7-配置说明)
- [8. 数据库概览](#8-数据库概览)
- [9. 测试](#9-测试)
- [10. 文档与治理](#10-文档与治理)

---

## 1. 仓库结构

这是一个 monorepo，根目录同时容纳后端、两套前端以及规格治理文档：

```
payment-demo-java/
├── payment-demo/                 # 后端：Spring Boot 服务（Maven 单模块）
│   ├── src/main/java/cc/ivera/   # 业务代码（config/controller/service/mapper/entity/...）
│   ├── src/main/resources/       # application.yml、mapper XML、支付渠道配置
│   ├── src/test/java/cc/ivera/   # 特征测试 + 单元/集成测试
│   ├── sql/payment-demo.sql      # 唯一完整的数据库初始化脚本（19 张表）
│   ├── docs/RABBITMQ_OPERATIONS.md
│   └── pom.xml
├── payment-demo-react/           # 前端 A：React 18 + Vite + Ant Design 5
├── payment-demo-vue/             # 前端 B：Vue 2 + Element UI（功能与 React 对齐）
├── spec/                         # Spec 状态账本（governance/planned/implemented/archived）
│   └── README.md                 # 所有 Spec 的状态跟踪入口
├── docs/superpowers/             # 设计方案与实施计划（plans/ + specs/）
├── materials/                    # 架构/测试/治理行动卡
├── live_spec_template/           # Spec 治理模板
├── AGENTS.md                     # AI 开发规则（Issue 分类、分支、测试、对账）
├── CODE_INTRO.md                 # 后端代码结构深度介绍（含完整 API 清单）
└── README.md                     # 本文件
```

---

## 2. 子项目一览

| 子项目 | 角色 | 技术栈 | 默认端口 | 启动命令 |
|--------|------|--------|----------|----------|
| [`payment-demo/`](payment-demo/) | 后端服务 | Spring Boot 2.3.7 · MyBatis-Plus · Redis/Redisson · RabbitMQ | `8080` | `mvn spring-boot:run` |
| [`payment-demo-react/`](payment-demo-react/) | 前端（推荐） | React 18.3 · Vite 5.4 · Ant Design 5.21 · Axios | `3000` | `npm run dev` |
| [`payment-demo-vue/`](payment-demo-vue/) | 前端（对照） | Vue 2.6 · Vue CLI 4.5 · Element UI 2.15 · Axios | `3000` | `npm run serve` |

> ⚠️ **端口提示**：React 与 Vue 的开发服务器默认都监听 `3000`，二者不能同时启动；如需并行请修改其中一方的端口。
> 后端 CORS 默认放行 `http://localhost:3000` 与 `http://localhost:8081`（见 `payment.auth.allowed-origins`）。

---

## 3. 技术栈

### 3.1 后端

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 2.3.7.RELEASE | 核心框架（web / validation / actuator / amqp / data-redis） |
| JDK | 21 构建，字节码目标 1.8 | `pom.xml` 中 `maven-compiler-plugin` 固定 source/target=1.8 |
| MyBatis-Plus | 3.3.1 | ORM 增强 |
| MySQL Connector/J | 5.1.47 | 数据库驱动（`com.mysql.jdbc.Driver`） |
| Redisson | 3.16.8 | 分布式锁（看门狗自动续期） |
| RabbitMQ (AMQP) | Spring Boot 内置 | 延迟消息：订单关闭、退款提交、退款状态同步 |
| JJWT | 0.11.5 | Access/Refresh Token 签发与校验 |
| spring-security-crypto | 随 Boot | BCrypt 密码哈希 |
| Apache POI (poi-ooxml) | 5.5.1 | 解析微信商户平台 XLSX 账单 |
| Springfox Swagger | 2.7.0 | API 文档 |
| 微信支付 SDK (V3) | wechatpay-apache-httpclient 0.3.0 | V3 官方 SDK |
| 微信支付 SDK (V2) | wxpay-sdk 0.0.3 | V2 官方 SDK |
| 支付宝 SDK | alipay-sdk-java 4.22.57.ALL | 沙箱环境 |
| Lombok | 1.18.32 | 简化实体代码 |

### 3.2 前端

| 项目 | 框架 | UI | 构建 | 路由 | HTTP |
|------|------|----|------|------|------|
| payment-demo-react | React 18.3.1 | Ant Design 5.21.6 | Vite 5.4.8 | React Router 6.26.2 | Axios 1.7.7 |
| payment-demo-vue | Vue 2.6.11 | Element UI 2.15.6 | Vue CLI 4.5.0 | Vue Router 3.5.3 | Axios 0.24.0 |

---

## 4. 核心功能模块

后端控制器统一以 `/api/**` 暴露，按模块归类如下（完整方法级路径见 [CODE_INTRO.md](CODE_INTRO.md) 与启动后的 Swagger）：

| 模块 | 控制器 | 路由前缀 | 能力 |
|------|--------|----------|------|
| 认证与账号 | `AuthController` | `/api/auth` | 注册、登录、Access/Refresh Token 轮换、登出、改密；USER/ADMIN 两级角色 |
| 商品（用户侧） | `ProductController` | `/api/product` | 商品列表查询 |
| 商品/库存（管理侧） | `AdminProductController` | `/api/admin/products` | 商品增改、上下架、库存调整 |
| 购物车 | `CartController` | `/api/cart` | 服务端购物车，按用户隔离，多课程多份数 |
| 订单 | `OrderInfoController` | `/api/order-info` | 合并下单（checkout）、我的订单、订单状态查询 |
| 下单幂等 | `OrderIdempotencyController` | `/api/order-info/idempotency-keys` | 后端预签发一次性 `Idempotency-Key`，防重复点击/重试 |
| 微信支付 V3 | `WxPayController` | `/api/wx-pay` | Native/JSAPI 下单、支付/退款通知、查单、退款、账单下载 |
| 微信支付 V2 | `WxPayV2Controller` | `/api/wx-pay-v2` | V2 Native 下单与 XML 通知处理 |
| 支付宝 | `AliPayController` | `/api/ali-pay` | PC 网站支付、通知、查单、退款 |
| 退款 | `RefundInfoController` · `RefundApplicationController` | `/api/refund-info` | 退款申请、审核通过/拒绝、状态查询与同步 |
| 渠道账单导入 | `ChannelBillController` | `/api/bill` | 自动拉取 / 手动上传（CSV/TXT/XLSX），作为对账依据 |
| 对账 | `ReconciliationController` | `/api/reconciliation` | 手动/定时对账、明细与差异、报告导出 |
| 支付配置 | `PaymentChannelController` · `PaymentAppController` · `PaymentConfigController` | `/api/payment-channel` · `/api/payment-app` · `/api/payment-config` | 多商户、多渠道动态配置 |
| Outbox 运维 | `AdminOutboxController` | `/api/admin/outbox` | 可靠消息发件箱的查询与运维 |

通知处理统一收敛到 [`WxPayNotifyHandler`](payment-demo/src/main/java/cc/ivera/controller/support/WxPayNotifyHandler.java)。

### 前端页面（React / Vue 一一对应）

首页（商品）· 登录 · 购物车 · 我的订单 · 退款 · 支付配置 · 对账管理 · 下载账单 · 支付成功 · 账号安全。

---

## 5. 关键架构特性

### 5.1 三层并发控制

```
① 通知/请求幂等     Redis SET NX + notifyId / Idempotency-Key，防重复处理
        │
        ▼
② Redisson 分布式锁  看门狗自动续期（默认 30s 租期，每 10s 续期），防业务执行中锁过期
        │
        ▼
③ 数据库行锁 + CAS   SELECT ... FOR UPDATE + UPDATE ... WHERE status = ...，兜底一致性
```

- 锁模板：[`DistributedLockTemplate`](payment-demo/src/main/java/cc/ivera/lock/DistributedLockTemplate.java) / [`RedissonDistributedLockTemplate`](payment-demo/src/main/java/cc/ivera/lock/RedissonDistributedLockTemplate.java)
- 订单表乐观锁（`@Version`）+ 多处唯一约束（订单号、渠道交易号、退款单号）防重复写入

### 5.2 Outbox / Inbox 可靠消息

跨渠道、跨服务的副作用通过 **事务性发件箱（Outbox）** 落库，再由 [`OutboxPublisher`](payment-demo/src/main/java/cc/ivera/task/OutboxPublisher.java) 定时投递到 RabbitMQ；消费端用 **收件箱（Inbox / `t_message_consume_log`）** 做消费幂等，配合重试与租约（`payment.outbox.*` / `payment.inbox.*`）保证「至少一次 + 去重」。

### 5.3 延迟消息（RabbitMQ）

| 队列 | 用途 | 消费者 |
|------|------|--------|
| 订单关闭 | 下单后延迟关闭超时未支付订单并释放库存 | `OrderCloseConsumer` |
| 退款提交 | 退款审核通过后异步提交渠道 | `RefundSubmitConsumer` |
| 退款状态同步 | 定期回查并同步渠道退款状态 | `RefundStatusSyncConsumer` |

延迟时长由 `payment.order.close-delay-ms`、`payment.refund.status-sync-delay-ms` 控制。运维清单见 [RABBITMQ_OPERATIONS.md](payment-demo/docs/RABBITMQ_OPERATIONS.md)。

### 5.4 定时对账

[`ReconciliationScheduleConfig`](payment-demo/src/main/java/cc/ivera/config/ReconciliationScheduleConfig.java)：渠道账单 T+1 出账，定时任务先自动导入昨日账单再对账，导入失败则跳过当日、两渠道互不影响。默认微信 `10:30`、支付宝 `11:00`。

### 5.5 鉴权与统一响应

- JWT 认证：[`JwtTokenService`](payment-demo/src/main/java/cc/ivera/security/JwtTokenService.java) + `AuthInterceptor` / `AdminInterceptor`，`AuthContext` 提供当前用户与角色校验（如 `requireShoppingUser()` 限制 ADMIN 购买边界）
- 统一响应体 [`R`](payment-demo/src/main/java/cc/ivera/vo/R.java) + [`GlobalExceptionHandler`](payment-demo/src/main/java/cc/ivera/handler/GlobalExceptionHandler.java)，业务异常见 `exception/`（Biz/NotFound/Conflict/Forbidden/Unauthorized）

---

## 6. 快速开始

### 6.1 环境要求

| 工具 | 版本 |
|------|------|
| JDK | 21（构建，字节码目标 1.8） |
| Maven | 3.6+ |
| Node.js | 16+ |
| MySQL | 5.7+ |
| Redis | 5.0+ |
| RabbitMQ | 3.7+ |

### 6.2 初始化数据库

初始化脚本不含建库语句，请先创建数据库（名称与 `application.yml` 保持一致，默认 `payment_demo_v1`），再导入脚本：

```bash
mysql -u root -p -e "CREATE DATABASE payment_demo_v1 DEFAULT CHARSET utf8mb4;"
mysql -u root -p payment_demo_v1 < payment-demo/sql/payment-demo.sql
```

脚本会建好全部 19 张表，并写入初始商品、支付渠道/应用，以及一个演示管理员 `admin`（初始密码见 [payment-demo/README.md](payment-demo/README.md)，**首次登录后请立即在「账号安全」页修改**）。

### 6.3 启动后端

```bash
cd payment-demo
# 先在 src/main/resources/application.yml 中改好 MySQL / Redis / RabbitMQ 连接
mvn spring-boot:run
```

- 服务地址：http://localhost:8080
- Swagger 文档：http://localhost:8080/swagger-ui.html

### 6.4 启动前端（二选一）

```bash
# React（推荐）
cd payment-demo-react
npm install
npm run dev          # http://localhost:3000

# 或 Vue
cd payment-demo-vue
npm install
npm run serve        # http://localhost:3000
```

---

## 7. 配置说明

后端主配置 [`application.yml`](payment-demo/src/main/resources/application.yml) 关键项：

```yaml
server:
  port: 8080

spring:
  datasource:            # MySQL 连接
    url: jdbc:mysql://<host>:3308/payment_demo_v1?...
  redis:                 # 缓存 / 分布式锁
    host: <host>
  rabbitmq:              # 消息队列
    host: <host>

payment:
  auth:
    jwt-secret: ${PAYMENT_AUTH_JWT_SECRET:...}   # 生产必须用环境变量覆盖，≥32 字节
    access-token-seconds: 900
    refresh-token-seconds: 604800
    secure-cookie: false                          # 生产置 true
    allowed-origins: http://localhost:3000,http://localhost:8081
  order:
    close-delay-ms: 60000
    idempotency-key-ttl-seconds: 120
  refund:
    status-sync-delay-ms: 60000
  reconciliation:
    enabled: true
    wx-cron: 0 30 10 * * ?
    ali-cron: 0 0 11 * * ?
  outbox: { ... }        # 发件箱投递间隔、批量、租约、重试、最大次数
  inbox: { ... }         # 收件箱租约
```

支付渠道/商户配置存放在数据库（`t_payment_channel.config_params`、`t_payment_app.app_config`，均为 JSON），支持多商户多渠道动态配置，无需硬编码。微信/支付宝的本地兜底配置见 `wxpay.properties`、`alipay-sandbox.properties`。

> 🔒 **安全提醒**：仓库中的 `application.yml` 含演示用内网地址与口令，`apiclient_key.pem` 为商户私钥示例。生产部署前务必通过环境变量覆盖敏感项，并不要将真实密钥提交进版本库。

---

## 8. 数据库概览

共 19 张表，主要分组：

| 分组 | 表 |
|------|----|
| 认证与购物车 | `t_user`、`t_refresh_token`、`t_cart`、`t_cart_item` |
| 商品与库存 | `t_product`、`t_inventory_operation` |
| 订单 | `t_order_info`、`t_order_item`、`t_order_idempotency` |
| 支付与退款 | `t_payment_info`、`t_refund_info`、`t_refund_item` |
| 支付配置 | `t_payment_channel`、`t_payment_app` |
| 账单与对账 | `t_channel_bill`、`t_reconciliation`、`t_reconciliation_detail` |
| 可靠消息 | `t_message_outbox`、`t_message_consume_log` |

完整字段与建表语句见 [`payment-demo/sql/payment-demo.sql`](payment-demo/sql/payment-demo.sql)。

---

## 9. 测试

测试位于 [`payment-demo/src/test/java/cc/ivera/`](payment-demo/src/test/java/cc/ivera/)，按治理规则分层：

| 层级 | 类型 | 位置 |
|------|------|------|
| L0 | 特征测试（锁住现状） | `characterization/` |
| L1 | 单元测试（业务逻辑/边界） | `service/impl/`、`util/` |
| L2 | 集成/契约测试（MQ、Schema、鉴权矩阵） | `mq/`、`schema/`、`security/`、`controller/` |

运行：

```bash
cd payment-demo
mvn test
```

> 后端全量约 135 个测试；对账、账单解析、认证/购物车、库存并发、消息幂等等均有针对性覆盖。测试规则与「特征测试全绿才可重构」等约束见 [AGENTS.md](AGENTS.md) 与 [spec/governance/TESTING_GOVERNANCE_SPEC.md](spec/governance/TESTING_GOVERNANCE_SPEC.md)。

---

## 10. 文档与治理

| 文档 | 说明 |
|------|------|
| [CODE_INTRO.md](CODE_INTRO.md) | 后端代码结构深度介绍：分层架构、模块说明、完整 API 清单 |
| [AGENTS.md](AGENTS.md) | AI/协作开发规则：Issue 分类、分支命名、测试分层、Spec 对账、完成定义 |
| [spec/README.md](spec/README.md) | Spec 状态账本（governance / planned / implemented / archived） |
| [docs/superpowers/](docs/superpowers/) | 各功能的设计方案（specs/）与实施计划（plans/） |
| [materials/](materials/) | 架构梳理、特征测试、安全重构、Spec 治理等行动卡 |
| [payment-demo/docs/RABBITMQ_OPERATIONS.md](payment-demo/docs/RABBITMQ_OPERATIONS.md) | RabbitMQ 队列/交换机/延迟参数运维清单 |
| [payment-demo/README.md](payment-demo/README.md) | 后端子项目 README |

---

> 说明：本 README 依据当前仓库实际代码与配置生成，力求与 `pom.xml`、`application.yml`、`sql/` 及各子项目保持一致。若发现描述与代码不符，请以代码为准并同步更新本文件与 [spec/README.md](spec/README.md)。
