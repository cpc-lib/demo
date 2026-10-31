# 扫码登录流程说明

本文档梳理扫码登录的完整业务流程，帮助理解前后端交互、Redis 状态转换和 JWT 鉴权机制。

---

## 一、角色与端

| 角色 | 说明 |
|------|------|
| **PC 端** | PC 浏览器，端口 5174，展示二维码并等待扫码确认 |
| **手机端** | 手机浏览器 / H5，端口 5173，登录账号后扫描二维码并确认授权 |
| **后端** | Spring Boot 服务，端口 8080，提供 REST API，管理二维码状态和 JWT 颁发 |

---

## 二、前置条件

1. **手机端已注册账号**：通过 `/api/mobile/register` 注册（仅开发环境使用，生产环境应接入已有 IAM）。
2. **后端已启动**：MySQL、Redis 正常运行，JWT 密钥已配置。
3. **PC 端与手机端可访问**：PC 访问 `http://localhost:5174/`，手机访问 `http://localhost:5173/`（真机扫码需 HTTPS）。

---

## 三、完整扫码流程

### 步骤 1：PC 端生成二维码

**触发时机**：用户打开 PC 端首页，未登录时自动触发。

**前端行为**：
- `PcLoginPage` 检测到未登录，调用 `usePcQrLogin` hook。
- Hook 调用 `pcAuthApi.createQr()` → `POST /api/auth/qr/create`。

**后端行为**（`QrAuthController.create`）：
1. 生成 256-bit 随机 `binding`（PC 浏览器绑定值）。
2. 调用 `QrLoginService.create(binding)`：
   - 生成 43 位随机 `ticket`（二维码唯一标识）。
   - 生成 6 位随机 `verificationCode`（验证码，供人工核对）。
   - Redis Lua 脚本 `create.lua` 原子写入：`ticket → { bindingHash, code, status=WAIT_SCAN, ttl=120s }`。
   - 返回二维码 URL（形如 `http://localhost:5173/?ticket=xxx`）。
3. 通过 `AuthCookies.bindBrowser` 设置 HttpOnly Cookie `qr_browser=binding`（路径 `/api/auth/qr`，5 分钟有效）。

**前端展示**：
- `QrLoginCard` 组件渲染二维码（使用 `qrcode.react`），显示 6 位验证码。
- 开始轮询 `GET /api/auth/qr/status?ticket=xxx`（每 1.5 秒一次）。

---

### 步骤 2：手机端登录

**触发时机**：用户打开手机端首页，未登录时显示登录弹窗。

**前端行为**：
- `MobileApprovalPage` 检测到未登录，显示 `CredentialsForm`。
- 用户输入账号密码，点击「手机登录」。
- Hook `useMobileApproval` 调用 `mobileAuthApi.login` → `POST /api/mobile/login`。

**后端行为**（`MobileAuthController.login`）：
1. `UserAccountService.loginMobile` 校验用户名密码。
2. 调用 `JwtService.issue(userId, "mobile")` 颁发手机 JWT（2 小时有效）。
3. 写入登录审计日志 `login_audit` 表。
4. 返回 `{ accessToken, username, userId }`。

**前端状态**：
- 登录成功后，`session` 状态保存 `accessToken`。
- 页面跳转到 `/home`，右上角显示用户头像。

---

### 步骤 3：手机端扫描二维码

**触发时机**：用户在手机端点击头像 → 「扫一扫」，进入扫码页面。

**前端行为**：
- `MobileApprovalPage` 显示 `MobileApprovalCard`。
- `CameraQrScanner` 调用手机摄像头（需 HTTPS），实时解码二维码。
- 识别二维码 URL 中的 `ticket` 参数，调用 `parseQrTicket` 校验合法性。
- 调用 `mobileAuthApi.scan(ticket, accessToken)` → `POST /api/auth/qr/scan`，Header 携带 `Authorization: Bearer <accessToken>`。

**后端行为**（`QrAuthController.scan`）：
1. `JwtAuthenticationFilter` 校验手机 JWT，提取 `userId`，设置 `ROLE_MOBILE`。
2. `QrLoginService.scan(ticket, userId)`：
   - 校验 `userId` 有效（`requireActive`）。
   - Redis Lua 脚本 `scan.lua` 原子转换状态：
     - 若 `status != WAIT_SCAN`，拒绝（已被扫描或已过期）。
     - 若 `userId` 与首次扫描不一致，拒绝（防止冒领）。
     - 否则设置 `status=SCANNED`，锁定 `userId`。
   - 返回 `{ status: SCANNED, verificationCode }`。

