# 自有 App 扫码登录项目

## ⭐ 快速体验：演示账号

> 仅用于本地开发环境，禁止在生产环境使用。

| 信息          | 值                            |
|-------------|------------------------------|
| **用户名**     | `scan_demo`                  |
| **密码**      | `ScanDemo@2026!`             |
| **手机端登录**   | http://localhost:5173/ |
| **PC 扫码登录** | http://localhost:5174/       |

**使用方法：** PC 打开系统首页，未登录时页面显示遮罩并自动弹出二维码登录框。手机端使用上述账号登录后会进入手机工作台，点击右上角头像展开「扫一扫」，扫码并核对验证码后确认，PC 自动关闭登录弹窗并显示头像。详细说明见下方「演示账号」小节。

---

Java 21 + Spring Boot 3.5.16 + Spring Security + MySQL 8 + Redis 7.4 + Lua + JWT (JJWT 0.13.0) + React 19 + TypeScript。

本项目是可运行的完整扫码登录演示：真实数据库、真实 Redis 状态机、真实 JWT 和前后端 API，不依赖伪造/Mock 登录结果。

**双前端项目结构、独立安装、端口与公网代理：** [FRONTENDS.md](FRONTENDS.md)。完整扫码流程与系统架构见 [SCAN_FLOW.md](SCAN_FLOW.md)。

## 1. 目录结构

- backend/src/main/java/com/example/scanlogin/api/：手机认证与 PC 扫码 REST 接口
- backend/src/main/java/com/example/scanlogin/application/：账号认证与二维码业务逻辑
- backend/src/main/java/com/example/scanlogin/domain/：二维码状态模型
- backend/src/main/java/com/example/scanlogin/infrastructure/：MySQL / Redis 持久化
- backend/src/main/java/com/example/scanlogin/security/：JWT、Cookie、CSRF 与权限过滤
- backend/src/main/resources/lua/：create.lua、scan.lua、confirm.lua、consume.lua
- backend/src/test/：Testcontainers 真 MySQL/Redis 的 JUnit 集成及并发测试
- frontend-pc/：独立 PC React 项目，端口 5174，扫码登录遮罩与系统首页，详见 [frontend-pc/README.md](frontend-pc/README.md)。
- frontend-mobile/：独立 Mobile React 项目，端口 5173，手机摄像头扫码及人工确认，详见 [frontend-mobile/README.md](frontend-mobile/README.md)。
- infrastructure/mysql/001_schema.sql：建表 SQL，无外键
- compose.yaml：MySQL、Redis 本地开发中间件
- scripts/start-backend.ps1、scripts/start-frontend.ps1：Windows 启动脚本
- .env：本机随机 JWT 密钥（Git 排除）

## 2. 快速运行（Windows PowerShell）

本项目目录：D:\release\specs\scan-login

第一步，在项目根目录启动 MySQL、Redis：

~~~powershell
cd D:\release\specs\scan-login
docker compose up -d
docker compose ps
~~~

MySQL: 127.0.0.1:3307，数据库 scan_login，开发账号 scan / scan_dev_password。
Redis: 127.0.0.1:6380。
仅限本机开发，默认端口绑定 127.0.0.1。MySQL SQL 只在第一次创建空数据卷时初始化。

**JWT 启动配置说明（IDEA / Maven / java -jar）：** `backend/src/main/resources/application.yml` 已配置自动读取项目根目录
`.env`。在 IDEA 中运行 `ScanLoginApplication` 时，Working directory 设为 `D:\release\specs\scan-login\backend`（也支持项目根目录
`D:\release\specs\scan-login`），不必额外手工复制 `JWT_SECRET_BASE64`。如需部署到生产环境，优先使用安全注入的系统环境变量，并且不要把
`.env` 提交到 Git。

第二步，后端（新的 PowerShell 窗口）：

~~~powershell
cd D:\release\specs\scan-login
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\start-backend.ps1
~~~

第三步，两个独立前端（自动打开两个 PowerShell 终端）：

