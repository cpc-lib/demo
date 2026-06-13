# Payment Demo 代码导览

本文是当前仓库的快速地图，面向后续维护、测试和扩展。仓库包含 Go 后端、React 前端、Vue 前端、数据库初始化脚本、配置样例、spec 账本和过程文档。

## 1. 项目定位

Payment Demo 是一个支付演示系统，围绕商品下单、微信/支付宝支付、支付回调、订单关单、退款、账单下载和支付配置管理展开。

当前主实现是 `payment-demo-go/`：

- 后端使用 Gin + GORM + MySQL + Redis + RabbitMQ。
- 支付渠道包含微信支付 V3、微信支付 V2 和支付宝沙箱。
- 支付参数支持数据库配置，前端下单时必须传入支付应用 ID：`paymentAppId`。
- React 与 Vue 两套前端都调用同一组后端接口。
- `spec/` 是项目状态账本，行为变更需要先进入 planned，完成后再进入 implemented。

## 2. 顶层目录

| 路径 | 作用 |
|---|---|
| `payment-demo-go/` | Go 后端主项目，包含接口、服务、模型、配置、MQ、支付客户端和测试 |
| `payment-demo-react/` | React + Vite 前端 |
| `payment-demo-vue/` | Vue CLI 前端 |
| `spec/` | 设计、契约和实现状态账本 |
| `materials/` | 需求、任务拆解、说明材料 |
| `live_spec_template/` | live spec 模板示例，不参与运行时 |
| `AGENTS.md` | 仓库级协作、spec、测试和完成定义规则 |
| `CODE_INTRO.md` | 本文件，项目导览 |

## 3. 后端启动链路

入口文件：`payment-demo-go/cmd/server/main.go`

启动顺序：

```text
加载 application.yml / wxpay.properties / alipay-sandbox.properties
-> 初始化 MySQL / GORM
-> 初始化 Redis 并 Ping
-> 初始化 RabbitMQ
-> 初始化微信 V3 / 微信 V2 / 支付宝客户端配置
-> 创建 service
-> 启动订单延迟关单消费者
-> 注册 Gin 路由
-> 监听 server.port
```

配置文件默认位于 `payment-demo-go/config/`：

| 配置 | 默认文件 | 环境变量覆盖 |
|---|---|---|
| 应用配置 | `config/application.yml` | `APP_CONFIG` |
| 微信配置 | `config/wxpay.properties` | `WXPAY_CONFIG` |
| 支付宝配置 | `config/alipay-sandbox.properties` | `ALIPAY_CONFIG` |

`application.yml` 使用接近 Spring 的结构，包含服务端口、数据库、Redis、RabbitMQ、GORM 日志等。微信私钥路径会按配置文件位置解析相对路径。

## 4. 后端模块

| 路径 | 主要职责 |
|---|---|
| `internal/config/` | 读取应用配置、微信配置、支付宝配置 |
| `internal/db/` | MySQL/GORM 初始化 |
| `internal/cache/` | Redis 客户端与支付业务锁 |
| `internal/mq/` | RabbitMQ 连接与订单延迟关单消息 |
| `internal/model/` | GORM 模型与表名映射 |
| `internal/constant/` | 订单、支付、退款、渠道枚举 |
| `internal/pay/` | 微信 V3、微信 V2、支付宝客户端封装 |
| `internal/service/` | 业务编排：下单、支付查询、关单、退款、配置管理、回调处理 |
| `internal/handler/` | Gin 路由与 HTTP 参数/响应适配 |
| `internal/response/` | 统一 JSON 响应结构 |
| `sql/` | 初始化 SQL 与结构校验测试 |

## 5. 数据模型

初始化脚本：`payment-demo-go/sql/payment_demo.sql`

核心表：

| 表 | 作用 |
|---|---|
| `t_product` | 商品信息 |
| `t_order_info` | 订单主表，包含 `payment_app_id` 订单绑定字段 |
| `t_payment_info` | 支付记录 |
| `t_refund_info` | 退款记录 |
| `t_payment_channel` | 支付渠道配置，按 `channel_code` 区分 `wxpay` / `alipay` |
| `t_payment_app` | 支付应用配置，按应用维度保存渠道参数 |

