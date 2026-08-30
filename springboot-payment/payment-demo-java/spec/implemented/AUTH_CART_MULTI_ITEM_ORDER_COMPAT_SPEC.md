# SPEC-008：登录、服务端购物车与多课程合并下单兼容扩展

## 1. 元信息

- **状态**：implemented
- **分类**：`type: compatibility`
- **兼容版本**：v0.3.0
- **设计日期**：2026-08-30
- **影响范围**：数据库、认证鉴权、商品购买、购物车、订单、支付、退款权限、React 前端、Vue 前端
- **不在范围内**：手机号/邮箱验证、找回密码、第三方登录、管理员用户管理、优惠券、库存、按课程或份数退款

本变更同时包含数据流设计调整，但由于现有管理和购买接口将从匿名访问改为登录或 ADMIN 权限，主分类按兼容变更处理。现有 URL 与成功响应结构尽量保持不变，未认证和无权限请求分别新增 `401`、`403` 行为。

## 2. 背景与目标

当前购买页只能选择一个课程并直接调用渠道支付接口；`t_order_info` 只能保存一个 `product_id`，无法表达多课程和同一课程多份购买。系统也没有用户表、登录态或角色权限，无法在服务端隔离购物车和个人订单。

本 spec 目标：

1. 提供用户名/密码注册登录、Access Token、可轮换 Refresh Token 和退出登录。
2. 提供 `USER`、`ADMIN` 两级角色权限。
3. 登录用户在服务端保存购物车；同一账号在不同浏览器或设备登录后读取同一购物车。
4. 购物车支持多课程、同一课程多份，一次创建一张订单和多条订单明细。
5. 微信 V3、微信 V2、支付宝继续按订单总额支付。
6. 退款和对账继续以整张订单及订单总额为边界。
7. React、Vue 两套前端提供一致的登录、购物车、结算、个人订单和 ADMIN 导航。

## 3. 已确认业务规则

### 3.1 购物车

- 每个登录用户只有一个活动购物车。
- 每门课程数量为 `1–99`。
- 每个购物车最多 `20` 种课程。
- 添加已存在课程时累加数量；累加后超过 99 则拒绝，不自动截断。
- 修改数量使用绝对数量；删除课程或清空购物车立即生效。
- 购物车不保存价格快照，查询时返回课程当前价格。
- 游客可以浏览课程，但加入购物车、查看购物车和结算必须登录。

### 3.2 订单与退款

- 结算时以数据库课程当前标题和价格为准，忽略前端金额。
- 一次结算创建一张 `t_order_info` 和多条 `t_order_item`。
- `t_order_item` 保存课程 ID、标题、单价、数量、小计快照。
- 订单标题：一种课程时使用课程标题；多种课程时使用“首门课程等 N 种课程”。
- 订单金额等于全部明细小计之和，单位为分。
- 订单创建和明细保存成功后清空购物车。
- 支付渠道调用失败不回滚已创建订单；订单保持“未支付”，可从“我的订单”继续支付。
- 退款继续按订单金额申请部分或全额退款，不增加按课程或份数退款。
- 对账仍按 `order_no`、交易号、退款号和订单/退款金额匹配，不读取购物车或订单明细。

### 3.3 支付兼容

- 微信 V3、微信 V2、支付宝全部支持购物车订单。
- 保留现有按 `productId` 支付的 URL 和成功响应结构。
- 现有购买 URL 改为需要 USER 或 ADMIN 登录，并把创建的旧式单课程订单归属当前用户。
- 新增按 `orderNo` 发起支付的 URL，购物车前端只使用新 URL。
- 支付、退款渠道通知保持匿名可访问并继续执行现有签名/验签逻辑。

## 4. 认证与权限设计

### 4.1 用户与密码

