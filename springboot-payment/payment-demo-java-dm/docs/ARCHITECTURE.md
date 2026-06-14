# 支付 Demo V5 — 项目架构图

## 系统总览

```mermaid
graph TB
    subgraph Client["前端客户端"]
        Vue[Vue 前端]
        React[React 前端]
    end

    subgraph API["API 网关层"]
        PC[ProductController]
        OC[OrderInfoController]
        AP[AliPayController]
        WX3[WxPayController V3]
        WX2[WxPayV2Controller]
        RA[RefundApplicationController]
        RI[RefundInfoController]
        PCC[PaymentChannelController]
        PAC[PaymentAppController]
        PGC[PaymentConfigController]
    end

    subgraph Service["业务服务层"]
        subgraph OrderDomain["订单域"]
            OIS[OrderInfoService]
            PIS[PaymentInfoService]
        end
        subgraph PayDomain["支付域"]
            APS[AliPayService]
            WXOF[WxPayOrderFacade]
            WXRF[WxPayRefundFacade]
            WXBF[WxPayBillFacade]
        end
        subgraph RefundDomain["退款域"]
            RAS[RefundApplicationService]
            RIS[RefundInfoService]
            RSSM[RefundStatusSyncMessageService]
        end
        subgraph ConfigDomain["配置域"]
            PCS[PaymentChannelService]
            PAS[PaymentAppService]
            PCL[PaymentConfigLoader]
        end
        subgraph ProductDomain["商品域"]
            PRS[ProductService]
        end
    end

    subgraph Infra["基础设施层"]
        Lock[RedissonLock Template]
        MQC[RabbitMQ Consumers]
        WXHC[WxPayHttpClient]
        WXND[WxPayNotificationDecoder]
        WXNH[WxPayNotifyHandler]
    end

    subgraph Data["数据持久层"]
        OM[OrderInfoMapper]
        PM[PaymentInfoMapper]
        RM[RefundInfoMapper]
        PRM[ProductMapper]
        PCM[PaymentChannelMapper]
        PAM[PaymentAppMapper]
    end

    subgraph Storage["存储 & 中间件"]
        DM[(达梦 DM8)]
        Redis[(Redis)]
        MQ[(RabbitMQ)]
    end

    subgraph External["外部服务"]
        Ali[支付宝开放平台]
        WX[微信支付 API V2/V3]
    end

    Client -->|HTTP| API
    API --> Service
    Service --> Infra
    Service --> Data
    Data --> DM
    Lock --> Redis
    Infra --> Redis
    MQC --> MQ
    WXHC --> WX
    APS --> Ali
    WXOF --> WX

    classDef client fill:#e1f5fe
    classDef api fill:#fff3e0
    classDef service fill:#f3e5f5
    classDef infra fill:#e8f5e9
    classDef data fill:#fce4ec
    classDef storage fill:#fff9c4
    classDef external fill:#e0e0e0
    class Vue,React client
    class PC,OC,AP,WX3,WX2,RA,RI,PCC,PAC,PGC api
    class OIS,PIS,APS,WXOF,WXRF,WXBF,RAS,RIS,RSSM,PCS,PAS,PCL,PRS service
    class Lock,MQC,WXHC,WXND,WXNH infra
    class OM,PM,RM,PRM,PCM,PAM data
    class DM,Redis,MQ storage
    class Ali,WX external
```

## 支付流程

