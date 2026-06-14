# Payment Demo 支付系统架构图

## 架构总览

```mermaid
graph TB
    subgraph Frontend["前端应用层"]
        ReactApp["React App<br/>(React 18 + Ant Design)"]
        VueApp["Vue App<br/>(Vue 2 + Element UI)"]
    end

    subgraph Controller["Controller层 (API入口)"]
        WxPayCtrl["WxPayController<br/>微信支付V3"]
        WxPayV2Ctrl["WxPayV2Controller<br/>微信支付V2"]
        AliPayCtrl["AliPayController<br/>支付宝支付"]
        OrderCtrl["OrderInfoController<br/>订单管理"]
        RefundCtrl["RefundInfoController<br/>退款管理"]
        ConfigCtrl["PaymentConfigController<br/>配置管理"]
        ProductCtrl["ProductController<br/>商品管理"]
    end

    subgraph Facade["Facade层"]
        WxOrderFacade["WxPayOrderFacade<br/>微信订单门面"]
        WxRefundFacade["WxPayRefundFacade<br/>微信退款门面"]
        WxBillFacade["WxPayBillFacade<br/>微信对账门面"]
        WxNotifyHandler["WxPayNotifyHandler<br/>统一通知处理器"]
    end

    subgraph Service["Service层 (业务逻辑)"]
        OrderService["OrderInfoService<br/>订单服务"]
        PaymentService["PaymentInfoService<br/>支付服务"]
        RefundService["RefundInfoService<br/>退款服务"]
        ProductService["ProductService<br/>商品服务"]
        AliPayService["AliPayService<br/>支付宝服务"]
        WxPayHTTPClient["WxPayHttpClient<br/>微信HTTP客户端"]
        OrderRefundStatus["OrderRefundStatusService<br/>订单退款状态"]
    end

    subgraph Lock["并发控制层"]
        RedissonLock["Redisson分布式锁<br/>(看门狗自动续期)"]
        NotifyIdempotent["通知幂等控制<br/>(Redis SET NX 24h)"]
        DBRowLock["数据库行锁<br/>(SELECT FOR UPDATE)"]
        OptimisticLock["乐观锁<br/>(@Version)"]
    end

    subgraph MQ["消息队列层 (RabbitMQ)"]
        OrderCloseMsg["OrderCloseMessage<br/>订单关闭消息"]
        RefundSyncMsg["RefundStatusSyncMessage<br/>退款同步消息"]
        OrderCloseConsumer["OrderCloseConsumer<br/>订单关闭消费者"]
        RefundSyncConsumer["RefundStatusSyncConsumer<br/>退款同步消费者"]
    end

    subgraph Mapper["Mapper层 (数据访问)"]
        OrderMapper["OrderInfoMapper"]
        PaymentMapper["PaymentInfoMapper"]
        RefundMapper["RefundInfoMapper"]
        ProductMapper["ProductMapper"]
        AppMapper["PaymentAppMapper"]
        ChannelMapper["PaymentChannelMapper"]
    end

    subgraph Database["数据存储层"]
        MySQL[("MySQL数据库<br/>payment_demo_v1")]
        Redis[("Redis缓存<br/>分布式锁/幂等")]
        RabbitMQ[("RabbitMQ<br/>延迟消息队列")]
    end

    subgraph External["外部支付平台"]
        WxPayAPI[("微信支付API<br/>api.mch.weixin.qq.com")]
        AliPayAPI[("支付宝API<br/>openapi.alipay.com")]
    end

    ReactApp -->|HTTP/REST| WxPayCtrl
    ReactApp -->|HTTP/REST| WxPayV2Ctrl
    ReactApp -->|HTTP/REST| AliPayCtrl
    ReactApp -->|HTTP/REST| OrderCtrl
    ReactApp -->|HTTP/REST| RefundCtrl
    VueApp -->|HTTP/REST| WxPayCtrl
    VueApp -->|HTTP/REST| WxPayV2Ctrl
    VueApp -->|HTTP/REST| AliPayCtrl

    WxPayCtrl -->|委托| WxOrderFacade
    WxPayCtrl -->|委托| WxRefundFacade
    WxPayCtrl -->|委托| WxBillFacade
    WxPayCtrl -->|支付回调| WxNotifyHandler
    WxPayV2Ctrl -->|委托| WxOrderFacade
    AliPayCtrl -->|调用| AliPayService
    OrderCtrl -->|调用| OrderService
    RefundCtrl -->|调用| RefundService
    ProductCtrl -->|调用| ProductService

    WxOrderFacade -->|实现| WxPayOrderService
    WxRefundFacade -->|实现| WxPayRefundService
    WxNotifyHandler -->|处理通知| WxPayOrderService

    WxPayOrderService -->|依赖| OrderService
    WxPayOrderService -->|依赖| PaymentService
    WxPayOrderService -->|HTTP调用| WxPayHTTPClient
    WxPayRefundService -->|依赖| RefundService
    WxPayRefundService -->|HTTP调用| WxPayHTTPClient
    AliPayService -->|HTTP调用| AliPayHTTPClient

    OrderService -->|并发控制| RedissonLock
    OrderService -->|发送消息| OrderCloseMsg
    OrderService -->|行锁| DBRowLock
    RefundService -->|状态计算| OrderRefundStatus
    WxPayOrderService -->|并发控制| RedissonLock
    WxNotifyHandler -->|幂等检查| NotifyIdempotent

    OrderCloseMsg -->|延迟消息| RabbitMQ
    RefundSyncMsg -->|延迟消息| RabbitMQ
    RabbitMQ -->|消费| OrderCloseConsumer
    RabbitMQ -->|消费| RefundSyncConsumer
    OrderCloseConsumer -->|更新状态| OrderService
    RefundSyncConsumer -->|同步状态| RefundService

    OrderService -->|MyBatis-Plus| OrderMapper
    PaymentService -->|MyBatis-Plus| PaymentMapper
    RefundService -->|MyBatis-Plus| RefundMapper
    ProductService -->|MyBatis-Plus| ProductMapper
    AppMapper -->|配置查询| PaymentConfigLoader
    ChannelMapper -->|渠道查询| PaymentConfigLoader

    OrderMapper -->|持久化| MySQL
    PaymentMapper -->|持久化| MySQL
    RefundMapper -->|持久化| MySQL
    ProductMapper -->|持久化| MySQL
    AppMapper -->|持久化| MySQL
    ChannelMapper -->|持久化| MySQL
    RedissonLock -->|Redis协议| Redis
    NotifyIdempotent -->|Redis协议| Redis

    WxPayHTTPClient -.->|HTTPS| WxPayAPI
    AliPayHTTPClient -.->|HTTPS| AliPayAPI
    WxPayAPI -.->|支付回调| WxPayCtrl
    WxPayAPI -.->|退款回调| WxPayCtrl
    AliPayAPI -.->|支付回调| AliPayCtrl

    classDef frontend fill:#e3f2fd,stroke:#1976d2,stroke-width:2px
    classDef controller fill:#fff3e0,stroke:#f57c00,stroke-width:2px
    classDef facade fill:#f3e5f5,stroke:#7b1fa2,stroke-width:2px
    classDef service fill:#e8f5e9,stroke:#388e3c,stroke-width:2px
    classDef lock fill:#ffebee,stroke:#c62828,stroke-width:2px
    classDef mq fill:#fff9c4,stroke:#f9a825,stroke-width:2px
    classDef mapper fill:#e0f2f1,stroke:#00695c,stroke-width:2px
    classDef database fill:#f5f5f5,stroke:#616161,stroke-width:2px
    classDef external fill:#eceff1,stroke:#455a64,stroke-width:2px,stroke-dasharray: 5 5

    class ReactApp,VueApp frontend
    class WxPayCtrl,WxPayV2Ctrl,AliPayCtrl,OrderCtrl,RefundCtrl,ConfigCtrl,ProductCtrl controller
    class WxOrderFacade,WxRefundFacade,WxBillFacade,WxNotifyHandler facade
    class OrderService,PaymentService,RefundService,ProductService,AliPayService,WxPayHTTPClient,OrderRefundStatus service
    class RedissonLock,NotifyIdempotent,DBRowLock,OptimisticLock lock
    class OrderCloseMsg,RefundSyncMsg,OrderCloseConsumer,RefundSyncConsumer mq
    class OrderMapper,PaymentMapper,RefundMapper,ProductMapper,AppMapper,ChannelMapper mapper
    class MySQL,Redis,RabbitMQ database
    class WxPayAPI,AliPayAPI external
```

