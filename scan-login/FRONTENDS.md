# PC / Mobile 独立前端架构

现在 PC 和 Mobile 是完全独立的 React + Vite 应用，并且**都从各自站点的根路径 `/` 部署**。Mobile 不再需要 URL 路径前缀。

## 目录和端口

```text
scan-login/
├── backend/                  Spring Boot API，8080
├── frontend-pc/              PC React 前端，5174，base: /
│   ├── src/features/pc-login/
│   ├── src/shared/config/mobileSite.ts
│   ├── package.json
│   ├── package-lock.json
│   └── vite.config.ts
├── frontend-mobile/          Mobile React 前端，5173，base: /
│   ├── src/features/mobile-approval/
│   ├── package.json
│   ├── package-lock.json
│   └── vite.config.ts
└── scripts/start-frontend.ps1
```

两套应用分别安装依赖、测试、构建、部署，不相互导入源码。两者各自用 Vite `/api` 反向代理到同一台 Spring Boot 8080。

## 路由

| 应用 | 本地 URL | 用途 |
|---|---|---|
| PC | http://localhost:5174/ | 首页、自动扫码登录遮罩 |
| Mobile | http://localhost:5173/ | 登录入口 |
| Mobile | http://localhost:5173/home | 登录成功后的首页 |
| Mobile | http://localhost:5173/scan | 扫一扫与授权确认 |

PC 不再转发手机站点页面；Mobile 所有入口都是独立应用根路径下的路由。原来使用的路径前缀不再作为受支持的入口，旧二维码 URL 应重新生成。

## 快速启动

启动 MySQL、Redis 和后端 8080 后：

~~~powershell
# 终端一
cd D:\release\specs\scan-login\frontend-pc
npm ci
npm run dev

# 终端二
cd D:\release\specs\scan-login\frontend-mobile
npm ci
npm run dev
~~~

或者在项目根目录执行：

~~~powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\start-frontend.ps1
~~~

## 二维码目标地址

本地开发后端 `.env` 中配置：

~~~ini
SCAN_URL=http://localhost:5173/
~~~

Spring Boot 生成形如 `http://localhost:5173/?ticket=...` 的二维码链接，Mobile 先显示登录弹窗，登录成功后前往 `/scan`，显示短码等待人工确认。

由于运行中的 Spring Boot 仍可能缓存旧配置，PC 项目会用后端生成的原始 **ticket** 和自己配置的 Mobile 地址重新构造二维码链接，避免前端显示旧路径。**更改后端 .env 后必须重启 Spring Boot，才能让 API 的 qrUrl 字段也更新。** 此行为不会绕过服务端的 Redis 消费、浏览器绑定和 JWT 校验。

### PC -> Mobile 地址配置

本地开发 PC 前端默认链接到当前主机的 `:5173/`；公网部署时，必须将 PC 项目的 `VITE_MOBILE_URL` 配置为 Mobile 独立的 **HTTPS 域名根地址**：

~~~ini
# frontend-pc/.env.local （示例；请换成自己的独立 Mobile HTTPS 域名）
VITE_MOBILE_URL=https://YOUR_MOBILE_DOMAIN/
~~~

Mobile 的“PC 工作台”链接也可通过 `VITE_PC_URL` 配置：

~~~ini
# frontend-mobile/.env.local （示例）
VITE_PC_URL=https://YOUR_PC_DOMAIN/
~~~

设置或变更 Vite 环境变量后需要重启对应的 `npm run dev`；生产构建请在 `npm run build` 前配置好域名。勿将密钥写在 `VITE_*` 中，因为它们会进入浏览器构建文件。

**公网隧道需要同步修改端口映射：** 原 PC 隧道 `https://12fxxb9768168.vicp.fun/` 如果仍指向旧端口 5173，端口交换后会显示 Mobile，而不是 PC。请在花生壳/内网穿透配置中将 PC 隧道的本地目标改为 **5174**。Mobile 若要从公网直接访问，需另配一个可信 HTTPS 域名或隧道指向 **5173**（开发），并把该地址配置到 PC 的 `VITE_MOBILE_URL`。原公网隧道的映射尚未由本次代码修改自动更改。一个域名的根路径不能同时指向两个不同应用。

## 相机和跨域

手机浏览器实时摄像头要求安全上下文（HTTPS 或手机设备本机 localhost）。手机用电脑的局域网 IP + HTTP 访问 5173 时，浏览器通常不会提供摄像头；可以使用“拍照识别”或“从相册选择”功能作为备用。

Mobile 的 `/api` 通过同站点 Vite 代理调用后端，因此 PC Cookie 和 Mobile Bearer JWT 仍隔离，不需要对外暴露 Spring Boot 8080。生产环境建议让各自 HTTPS 站点反向代理 `/api`，而非开放后端 CORS。PC 与 Mobile 分属两个域名时，请在生产环境核对 PC 会话 Cookie 的 SameSite/Secure 配置。

## 独立测试、打包

~~~powershell
cd D:\release\specs\scan-login\frontend-pc
npm run check

cd D:\release\specs\scan-login\frontend-mobile
npm run check
~~~

部署时分别发布两个 `dist/` 作为各自站点的根目录，并为 SPA 入口提供 index.html 回退。不要将 Vite 开发服务器长期开在公网。

## 回退

本次改造前的主要配置和前端源码备份：
`D:\release\scan-login-archives\mobile-root-20261011`。

初次拆分前旧项目另保存在：
`D:\release\scan-login-archives\legacy-frontend-full-after-split-20261011`。
