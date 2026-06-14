# Payment Demo 项目代码结构介绍

## 项目概述

这是一个基于 Spring Boot 的支付集成演示项目，集成了**微信支付 V2/V3** 和**支付宝**支付渠道，实现了完整的订单管理、支付流程、退款管理等功能，并采用企业级并发控制架构确保数据一致性。

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 2.3.7 | 核心框架 |
| Java | 1.8 | 开发语言 |
| MyBatis-Plus | 3.3.1 | ORM框架 |
| MySQL | 5.7+ | 关系型数据库 |
| Redis | 5.0+ | 缓存/分布式锁 |
| Redisson | 3.16.8 | 分布式锁框架（看门狗机制） |
| RabbitMQ | 3.7+ | 消息队列（延迟消息） |
| Swagger | 2.7.0 | API文档 |
| WechatPay SDK | 0.3.0/0.0.3 | 微信支付V3/V2 SDK |
| Alipay SDK | 4.22.57 | 支付宝SDK |

## 前端技术栈

| 项目 | 框架 | UI组件库 | 构建工具 |
|------|------|----------|----------|
| payment-demo-react | React 18 | Ant Design 5 | Vite 5 |
| payment-demo-vue | Vue 2 | Element UI 2 | Vue CLI 4 |

## 项目结构