## 核心业务流程

### 1. 微信支付V3扫码支付流程

```mermaid
sequenceDiagram
    participant User as 用户
    participant Frontend as 前端应用
    participant WxPayCtrl as WxPayController
    participant Facade as WxPayOrderFacade
    participant Lock as Redisson分布式锁
    participant OrderSvc as OrderInfoService
    participant WxHTTP as WxPayHttpClient
    participant WxAPI as 微信支付API
    participant MySQL as MySQL数据库

    User->>Frontend: 选择商品，发起支付
    Frontend->>WxPayCtrl: POST /api/wx-pay/native/{productId}
    WxPayCtrl->>Facade: nativePay(productId, paymentAppId)
    Facade->>Lock: 获取锁 payment:wx:native:v3:{productId}
    Lock-->>Facade: 锁获取成功
    Facade->>OrderSvc: createOrReuseOrder()
    OrderSvc->>Lock: 获取订单创建锁
    OrderSvc->>OrderSvc: SELECT ... FOR UPDATE (检查未支付订单)
    alt 存在未支付订单
        OrderSvc-->>Facade: 返回已有订单
    else 不存在
        OrderSvc->>MySQL: INSERT INTO t_order_info
        OrderSvc->>OrderSvc: 发送延迟关单消息 (afterCommit)
        OrderSvc-->>Facade: 返回新订单
    end
    Facade->>WxHTTP: POST /v3/pay/transactions/native
    WxHTTP->>WxAPI: 微信Native下单
    WxAPI-->>WxHTTP: 返回 code_url
    WxHTTP-->>Facade: 返回二维码URL
    Facade->>OrderSvc: saveCodeUrl(orderNo, codeUrl)
    OrderSvc->>MySQL: UPDATE t_order_info SET code_url
    Facade-->>WxPayCtrl: 返回 {orderNo, codeUrl}
    WxPayCtrl-->>Frontend: R.ok().setData(map)
    Frontend->>User: 显示支付二维码
    Facade->>Lock: 释放锁
```