- 用户名去除首尾空格后保存，长度 `3–64`，在数据库 utf8mb4 通用排序规则下唯一。
- 密码长度 `8–72`，只保存 BCrypt 哈希，禁止在日志、异常和响应中输出密码。
- 注册用户角色固定为 `USER`，请求不能提交角色字段。
- 初始化 SQL 预置 `admin` 用户，角色为 `ADMIN`，数据库只保存 BCrypt 哈希。
- README 记录演示初始密码 `Admin@123456`，并要求首次使用后通过修改密码接口更新。
- 修改密码必须验证旧密码；成功后撤销该用户全部 Refresh Token，并要求重新登录。

### 4.2 Access Token

- 使用 HMAC 签名 JWT，有效期 15 分钟。
- Claim 只包含用户 ID、用户名、角色、签发时间、到期时间和唯一 Token ID。
- 签名密钥从 `PAYMENT_AUTH_JWT_SECRET` 环境变量读取，生产环境不得使用仓库默认演示密钥。
- React/Vue 只在内存保存 Access Token，通过 `Authorization: Bearer <token>` 发送。

### 4.3 Refresh Token

- Refresh Token 是至少 256 bit 的密码学随机值，有效期 7 天。
- 原始 Token 只放在 `HttpOnly` Cookie；数据库只保存 SHA-256 哈希。
- Cookie 使用可配置的 `Secure`、`SameSite` 和固定 `/api/auth` Path；本地开发允许非 Secure，生产必须 Secure。
- 每次刷新都签发新的 Refresh Token 并撤销旧 Token。
- 已撤销 Token 再次出现视为重放，撤销同一 Token family 的全部有效 Token。
- 每次登录建立新的 Token family，允许同一账号多设备登录。
- 退出登录只撤销当前 family；修改密码撤销该用户全部 family。

### 4.4 CORS 与错误响应

- 后端使用全局 CORS 白名单并允许凭据，React/Vue 请求设置 `withCredentials: true`。
- Refresh 请求检查允许的 Origin，避免任意站点借用 Cookie 刷新登录态。
- 未认证返回 HTTP `401`，权限不足返回 HTTP `403`。
- 401/403 响应体继续使用现有 `R` JSON 字段：`code`、`message`、`data`。
- 前端收到 401 时只允许一个刷新请求在途，其余请求排队；刷新成功后重放一次，刷新失败则清空用户状态并跳转登录页。

### 4.5 角色矩阵

| 能力 | 游客 | USER | ADMIN |
|------|------|------|-------|
| 课程列表、注册、登录 | ✅ | ✅ | ✅ |
| 服务端购物车 | ❌ | 本人 | 本人 |
| 结算、按订单支付 | ❌ | 本人 | 本人或管理操作 |
| 我的订单、订单明细、退款申请 | ❌ | 本人 | 本人或管理操作 |
| 全部订单、退款审核/拒绝/状态核对 | ❌ | ❌ | ✅ |
| 支付渠道/应用配置 | ❌ | ❌ | ✅ |
| 账单导入、对账、导出 | ❌ | ❌ | ✅ |
| 支付和退款渠道通知 | 公共验签入口 | 公共验签入口 | 公共验签入口 |

## 5. 数据库设计

所有表结构统一写入唯一初始化脚本 `payment-demo/sql/payment-demo.sql`。

### 5.1 `t_user`

| 字段 | 类型 | 约束/说明 |
|------|------|-----------|
| id | bigint unsigned | PK，自增 |
| username | varchar(64) | NOT NULL，唯一 |
| password_hash | varchar(100) | NOT NULL，BCrypt |
| role | varchar(16) | NOT NULL，USER/ADMIN |
| create_time | datetime | 创建时间 |
| update_time | datetime | 更新时间 |

### 5.2 `t_refresh_token`

| 字段 | 类型 | 约束/说明 |
|------|------|-----------|
| id | bigint unsigned | PK，自增 |
| user_id | bigint unsigned | NOT NULL，用户 ID |
| token_hash | char(64) | NOT NULL，SHA-256，唯一 |
| token_family | char(36) | NOT NULL，登录会话 family |
| expires_at | datetime | NOT NULL |
| revoked_at | datetime | 可空 |
| replaced_by_hash | char(64) | 可空，轮换后的 Token 哈希 |
| create_time | datetime | 创建时间 |