**前端展示**：
- 手机端显示 6 位验证码，提示「请核对电脑端验证码后确认」。

---

### 步骤 4：手机端确认授权

**触发时机**：用户核对验证码一致后，点击「确认登录该电脑」。

**前端行为**：
- `useMobileApproval` 调用 `mobileAuthApi.confirm(ticket, accessToken)` → `POST /api/auth/qr/confirm`。

**后端行为**（`QrAuthController.confirm`）：
1. JWT 校验同上。
2. `QrLoginService.confirm(ticket, userId)`：
   - Redis Lua 脚本 `confirm.lua` 原子转换：
     - 校验 `userId` 与扫描时一致。
     - 设置 `status=CONFIRMED`。
   - 返回 `{ status: CONFIRMED }`。

**前端展示**：
- 手机端提示「授权成功！请返回电脑查看登录结果。」

---

### 步骤 5：PC 端完成登录

**触发时机**：PC 端轮询发现 `status=CONFIRMED`。

**前端行为**：
- `usePcQrLogin` 轮询 `/api/auth/qr/status` 返回 `CONFIRMED`。
- 调用 `pcAuthApi.consume(ticket)` → `POST /api/auth/qr/consume`，Cookie 自动携带 `qr_browser`。

**后端行为**（`QrAuthController.consume`）：
1. `QrLoginService.consume(ticket, browserSecret)`：
   - 从 Cookie 读取 `qr_browser`，校验 SHA-256 哈希与 Redis 中 `bindingHash` 一致（防止二维码被其他浏览器消费）。
   - Redis Lua 脚本 `consume.lua` 原子转换：
     - 校验 `status=CONFIRMED` 且 `userId` 已锁定。
     - 设置 `status=CONSUMED`，防止重复消费。
   - 调用 `JwtService.issue(userId, "pc")` 颁发 PC JWT（1 小时有效）。
2. `AuthCookies.loginPc` 设置 HttpOnly Cookie `PC_ACCESS=<jwt>`（路径 `/api`，1 小时有效）。
3. 写入审计日志 `QR_PC_LOGIN`。
4. 返回 `{ status: LOGGED_IN }`。

**前端行为**：
- 调用 `pcAuthApi.me()` → `GET /api/auth/me`，Cookie 自动携带 `PC_ACCESS`。
- 后端 `JwtAuthenticationFilter` 校验 PC JWT，返回 `{ id, username }`。
- 前端设置 `account` 状态，关闭登录遮罩，显示系统首页和头像。

---

### 步骤 6：退出登录

**PC 端退出**：
- 点击头像 → 「退出登录」，调用 `pcAuthApi.logout()` → `POST /api/auth/logout`。
- 后端 `AuthCookies.logoutPc` 清空 `PC_ACCESS` Cookie。
- 前端清除 `account` 状态，重新显示二维码登录遮罩。

**手机端退出**：
- 点击头像 → 「退出登录」，前端清除 `session` 状态，重新显示登录弹窗。
- 手机 JWT 仅在内存，不持久化，退出即失效。

---

## 四、Redis 状态机

```
WAIT_SCAN ──扫描──> SCANNED ──确认──> CONFIRMED ──消费──> CONSUMED
   │                  │                   │
   └──120秒过期──> EXPIRED           └──重复消费拒绝──> CONFLICT
```

**Lua 脚本原子性**：
- `create.lua`：初始化 ticket，设置 bindingHash、code、status。
- `scan.lua`：校验状态为 WAIT_SCAN，锁定 userId。
- `confirm.lua`：校验 userId 一致，状态转为 CONFIRMED。
- `consume.lua`：校验 bindingHash 和状态，转为 CONSUMED，返回 JWT。

**安全特性**：
- 首次扫描锁定 userId，其他用户不可冒领。
- 同一用户扫码和确认可幂等重试。
- 只有原 PC 浏览器的 binding 可以消费。
- 同一 ticket 只能消费一次。

---

## 五、双 Token 与 Cookie 隔离

系统采用双 token 机制：短效 access token 用于日常鉴权，长效 refresh token 仅用于换取新的 token 对。

