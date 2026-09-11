# payment-demo-vue · 支付演示前端（Vue 2）

[![Vue](https://img.shields.io/badge/Vue-2.6-42b883.svg)](https://v2.vuejs.org/)
[![Vue CLI](https://img.shields.io/badge/Vue%20CLI-4.5-41b883.svg)](https://cli.vuejs.org/)
[![Element UI](https://img.shields.io/badge/Element%20UI-2.15-409eff.svg)](https://element.eleme.io/)

> Payment Demo 的 Vue 2 前端，功能与 [React 版](../payment-demo-react/) 对齐。
> 覆盖 **登录鉴权 → 商品浏览 → 购物车 → 合并下单 → 微信/支付宝支付 → 订单 → 退款审核 → 账单导入 → 对账 → 支付配置** 全流程。
>
> 仓库总览见 [根 README](../README.md)，后端接口见 [payment-demo/README.md](../payment-demo/README.md)。

---

## 目录

- [1. 技术栈](#1-技术栈)
- [2. 环境要求](#2-环境要求)
- [3. 快速开始](#3-快速开始)
- [4. 后端接口地址](#4-后端接口地址)
- [5. 目录结构](#5-目录结构)
- [6. 路由与权限](#6-路由与权限)
- [7. 鉴权与会话机制](#7-鉴权与会话机制)
- [8. API 模块](#8-api-模块)
- [9. 页面功能](#9-页面功能)
- [10. 与 React 版的差异](#10-与-react-版的差异)
- [11. 相关文档](#11-相关文档)

---

## 1. 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 2.6.11 | UI 框架（选项式 API） |
| Vue CLI | 4.5.0 | 脚手架与构建（`serve` / `build` / `lint`） |
| Element UI | 2.15.6 | UI 组件库（全量引入，默认中文） |
| Vue Router | 3.5.3 | 路由（**hash 模式**） |
| Axios | 0.24.0 | HTTP 客户端 |
| vue-qriously | 1.1.1 | 微信支付二维码渲染 |
| core-js | 3.6.5 | Polyfill |
| ESLint | 6.7 + eslint-plugin-vue 6.2 | 代码检查（`plugin:vue/essential` + `eslint:recommended`） |

---

## 2. 环境要求

- **Node.js 14+**（项目原记录 `14.17.0`，建议 14 或 16）
- npm（随 Node 安装）
- 运行中的后端服务（默认 `http://localhost:8080`）

---

## 3. 快速开始

```bash
# 安装依赖
npm install

# 启动开发服务器（http://localhost:3000，热更新）
npm run serve

# 生产构建（输出到 dist/）
npm run build

# 代码检查并自动修复
npm run lint
```

> 开发服务器端口在 [`vue.config.js`](vue.config.js) 中固定为 `3000`。React 版默认端口同为 `3000`，两者不能同时启动，如需并行请修改其中一方端口。

---

## 4. 后端接口地址

前端**不使用 devServer 代理**，而是直接请求后端。基址硬编码在 [`src/auth/session.js`](src/auth/session.js)：

```js
export const API_BASE_URL = 'http://localhost:8080'
```

- 修改后端地址：编辑上述 `API_BASE_URL` 常量即可（`session.js` 与 `utils/request.js` 共用）。
- 由于跨域携带 Cookie（`withCredentials: true`），后端 CORS 必须**显式放行当前来源**并允许凭证。后端默认放行 `http://localhost:3000` 与 `http://localhost:8081`（见后端 `payment.auth.allowed-origins`）。
- 若前端改用其他端口/域名，需同步更新后端 `allowed-origins`，否则刷新会话（Refresh Cookie）会失败。

---

## 5. 目录结构

```
payment-demo-vue/
├── public/
│   ├── index.html            # HTML 模板
│   └── favicon.ico
├── vue.config.js             # Vue CLI 配置（devServer 端口 3000）
├── babel.config.js
├── package.json
└── src/
    ├── main.js               # 应用入口：ElementUI、vue-qriously、router 注册
    ├── App.vue               # 根组件：AppHeader + <router-view> + AppFooter
    ├── api/                  # 后端接口封装（按模块拆分）
    │   ├── auth.js  product.js  cart.js  orderInfo.js
    │   ├── wxPay.js  aliPay.js  refundInfo.js
    │   └── bill.js  reconciliation.js  paymentConfig.js
    ├── auth/
    │   └── session.js        # API_BASE_URL、authState(Vue.observable)、refreshSession（单飞）
    ├── router/
    │   └── index.js          # 路由表 + 全局前置守卫（登录态 / 角色）
    ├── components/
    │   ├── AppHeader.vue     # 公共头（导航、用户态、购物车角标）
    │   └── AppFooter.vue     # 公共底
    ├── views/                # 页面组件（见下方路由表）
    ├── utils/
    │   └── request.js        # Axios 实例：Bearer 注入、401 自动刷新、统一响应处理
    └── assets/               # css（reset/theme/global）与图片
```

---

## 6. 路由与权限

使用 Vue Router **hash 模式**，URL 形如 `http://localhost:3000/#/orders`（哈希路由，部署时无需服务端 rewrite）。

| 路径 | 页面 | 访问权限 |
|------|------|----------|
| `/` | index（商品列表/首页） | 公开（ADMIN 访问自动跳 `/orders`） |
| `/login` | Login（登录/注册） | 公开（已登录访问自动跳 `/`） |
| `/cart` | Cart（购物车） | 仅 USER |
| `/orders` | Orders（我的订单） | 登录用户 |
| `/account` | Account（账号安全） | 登录用户 |
| `/success` | Success（支付成功） | 仅 USER |
| `/download` | Download（账单下载） | 仅 ADMIN |
| `/payment-config` | PaymentConfig（支付配置） | 仅 ADMIN |
| `/reconciliation` | Reconciliation（对账管理） | 仅 ADMIN |
| `/refunds` | Refunds（退款审核） | 仅 ADMIN |

权限由 [`router/index.js`](src/router/index.js) 的全局 `beforeEach` 守卫实现：

1. 首次导航时若会话未初始化，先 `await refreshSession()` 恢复登录态；
2. `meta.requiresAuth` 且未登录 → 跳 `/login?redirect=<原路径>`；
3. ADMIN 访问 `/` → 跳 `/orders`（管理端无购买首页）；
4. `meta.role` 不匹配 → ADMIN 跳 `/orders`，其余跳 `/`；
5. 已登录访问 `/login` → 跳 `/`。

---

## 7. 鉴权与会话机制

会话状态用 Vue 2 的响应式全局对象 `authState`（`Vue.observable`，**未引入 Vuex**），定义在 [`src/auth/session.js`](src/auth/session.js)：

- **Access Token**：存于 `authState.accessToken`，由 [`utils/request.js`](src/utils/request.js) 请求拦截器注入 `Authorization: Bearer <token>`。
- **Refresh Token**：由后端通过 **HttpOnly Cookie** 下发，前端不接触；Axios 开启 `withCredentials: true` 携带。
- **自动续期**：响应拦截器捕获 `401` 后调用 `refreshSession()`（`POST /api/auth/refresh`）换取新 Access Token 并重放原请求；刷新使用**单飞（single-flight）** Promise，避免并发重复刷新；`/api/auth/*` 及标记 `skipAuthRefresh` 的请求不触发刷新。
- **统一响应**：后端返回体 `R`（`code`/`message`/`data`），`code < 0` 时用 Element UI `Message` 弹出错误并 reject。
- **购物车角标**：`authState.cartCount` 供 `AppHeader` 展示（USER 角色）。

---

## 8. API 模块

[`src/api/`](src/api/) 按后端模块拆分，均基于 `utils/request.js` 的 Axios 实例：

| 文件 | 对应后端 | 主要用途 |
|------|----------|----------|
| `auth.js` | `/api/auth` | 注册、登录、刷新、登出、当前用户、改密 |
| `product.js` | `/api/product` | 商品列表 |
| `cart.js` | `/api/cart` | 购物车增删改查 |
| `orderInfo.js` | `/api/order-info` | 合并下单、我的订单、订单明细、状态查询、幂等键 |
| `wxPay.js` | `/api/wx-pay`、`/api/wx-pay-v2` | 微信 V3/V2 下单、查单 |
| `aliPay.js` | `/api/ali-pay` | 支付宝网站支付、查单 |
| `refundInfo.js` | `/api/refund-info` | 退款申请、审核、查询 |
| `bill.js` | `/api/bill` | 渠道账单导入（拉取/上传）、列表、记录 |
| `reconciliation.js` | `/api/reconciliation` | 触发对账、记录、明细、差异、导出 |
| `paymentConfig.js` | `/api/payment-channel`、`/api/payment-app`、`/api/payment-config` | 支付渠道/应用配置 |

---

## 9. 页面功能

| 页面 | 文件 | 功能 |
|------|------|------|
| 首页 | [`index.vue`](src/views/index.vue) | 商品列表、加入购物车、选择支付方式发起支付 |
| 登录 | [`Login.vue`](src/views/Login.vue) | 登录 / 注册 |
| 购物车 | [`Cart.vue`](src/views/Cart.vue) | 多课程多份数管理、合并结算下单 |
| 我的订单 | [`Orders.vue`](src/views/Orders.vue) | 订单列表、状态查询、取消、对已有未支付订单重新发起支付 |
| 支付成功 | [`Success.vue`](src/views/Success.vue) | 支付结果展示 |
| 账号安全 | [`Account.vue`](src/views/Account.vue) | 查看当前用户、修改密码、登出 |
| 退款审核 | [`Refunds.vue`](src/views/Refunds.vue) | （ADMIN）退款申请审核通过/拒绝、状态核对 |
| 账单下载 | [`Download.vue`](src/views/Download.vue) | （ADMIN）微信/支付宝渠道账单下载 |
| 支付配置 | [`PaymentConfig.vue`](src/views/PaymentConfig.vue) | （ADMIN）支付渠道与应用配置管理 |
| 对账管理 | [`Reconciliation.vue`](src/views/Reconciliation.vue) | （ADMIN）账单导入、手动对账、记录/明细/差异查看、报告导出 |

---

## 10. 与 React 版的差异

两套前端功能对齐，主要技术差异：

| 维度 | Vue 版（本项目） | React 版 |
|------|------------------|----------|
| 框架 | Vue 2.6（选项式） | React 18.3 |
| 构建 | Vue CLI 4.5（Webpack） | Vite 5.4 |
| UI 库 | Element UI 2.15 | Ant Design 5.21 |
| 二维码 | vue-qriously | qrcode.react |
| 状态 | `Vue.observable` 全局 `authState` | `AuthContext` + 订阅/发布 |
| 路由守卫 | `router.beforeEach` 全局守卫 | `ProtectedRoute` 组件包裹 |
| 路由模式 | hash | hash |
| 后端基址 | `src/auth/session.js` 硬编码 | `src/auth/session.js` 硬编码 |

---

## 11. 相关文档

| 文档 | 说明 |
|------|------|
| [根 README](../README.md) | Monorepo 总览（后端 + React + Vue + 治理文档） |
| [payment-demo/README.md](../payment-demo/README.md) | 后端服务与完整 API 清单 |
| [CODE_INTRO.md](../CODE_INTRO.md) | 项目代码结构深度介绍 |
| [payment-demo-react/](../payment-demo-react/) | 功能对齐的 React 前端 |

---

> 本 README 依据当前 Vue 源码（`router/index.js` 路由与守卫、`session.js`/`request.js` 鉴权、`api/` 模块、`main.js`、`vue.config.js`、`package.json`）生成。若描述与代码不符，请以代码为准并同步更新本文件。