```mermaid
sequenceDiagram
    participant FE as 前端
    participant API as Controller
    participant SVC as Service
    participant LOCK as Redisson锁
    participant DB as 达梦DM8
    participant MQ as RabbitMQ
    participant WX as 微信支付
    participant NOTIFY as 微信通知回调

    FE->>API: POST /api/wx-pay/native/{productId}
    API->>SVC: nativePay(productId, paymentAppId)
    SVC->>LOCK: 获取分布式锁
    LOCK-->>SVC: 锁成功
    SVC->>SVC: createOrReuseOrder()
    SVC->>LOCK: 获取订单创建锁
    SVC->>DB: SELECT ... FOR UPDATE
    alt 存在未支付订单
        DB-->>SVC: 返回未支付订单
        SVC-->>API: 复用订单
    else 不存在未支付订单
        SVC->>DB: INSERT order
        SVC->>MQ: 发送延迟关单消息(60s)
        SVC->>WX: POST /v3/pay/transactions/native
        WX-->>SVC: 返回 codeUrl
        SVC->>DB: 保存 codeUrl
        SVC-->>API: 返回订单+二维码
    end
    SVC->>LOCK: 释放锁
    API-->>FE: 返回 codeUrl
    FE->>FE: 展示二维码
    FE->>WX: 用户扫码支付
    WX->>N NOTIFY: 异步通知
    NOTIFY->>API: POST /api/wx-pay/native/notify
    API->>SVC: handle(notify)
    SVC->>DB: Redis SETNX 幂等检查
    alt 已处理过
        SVC-->>API: 返回成功
    end
    SVC->>SVC: 验签 WechatPay2Validator
    SVC->>LOCK: 获取通知处理锁
    SVC->>DB: SELECT ... FOR UPDATE
    SVC->>DB: 金额校验
    SVC->>DB: CAS UPDATE NOTPAY → SUCCESS
    SVC->>DB: INSERT payment_info
    SVC->>LOCK: 释放锁
    SVC-->>API: 返回成功应答
    API-->>NOTIFY: success
    FE->>API: 轮询订单状态
    API-->>FE: SUCCESS → 跳转成功页
```

## 退款流程

```mermaid
sequenceDiagram
    participant FE as 前端
    participant API as Controller
    participant RAS as RefundApplicationService
    participant RIS as RefundInfoService
    participant LOCK as Redisson锁
    participant DB as 达梦DM8
    participant MQ as RabbitMQ
    participant WX as 微信支付/支付宝

    FE->>API: POST /api/refund-info/apply
    API->>RIS: createRefundApplication(orderNo, amount, reason)
    RIS->>DB: SELECT order FOR UPDATE
    RIS->>RIS: 校验可退余额
    RIS->>DB: INSERT refund_info (CREATED/PENDING)
    RIS-->>FE: 退款申请已创建

    FE->>API: POST /api/refund-info/approve/{refundNo}
    API->>RAS: approve(refundNo)
    RAS->>LOCK: 获取退款审核锁
    RAS->>DB: 查询退款单
    RAS->>RIS: markApprovalPassed()
    RAS->>RIS: updateRefund → PROCESSING
    RAS->>WX: executeRefund()
    WX-->>RAS: 退款受理成功
    RAS->>MQ: 发送延迟同步消息(60s)
    RAS->>LOCK: 释放锁
    API-->>FE: 审核通过，退款已提交

    MQ->>MQ: 60s 延迟到期
    MQ->>API: RefundStatusSyncConsumer
    API->>RAS: queryRefundStatus(refundNo)
    RAS->>WX: 查询退款状态
    WX-->>RAS: 返回退款结果
    RAS->>DB: 更新 refund_status
    RAS-->>API: 同步完成
```

## 延迟关单流程

```mermaid
sequenceDiagram
    participant DB as 达梦DM8
    participant MQ as RabbitMQ
    participant OC as OrderCloseConsumer
    participant APS as AliPayService
    participant WXOF as WxPayOrderFacade

    DB->>MQ: 订单创建时发送延迟消息(60s)
    MQ->>MQ: 60s 延迟到期
    MQ->>OC: 投递关单消息
    OC->>DB: 查询订单状态
    alt 订单已支付/已关闭
        OC-->>MQ: 无需处理
    else 订单仍为 NOTPAY
        OC->>OC: 判断支付类型
        alt 微信支付
            OC->>WXOF: checkOrderStatus(orderNo)
            WXOF->>WX: 主动查询微信支付状态
            WX-->>WXOF: 返回支付结果
            alt 微信侧未支付
                WXOF->>DB: 关单 + 更新本地状态为 CLOSED
            else 微信侧已支付
                WXOF->>DB: 同步本地状态为 SUCCESS
            end
        else 支付宝
            OC->>APS: checkOrderStatus(orderNo)
            APS->>Ali: 主动查询支付宝订单状态
            Ali-->>APS: 返回支付结果
            alt 支付宝侧未支付
                APS->>DB: 关单 + 更新本地状态为 CLOSED
            else 支付宝侧已支付
                APS->>DB: 同步本地状态为 SUCCESS
            end
        end
    end
```

## 三层并发控制