索引：`uk_refresh_token_hash`、`idx_refresh_user_family`、`idx_refresh_expire`。

### 5.3 `t_cart`

| 字段 | 类型 | 约束/说明 |
|------|------|-----------|
| id | bigint unsigned | PK，自增 |
| user_id | bigint unsigned | NOT NULL，每用户唯一 |
| version | int | NOT NULL，默认 0，乐观锁 |
| create_time | datetime | 创建时间 |
| update_time | datetime | 更新时间 |

### 5.4 `t_cart_item`

| 字段 | 类型 | 约束/说明 |
|------|------|-----------|
| id | bigint unsigned | PK，自增 |
| cart_id | bigint unsigned | NOT NULL |
| product_id | bigint | NOT NULL |
| quantity | int | NOT NULL，1–99 |
| create_time | datetime | 创建时间 |
| update_time | datetime | 更新时间 |

唯一约束：`uk_cart_product(cart_id, product_id)`；索引：`idx_cart_item_cart`。

### 5.5 `t_order_item`

| 字段 | 类型 | 约束/说明 |
|------|------|-----------|
| id | bigint unsigned | PK，自增 |
| order_id | bigint unsigned | NOT NULL，关联 `t_order_info.id` |
| product_id | bigint | NOT NULL |
| product_title | varchar(256) | NOT NULL，结算快照 |
| unit_price | int | NOT NULL，分 |
| quantity | int | NOT NULL |
| subtotal | int | NOT NULL，分 |
| create_time | datetime | 创建时间 |

索引：`idx_order_item_order(order_id)`、`idx_order_item_product(product_id)`。

### 5.6 `t_order_info` 调整

- 复用现有 `user_id` 保存订单所属用户，存量订单允许为 null。
- `product_id` 改为可空；旧式单课程订单继续保存，购物车订单设为 null。
- 新增 `checkout_request_id varchar(64)`，与 `user_id` 组成唯一约束，用于结算幂等。
- 新增 `idx_order_user_time(user_id, create_time)`。
- 不改变 `order_no`、`total_fee`、支付状态、退款状态和对账字段语义。

## 6. API 契约

### 6.1 认证 API

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| POST | `/api/auth/register` | 公共 | 用户名、密码注册 USER |
| POST | `/api/auth/login` | 公共 | 返回 Access Token，设置 Refresh Cookie |
| POST | `/api/auth/refresh` | Refresh Cookie | 轮换 Token 并返回 Access Token |
| POST | `/api/auth/logout` | Refresh Cookie | 撤销当前 Token family 并清 Cookie |
| GET | `/api/auth/me` | USER/ADMIN | 当前用户信息 |
| PUT | `/api/auth/password` | USER/ADMIN | 验证旧密码并修改密码 |

### 6.2 购物车 API

| 方法 | 路径 | 请求 | 说明 |
|------|------|------|------|
| GET | `/api/cart` | - | 当前用户购物车、当前价格与合计 |
| POST | `/api/cart/items` | productId, quantity | 添加或累加课程 |
| PUT | `/api/cart/items/{productId}` | quantity | 设置绝对数量 |
| DELETE | `/api/cart/items/{productId}` | - | 删除课程 |
| DELETE | `/api/cart` | - | 清空购物车 |

购物车响应包含：`items`、`distinctCount`、`totalQuantity`、`totalAmount`。每项包含 `productId`、`productTitle`、`unitPrice`、`quantity`、`subtotal`。

