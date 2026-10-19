# ScanLogin Mobile（独立根站点）

Mobile Vite 基础路径为 `/`，独立开发端口 5173。

| 路由 | 用途 |
|---|---|
| `/` | 手机登录入口 |
| `/home` | 已登录工作台 |
| `/scan` | 扫码和人工确认 |

~~~powershell
cd D:\release\specs\scan-login\frontend-mobile
npm ci
npm run dev
npm run check
npm run build
~~~

Mobile 登录后 JWT 仅保存在 React 内存中；刷新页面需要重新登录。手机端支持实时摄像头扫码、原生拍照、相册文件识别、手动输入 ticket，扫码不等于授权，仍需核对六位验证码并手动确认。

`src/features/mobile-approval/model/qrPayload.ts` 只接受独立根站点二维码 `https://YOUR_MOBILE_HOST/?ticket=...` 或原始 ticket，不会导航到二维码携带的任意 URL。

本地手机用 `http://电脑IP:5173/` 访问时，浏览器可能禁用实时摄像头。使用 Mobile 独立可信 HTTPS 域名，或项目 `scripts/setup-camera-https.ps1`、`scripts/start-camera-https.ps1` 配置可被设备信任的开发证书。

Mobile 的 `/api` 在开发环境代理到 Spring Boot 8080。可设置 `VITE_PC_URL` 指定 PC 站点根地址。详见 [../FRONTENDS.md](../FRONTENDS.md)。