| Token 类型 | 用途 | 存储方式 | 有效期 |
|-----------|------|---------|--------|
| 手机 Access Token | 手机端扫码、确认 | 内存，不持久化 | 30 分钟 |
| 手机 Refresh Token | 换取新的手机双 token | Bearer 响应体 + 数据表 `refresh_token`（SHA-256 摘要） | 7 天 |
| PC Access Token | PC 端登录态 | HttpOnly Cookie `PC_ACCESS`（路径 /api） | 30 分钟 |
| PC Refresh Token | 自动续期 PC 会话 | HttpOnly Cookie `PC_REFRESH`（路径 /api/auth）+ 数据表 `refresh_token`（SHA-256 摘要） | 7 天 |
| 浏览器绑定 | PC 二维码消费校验 | HttpOnly Cookie `qr_browser` | 5 分钟 |
| 图片验证码答案 | 登录防刷校验 | Redis `auth:captcha:{id}` | 3 分钟 |

**隔离机制**：
- token 的 `kind` 字段区分四种类型：`pc`、`mobile`、`pc-refresh`、`mobile-refresh`。
- 手机 Access Token 通过 `Authorization: Bearer` 传递，仅用于 `/api/mobile/me`、`/api/auth/qr/scan`、`/api/auth/qr/confirm`。
- PC Access Token 通过 Cookie 传递，仅用于 `/api/auth/me`（登出接口放行匿名，便于只带 refresh cookie 时吊销会话）。
- Refresh Token 不能当 Access Token 使用：`JwtAuthenticationFilter` 只认可对应 kind 的 token，刷新接口单独校验 refresh kind。
- PC 端 401 时前端自动携带 `PC_REFRESH` Cookie 调用 `/api/auth/refresh` 并重试原请求（单飞，防并发重复刷新）。
- 手机端扫码/确认遇 401 时，自动用 refresh token 换新 token 对并重试一次。

**Refresh Token 落库（`refresh_token` 表，`RefreshTokenService` 管理）**：
- 签发即登记：登录（密码/扫码消费）与刷新时，把 refresh JWT 的 **SHA-256 摘要**连同 `user_id`、`client`（PC/MOBILE）、`expires_at` 写入数据表；明文 token 不落库。
- 刷新先查库：`/api/auth/refresh`、`/api/mobile/refresh` 除校验 JWT 签名和 kind 外，还必须命中一条**未吊销、未过期、client 匹配、归属正确**的记录。
- 一次性轮换：每次刷新成功即把旧记录标记 `revoked=1`，新 token 另起一行。
- 盗用检测：已吊销 token 再次被使用 → 吊销该用户该端**全部**活跃 token（防止攻击者用窃取的旧 token 续命），需重新登录。
- 登出吊销：`/api/auth/logout` 将请求携带的 refresh token 记录标记 `revoked=1`。
- 表结构见 `backend/src/main/resources/schema.sql`（启动时 `CREATE TABLE IF NOT EXISTS` 自动建表）；吊销记录保留用于审计，不做物理删除。

**会话版本与踢出机制（`app_user.token_version`）**：
- 每个用户持有单调递增的 `token_version`；签发的所有 JWT（四种 kind）都内嵌 `ver` 声明。
- `JwtAuthenticationFilter` 对每个携带 token 的请求校验 `ver` 与数据库当前版本一致，不一致立即拒绝——被踢用户的 access token 即使未过期也当场失效。
- 刷新接口（`RefreshTokenService.requireActive`）同样校验版本，旧版本 token 无法续期。
- 踢出原语 `UserAccountService.kickUser(userId)`：版本号 +1（access 全灭）+ 吊销该用户全部 refresh token（双端，无法续命）。
- 管理接口（`/api/admin/**`，由 `X-Admin-Key` 请求头守卫，密钥配置 `app.admin.key`，默认 `dev-admin-key`，可用环境变量 `APP_ADMIN_KEY` 覆盖）：
  - `GET /api/admin/users`：用户列表（含会话版本、活跃 refresh token 数）；
  - `POST /api/admin/users/{id}/kick`：踢出指定用户（仍受 CSRF 保护）。
- 管理控制台页面：PC 前端 `#/admin` 路由（`http://localhost:5174/#/admin`），填写密钥后可查看用户并一键踢出。
- 正常登录/刷新签发的 token 携带当前版本，天然免疫该机制；跨版本升级部署后旧 token（无 `ver` 声明）会被拒绝一次，用户重新登录即可。