当前数据库字段 `t_order_info.payment_app_id` 仍保持 nullable，用于兼容历史订单。新下单必须传入有效的正整数 `paymentAppId`，并在订单创建后写入该字段。

种子支付应用包括：

| ID | app_code | channel_code | 用途 |
|---|---|---|---|
| 1 | `wxpay-default` | `wxpay` | 微信支付默认应用 |
| 2 | `alipay-sandbox` | `alipay` | 支付宝沙箱应用 |

## 6. 支付配置规则

支付配置由 properties 默认值、渠道配置和应用配置合并得到。

新下单按 `paymentAppId + paymentType` 查找配置：

```text
properties 默认值
< t_payment_channel.config_params
< t_payment_app.app_config
```

关键规则：

- `paymentAppId` 必须是正整数。
- 支付应用必须存在、启用，并且应用的 `channel_code` 必须匹配当前支付方式。
- 支付渠道也必须启用，否则不能创建新订单。
- 新订单会绑定 `t_payment_app.id` 到 `t_order_info.payment_app_id`。
- 未支付订单复用维度包含 `product_id + payment_type + payment_app_id`，不同支付应用不会互相复用订单。
- 历史订单如果 `payment_app_id` 为空，只在非新建操作中保留 properties fallback。
- 订单取消、查单、延迟关单、退款提交/查询/对账、支付回调和退款回调都优先使用订单绑定的支付应用配置。

回调应用识别规则：

- 微信 V2 根据通知中的 `appid` / `mch_id` 选择支付应用并验签。
- 支付宝根据通知中的 `app_id` / `seller_id` 选择支付应用并验签。
- 微信 V3 会遍历已配置微信应用尝试验签/解密，再校验通知订单绑定应用是否匹配。

## 7. HTTP 契约

统一 JSON 响应大致为：

```json
{
  "code": 0,
  "message": "成功",
  "data": {}
}
```

业务拒绝通常仍返回 HTTP 200，并使用 `code=-1`。订单状态轮询未成功时返回 `code=101`。

支付回调响应按渠道要求返回：

| 渠道 | 成功响应 | 失败响应 |
|---|---|---|
| 微信 V3 | JSON：`{"code":"SUCCESS","message":"成功"}` | HTTP 500 + JSON：`{"code":"ERROR","message":"..."}` |
| 微信 V2 | XML：`return_code=SUCCESS` | XML：`return_code=FAIL` |
| 支付宝 | 字符串：`success` | 字符串：`failure` |

## 8. 主要 API

商品与订单：

| 方法 | 路径 | 说明 |
|---|---|---|
| `GET` | `/api/product/test` | 测试接口 |
| `GET` | `/api/product/list` | 商品列表 |
| `GET` | `/api/order-info/list` | 订单列表 |
| `GET` | `/api/order-info/query-order-status/:orderNo` | 查询订单状态 |

支付配置：

| 方法 | 路径 | 说明 |
|---|---|---|
| `GET` | `/api/payment-channel/list` | 支付渠道列表 |
| `POST` | `/api/payment-channel/save` | 新增支付渠道 |
| `PUT` | `/api/payment-channel/update/:channelCode` | 更新支付渠道 |
| `DELETE` | `/api/payment-channel/delete/:channelCode` | 删除支付渠道 |
| `GET` | `/api/payment-app/list` | 支付应用列表 |
| `POST` | `/api/payment-app/save` | 新增支付应用 |
| `PUT` | `/api/payment-app/update/:appCode` | 更新支付应用 |
| `DELETE` | `/api/payment-app/delete/:appCode` | 删除支付应用 |
| `GET` | `/api/payment-config/apps` | 前端下单可选支付应用列表 |
| `POST` | `/api/payment-config/reload` | 重新加载支付配置 |

微信 V3：

| 方法 | 路径 | 说明 |
|---|---|---|
| `POST` | `/api/wx-pay/native/:productId?paymentAppId=...` | Native 下单，`paymentAppId` 必填 |
| `POST` | `/api/wx-pay/native/notify` | 支付回调 |
| `POST` | `/api/wx-pay/refunds/notify` | 退款回调 |
| `POST` | `/api/wx-pay/cancel/:orderNo` | 取消订单 |
| `GET` | `/api/wx-pay/query/:orderNo` | 查单 |
| `GET` | `/api/wx-pay/check-order-status/:orderNo` | 前端轮询订单状态 |
| `GET` | `/api/wx-pay/query-refund/:refundNo` | 查询退款 |
| `POST` | `/api/wx-pay/refunds/:orderNo/:reason` | 发起退款 |
| `GET` | `/api/wx-pay/querybill/:billDate/:billType` | 申请账单 |
| `GET` | `/api/wx-pay/downloadbill/:billDate/:billType` | 下载账单 |
| `GET` | `/api/wx-pay/jsapi/:productId` | JSAPI 下单 |
| `POST` | `/api/wx-pay/jsapi/notify/v1` | JSAPI 支付回调 |

