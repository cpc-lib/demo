# Payment Demo · 后端服务（Spring Boot）

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.3.7.RELEASE-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/JDK-21%20(target%201.8)-orange.svg)](https://www.oracle.com/java/technologies/downloads/)
[![MyBatis-Plus](https://img.shields.io/badge/MyBatis--Plus-3.3.1-blue.svg)](https://baomidou.com/)
[![Swagger](https://img.shields.io/badge/Swagger-2.7.0-85ea2d.svg)](https://swagger.io/)

> 企业级支付集成演示后端，打通 **微信支付 V2/V3** 与 **支付宝沙箱**，覆盖
> **认证 → 商品/库存 → 购物车 → 合并下单 → 支付 → 退款 → 账单导入 → 对账** 全链路，
> 并以 **分布式锁 + 行锁/CAS + 幂等键 + Outbox/Inbox 可靠消息** 保障并发一致性。
>
> 本文件只描述后端子项目。仓库总览（含 React/Vue 前端）见 [根 README](../README.md)，代码结构深度介绍见 [CODE_INTRO.md](../CODE_INTRO.md)。

---

## 目录

- [1. 核心特性](#1-核心特性)
- [2. 技术栈](#2-技术栈)
- [3. 项目结构](#3-项目结构)
- [4. 分层架构与核心模块](#4-分层架构与核心模块)
- [5. 并发控制与可靠性](#5-并发控制与可靠性)
- [6. API 接口](#6-api-接口)
- [7. 数据库设计](#7-数据库设计)
- [8. 配置说明](#8-配置说明)
- [9. 快速开始](#9-快速开始)
- [10. 测试](#10-测试)
- [11. 相关文档](#11-相关文档)

---

## 1. 核心特性

### 支付与订单
- **微信支付 V3**：Native / JSAPI 下单、支付与退款通知、查单、退款、账单下载
- **微信支付 V2**：Native 下单、XML 通知处理
- **支付宝（沙箱）**：PC 网站支付、通知、查单、关单、退款、账单下载地址查询
- **订单**：购物车合并下单（多课程多份数）、我的订单、订单明细、状态查询、超时自动关闭并释放库存
- **下单幂等**：后端预签发一次性 `Idempotency-Key`，防重复点击 / Nginx 重试 / 多实例并发

### 账号与权限
- USER / ADMIN 两级角色，BCrypt 密码哈希
- JWT Access/Refresh Token 轮换，Refresh 走 HttpOnly Cookie
- 注册、登录、刷新、登出、查询当前用户、修改密码
- ADMIN 购买边界限制（禁止购物车/结算/退款申请），管理端可查看全部订单、审核退款、主动查单

### 商品与库存
- 商品列表查询（用户侧）
- 管理端商品增改、上下架、库存调整与库存流水查询
- 库存状态机：下单预占 → 支付实扣 → 取消/关闭释放 → 退款回补

### 退款
- 退款申请（按订单/明细）、审核通过 / 拒绝、状态查询与订单对账
- 审核通过后经 RabbitMQ 异步提交渠道，并定期同步渠道退款状态

### 账单与对账
- 渠道账单导入：自动拉取（微信/支付宝 API）或手动上传（微信 CSV/TXT/XLSX），账单 T+1 出账
- 微信进账逐笔匹配支付流水、退款逐笔匹配退款流水（支持跨日退款）
- 识别漏单 / 多单 / 金额不符 / 状态不符，生成对账报告并支持 CSV 导出
- 手动触发 + 定时自动（先导入账单再对账，两渠道互不影响）

### 支付配置
- 多商户、多渠道动态配置（`t_payment_channel` + `t_payment_app`，JSON 存储），支持运行时 reload

---

## 2. 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 2.3.7.RELEASE | web / validation / actuator / amqp / data-redis |
| JDK | 21 构建，字节码目标 1.8 | `maven-compiler-plugin` 固定 source/target=1.8 |
| MyBatis-Plus | 3.3.1 | ORM 增强（乐观锁、条件构造器） |
| MySQL Connector/J | 5.1.47 | 驱动 `com.mysql.jdbc.Driver`，MySQL 5.7+ |
| Redisson | 3.16.8 | 分布式锁（看门狗自动续期） |
| RabbitMQ (AMQP) | 随 Boot | 延迟消息：订单关闭、退款提交、退款状态同步 |
| JJWT | 0.11.5 | Access/Refresh Token 签发与校验 |
| spring-security-crypto | 随 Boot | BCrypt 密码哈希 |
| Apache POI (poi-ooxml) | 5.5.1 | 解析微信商户平台 XLSX 账单 |
| Springfox Swagger | 2.7.0 | API 文档（`/swagger-ui.html`） |
| wechatpay-apache-httpclient | 0.3.0 | 微信支付 V3 SDK |
| wxpay-sdk | 0.0.3 | 微信支付 V2 SDK |
| alipay-sdk-java | 4.22.57.ALL | 支付宝 SDK |
| Lombok | 1.18.32 | 简化实体代码 |

---

## 3. 项目结构

```
payment-demo/
├── src/main/java/cc/ivera/
│   ├── PaymentDemoApplication.java   # 启动类
│   ├── config/                       # 配置类（Redisson、WxPay、Alipay、Auth、Rabbit、对账定时、Swagger、WebMvc...）
│   ├── controller/                   # 控制层（API 入口）
│   │   └── support/WxPayNotifyHandler.java   # 微信 V3 通知统一处理
│   ├── security/                     # JWT、认证/管理员拦截器、AuthContext、AuthUser
│   ├── service/                      # 业务接口
│   │   ├── impl/                     # 业务实现
│   │   │   ├── reconciliation/       # 账单解析、逐笔匹配、对账编排
│   │   │   └── wxpay/                # 微信下单/退款/账单/HTTP/通知解码
│   │   ├── reconciliation/           # 对账领域接口
│   │   ├── refund/                   # 退款状态同步接口
│   │   └── wxpay/                    # 微信门面接口（Facade）
│   ├── mapper/                       # MyBatis Mapper 接口
│   ├── entity/                       # 数据库实体
│   ├── dto/                          # 请求 DTO
│   ├── vo/                           # 响应 VO（含统一响应 R）
│   ├── enums/                        # 枚举（含 alipay/、wxpay/ 子包）
│   ├── exception/                    # 业务异常（Biz/NotFound/Conflict/Forbidden/Unauthorized）
│   ├── handler/                      # 全局异常处理 GlobalExceptionHandler
│   ├── lock/                         # 分布式锁模板（DistributedLockTemplate / Redisson 实现）
│   ├── mq/                           # RabbitMQ 消费者与消息体、Outbox 事件类型
│   ├── task/                         # 定时任务（OutboxPublisher）
│   └── util/                         # 工具类（Money、OrderNo、Json、Http、指纹、V2 验签器）
├── src/main/resources/
│   ├── application.yml               # 主配置
│   ├── mapper/*.xml                  # MyBatis XML
│   ├── wxpay.properties              # 微信支付本地兜底配置
│   ├── alipay-sandbox.properties     # 支付宝沙箱配置
│   └── apiclient_key.pem             # 微信商户私钥（示例）
├── src/test/java/cc/ivera/           # 特征测试 + 单元/集成/契约测试
├── sql/payment-demo.sql              # 唯一完整数据库初始化脚本（19 张表）
├── docs/RABBITMQ_OPERATIONS.md       # RabbitMQ 运维清单
└── pom.xml
```

---

## 4. 分层架构与核心模块

```
Controller ──► Facade（微信门面） ──► Service ──► Mapper ──► Entity
     │                                  │
     └── 统一响应 R / GlobalExceptionHandler
                                        └── Lock（Redisson） / MQ（RabbitMQ） / Outbox-Inbox
```

| 模块 | 关键类 | 说明 |
|------|--------|------|
| 认证 | `AuthController` · `AuthServiceImpl` · `JwtTokenService` · `AuthInterceptor` · `AdminInterceptor` · `AuthContext` | 注册登录、Token 轮换、角色与购买边界校验 |
| 商品/库存 | `ProductController` · `AdminProductController` · `ProductServiceImpl` · `ProductAdminServiceImpl` · `InventoryServiceImpl` | 商品查询、管理端增改上下架、库存状态机 |
| 购物车/结算 | `CartController` · `CartServiceImpl` · `CheckoutServiceImpl` | 服务端购物车、多课程合并下单 |
| 订单 | `OrderInfoController` · `OrderInfoServiceImpl` · `OrderIdempotencyController` | 下单、复用未支付订单、状态查询、幂等键预签发 |
| 微信 V3 | `WxPayController` · `WxPayOrderService` · `WxPayRefundService` · `WxPayBillService` · `WxPayNotifyHandler` | 下单、通知、退款、账单 |
| 微信 V2 | `WxPayV2Controller` | V2 Native 下单与 XML 通知 |
| 支付宝 | `AliPayController` · `AliPayServiceImpl` | 网站支付、通知、查单、退款、账单 |
| 退款 | `RefundApplicationController` · `RefundInfoController` · `RefundApplicationServiceImpl` · `RefundInfoServiceImpl` | 申请、审核、状态同步 |
| 账单/对账 | `ChannelBillController` · `ReconciliationController` · `ChannelBillServiceImpl` · `ReconciliationServiceImpl` · `WxBillParser` · `AliPayBillParser` · `WxPaymentRefundMatcher` | 账单导入、逐笔匹配、差异报告 |
| 支付配置 | `PaymentChannelController` · `PaymentAppController` · `PaymentConfigController` · `PaymentConfigLoader` | 多渠道多商户动态配置与 reload |
| 可靠消息 | `OutboxPublisher` · `MessageOutboxServiceImpl` · `MessageConsumeLogServiceImpl` · `AdminOutboxController` | 事务性发件箱投递、消费幂等、运维重试 |

---

## 5. 并发控制与可靠性

### 5.1 三层并发控制

```
① 幂等检查        Redis SET NX + notifyId / Idempotency-Key，防重复处理通知与重复下单
        ▼
② Redisson 分布式锁  看门狗自动续期（默认 30s 租期、每 10s 续期），防业务执行中锁过期
        ▼
③ 行锁 + CAS       SELECT ... FOR UPDATE + UPDATE ... WHERE status = ...，数据库层兜底
```

锁模板：[`DistributedLockTemplate`](src/main/java/cc/ivera/lock/DistributedLockTemplate.java) / [`RedissonDistributedLockTemplate`](src/main/java/cc/ivera/lock/RedissonDistributedLockTemplate.java)

```java
lockTemplate.execute("payment:create:order:" + key, waitTime, leaseTime, () -> {
    // 业务逻辑；leaseTime = -1 时由看门狗自动续期
    return result;
});
```

订单表启用乐观锁（`@Version`），并通过多处唯一约束防重复写入：

| 表 | 约束 | 作用 |
|----|------|------|
| `t_order_info` | `uk_order_no` | 防商户订单号重复 |
| `t_payment_info` | `uk_order_payment_type` | 防同一订单同一支付方式重复写流水 |
| `t_payment_info` | `uk_transaction_id_payment_type` | 防同一渠道交易号重复写流水 |
| `t_refund_info` | `uk_refund_no` / `uk_refund_id` | 防商户/渠道退款单号重复 |

### 5.2 Outbox / Inbox 可靠消息

跨渠道副作用先随业务事务写入 **发件箱** `t_message_outbox`，再由 [`OutboxPublisher`](src/main/java/cc/ivera/task/OutboxPublisher.java) 定时批量投递到 RabbitMQ；消费端用 **收件箱** `t_message_consume_log` 做消费幂等。投递间隔、批量、租约、重试与最大次数由 `payment.outbox.*` / `payment.inbox.*` 控制，实现「至少一次 + 去重」。管理端可通过 `POST /api/admin/outbox/{eventId}/retry` 手动重投。

### 5.3 延迟消息（RabbitMQ）

| 用途 | 消费者 | 延迟参数 |
|------|--------|----------|
| 订单超时关闭并释放库存 | `OrderCloseConsumer` | `payment.order.close-delay-ms` |
| 退款审核后异步提交渠道 | `RefundSubmitConsumer` | — |
| 退款状态定期同步 | `RefundStatusSyncConsumer` | `payment.refund.status-sync-delay-ms` |

队列 / 交换机 / routing key 部署清单见 [docs/RABBITMQ_OPERATIONS.md](docs/RABBITMQ_OPERATIONS.md)。

### 5.4 定时对账

[`ReconciliationScheduleConfig`](src/main/java/cc/ivera/config/ReconciliationScheduleConfig.java)：渠道账单 T+1 出账，定时任务先自动导入昨日账单再对账，导入失败跳过当日、两渠道互不影响。默认微信 `10:30`、支付宝 `11:00`（`payment.reconciliation.*`）。

---

## 6. API 接口

统一前缀 `/api`，统一响应体 `R`（`code` / `message` / `data`）。完整实时文档见启动后的 Swagger：http://localhost:8080/swagger-ui.html

### 6.1 认证 `/api/auth`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/register` | 注册 USER 账号 |
| POST | `/login` | 登录，下发 Access Token 并设置 HttpOnly Refresh Cookie |
| POST | `/refresh` | 轮换 Refresh Token，返回新的短期 Access Token |
| POST | `/logout` | 撤销当前设备会话 |
| GET | `/me` | 查询当前登录用户 |
| PUT | `/password` | 修改密码 |

### 6.2 商品与库存

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/product/list` | 商品列表（用户侧） |
| GET | `/api/admin/products` | 商品列表（管理端） |
| POST | `/api/admin/products` | 新增商品 |
| PUT | `/api/admin/products/{id}` | 修改商品 |
| PATCH | `/api/admin/products/{id}/status` | 上下架 |
| POST | `/api/admin/products/{id}/stock-adjustments` | 库存调整 |
| GET | `/api/admin/products/{id}/stock-operations` | 库存流水 |

### 6.3 购物车 `/api/cart`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/cart` | 查看当前用户购物车 |
| POST | `/items` | 添加课程并累加数量 |
| PUT | `/items/{productId}` | 修改某课程数量 |
| DELETE | `/items/{productId}` | 移除某课程 |
| DELETE | `/api/cart` | 清空购物车 |

### 6.4 订单 `/api/order-info`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/checkout` | 将购物车合并创建一笔订单 |
| GET | `/my-list` | 当前用户订单 |
| GET | `/list` | 订单列表 |
| GET | `/{orderNo}/items` | 订单明细 |
| GET | `/query-order-status/{orderNo}` | 查询订单状态 |
| POST | `/api/order-info/idempotency-keys` | 预签发一次性下单幂等键 |

### 6.5 微信支付 V3 `/api/wx-pay`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/native/{productId}` | Native 扫码下单 |
| POST | `/native/order/{orderNo}` | 为已有未支付订单重新生成二维码 |
| POST | `/native/notify` | 支付结果通知 |
| POST | `/jsapi` | JSAPI 下单 |
| POST | `/jsapi/notify/v1` | JSAPI 支付通知 |
| POST | `/cancel/{orderNo}` | 取消订单 |
| GET | `/query/{orderNo}` | 查询订单 |
| GET | `/check-order-status/{orderNo}` | 主动查单并同步支付状态 |
| GET | `/query-refund/{refundNo}` | 查询退款 |
| POST | `/refunds/notify` | 退款结果通知 |
| GET | `/querybill/{billDate}/{type}` | 查询账单 |
| GET | `/downloadbill/{billDate}/{type}` | 下载账单 |

### 6.6 微信支付 V2 `/api/wx-pay-v2`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/native/{productId}` | V2 Native 下单 |
| POST | `/native/order/{orderNo}` | 为已有未支付订单重新生成 V2 二维码 |
| POST | `/native/notify` | V2 支付结果通知（XML） |

### 6.7 支付宝 `/api/ali-pay`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/trade/page/pay/{productId}` | PC 网站支付下单 |
| POST | `/trade/page/pay/order/{orderNo}` | 为已有未支付订单重新生成支付页 |
| POST | `/trade/notify` | 支付结果通知 |
| POST | `/trade/close/{orderNo}` | 关闭交易 |
| GET | `/trade/query/{orderNo}` | 查询交易 |
| GET | `/check-order-status/{orderNo}` | 主动查单并同步状态 |
| GET | `/trade/fastpay/refund/{refundNo}` | 查询退款 |
| GET | `/bill/downloadurl/query/{billDate}/{type}` | 查询账单下载地址 |

### 6.8 退款

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/refund-info/apply` | 申请退款（别名：`/api/wx-pay/refunds`、`/api/ali-pay/trade/refund`） |
| POST | `/api/refund-info/apply/{orderNo}/{reason}` | 申请退款·兼容旧路径（同名别名见上） |
| GET | `/api/refund-info/list` | 退款列表 |
| GET | `/api/refund-info/list/{orderNo}` | 按订单查退款 |
| POST | `/api/refund-info/approve/{refundNo}` | 审核通过 |
| POST | `/api/refund-info/reject/{refundNo}` | 审核拒绝 |
| POST | `/api/refund-info/query/{refundNo}` | 查询退款状态 |
| POST | `/api/refund-info/reconcile/{orderNo}` | 订单对账 |

### 6.9 账单导入 `/api/bill`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/auto-fetch` | 自动拉取渠道账单并导入 |
| POST | `/upload` | 手动上传账单文件导入（CSV/TXT/XLSX） |
| GET | `/list` | 已导入账单列表（分页） |
| GET | `/{id}` | 账单详情 |
| GET | `/{id}/records` | 账单解析记录（分页） |
| DELETE | `/{id}` | 删除账单（被对账引用时禁止） |

### 6.10 对账 `/api/reconciliation`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/execute` | 手动触发对账（需先导入账单） |
| GET | `/list` | 对账记录列表（分页） |
| GET | `/{id}` | 对账记录详情 |
| GET | `/{id}/details` | 对账明细 |
| GET | `/{id}/diff` | 差异明细 |
| GET | `/{id}/export` | 导出对账报告（CSV） |

### 6.11 支付配置

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/payment-channel/list` · `/list-all` · `/{id}` | 渠道查询 |
| POST | `/api/payment-channel` | 新增渠道 |
| PUT | `/api/payment-channel/{id}` | 修改渠道 |
| PATCH | `/api/payment-channel/{id}/status` | 启用/停用渠道 |
| DELETE | `/api/payment-channel/{id}` | 删除渠道 |
| GET | `/api/payment-app/list` · `/list-all` · `/list-by-channel/{channelId}` · `/{id}` · `/channels` | 应用查询 |
| POST | `/api/payment-app` | 新增应用 |
| PUT | `/api/payment-app/{id}` | 修改应用 |
| PATCH | `/api/payment-app/{id}/status` | 启用/停用应用 |
| DELETE | `/api/payment-app/{id}` | 删除应用 |
| POST | `/api/payment-config/reload` | 重载内存中的支付配置 |
| GET | `/api/payment-config/apps` | 查看当前生效的应用配置 |

### 6.12 运维

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/admin/outbox/{eventId}/retry` | 手动重投发件箱事件 |
| GET | `/api/test` | 连通性自测 |
| GET | `/actuator/health` | 健康检查（Spring Boot Actuator） |

---

## 7. 数据库设计

初始化脚本 [`sql/payment-demo.sql`](sql/payment-demo.sql) 是唯一完整脚本，共 **19 张表**，含初始商品、支付渠道/应用与演示管理员。

| 分组 | 表 |
|------|----|
| 认证与购物车 | `t_user`、`t_refresh_token`、`t_cart`、`t_cart_item` |
| 商品与库存 | `t_product`、`t_inventory_operation` |
| 订单 | `t_order_info`、`t_order_item`、`t_order_idempotency` |
| 支付与退款 | `t_payment_info`、`t_refund_info`、`t_refund_item` |
| 支付配置 | `t_payment_channel`、`t_payment_app` |
| 账单与对账 | `t_channel_bill`、`t_reconciliation`、`t_reconciliation_detail` |
| 可靠消息 | `t_message_outbox`、`t_message_consume_log` |

库存操作类型（`t_inventory_operation.operation_type`）：`ADMIN_ADJUST`、`ORDER_RESERVE`、`ORDER_COMMIT`、`ORDER_RELEASE`、`REFUND_RESTORE`。

---

## 8. 配置说明

主配置 [`src/main/resources/application.yml`](src/main/resources/application.yml)：

```yaml
server:
  port: 8080

spring:
  datasource:            # MySQL（驱动 com.mysql.jdbc.Driver）
    url: jdbc:mysql://<host>:3308/payment_demo_v1?serverTimezone=GMT%2B8&characterEncoding=utf-8&useSSL=false
    username: <user>
    password: <password>
  redis:                 # 缓存 / 分布式锁
    host: <host>
    port: 6379
  rabbitmq:              # 消息队列（开启 publisher confirm / returns）
    host: <host>
    port: 5672
  servlet:
    multipart:           # 手动上传账单大小限制
      max-file-size: 10MB
      max-request-size: 10MB

payment:
  auth:
    jwt-secret: ${PAYMENT_AUTH_JWT_SECRET:...}   # 生产必须用环境变量覆盖，≥32 字节
    access-token-seconds: 900
    refresh-token-seconds: 604800
    secure-cookie: false                          # 生产置 true
    allowed-origins: ${PAYMENT_AUTH_ALLOWED_ORIGINS:http://localhost:3000,http://localhost:8081}
  order:
    close-delay-ms: 60000
    idempotency-key-ttl-seconds: 120
  refund:
    status-sync-delay-ms: 60000
  reconciliation:
    enabled: true
    wx-cron: 0 30 10 * * ?
    ali-cron: 0 0 11 * * ?
  outbox:                # 发件箱：投递间隔/批量/租约/确认超时/重试/最大次数
    publish-interval-ms: 1000
    batch-size: 50
    max-attempts: 5
  inbox:
    lease-seconds: 30
```

支付渠道与商户配置存于数据库（`t_payment_channel.config_params`、`t_payment_app.app_config`，均为 JSON），支持多商户多渠道动态配置；微信/支付宝本地兜底配置见 `wxpay.properties`、`alipay-sandbox.properties`。

> 🔒 **安全提醒**：仓库内 `application.yml` 含演示用内网地址与口令，`apiclient_key.pem` 为商户私钥示例。生产部署前务必用环境变量覆盖 `PAYMENT_AUTH_JWT_SECRET` 等敏感项、启用 `secure-cookie`，并不要将真实密钥提交进版本库。初始化脚本中的演示管理员为 `admin`（初始密码 `Admin@123456`），**首次登录后请立即修改**。

---

## 9. 快速开始

### 9.1 环境要求

| 工具 | 版本 |
|------|------|
| JDK | 21（构建，字节码目标 1.8） |
| Maven | 3.6+ |
| MySQL | 5.7+ |
| Redis | 5.0+ |
| RabbitMQ | 3.7+ |

### 9.2 初始化数据库

脚本不含建库语句，请先建库（名称与 `application.yml` 一致，默认 `payment_demo_v1`）再导入：

```bash
mysql -u root -p -e "CREATE DATABASE payment_demo_v1 DEFAULT CHARSET utf8mb4;"
mysql -u root -p payment_demo_v1 < sql/payment-demo.sql
```

### 9.3 配置连接

修改 `src/main/resources/application.yml` 中的 MySQL / Redis / RabbitMQ 连接，并按需配置 `payment.auth.*`。RabbitMQ 队列与延迟参数的部署清单见 [docs/RABBITMQ_OPERATIONS.md](docs/RABBITMQ_OPERATIONS.md)。

### 9.4 启动

```bash
mvn clean install
mvn spring-boot:run
```

- 服务地址：http://localhost:8080
- Swagger 文档：http://localhost:8080/swagger-ui.html
- 健康检查：http://localhost:8080/actuator/health

前端（React / Vue）启动方式见 [根 README](../README.md#6-快速开始)。

---

## 10. 测试

测试位于 [`src/test/java/cc/ivera/`](src/test/java/cc/ivera/)，按治理规则分层：

| 层级 | 类型 | 位置 |
|------|------|------|
| L0 | 特征测试（锁住现状） | `characterization/` |
| L1 | 单元测试（业务逻辑/边界） | `service/impl/`、`util/` |
| L2 | 集成/契约测试（MQ、Schema、鉴权矩阵、控制器） | `mq/`、`schema/`、`security/`、`controller/` |

```bash
mvn test
```

后端全量约 **135** 个测试，覆盖对账与账单解析、认证/购物车/合单、库存并发、消息幂等、退款同步、鉴权矩阵等。测试规则与「特征测试全绿才可重构」等约束见 [AGENTS.md](../AGENTS.md) 与 [spec/governance/TESTING_GOVERNANCE_SPEC.md](../spec/governance/TESTING_GOVERNANCE_SPEC.md)。

---

## 11. 相关文档

| 文档 | 说明 |
|------|------|
| [根 README](../README.md) | Monorepo 总览（后端 + React + Vue + 治理文档） |
| [CODE_INTRO.md](../CODE_INTRO.md) | 后端代码结构深度介绍 |
| [AGENTS.md](../AGENTS.md) | 开发规则：Issue 分类、分支命名、测试分层、Spec 对账、完成定义 |
| [spec/README.md](../spec/README.md) | Spec 状态账本 |
| [docs/RABBITMQ_OPERATIONS.md](docs/RABBITMQ_OPERATIONS.md) | RabbitMQ 运维清单 |
| [sql/payment-demo.sql](sql/payment-demo.sql) | 数据库初始化脚本 |

---

> 本 README 依据当前后端代码与配置生成，力求与 `pom.xml`、`application.yml`、`sql/` 及各控制器实际路由一致。若描述与代码不符，请以代码为准并同步更新本文件与 [spec/README.md](../spec/README.md)。