---

## 六、API 列表

| 方法 | 路径 | 鉴权 | 功能 |
|------|------|------|------|
| POST | `/api/mobile/register` | 无 | 注册手机账号（需图片验证码，用户名唯一，重复返回 409） |
| GET  | `/api/auth/captcha` | 无 | 获取图片验证码（4 位字符，3 分钟有效） |
| POST | `/api/mobile/login` | 账号密码 + 图片验证码 + IP 限流 | 手机登录，返回手机双 token |
| POST | `/api/mobile/refresh` | body 携带 refreshToken | 手机刷新双 token |
| GET  | `/api/mobile/me` | 手机 Bearer | 查询手机用户信息 |
| GET  | `/api/auth/csrf` | 无 | 获取 CSRF Token |
| POST | `/api/auth/login` | 账号密码 + 图片验证码 + IP 限流 | PC 账号密码登录，设置双 Cookie |
| POST | `/api/auth/refresh` | `PC_REFRESH` Cookie | PC 刷新双 Cookie |
| POST | `/api/auth/qr/create` | PC CSRF | 创建二维码，设置 binding Cookie |
| GET  | `/api/auth/qr/status` | PC Cookie | 轮询二维码状态 |
| POST | `/api/auth/qr/scan` | 手机 Bearer | 手机扫码，锁定 userId |
| POST | `/api/auth/qr/confirm` | 手机 Bearer | 手机确认授权 |
| POST | `/api/auth/qr/consume` | PC Cookie + CSRF | PC 消费二维码，获取双 token Cookie |
| GET  | `/api/auth/me` | PC Access Cookie | 查询 PC 用户信息 |
| POST | `/api/auth/logout` | PC Access Cookie + CSRF | 清除 PC 双 Cookie 登录态 |

---

## 七、安全注意事项

1. **演示账号**：`scan_demo / ScanDemo@2026!` 仅用于本地开发，生产环境必须禁用公开注册。
2. **HTTPS**：真机扫码需要手机浏览器信任的 HTTPS 来源，否则摄像头不可用。
3. **Cookie 安全**：生产环境应设置 `SECURE_COOKIE=true`，启用 `Secure` 属性。
4. **JWT 密钥**：`.env` 中的 `JWT_SECRET_BASE64` 不得提交到 Git，生产环境使用安全注入。
5. **CSRF**：PC 端写操作（create/consume/logout）需携带 CSRF Token，防止跨站请求伪造。
6. **限流风控**：生产环境应对登录、扫码接口限流，防止暴力破解和滥用。

---

## 八、前端文件结构

```
frontend-pc/
├── src/
│   ├── features/pc-login/
│   │   ├── pages/PcLoginPage.tsx       # PC 登录页组合
│   │   ├── components/QrLoginCard.tsx  # 二维码展示组件
│   │   ├── hooks/usePcQrLogin.ts       # PC 扫码登录状态管理
│   │   ├── api/pcAuthApi.ts            # PC 端 API 封装
│   │   └── model/types.ts              # PC 端类型定义
│   └── shared/                         # 通用组件、样式、API 客户端

frontend-mobile/
├── src/
│   ├── features/mobile-approval/
│   │   ├── pages/MobileApprovalPage.tsx     # 手机登录 + 扫码页组合
│   │   ├── components/CredentialsForm.tsx   # 账号密码表单
│   │   ├── components/MobileApprovalCard.tsx # 扫码确认卡片
│   │   ├── components/CameraQrScanner.tsx   # 摄像头扫码组件
│   │   ├── hooks/useMobileApproval.ts       # 手机端状态管理
│   │   ├── api/mobileAuthApi.ts             # 手机端 API 封装
│   │   └── model/types.ts                   # 手机端类型定义
│   └── shared/                              # 通用组件、样式、API 客户端
```

---

## 九、后端文件结构