```mermaid
graph TD
    subgraph Layer1["第一层: 通知幂等检查"]
        A1[Redis SETNX] --> A2{key已存在?}
        A2 -->|是| A3[直接返回成功]
        A2 -->|否| A4[继续处理]
    end

    subgraph Layer2["第二层: Redisson 分布式锁"]
        B1[lock.tryLock] --> B2{获取锁成功?}
        B2 -->|否| B3[返回 系统繁忙]
        B2 -->|是| B4[执行业务逻辑]
        B4 --> B5[看门狗自动续期]
    end

    subgraph Layer3["第三层: 数据库行锁 + CAS"]
        C1[SELECT ... FOR UPDATE] --> C2[获取行级排他锁]
        C2 --> C3[检查当前状态]
        C3 --> C4{状态匹配?}
        C4 -->|否| C5[幂等忽略]
        C4 -->|是| C6[UPDATE ... WHERE status = old]
        C6 --> C7{影响行数 > 0?}
        C7 -->|否| C5
        C7 -->|是| C8[状态更新成功]
    end

    A4 --> B1
    B4 --> C1

    classDef l1 fill:#e3f2fd
    classDef l2 fill:#fff3e0
    classDef l3 fill:#e8f5e9
    class A1,A2,A3,A4 l1
    class B1,B2,B3,B4,B5 l2
    class C1,C2,C3,C4,C5,C6,C7,C8 l3
```

## 配置加载机制

```mermaid
graph LR
    subgraph StaticConfig["静态配置"]
        SP[wxpay.properties]
        AP[application.yml alipay节点]
    end

    subgraph DynamicConfig["动态配置 数据库"]
        PC[t_payment_channel]
        PA[t_payment_app]
    end

    subgraph Loader["PaymentConfigLoader"]
        PCL[@PostConstruct init]
        RELOAD[reloadConfigs]
    end

    subgraph Cache["内存缓存"]
        CC[appConfigCache ConcurrentHashMap]
        CHC[channelCache ConcurrentHashMap]
        VC[verifierCache ConcurrentHashMap]
    end

    subgraph Usage["使用场景"]
        WXV3[微信V3: 动态构建HTTP Client]
        WXV2[微信V2: 静态WxPayConfig]
        ALI[支付宝: buildAlipayClient 动态构建]
    end

    SP --> PCL
    AP --> PCL
    PC --> PCL
    PA --> PCL
    PCL --> CC
    PCL --> CHC
    CC --> WXV3
    CC --> ALI
    SP --> WXV2

    classDef static fill:#fff9c4
    classDef dynamic fill:#e1f5fe
    classDef loader fill:#f3e5f5
    classDef cache fill:#e8f5e9
    classDef usage fill:#fce4ec
    class SP,AP static
    class PC,PA dynamic
    class PCL,RELOAD loader
    class CC,CHC,VC cache
    class WXV3,WXV2,ALI usage
```

## 数据库实体关系

```mermaid
erDiagram
    t_product {
        bigint id PK
        string title
        int price
        timestamp create_time
        timestamp update_time
    }

    t_order_info {
        bigint id PK
        string title
        string order_no UK
        bigint user_id
        bigint product_id FK
        int total_fee
        string code_url
        string order_status
        string payment_type
        bigint payment_app_id FK
        string payment_channel_code
        int version
        timestamp create_time
        timestamp update_time
    }

    t_payment_info {
        bigint id PK
        string order_no
        string transaction_id
        string payment_type
        string trade_type
        string trade_state
        int payer_total
        string content
        timestamp create_time
        timestamp update_time
    }

    t_refund_info {
        bigint id PK
        string order_no
        string refund_no UK
        string refund_id
        int total_fee
        int refund
        string reason
        string approval_status
        string approve_remark
        timestamp approved_time
        string refund_status
        string content_return
        string content_notify
        timestamp create_time
        timestamp update_time
    }

    t_payment_channel {
        bigint id PK
        string channel_name
        string channel_code UK
        string channel_status
        string channel_desc
        clob config_params
        int sort_order
        timestamp create_time
        timestamp update_time
    }

    t_payment_app {
        bigint id PK
        string app_name
        string app_code UK
        string app_status
        bigint channel_id FK
        string app_desc
        clob app_config
        int sort_order
        timestamp create_time
        timestamp update_time
    }

    t_product ||--o{ t_order_info : "1:N"
    t_order_info ||--o{ t_payment_info : "1:N"
    t_order_info ||--o{ t_refund_info : "1:N"
    t_payment_channel ||--o{ t_payment_app : "1:N"
    t_payment_app ||--o{ t_order_info : "1:N"
```