### 2. 微信支付回调处理流程

```mermaid
sequenceDiagram
    participant WxAPI as 微信支付平台
    participant WxPayCtrl as WxPayController
    participant NotifyHandler as WxPayNotifyHandler
    participant Redis as Redis
    participant Facade as WxPayOrderFacade
    participant OrderSvc as OrderInfoService
    participant PaySvc as PaymentInfoService
    participant MySQL as MySQL

    WxAPI->>WxPayCtrl: POST /api/wx-pay/native/notify
    WxPayCtrl->>NotifyHandler: handle(request, response, processOrder)
    NotifyHandler->>NotifyHandler: 解析通知报文
    NotifyHandler->>Redis: SET NX payment:wx:notify:processed:{requestId}
    alt 已处理或正在处理
        Redis-->>NotifyHandler: 获取锁失败
        NotifyHandler-->>WxPayCtrl: 返回成功 (幂等)
        WxPayCtrl-->>WxAPI: success
    else 首次处理
        Redis-->>NotifyHandler: 获取锁成功
        NotifyHandler->>NotifyHandler: 验签 (支持多商户)
        alt 验签失败
            NotifyHandler-->>WxPayCtrl: 返回 failure
            WxPayCtrl-->>WxAPI: failure
        else 验签成功
            NotifyHandler->>Facade: processOrder(bodyMap)
            Facade->>Facade: 解密通知资源
            Facade->>Lock: 获取通知处理锁 payment:wx:notify:pay:{orderNo}
            Facade->>OrderSvc: 查询订单 FOR UPDATE
            OrderSvc->>MySQL: SELECT ... FOR UPDATE
            MySQL-->>OrderSvc: 返回订单
            alt 订单已处理
                Facade-->>NotifyHandler: 直接返回
            else 订单未处理
                Facade->>PaySvc: createPaymentInfo()
                PaySvc->>MySQL: INSERT t_payment_info
                Facade->>OrderSvc: updateStatusByOrderNo(SUCCESS)
                OrderSvc->>MySQL: UPDATE t_order_info (乐观锁)
                Facade-->>NotifyHandler: 处理完成
            end
            NotifyHandler->>Redis: SET payment:wx:notify:processed:{requestId} (24h)
            NotifyHandler->>Lock: 释放锁
            NotifyHandler-->>WxPayCtrl: 返回 success
            WxPayCtrl-->>WxAPI: success
        end
    end
```

### 3. 退款申请与审核流程