```
payment-demo-java/
├── payment-demo/                          # 后端服务（Spring Boot）
│   ├── src/main/java/cc/ivera/
│   │   ├── PaymentDemoApplication.java    # 启动类
│   │   ├── config/                        # 配置层
│   │   │   ├── AlipayClientConfig.java    # 支付宝客户端配置
│   │   │   ├── AlipayProperties.java      # 支付宝属性配置
│   │   │   ├── MyBatisPlusConfig.java     # MyBatis-Plus配置
│   │   │   ├── OrderCloseRabbitConfig.java           # 订单关闭延迟队列配置
│   │   │   ├── PaymentAppConfig.java                 # 支付应用配置
│   │   │   ├── PaymentConfigLoader.java              # 支付配置加载器
│   │   │   ├── RedissonConfig.java                   # Redisson分布式锁配置
│   │   │   ├── RefundStatusSyncRabbitConfig.java     # 退款状态同步队列配置
│   │   │   ├── Swagger2Config.java                   # Swagger文档配置
│   │   │   └── WxPayConfig.java                      # 微信支付配置
│   │   ├── controller/                    # 控制层
│   │   │   ├── support/
│   │   │   │   └── WxPayNotifyHandler.java           # 微信支付通知处理器
│   │   │   ├── AliPayController.java                 # 支付宝接口
│   │   │   ├── OrderInfoController.java              # 订单管理接口
│   │   │   ├── PaymentAppController.java             # 支付应用接口
│   │   │   ├── PaymentChannelController.java         # 支付渠道接口
│   │   │   ├── PaymentConfigController.java          # 支付配置接口
│   │   │   ├── ProductController.java                # 商品接口
│   │   │   ├── RefundApplicationController.java      # 退款申请接口
│   │   │   ├── RefundInfoController.java             # 退款管理接口
│   │   │   ├── TestController.java                   # 测试接口
│   │   │   ├── WxPayController.java                  # 微信支付V3接口
│   │   │   └── WxPayV2Controller.java                # 微信支付V2接口
│   │   ├── dto/                           # 数据传输对象
│   │   │   ├── PaymentAppRequest.java                # 支付应用请求DTO
│   │   │   ├── PaymentChannelRequest.java            # 支付渠道请求DTO
│   │   │   ├── PaymentStatusRequest.java             # 支付状态请求DTO
│   │   │   ├── RefundApproveRequest.java             # 退款审核请求DTO
│   │   │   └── RefundRequest.java                    # 退款请求DTO
│   │   ├── entity/                        # 实体层
│   │   │   ├── BaseEntity.java                       # 基础实体（包含公共字段）
│   │   │   ├── OrderInfo.java                        # 订单实体
│   │   │   ├── PaymentApp.java                       # 支付应用实体
│   │   │   ├── PaymentChannel.java                   # 支付渠道实体
│   │   │   ├── PaymentInfo.java                      # 支付信息实体
│   │   │   ├── Product.java                          # 商品实体
│   │   │   └── RefundInfo.java                       # 退款信息实体
│   │   ├── enums/                         # 枚举层
│   │   │   ├── alipay/
│   │   │   │   └── AliPayTradeState.java             # 支付宝交易状态枚举
│   │   │   ├── wxpay/
│   │   │   │   ├── WxApiType.java                    # 微信API类型枚举
│   │   │   │   ├── WxNotifyType.java                 # 微信通知类型枚举
│   │   │   │   ├── WxRefundStatus.java               # 微信退款状态枚举
│   │   │   │   └── WxTradeState.java                 # 微信交易状态枚举
│   │   │   ├── OrderStatus.java                      # 订单状态枚举
│   │   │   ├── PayType.java                          # 支付类型枚举
│   │   │   ├── RefundApprovalStatus.java             # 退款审核状态枚举
│   │   │   └── RefundStatus.java                     # 退款状态枚举
│   │   ├── exception/                     # 异常层
│   │   │   ├── BizException.java                     # 业务异常
│   │   │   └── handler/
│   │   │       └── GlobalExceptionHandler.java       # 全局异常处理器
│   │   ├── handler/                       # 处理器层
│   │   │   └── GlobalExceptionHandler.java           # 全局异常处理器
│   │   ├── lock/                          # 分布式锁
│   │   │   ├── DistributedLockTemplate.java          # 分布式锁模板接口
│   │   │   └── RedissonDistributedLockTemplate.java  # Redisson锁实现
│   │   ├── mapper/                        # 数据访问层
│   │   │   ├── OrderInfoMapper.java                  # 订单Mapper
│   │   │   ├── PaymentAppMapper.java                 # 支付应用Mapper
│   │   │   ├── PaymentChannelMapper.java             # 支付渠道Mapper
│   │   │   ├── PaymentInfoMapper.java                # 支付信息Mapper
│   │   │   ├── ProductMapper.java                    # 商品Mapper
│   │   │   └── RefundInfoMapper.java                 # 退款信息Mapper
│   │   ├── mq/                            # 消息队列
│   │   │   ├── OrderCloseConsumer.java               # 订单关闭消息消费者
│   │   │   ├── OrderCloseMessage.java                # 订单关闭消息体
│   │   │   ├── RefundStatusSyncConsumer.java         # 退款状态同步消费者
│   │   │   └── RefundStatusSyncMessage.java          # 退款状态同步消息体
│   │   ├── service/                       # 业务服务层
│   │   │   ├── impl/
│   │   │   │   ├── wxpay/
│   │   │   │   │   ├── WxPayBillService.java         # 微信对账服务
│   │   │   │   │   ├── WxPayHttpClient.java          # 微信HTTP客户端
│   │   │   │   │   ├── WxPayNotificationDecoder.java # 微信通知解码器
│   │   │   │   │   ├── WxPayOrderService.java        # 微信订单服务
│   │   │   │   │   └── WxPayRefundService.java       # 微信退款服务
│   │   │   │   ├── AliPayServiceImpl.java            # 支付宝服务实现
│   │   │   │   ├── OrderCloseMessageServiceImpl.java # 订单关闭消息服务
│   │   │   │   ├── OrderInfoServiceImpl.java         # 订单服务实现
│   │   │   │   ├── PaymentAppServiceImpl.java        # 支付应用服务实现
│   │   │   │   ├── PaymentChannelServiceImpl.java    # 支付渠道服务实现
│   │   │   │   ├── PaymentInfoServiceImpl.java       # 支付信息服务实现
│   │   │   │   ├── ProductServiceImpl.java           # 商品服务实现
│   │   │   │   ├── RefundApplicationServiceImpl.java # 退款申请服务实现
│   │   │   │   ├── RefundInfoServiceImpl.java        # 退款服务实现
│   │   │   │   └── RefundStatusSyncMessageServiceImpl.java # 退款同步消息服务
│   │   │   ├── refund/
│   │   │   │   ├── OrderRefundStatusService.java     # 订单退款状态服务
│   │   │   │   └── RefundStatusSyncResult.java       # 退款状态同步结果
│   │   │   ├── wxpay/
│   │   │   │   ├── WxPayBillFacade.java              # 微信对账门面
│   │   │   │   ├── WxPayOrderFacade.java             # 微信订单门面
│   │   │   │   └── WxPayRefundFacade.java            # 微信退款门面
│   │   │   ├── AliPayService.java                    # 支付宝服务接口
│   │   │   ├── OrderCloseMessageService.java         # 订单关闭消息服务接口
│   │   │   ├── OrderInfoService.java                 # 订单服务接口
│   │   │   ├── PaymentAppService.java                # 支付应用服务接口
│   │   │   ├── PaymentChannelService.java            # 支付渠道服务接口
│   │   │   ├── PaymentInfoService.java               # 支付信息服务接口
│   │   │   ├── ProductService.java                   # 商品服务接口
│   │   │   ├── RefundApplicationService.java         # 退款申请服务接口
│   │   │   ├── RefundInfoService.java                # 退款服务接口
│   │   │   └── RefundStatusSyncMessageService.java   # 退款同步消息服务接口
│   │   ├── util/                          # 工具类
│   │   │   ├── HttpClientUtils.java                  # HTTP客户端工具
│   │   │   ├── HttpUtils.java                        # HTTP工具
│   │   │   ├── JsonUtils.java                        # JSON工具
│   │   │   ├── MoneyUtils.java                       # 金额计算工具
│   │   │   ├── OrderNoUtils.java                     # 订单号生成工具
│   │   │   └── WechatPay2ValidatorForRequest.java    # 微信支付2验证器
│   │   └── vo/
│   │       └── R.java                                # 统一响应对象
│   ├── src/main/resources/
│   │   ├── mapper/                        # MyBatis XML映射文件
│   │   │   ├── OrderInfoMapper.xml
│   │   │   ├── PaymentAppMapper.xml
│   │   │   ├── PaymentChannelMapper.xml
│   │   │   ├── PaymentInfoMapper.xml
│   │   │   └── ProductMapper.xml
│   │   ├── alipay-sandbox.properties      # 支付宝沙箱配置
│   │   ├── apiclient_key.pem              # 微信支付API证书密钥
│   │   └── application.yml                # 应用主配置
│   ├── sql/                               # 数据库脚本
│   │   ├── payment_config_upgrade.sql     # 支付配置升级脚本
│   │   └── payment_demo_v1.sql            # 数据库初始化脚本
│   ├── docs/
│   │   └── RABBITMQ_OPERATIONS.md         # RabbitMQ运维文档
│   └── pom.xml                            # Maven依赖配置
├── payment-demo-react/                    # React前端
│   ├── src/
│   │   ├── api/                           # API接口封装
│   │   │   ├── aliPay.js                  # 支付宝API
│   │   │   ├── bill.js                    # 对账单API
│   │   │   ├── orderInfo.js               # 订单API
│   │   │   ├── paymentConfig.js           # 支付配置API
│   │   │   ├── product.js                 # 商品API
│   │   │   ├── refundInfo.js              # 退款API
│   │   │   └── wxPay.js                   # 微信支付API
│   │   ├── assets/                        # 静态资源
│   │   ├── components/                    # 公共组件
│   │   │   ├── AppFooter.jsx              # 页脚组件
│   │   │   └── AppHeader.jsx              # 页头组件
│   │   ├── pages/                         # 页面组件
│   │   │   ├── Download.jsx               # 下载页
│   │   │   ├── Home.jsx                   # 首页
│   │   │   ├── Orders.jsx                 # 订单页
│   │   │   ├── PaymentConfig.jsx          # 支付配置页
│   │   │   └── Success.jsx                # 成功页
│   │   ├── utils/
│   │   │   └── request.js                 # Axios请求封装
│   │   ├── App.jsx                        # 根组件
│   │   └── main.jsx                       # 入口文件
│   ├── package.json
│   └── vite.config.js
├── payment-demo-vue/                      # Vue前端
│   ├── src/
│   │   ├── api/                           # API接口封装
│   │   ├── assets/                        # 静态资源
│   │   ├── components/                    # 公共组件
│   │   ├── router/                        # 路由配置
│   │   ├── utils/                         # 工具类
│   │   └── views/                         # 页面视图
│   ├── package.json
│   └── vue.config.js
├── live_spec_template/                    # 规范模板
├── materials/                             # 行动卡文档
└── CODE_INTRO.md                          # 项目代码介绍（本文件）
```