~~~powershell
cd D:\release\specs\scan-login
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\start-frontend.ps1
~~~

PC 直接访问 http://localhost:5174/；Mobile 独立服务地址 http://localhost:5173/。PC 与 Mobile 使用独立的根站点，PC 不再代理 Mobile 路由；两者 `/api` 均代理至后端 8080。也可在两个目录分别执行 `npm run dev`。详见 [FRONTENDS.md](FRONTENDS.md)。

本机已生成随机 JWT 签名密钥，保存在被 .gitignore 排除的 .env 文件。若迁移到其他电脑，在根目录用 PowerShell 5.1 执行：

~~~powershell
$key = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($key)
$rng.Dispose()
"JWT_SECRET_BASE64=$([Convert]::ToBase64String($key))" | Set-Content -Encoding Ascii .env
"SCAN_URL=http://localhost:5173/" | Add-Content .env
~~~

### 演示账号（仅限本地开发环境）

已在本机 MySQL 的 `scan_login.app_user` 中创建并验证以下账号：

| 项目        | 内容                           |
|-----------|------------------------------|
| 登录端       | 手机端 H5 / 模拟 App              |
| 用户名       | `scan_demo`                  |
| 密码        | `ScanDemo@2026!`             |
| 用户 ID     | `1`（当前本机数据库）                 |
| 手机登录地址    | http://localhost:5173/ |
| PC 扫码登录地址 | http://localhost:5174/       |

演示步骤：PC 首页自动弹出扫码登录框。手机端输入上述用户名和密码登录后进入 `/home`，右上角头像菜单选择「扫一扫」，扫码后核对电脑端的六位验证码，再点击「确认登录该电脑」。PC 端会自动关闭遮罩、显示工作台和头像。手机端头像菜单还提供「退出登录」，PC 端退出后重新出现扫码登录遮罩。

如果清空了数据库或迁移到全新环境，先启动后端与前端，然后执行：

~~~powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-demo-user.ps1
~~~

该脚本只在用户名不存在时创建演示账号，并检查是否能够正常登录；如果账号已存在但密码被修改，检查会失败，不会覆盖现有用户密码。

**安全提示：** 上面的密码是公开的本地演示凭据，禁止在生产环境或公网可访问的服务中使用。正式部署必须禁用公开演示注册、移除演示账号，并使用专用身份认证与密钥管理机制。

## 3. 完整体验流程

1. PC 进入系统首页，未登录自动显示遮罩和二维码弹窗，服务端创建 120 秒有效的 ticket，并绑定 HttpOnly `qr_browser` Cookie。
2. 手机端 `/` 登录成功后，进入 `/home` 工作台，右上角显示头像。
3. 点击头像展开菜单并选择「扫一扫」，通过实时相机、拍照、相册或手动输入识别二维码 ticket，调用 scan 接口得到 6 位验证码。
4. 人工核对短码，点击「确认登录该电脑」。
5. PC 轮询发现 CONFIRMED 后，用原始 PC Cookie 发起 consume；Redis Lua 原子校验绑定、状态和用户。
6. PC 获得 HttpOnly JWT Cookie、通过 `/api/auth/me` 确认身份，自动关闭遮罩进入系统首页，右上角显示头像，提供「退出登录」。
7. PC 退出后重新出现扫码弹窗；手机端退出后重新显示账号登录弹窗。

手机端页面是原生 App API 的 H5 模拟调用者，方便直接测试。正式原生 Android/iOS 应接入企业已有手机端认证，并使用
Keychain/Keystore 安全保存手机 Token。

## 4. Redis 状态转换

WAIT_SCAN → SCANNED → CONFIRMED → CONSUMED

- 首次扫描锁定手机用户 ID，其他用户不可冒领。
- 同一用户扫描和确认接口可以幂等重试。
- 只有同一 PC 浏览器的原始绑定值可以消费。
- 同一 ticket 只能成功消费一次；重复消费的 Lua 返回 ALREADY_CONSUMED。
- TTL=120 秒，到期自动销毁，查询返回 EXPIRED。
- PC 绑定值为独立强随机 256 bit Cookie，Redis 只保存 SHA-256 摘要，二维码本身没有 JWT。