```mermaid
sequenceDiagram
    participant User as 用户
    participant Frontend as 前端应用
    participant RefundCtrl as RefundInfoController
    participant RefundSvc as RefundInfoService
    participant OrderSvc as OrderInfoService
    participant OrderRefund as OrderRefundStatusService
    participant MySQL as MySQL

    User->>Frontend: 申请退款
    Frontend->>RefundCtrl: POST /api/refund-info/apply
    RefundCtrl->>RefundSvc: createRefundApplication(orderNo, amount, reason)
    RefundSvc->>OrderSvc: getOrderByOrderNoForUpdate(orderNo)
    OrderSvc->>MySQL: SELECT ... FOR UPDATE (行锁)
    MySQL-->>OrderSvc: 返回订单
    OrderSvc-->>RefundSvc: 返回订单信息
    RefundSvc->>RefundSvc: 验证订单状态 (SUCCESS/PARTIAL_REFUND/REFUND_PROCESSING)
    RefundSvc->>RefundSvc: 计算可退金额 (订单总额 - 已审核退款)
    alt 不可退款或超额
        RefundSvc-->>RefundCtrl: 抛出 BizException
        RefundCtrl-->>Frontend: 返回错误信息
        Frontend-->>User: 显示错误
    else 可以退款
        RefundSvc->>RefundSvc: 创建 RefundInfo (approvalStatus=PENDING)
        RefundSvc->>MySQL: INSERT t_refund_info
        RefundSvc-->>RefundCtrl: 返回退款申请信息
        RefundCtrl-->>Frontend: R.ok()
        Frontend-->>User: 显示申请成功
    end

    User->>Frontend: 审核退款
    Frontend->>RefundCtrl: POST /api/refund-info/approve/{refundNo}
    RefundCtrl->>RefundSvc: approveRefund(refundNo, remark)
    RefundSvc->>MySQL: UPDATE t_refund_info SET approvalStatus=APPROVED
    RefundSvc->>OrderRefund: syncOrderRefundStatus(orderNo)
    OrderRefund->>MySQL: 查询订单所有退款记录
    OrderRefund->>OrderRefund: 计算订单退款状态
    alt 部分退款
        OrderRefund->>OrderSvc: updateOrderStatus(PARTIAL_REFUND)
    else 全额退款
        OrderRefund->>OrderSvc: updateOrderStatus(REFUND_SUCCESS)
    end
    OrderSvc->>MySQL: UPDATE t_order_info (乐观锁)
    RefundSvc-->>RefundCtrl: 审核完成
    RefundCtrl-->>Frontend: R.ok()
    Frontend-->>User: 显示审核成功
```

## 数据库实体关系

```mermaid
erDiagram
    t_product {
        bigint id PK
        string title
        int price
        datetime create_time
        datetime update_time
    }

    t_order_info {
        bigint id PK
        string order_no UK
        string title
        bigint user_id
        bigint product_id FK
        int total_fee
        string code_url
        string order_status
        string payment_type
        bigint payment_app_id FK
        string payment_channel_code
        int version
        datetime create_time
        datetime update_time
    }

    t_payment_info {
        bigint id PK
        string order_no FK
        string transaction_id UK
        string payment_type
        string trade_type
        string trade_state
        int payer_total
        text content
        datetime create_time
        datetime update_time
    }

    t_refund_info {
        bigint id PK
        string order_no FK
        string refund_no UK
        string refund_id UK
        int total_fee
        int refund
        string reason
        string approval_status
        string approve_remark
        datetime approved_time
        string refund_status
        text content_return
        text content_notify
        datetime create_time
        datetime update_time
    }

    t_payment_channel {
        bigint id PK
        string channel_code UK
        string channel_name
        string channel_status
        json config_params
        int sort_order
        datetime create_time
        datetime update_time
    }

    t_payment_app {
        bigint id PK
        string app_code UK
        string app_name
        string app_status
        bigint channel_id FK
        json app_config
        int sort_order
        datetime create_time
        datetime update_time
    }

    t_product ||--o{ t_order_info : "1对多"
    t_order_info ||--o{ t_payment_info : "1对多"
    t_order_info ||--o{ t_refund_info : "1对多"
    t_payment_channel ||--o{ t_payment_app : "1对多"
    t_payment_app ||--o{ t_order_info : "1对多"
```

## 并发控制架构