## 核心业务模块

### 1. 订单模块
- **实体**: `OrderInfo`
- **状态流转**: 未支付 → 支付成功/已取消/超时关闭 → 退款中 → 退款成功/退款异常
- **特性**: 支持乐观锁（version字段），防止并发更新冲突

### 2. 支付模块
- **实体**: `PaymentInfo`
- **支付渠道**: 微信支付V2/V3、支付宝
- **支付方式**: Native扫码支付、JSAPI支付
- **特性**: 支持支付通知回调处理，防重复回调

### 3. 退款模块
- **实体**: `RefundInfo`
- **流程**: 申请退款 → 审核（通过/拒绝） → 执行退款 → 退款结果同步
- **特性**: 支持退款审核流程，退款状态异步同步

### 4. 支付应用与渠道模块
- **实体**: `PaymentApp`, `PaymentChannel`
- **用途**: 多支付应用管理，多支付渠道配置
- **特性**: 支持动态配置支付渠道参数

### 5. 商品模块
- **实体**: `Product`
- **用途**: 定义可支付的商品/服务

## 并发控制架构

### 三层并发控制

```
┌─────────────────────────────────────────────────────────────┐
│  第一层：通知幂等检查                                        │
│  Redis + notifyId，防止重复处理支付/退款通知                 │
└──────────────────────────┬──────────────────────────────────┘
                           ▼
┌─────────────────────────────────────────────────────────────┐
│  第二层：Redisson 分布式锁（看门狗自动续期）                  │
│  防止并发操作同一订单，锁自动续期防止业务未执行完锁过期       │
└──────────────────────────┬──────────────────────────────────┘
                           ▼
┌─────────────────────────────────────────────────────────────┐
│  第三层：数据库行锁 + 状态条件更新（CAS）                     │
│  SELECT ... FOR UPDATE + UPDATE ... WHERE status = ...     │
└─────────────────────────────────────────────────────────────┘
```

