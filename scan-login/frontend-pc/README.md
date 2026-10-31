# ScanLogin PC（独立根站点）

PC React + Vite 开发端口 5174，首页为 `/`。未登录自动出现扫码登录遮罩，认证后进入系统首页并显示用户头像。

~~~powershell
cd D:\release\specs\scan-login\frontend-pc
npm ci
npm run dev
npm run check
npm run build
~~~

PC 只将 `/api` 代理到后端 Spring Boot 8080，**不再反向代理 Mobile 页面路由**。Mobile 是独立 5173 根站点。PC 右上角手机工作台链接、动态二维码目标都由 `src/shared/config/mobileSite.ts` 生成。

本地默认链接当前主机的 `http://localhost:5173/`，公网发布时需配置：

~~~ini
# frontend-pc/.env.local
VITE_MOBILE_URL=https://YOUR_MOBILE_DOMAIN/
~~~

更改环境变量后重启 Vite，重新生成二维码即可。配置详情见 [../FRONTENDS.md](../FRONTENDS.md)。
