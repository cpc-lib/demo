# SPEC-009：管理员运营边界与主动状态核对

## 元信息

- **状态**：implemented
- **分类**：`type: design-change`
- **计划版本**：v0.4.0
- **实现提交**：`c6344af`, `01a9cc7`, `57d9747`, `3de9775`, `7ed3f7e`, `718a2ee`, `f400ac7`
- **设计日期**：2026-08-31
- **依赖**：SPEC-008
- **影响范围**：认证鉴权、订单支付、退款、React 前端、Vue 前端、测试

## 目标

管理员账号只用于订单和支付运维，不参与商品购买。管理员需要能够审核退款申请，并主动查询支付单与退款单状态。

## 业务规则

### 管理员禁止的能力

ADMIN 访问以下接口统一返回 HTTP 403，错误消息为“管理员账号不参与购物”：

- `/api/cart` 及其所有购物车操作；
- `/api/order-info/checkout`；
- 微信 V3、微信 V2、支付宝商品支付入口；
- 微信 V3、微信 V2、支付宝已有订单支付入口；
- `/api/refund-info/apply` 及兼容退款申请入口。

USER 行为保持不变。课程列表仍然可以公开读取，但管理员前端不展示课程和购物车入口。

### 管理员保留的能力

- 查看全部订单和订单明细；
- 主动查询微信/支付宝支付单状态并同步本地订单；
- 查看全部退款申请；
- 通过或拒绝待审核退款申请；
- 主动查询单笔退款状态；
- 按订单核对全部退款状态；
- 使用既有支付配置、账单和对账管理。

## API 设计

复用现有接口：

- `GET /api/order-info/list`
- `GET /api/order-info/{orderNo}/items`
- `GET /api/wx-pay/check-order-status/{orderNo}`
- `GET /api/refund-info/list`
- `POST /api/refund-info/approve/{refundNo}`
- `POST /api/refund-info/reject/{refundNo}`
- `POST /api/refund-info/query/{refundNo}`
- `POST /api/refund-info/reconcile/{orderNo}`

新增一个薄包装接口：

- `GET /api/ali-pay/check-order-status/{orderNo}`：调用现有支付宝查单同步服务，返回订单号和同步后的本地订单状态。

新增接口纳入 ADMIN 路由保护；微信查单接口也纳入管理员运维保护。

## 前端设计

React/Vue 同步：

- ADMIN 导航显示“全部订单”“退款审核”“账单”“支付配置”“对账”；
- ADMIN 根路径进入全部订单，隐藏课程和购物车；
- `/cart` 和支付成功页限制 USER；
- ADMIN 订单页只显示查看明细、支付状态查询、退款记录和订单退款核对；
- 新增 `/refunds` 退款审核页面，支持审核备注、通过、拒绝和退款状态查询；
- ADMIN 不请求购物车数量。

## 实现锚点

| 模块 | 文件 | 关键实现 |
|------|------|---------|
| 购买边界 | `payment-demo/src/main/java/cc/ivera/security/AuthContext.java` | `requireShoppingUser()` |
| 购物车/结算/退款申请 | `CartController.java`, `OrderInfoController.java`, `RefundApplicationController.java` | ADMIN 统一抛出 403 |
| 三渠道支付入口 | `WxPayController.java`, `WxPayV2Controller.java`, `AliPayController.java` | 商品支付与已有订单支付使用购物用户校验 |
| 管理员支付查单 | `WxPayController.java`, `AliPayController.java`, `WebMvcConfig.java` | 微信既有查单复用，支付宝薄包装查单并返回本地状态 |
| 退款运维 | `RefundInfoController.java`, `RefundApplicationServiceImpl.java` | 审核、单笔查询、按订单核对复用既有服务 |
| React 管理端 | `payment-demo-react/src/pages/Refunds.jsx`, `Orders.jsx` | 退款审批、支付查单、退款核对与角色路由 |
| Vue 管理端 | `payment-demo-vue/src/views/Refunds.vue`, `Orders.vue` | 与 React 保持同一角色和操作边界 |

## 测试覆盖

- 后端全量：135 个测试，31 个测试类，Failures=0，Errors=0。
- 本变更专用锚点：`AdminPurchaseBoundaryTest`（6）、`AdminOrderCreationBoundaryTest`（1）、`PaymentStatusControllerTest`（5）、`ApiAuthorizationMatrixTest`（2），共 14 个测试。
- 前端构建：React `npm run build`、Vue `npm run build` 和 `npm run lint` 均通过；仅有既有 bundle 体积警告。

## 验收标准

- [x] ADMIN 访问购物车、结算、商品支付、已有订单支付和退款申请均得到 403；
- [x] USER 购买、结算、支付和退款申请回归测试全绿；
- [x] ADMIN 可以查看全部订单和明细；
- [x] ADMIN 可以查询微信/支付宝支付状态并看到本地状态刷新；
- [x] ADMIN 可以通过/拒绝退款申请并查询退款状态；
- [x] React、Vue 构建通过；
- [x] spec 已从 `planned` 迁移到 `implemented`，并补充实现锚点和测试数量。