### 数据库唯一约束

| 表 | 约束 | 作用 |
|----|------|------|
| `t_order_info` | `uk_order_no` | 防止商户订单号重复 |
| `t_payment_info` | `uk_order_payment_type` | 防止同一订单同一支付方式重复支付 |
| `t_payment_info` | `uk_transaction_id_payment_type` | 防止同一渠道交易号重复 |
| `t_refund_info` | `uk_refund_no` | 防止商户退款单号重复 |
| `t_refund_info` | `uk_refund_id` | 防止渠道退款单号重复 |

## 消息队列应用

### RabbitMQ延迟队列

| 队列 | Exchange | Routing Key | 用途 | 延迟时间 |
|------|----------|-------------|------|----------|
| order.close.queue | order.close.exchange | order.close | 订单超时自动关闭 | 可配置（默认60s） |
| refund.status.sync.queue | refund.status.sync.exchange | refund.status.sync | 退款状态异步同步 | 可配置（默认60s） |

## 配置说明

### 核心配置项

```yaml
server:
  port: 8080                          # 服务端口

spring:
  datasource:
    url: jdbc:mysql://host:3308/payment_demo_v1  # 数据库连接
  redis:
    host: 192.168.220.200             # Redis地址
    port: 6379
  rabbitmq:
    host: 192.168.220.200             # RabbitMQ地址
    port: 5672

payment:
  order:
    close-delay-ms: 60000             # 订单超时关闭时间（毫秒）
  refund:
    status-sync-delay-ms: 60000       # 退款状态同步延迟（毫秒）
```

## API接口概览

### 微信支付V3
| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/wx-pay/native/{productId}` | POST | 扫码支付下单 |
| `/api/wx-pay/jsapi/{productId}/{openid}` | POST | JSAPI支付 |
| `/api/wx-pay/native/notify` | POST | 支付通知回调 |
| `/api/wx-pay/refunds/{orderNo}/{reason}` | POST | 申请退款 |
| `/api/wx-pay/refunds/notify` | POST | 退款通知回调 |
| `/api/wx-pay/cancel/{orderNo}` | POST | 取消订单 |
| `/api/wx-pay/check-order-status/{orderNo}` | GET | 查询订单状态 |

### 微信支付V2
| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/wx-pay-v2/native/{productId}/{remoteAddr}` | POST | V2扫码支付 |
| `/api/wx-pay-v2/native/notify` | POST | V2支付通知 |

### 支付宝
| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/ali-pay/native/{productId}` | POST | 扫码支付 |
| `/api/ali-pay/refund/{orderNo}/{reason}` | POST | 退款 |
| `/api/ali-pay/notify` | POST | 支付通知回调 |
| `/api/ali-pay/refund/notify` | POST | 退款通知回调 |

### 订单管理
| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/order-info` | GET | 查询订单列表 |
| `/api/order-info/{orderNo}` | GET | 查询订单详情 |

### 退款管理
| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/refund-info/apply` | POST | 申请退款 |
| `/api/refund-info/approve/{refundNo}` | POST | 审核通过 |
| `/api/refund-info/reject/{refundNo}` | POST | 审核拒绝 |
| `/api/refund-info/query/{refundNo}` | GET | 查询退款状态 |
| `/api/refund-info/reconcile/{orderNo}` | POST | 订单对账 |

### 支付配置
| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/payment-app` | CRUD | 支付应用管理 |
| `/api/payment-channel` | CRUD | 支付渠道管理 |
| `/api/payment-config` | GET/PUT | 支付配置管理 |

## 启动方式

### 后端
```bash
cd payment-demo
mvn clean install
mvn spring-boot:run
```
访问 http://localhost:8080
Swagger文档: http://localhost:8080/swagger-ui.html

### React前端
```bash
cd payment-demo-react
npm install
npm run dev
```
访问 http://localhost:3000

### Vue前端
```bash
cd payment-demo-vue
npm install
npm run serve
```
访问 http://localhost:8081（默认）

## 环境要求

- JDK 1.8+
- Maven 3.6+
- Node.js 14+
- MySQL 5.7+
- Redis 5.0+
- RabbitMQ 3.7+