微信 V2：

| 方法 | 路径 | 说明 |
|---|---|---|
| `POST` | `/api/wx-pay-v2/native/:productId?paymentAppId=...` | Native 下单，`paymentAppId` 必填 |
| `POST` | `/api/wx-pay-v2/native/notify` | 支付回调 |

支付宝：

| 方法 | 路径 | 说明 |
|---|---|---|
| `POST` | `/api/ali-pay/trade/page/pay/:productId?paymentAppId=...` | 电脑网站支付，`paymentAppId` 必填 |
| `POST` | `/api/ali-pay/trade/notify` | 支付回调 |
| `POST` | `/api/ali-pay/trade/close/:orderNo` | 关闭交易 |
| `GET` | `/api/ali-pay/trade/query/:orderNo` | 查询交易 |
| `POST` | `/api/ali-pay/trade/fastpay/refund/:refundNo` | 支付宝退款 |
| `GET` | `/api/ali-pay/bill/downloadurl/query/:billDate/:type` | 账单下载地址 |

退款聚合接口：

| 方法 | 路径 | 说明 |
|---|---|---|
| `POST` | `/api/refund-info/apply` | 申请退款 |
| `GET` | `/api/refund-info/list` | 退款列表 |
| `GET` | `/api/refund-info/list/:orderNo` | 指定订单退款列表 |
| `POST` | `/api/refund-info/approve/:refundNo` | 审核通过 |
| `POST` | `/api/refund-info/reject/:refundNo` | 审核拒绝 |
| `GET` | `/api/refund-info/query/:refundNo` | 查询退款 |
| `POST` | `/api/refund-info/reconcile/:orderNo` | 对账 |

## 9. 核心业务流程

下单与支付：

```text
前端加载商品和支付应用
-> 用户选择支付方式与支付应用
-> 调用微信 V3 / 微信 V2 / 支付宝下单接口并传 paymentAppId
-> 后端校验 paymentAppId、支付应用、支付渠道
-> 创建或复用同支付应用下的未支付订单
-> 写入 order.payment_app_id
-> 调用渠道预下单
-> 投递延迟关单消息
-> 前端展示二维码或跳转支付页
```

支付状态同步：

```text
支付渠道异步回调
-> 根据回调内容选择或尝试支付应用配置
-> 验签 / 解密
-> 校验订单绑定支付应用
-> 更新订单与支付记录
```

延迟关单：

```text
创建订单后投递延迟消息
-> 延迟队列到期进入释放队列
-> 消费者查询订单
-> 未支付则调用订单绑定配置关闭渠道订单
-> 更新本地订单状态
```

RabbitMQ 交换机与队列名：

| 名称 | 用途 |
|---|---|
| `payment.order.close.event.exchange` | 订单关单事件交换机 |
| `payment.order.close.delay.queue` | 延迟队列 |
| `payment.order.close.dead-letter.exchange` | 死信交换机 |
| `payment.order.close.release.queue` | 释放队列 |

退款：

```text
用户申请退款
-> 生成 t_refund_info，状态为待审核
-> 审核通过
-> 使用订单绑定支付应用配置提交渠道退款
-> 查询 / 回调 / 对账同步退款状态
-> 聚合更新订单退款状态
```

Redis 主要用于支付锁和回调幂等键，常见 key 前缀包含 `payment:*`、`payment:wx:notify:processed:*`、`payment:wx:v2:notify:processed:*`。

## 10. React 前端

目录：`payment-demo-react/`

技术栈：

- React 19
- React Router
- Axios
- Vite
- 页面主要用 JSX/CSS 组织界面

脚本：

```bash
npm run dev
npm run build
npm run preview
```

主要文件：