## 5. HTTP API

| 方法   | 路径                             | 鉴权                   | 功能               |
|------|--------------------------------|----------------------|------------------|
| POST | /api/mobile/register           | 无（演示专用）              | 注册账号             |
| POST | /api/mobile/login              | 账号密码                 | 手机 JWT           |
| GET  | /api/mobile/me                 | 手机 Bearer            | 手机用户详情           |
| GET  | /api/auth/csrf                 | 无                    | 获取 CSRF Token    |
| POST | /api/auth/qr/create            | PC CSRF              | 创建 QR 和绑定 Cookie |
| GET  | /api/auth/qr/status?ticket=... | 原始 PC Cookie         | 查询状态             |
| POST | /api/auth/qr/scan              | 手机 Bearer            | 手机扫描             |
| POST | /api/auth/qr/confirm           | 手机 Bearer            | 手机确认             |
| POST | /api/auth/qr/consume           | 原始 PC Cookie + CSRF  | 单次领取 PC 会话       |
| GET  | /api/auth/me                   | PC JWT Cookie        | PC 当前用户          |
| POST | /api/auth/logout               | PC JWT Cookie + CSRF | 清除 PC Cookie     |

手机 login 请求 JSON：{"username":"myuser","password":"StrongPass123!"}。
scan/confirm/consume 请求 JSON：{"ticket":"扫码的随机ticket"}。
前端不读取 PC JWT；PC JWT 只通过 HttpOnly Cookie 设置。

## 6. Testcontainers 并发和功能测试

~~~powershell
cd D:\release\specs\scan-login\backend
mvn test
cd ..\frontend-pc
npm ci
npm run check
cd ..\frontend-mobile
npm ci
npm run check
~~~

QrFlowIntegrationTest 包含五组真实 MySQL/Redis 集成测试：

- 注册 → 手机认证 → 扫码 → 确认 → PC 登录 → 身份校验；并验证错误 PC Cookie、重复消费及重复扫码被拒绝；
- 48 路并发尝试消费，只允许唯一胜出者；
- 不同手机账号抢码、伪造浏览器绑定被拒绝；
- 无效二维码、无 CSRF / 无认证的非法请求被拒绝；
- Redis 快照读取的状态、绑定摘要和验证码一致性。

构建成功不代表自动覆盖所有生产边界；测试用例适合继续扩展压力测试、服务断网恢复和跨实例容错。

## 7. 真机扫码 / 正式发布

若手机与 PC 使用同一可信局域网，**手机摄像头扫码需要浏览器信任的 HTTPS 来源**。在 `.env` 中将 `SCAN_URL` 改为：
https://你的Mobile独立可信域名/

然后重启后端，使 API 返回的新二维码地址生效；PC 前端在过渡期间也会独立生成新的 Mobile 根地址二维码。确保手机可访问 Mobile 站点（开发端口 5173）。
Mobile 独立项目在 `frontend-mobile/scripts/` 提供 `setup-camera-https.ps1` 和 `start-camera-https.ps1`，可为 5173 配置被手机信任的开发 HTTPS。公网访问需给 Mobile 配置独立 HTTPS 域名或隧道，PC 原公网域名只提供 PC 首页；详情见 [FRONTENDS.md](FRONTENDS.md)。局域网 HTTP 不适合使用手机浏览器摄像头。正式环境应使用企业可信 HTTPS 域名、SECURE_COOKIE=true、同源反向代理和自有 App 深链。

部署前必须完成：禁用演示注册并接入现有 IAM/用户中心；登录与扫码限流；JWT
撤销/续签和签名密钥轮换；账户封禁与风控；审计告警；手机确认设备信息和动态短码；隐私审查和安全测试。示例 logout 仅清理浏览器
Cookie，不能撤销已经泄露的 JWT，且服务器本身不执行设备指纹可信性校验。