```mermaid
graph LR
    subgraph Layer1["第一层：通知幂等检查"]
        A[Redis SET NX<br/>notifyId + 24h过期]
    end

    subgraph Layer2["第二层：分布式锁"]
        B[Redisson分布式锁<br/>看门狗自动续期]
    end

    subgraph Layer3["第三层：数据库锁"]
        C[SELECT FOR UPDATE<br/>数据库行锁]
        D[UPDATE ... WHERE status = ...<br/>状态条件更新]
        E[@Version<br/>乐观锁]
    end

    A -->|防止重复通知| B
    B -->|防止并发操作| C
    C -->|防止脏写| D
    D -->|防止冲突更新| E

    classDef layer1 fill:#ffebee,stroke:#c62828,stroke-width:2px
    classDef layer2 fill:#fff3e0,stroke:#f57c00,stroke-width:2px
    classDef layer3 fill:#e8f5e9,stroke:#388e3c,stroke-width:2px

    class A layer1
    class B layer2
    class C,D,E layer3
```

## 消息队列流转

```mermaid
graph LR
    subgraph Produce["消息生产"]
        OrderSvc[OrderInfoService<br/>创建订单后] -->|afterCommit| OrderMsg[OrderCloseMessage]
        RefundSvc[RefundInfoService<br/>退款状态更新后] -->|afterCommit| RefundMsg[RefundStatusSyncMessage]
    end

    subgraph MQ["RabbitMQ"]
        OrderMsg -->|x-delay: 60000| OrderQueue[(order.close.queue)]
        RefundMsg -->|x-delay: 60000| RefundQueue[(refund.status.sync.queue)]
    end

    subgraph Consume["消息消费"]
        OrderQueue -->|消费| OrderConsumer[OrderCloseConsumer]
        RefundQueue -->|消费| RefundConsumer[RefundStatusSyncConsumer]
    end

    subgraph Action["业务处理"]
        OrderConsumer -->|超时未支付| CloseOrder[更新订单状态为CLOSED]
        RefundConsumer -->|主动查询| SyncRefund[同步退款状态到本地]
    end

    classDef produce fill:#e8f5e9,stroke:#388e3c,stroke-width:2px
    classDef mq fill:#fff9c4,stroke:#f9a825,stroke-width:2px
    classDef consume fill:#fff3e0,stroke:#f57c00,stroke-width:2px
    classDef action fill:#ffebee,stroke:#c62828,stroke-width:2px

    class OrderSvc,RefundSvc,OrderMsg,RefundMsg produce
    class OrderQueue,RefundQueue mq
    class OrderConsumer,RefundConsumer consume
    class CloseOrder,SyncRefund action
```

## 配置管理架构

```mermaid
graph TB
    subgraph DB["数据库配置"]
        PaymentChannel[(t_payment_channel<br/>渠道公共配置)]
        PaymentApp[(t_payment_app<br/>商户应用配置)]
    end

    subgraph Loader["配置加载器"]
        ConfigLoader[PaymentConfigLoader<br/>启动时加载到内存]
    end

    subgraph Cache["配置缓存"]
        AppConfigs[Map<channelCode, List<AppConfig>>]
        VerifierCache[ConcurrentHashMap<br/>微信验签器缓存]
    end

    subgraph Usage["配置使用"]
        WxPayConfig[WxPayConfig<br/>默认配置]
        AliPayConfig[AlipayProperties<br/>静态配置]
        DynamicConfig[动态配置解析<br/>resolveWxPayConfig]
    end

    PaymentChannel -->|渠道配置| ConfigLoader
    PaymentApp -->|应用配置| ConfigLoader
    ConfigLoader -->|listAppConfigsByChannelCode| AppConfigs
    AppConfigs -->|默认配置| WxPayConfig
    AppConfigs -->|动态配置| DynamicConfig
    AppConfigs -->|构建验签器| VerifierCache

    classDef db fill:#f5f5f5,stroke:#616161,stroke-width:2px
    classDef loader fill:#e8f5e9,stroke:#388e3c,stroke-width:2px
    classDef cache fill:#fff3e0,stroke:#f57c00,stroke-width:2px
    classDef usage fill:#fff9c4,stroke:#f9a825,stroke-width:2px

    class PaymentChannel,PaymentApp db
    class ConfigLoader loader
    class AppConfigs,VerifierCache cache
    class WxPayConfig,AliPayConfig,DynamicConfig usage
```