| 路径 | 作用 |
|---|---|
| `src/App.jsx` | 路由入口 |
| `src/pages/Home.jsx` | 商品、支付方式、支付应用选择和下单 |
| `src/pages/Orders.jsx` | 订单列表、取消、退款入口 |
| `src/pages/Download.jsx` | 账单下载 |
| `src/pages/Success.jsx` | 支付成功页 |
| `src/api/product.js` | 商品接口 |
| `src/api/wxpay.js` | 微信 V3 接口 |
| `src/api/wxpay-v2.js` | 微信 V2 接口 |
| `src/api/alipay.js` | 支付宝接口 |
| `src/api/paymentConfig.js` | 支付应用列表 |
| `src/api/refund.js` | 退款接口 |

首页会调用 `/api/payment-config/apps`，按当前支付方式过滤 `channelCode`，默认选择第一个可用应用。没有商品或没有可用支付应用时，下单按钮不可提交。

## 11. Vue 前端

目录：`payment-demo-vue/`

技术栈：

- Vue 3
- Vue Router
- Axios
- Ant Design Vue
- Vue CLI

脚本：

```bash
npm run serve
npm run build
npm run lint
```

主要文件：

| 路径 | 作用 |
|---|---|
| `src/router/index.js` | 路由入口 |
| `src/views/index.vue` | 商品、支付方式、支付应用选择和下单 |
| `src/views/Orders.vue` | 订单列表、取消、退款入口 |
| `src/views/Download.vue` | 账单下载 |
| `src/views/Success.vue` | 支付成功页 |
| `src/api/product.js` | 商品接口 |
| `src/api/wxpay.js` | 微信 V3 接口 |
| `src/api/wxpay-v2.js` | 微信 V2 接口 |
| `src/api/alipay.js` | 支付宝接口 |
| `src/api/paymentConfig.js` | 支付应用列表 |
| `src/api/refund.js` | 退款接口 |

Vue 首页同样显式展示支付应用选择，并在下单 API 中传递 `paymentAppId`。

## 12. Spec 与当前实现账本

先看：`spec/README.md`

当前与支付配置强相关的已实现 spec：

| Spec | 说明 |
|---|---|
| `spec/implemented/payment-config/PAYMENT_CONFIG_MANAGEMENT_SPEC.md` | 支付渠道和支付应用管理 |
| `spec/implemented/payment-config/REQUIRED_PAYMENT_APP_ID_ORDER_BINDING_SPEC.md` | 下单必传支付应用 ID，订单绑定配置，回调校验 |

仓库规则要求：

- 新功能、公共接口变化、状态机变化、兼容影响要先进入 `spec/planned/`。
- 代码、测试、文档、示例和 spec 不一致时，不能宣称完成。
- 已实现且有测试保护的行为才能移入 `spec/implemented/`。

## 13. 测试

后端基础验证命令：

```bash
cd payment-demo-go
go test ./...
```

当前 Go 后端已有特征和回归测试，覆盖配置解析、HTTP handler、锁、模型、MQ、支付参数、响应结构、服务层和 SQL 结构等。

重点测试方向包括：

- 三个下单接口缺失、非法、非正数 `paymentAppId` 返回 `code=-1`。
- 启用/禁用支付应用和渠道对新下单的影响。
- 支付应用配置覆盖渠道配置和 properties 默认值。
- 不同 `paymentAppId` 的未支付订单互不复用。
- 取消、查单、延迟关单、退款和回调使用订单绑定配置。
- 回调中的支付应用与订单绑定应用不匹配时拒绝。

前端当前主要通过构建命令验证：

```bash
cd payment-demo-react
npm run build

cd payment-demo-vue
npm run build
```

## 14. 维护注意点

- 新下单接口不要再把 `paymentAppId` 当作可选参数。
- 新增支付渠道或支付应用字段时，需要同步 SQL、模型、service、handler、前端 API、spec 和测试。
- 订单生命周期的支付参数应来自订单绑定应用；只有历史空绑定订单的后续操作可以 fallback 到 properties。
- 回调处理属于外部契约面，任何响应格式、验签、幂等或拒绝路径变更都需要测试。
- 涉及 Redis、RabbitMQ、数据库和全局配置的测试要做隔离与 reset。
- 配置样例可能包含演示或沙箱凭据，实际部署不要提交真实生产密钥。