### 6.3 订单 API

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| POST | `/api/order-info/checkout` | USER/ADMIN | 请求 paymentAppId、checkoutRequestId；创建订单并清空购物车 |
| GET | `/api/order-info/my-list` | USER/ADMIN | 当前用户订单，含课程摘要 |
| GET | `/api/order-info/{orderNo}/items` | 本人/ADMIN | 订单课程明细 |
| GET | `/api/order-info/query-order-status/{orderNo}` | 本人/ADMIN | 保留路径，增加所有权校验 |
| GET | `/api/order-info/list` | ADMIN | 保留响应，改为全量管理列表 |

重复 `checkoutRequestId` 返回第一次创建的订单；同一用户并发结算通过分布式锁和购物车行锁串行化。

### 6.4 新增按订单支付 API

| 方法 | 路径 | 权限 |
|------|------|------|
| POST | `/api/wx-pay/native/order/{orderNo}` | 本人/ADMIN |
| POST | `/api/wx-pay-v2/native/order/{orderNo}` | 本人/ADMIN |
| POST | `/api/ali-pay/trade/page/pay/order/{orderNo}` | 本人/ADMIN |

支付服务读取订单中已保存的 `payment_app_id`、渠道、标题和总额。订单必须为“未支付”，payment app 必须存在且启用，渠道必须与 API 一致。

### 6.5 既有 API 权限调整

- `POST /api/wx-pay/native/{productId}`、`POST /api/wx-pay-v2/native/{productId}`、`POST /api/ali-pay/trade/page/pay/{productId}`：USER/ADMIN。
- `/api/refund-info/apply` 及两个渠道兼容退款申请路径：订单本人/ADMIN。
- `/api/refund-info/list/{orderNo}`：订单本人/ADMIN。
- `/api/refund-info/list`、approve、reject、query、reconcile：ADMIN。
- `/api/payment-channel/**`、`/api/payment-app/**`、`/api/payment-config/**`：ADMIN；前台结算所需启用应用列表由新增的只读 USER 接口提供。
- `/api/bill/**`、`/api/reconciliation/**`、账单下载查询：ADMIN。
- 微信 V2/V3、支付宝支付和退款通知路径：公共，继续验签。

## 7. 核心数据流

### 7.1 结算

1. 前端生成并在本次交互中保留 `checkoutRequestId`。
2. 后端校验当前用户、支付应用和请求 ID。
3. 获取 `payment:cart:checkout:{userId}` 分布式锁。
4. 在事务内按 userId 锁定购物车，检查幂等订单。
5. 查询购物车项和全部课程；校验种类、数量、课程存在性。
6. 使用 `Math.multiplyExact`、`Math.addExact` 计算明细小计和订单总额。
7. 插入 `t_order_info`，批量插入 `t_order_item`。
8. 清空 `t_cart_item`，事务提交。
9. 提交后发送现有延迟关单消息，返回订单和明细摘要。
10. 前端按 payment app 渠道调用对应的按订单支付 API。

任一步在事务提交前失败，订单、明细和购物车保持原状。渠道支付失败发生在提交后，只影响支付动作，不恢复购物车。

### 7.2 Access Token 自动刷新

1. 应用启动调用 `/api/auth/refresh`，成功后恢复内存 Access Token 和当前用户。
2. 普通请求携带 Access Token。
3. 收到 401 时，Axios 拦截器进入单飞刷新；其他 401 请求等待同一 Promise。
4. 刷新成功后每个请求最多重放一次。
5. 刷新失败清空认证状态，跳转登录页，并保留原目标路由用于登录后返回。

## 8. 前端设计

React、Vue 功能保持一致：

- 新增登录页、注册页和认证状态模块。
- 请求实例开启 `withCredentials`，注入内存 Access Token，并实现单飞刷新队列。
- 路由守卫区分游客、USER、ADMIN。
- Header 展示用户名、退出、购物车数量；ADMIN 显示管理中心入口。
- 课程页从单选按钮改为课程卡片和“加入购物车”。
- 采用已确认的布局 B：独立购物车页。
- 购物车页显示课程、当前单价、数量、小计、总额和支付应用；结算按钮串联创建订单和发起支付。
- 我的订单按当前用户查询，可展开订单明细；未支付订单显示“继续支付”。
- ADMIN 保留并保护全部订单、退款审核、支付配置、账单和对账页面。
- 移动端课程和购物车表格重排，不依赖横向滚动完成核心操作。

