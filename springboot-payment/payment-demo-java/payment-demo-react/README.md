# payment-demo-react · 支付演示前端（React）

[![React](https://img.shields.io/badge/React-18.3-61dafb.svg)](https://react.dev/)
[![Vite](https://img.shields.io/badge/Vite-5.4-646cff.svg)](https://vitejs.dev/)
[![Ant Design](https://img.shields.io/badge/Ant%20Design-5.21-0170fe.svg)](https://ant.design/)

> Payment Demo 的 React 前端，由 Vue2 版本改写而来，功能与 [Vue 版](../payment-demo-vue/) 对齐。
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
- [10. 相关文档](#10-相关文档)

---

## 1. 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| React | 18.3.1 | UI 框架（`React.StrictMode`） |
| Vite | 5.4.8 | 开发服务器与构建工具 |
| Ant Design | 5.21.6 | UI 组件库（中文 locale `zhCN`） |
| React Router | 6.26.2 | 路由（`HashRouter`） |
| Axios | 1.7.7 | HTTP 客户端 |
| qrcode.react | 4.0.1 | 微信支付二维码渲染 |
| dayjs | 1.11.13 | 日期处理 |

---

## 2. 环境要求

- **Node.js 18+**（Vite 5 要求 Node `^18.0.0 || >=20.0.0`）
- npm（随 Node 安装）
- 运行中的后端服务（默认 `http://localhost:8080`）

---

## 3. 快速开始

```bash
# 安装依赖
npm install

# 启动开发服务器（http://localhost:3000）
npm run dev

# 生产构建（输出到 dist/）
npm run build

# 本地预览构建产物（http://localhost:3000）
npm run preview
```

> 开发服务器监听 `0.0.0.0:3000`（见 [`vite.config.js`](vite.config.js)）。Vue 版默认端口同为 `3000`，两者不能同时启动，如需并行请修改其中一方端口。

---

## 4. 后端接口地址

前端**不使用 Vite 代理**，而是直接请求后端。基址硬编码在 [`src/auth/session.js`](src/auth/session.js)：

```js
const API_BASE_URL = 'http://localhost:8080'
```

- 修改后端地址：编辑上述 `API_BASE_URL` 常量即可（`session.js` 与 `utils/request.js` 共用）。
- 由于跨域携带 Cookie（`withCredentials: true`），后端 CORS 必须**显式放行当前来源**并允许凭证。后端默认放行 `http://localhost:3000` 与 `http://localhost:8081`（见后端 `payment.auth.allowed-origins`）。
- 若前端改用其他端口/域名，需同步更新后端 `allowed-origins`，否则刷新会话（Refresh Cookie）会失败。

---

## 5. 目录结构

```
payment-demo-react/
├── index.html                  # Vite 入口 HTML
├── vite.config.js              # Vite 配置（端口、@ → src 别名）
├── package.json
├── public/                     # 静态资源（favicon）
└── src/
    ├── main.jsx                # 应用入口：ConfigProvider(zhCN) + 全局样式
    ├── App.jsx                 # 路由表（HashRouter + AuthProvider）
    ├── api/                    # 后端接口封装（按模块拆分）
    │   ├── auth.js  product.js  cart.js  orderInfo.js
    │   ├── wxPay.js  aliPay.js  refundInfo.js
    │   └── bill.js  reconciliation.js  paymentConfig.js
    ├── auth/
    │   ├── session.js          # 会话快照、API_BASE_URL、refreshSession（单飞）
    │   └── AuthContext.jsx     # AuthProvider：login/register/logout、购物车角标
    ├── components/
    │   ├── AppHeader.jsx  AppFooter.jsx
    │   ├── ProtectedRoute.jsx  # 路由守卫（登录态 + 角色）
    │   └── WxPayDialog.jsx     # 微信支付二维码弹窗
    ├── pages/                  # 页面组件（见下方路由表）
    ├── utils/
    │   └── request.js          # Axios 实例：Bearer 注入、401 自动刷新、统一响应处理
    └── assets/                 # css（reset/theme/global）与图片
```

---

## 6. 路由与权限

使用 `HashRouter`，URL 形如 `http://localhost:3000/#/orders`（哈希路由，部署时无需服务端 rewrite）。

| 路径 | 页面 | 访问权限 |
|------|------|----------|
| `/` | Home（商品列表） | 公开 |
| `/login` | Login（登录/注册） | 公开 |
| `/cart` | Cart（购物车） | 仅 USER |
| `/orders` | Orders（我的订单） | 登录用户 |
| `/account` | Account（账号安全） | 登录用户 |
| `/success` | Success（支付成功） | 仅 USER |
| `/download` | Download（账单下载） | 仅 ADMIN |
| `/payment-config` | PaymentConfig（支付配置） | 仅 ADMIN |
| `/reconciliation` | Reconciliation（对账管理） | 仅 ADMIN |
| `/refunds` | Refunds（退款审核） | 仅 ADMIN |

权限由 [`ProtectedRoute`](src/components/ProtectedRoute.jsx) 实现：会话未初始化时显示骨架屏；未登录跳转 `/login`（记录来源路径）；角色不匹配跳转 `/`。

---

## 7. 鉴权与会话机制

会话状态集中在 [`src/auth/session.js`](src/auth/session.js)，通过订阅/发布通知 React 组件：

- **Access Token**：内存保存，由 [`utils/request.js`](src/utils/request.js) 的请求拦截器注入 `Authorization: Bearer <token>`。
- **Refresh Token**：由后端通过 **HttpOnly Cookie** 下发，前端不接触；Axios 开启 `withCredentials: true` 携带。
- **自动续期**：响应拦截器捕获 `401` 后调用 `refreshSession()`（`POST /api/auth/refresh`）换取新 Access Token 并重放原请求；刷新使用**单飞（single-flight）** Promise，避免并发重复刷新；`/api/auth/*` 及标记 `skipAuthRefresh` 的请求不触发刷新。
- **启动引导**：`AuthProvider` 挂载时若会话未初始化则自动尝试刷新，恢复登录态。
- **统一响应**：后端返回体 `R`（`code`/`message`/`data`），`code < 0` 时弹出错误提示并 reject。

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
| 首页 | [`Home.jsx`](src/pages/Home.jsx) | 商品列表、加入购物车、选择支付方式发起支付 |
| 登录 | [`Login.jsx`](src/pages/Login.jsx) | 登录 / 注册 |
| 购物车 | [`Cart.jsx`](src/pages/Cart.jsx) | 多课程多份数管理、合并结算下单 |
| 我的订单 | [`Orders.jsx`](src/pages/Orders.jsx) | 订单列表、状态查询、取消、对已有未支付订单重新发起支付 |
| 支付成功 | [`Success.jsx`](src/pages/Success.jsx) | 支付结果展示 |
| 账号安全 | [`Account.jsx`](src/pages/Account.jsx) | 查看当前用户、修改密码、登出 |
| 退款审核 | [`Refunds.jsx`](src/pages/Refunds.jsx) | （ADMIN）退款申请审核通过/拒绝、状态核对 |
| 账单下载 | [`Download.jsx`](src/pages/Download.jsx) | （ADMIN）微信/支付宝渠道账单下载 |
| 支付配置 | [`PaymentConfig.jsx`](src/pages/PaymentConfig.jsx) | （ADMIN）支付渠道与应用配置管理 |
| 对账管理 | [`Reconciliation.jsx`](src/pages/Reconciliation.jsx) | （ADMIN）账单导入、手动对账、记录/明细/差异查看、报告导出 |

---

## 10. 相关文档

| 文档 | 说明 |
|------|------|
| [根 README](../README.md) | Monorepo 总览（后端 + React + Vue + 治理文档） |
| [payment-demo/README.md](../payment-demo/README.md) | 后端服务与完整 API 清单 |
| [CODE_INTRO.md](../CODE_INTRO.md) | 项目代码结构深度介绍 |
| [payment-demo-vue/](../payment-demo-vue/) | 功能对齐的 Vue 2 前端 |

---

> 本 README 依据当前 React 源码（`App.jsx` 路由、`session.js`/`request.js` 鉴权、`api/` 模块、`vite.config.js`、`package.json`）生成。若描述与代码不符，请以代码为准并同步更新本文件。