```
backend/src/main/java/com/example/scanlogin/
├── api/
│   ├── MobileAuthController.java    # 手机注册、登录、查询
│   ├── QrAuthController.java        # 二维码创建、扫码、确认、消费
│   └── ApiExceptionHandler.java     # 统一异常处理
├── application/
│   ├── UserAccountService.java      # 账号认证、密码校验、JWT 颁发
│   └── QrLoginService.java          # 二维码生命周期管理
├── domain/
│   └── QrStatus.java                # 二维码状态枚举
├── infrastructure/
│   ├── UserRepository.java          # MySQL 用户持久化
│   └── RedisQrStore.java            # Redis Lua 调用
└── security/
    ├── SecurityConfig.java          # Spring Security 配置
    ├── JwtService.java              # JWT 颁发与校验
    ├── JwtAuthenticationFilter.java # JWT 鉴权过滤器
    └── AuthCookies.java             # Cookie 管理
```

---

## 十、常见问题

**Q：二维码过期了怎么办？**  
A：PC 端轮询发现 `EXPIRED` 状态后，显示「二维码已过期，请刷新」，用户点击刷新按钮重新生成二维码。

**Q：手机端扫码后验证码不一致？**  
A：请确保手机和电脑显示的是同一个二维码。如果二维码被其他手机扫描，后续扫码会被拒绝（状态已变为 SCANNED）。

**Q：PC 端退出后，手机端还需要退出吗？**  
A：不需要。PC 退出只清除 PC JWT Cookie，手机端 JWT 仍然有效，可以继续扫描其他电脑。

**Q：真机扫码时摄像头打不开？**  
A：手机浏览器要求 HTTPS 安全上下文才能调用摄像头。请使用 `https://` 地址访问手机端，或在局域网内使用 `localhost`（仅限本机调试）。

**Q：如何防止二维码被伪造？**  
A：二维码 URL 中的 `ticket` 是 43 位强随机字符串，Redis 中存储 bindingHash，消费时校验 PC 浏览器的 binding Cookie。伪造的 ticket 无法通过 Redis 校验。

---

## 十一、图片验证码机制

账号密码登录（PC 与手机端）必须先通过图片验证码校验，后端校验失败不会触碰账号密码逻辑。

**获取**：`GET /api/auth/captcha` 返回 `{ captchaId, image }`，`image` 为 PNG 的 data URL。

**生成规则**（`CaptchaService`）：
- 4 位字符，取自去歧义字符表 `23456789ABCDEFGHJKMNPQRSTUVWXYZ`（不含 0/O、1/I/L）。
- Java AWT 绘制：随机旋转、5 色调色板、干扰线与噪点，输出 124x44 PNG。
- 答案以小写存入 Redis `auth:captcha:{id}`，TTL 3 分钟。

**校验规则**：
- 登录请求必须携带 `captchaId` + `captchaCode`，大小写不敏感。
- Lua 脚本 GET+DEL 原子消费：**无论对错，验证码只能尝试一次**。
- 错误或过期返回 400「验证码错误或已过期，请刷新后重试」。

**前端交互**：
- 验证码图片与输入框并排，点击图片即可刷新。
- 登录失败后自动获取新验证码并清空输入框（旧码已消费，必然不可重放）。

---

## 十二、登录限流与注册

**登录限流**（`LoginRateLimitFilter`，无新增依赖）：
- 作用于 `POST /api/auth/login` 与 `POST /api/mobile/login`，两个端点共享同一 IP 预算。
- Redis 固定窗口计数：Lua 原子 `INCR + EXPIRE`，键 `auth:ratelimit:login:{ip}`，默认 **每 IP 每分钟 5 次**（`app.rate-limit.login.*` 可调）。
- 超限返回 **429** + `Retry-After` 头 + `{"error":"请求过于频繁，请稍后再试"}`，在验证码与密码校验之前直接拒绝。
- 客户端 IP 取 `X-Forwarded-For` 首段（经 Vite/网关代理时），否则取 `remoteAddr`；生产网关必须正确设置该头。

**注册逻辑**（`POST /api/mobile/register`）：
- PC 与手机端登录框均提供「没有账号？注册新账号」切换到注册模式，表单含账号、密码、**确认密码**（两次一致才提交，纯前端校验）与**图片验证码**（后端校验，一次性消费，失败自动换图）。
- 用户名全局唯一：数据库 `uk_user_name` 唯一键兜底，重复注册返回 **409**，前端提示「账号名已被占用，请更换后重试」。
- 注册成功后自动切回登录模式并保留账号名，密码输入框切换 `autocomplete=new-password`，并自动刷新验证码供登录使用。

---

**文档版本**：v1.2  
**最后更新**：2026-10-11