## 9. 异常与边界

- 用户名重复：409 语义错误，不泄露密码或哈希。
- 登录失败：统一“用户名或密码错误”，避免枚举账号。
- 购物车为空、课程不存在、课程种类或数量超限：不创建订单。
- 前端价格字段即使被构造也不参与后端金额计算。
- payment app 不存在、停用或渠道不匹配：订单不进入渠道支付。
- 用户访问他人购物车、订单、订单明细或退款：403。
- 结算请求重试：按 `user_id + checkout_request_id` 返回同一订单。
- Token 重放：撤销 Token family，要求重新登录。
- 订单创建成功、渠道支付失败：保留未支付订单和明细，允许继续支付。

## 10. 测试要求

### L0 特征测试

- 现有 Public API、微信 V2/V3、支付宝、退款、消息和对账特征测试保持全绿。
- 锁定既有按 productId 支付路径和成功响应结构。
- 锁定新增 401/403 `R` 响应结构。

### L1 单元测试

- 注册校验、BCrypt 哈希、统一登录失败提示。
- JWT 签发/校验/过期。
- Refresh Token 哈希、轮换、退出、重放和密码修改撤销。
- 角色和订单所有权判断。
- 两用户购物车隔离、添加累加、绝对修改、删除、清空。
- 1/99/100 数量边界、20/21 种课程边界。
- 服务端核价、金额溢出防护、订单标题和明细快照。
- 结算幂等、并发串行、事务回滚、成功后清空购物车。
- 三个渠道的按订单支付校验。
- 订单级部分/全额退款行为不变。

### L2 集成测试

- MySQL 唯一约束、外键、行锁和事务回滚。
- Spring Security 路由权限矩阵。
- Refresh Cookie 属性、CORS 凭据和 Origin 校验。
- ADMIN、USER 对同一接口的 200/401/403 差异。

### 前端验证

- React、Vue 构建通过；Vue lint 通过。
- 注册、登录、页面刷新恢复、Token 自动刷新、退出。
- 加入购物车、数量边界、清空、结算和继续支付。
- USER 看不到 ADMIN 导航且不能直接访问 ADMIN 路由。

## 11. 兼容与迁移

- 数据库使用更新后的唯一 `payment-demo.sql` 初始化。
- 存量订单的 `user_id`、订单明细允许为空；管理端仍可查看，普通用户不能认领。
- 原按 productId 支付 URL 保留，但匿名调用从成功变为 401。
- 原管理 URL 保留，但非 ADMIN 从成功变为 401/403。
- 渠道通知 URL 和响应协议不变。
- `R` 的字段结构不变；认证错误通过 HTTP 状态和负数业务 code 表达。
- README 增加环境变量、初始管理员、Cookie/CORS 和前端启动说明。

## 12. 验收标准

- [x] 注册密码以 BCrypt 哈希保存，数据库无明文密码。
- [x] Access Token 15 分钟，Refresh Token 7 天并轮换，退出可撤销。
- [x] USER/ADMIN 权限矩阵有自动化测试。
- [x] 不同用户购物车完全隔离，同一用户跨设备登录可读取同一购物车。
- [x] 同一课程可买 1–99 份，购物车最多 20 种课程。
- [x] 多课程一次生成一张订单和正确的订单明细快照。
- [x] 后端重新核价且金额计算无溢出。
- [x] 重复结算不创建重复订单。
- [x] 微信 V3、微信 V2、支付宝均可按订单号支付。
- [x] 支付失败后可从“我的订单”继续支付。
- [x] 退款和对账仍以整张订单为边界且回归测试全绿。
- [x] React、Vue 均完成布局 B 的登录、购物车、结算和角色导航。
- [x] Spec 实现后从 `planned` 迁移到 `implemented`。
