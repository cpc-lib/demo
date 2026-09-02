# Spring Boot WebSocket + ActiveMQ Artemis 实时消息室

基于 **Spring Boot + WebSocket + STOMP + SockJS + ActiveMQ Artemis + Redis** 实现的实时消息室示例项目。

项目支持：

- 在线用户列表
- 用户进入 / 离开状态同步
- 群聊
- `/user` 单对单私聊
- 私聊发送方消息回显
- 同一用户多个浏览器 Tab 会话处理
- ActiveMQ Artemis STOMP Broker Relay
- Redis 在线用户状态存储
- 前端动态切换“所有人 / 指定用户”发送目标

当前版本已经移除旧接口：

```text
/app/chat.sendMessageTest
```

前端发送消息统一使用：

```text
/app/chat.sendMessage
```

---

## 1. 技术栈

| 技术 | 版本 / 用途 |
|---|---|
| Java | 8 |
| Spring Boot | 2.7.12 |
| Spring WebSocket | WebSocket / STOMP 消息处理 |
| SockJS | WebSocket 降级兼容 |
| STOMP.js | 浏览器 STOMP 客户端 |
| ActiveMQ Artemis | STOMP Broker Relay |
| Redis | 在线用户集合 |
| Maven | 项目构建 |
| JUnit 5 | 单元测试 |
| Mockito | Mock 测试 |

> 本项目中的 Artemis 主要作为 **STOMP Broker Relay** 使用，并不是通过 `JmsTemplate` 发送聊天室消息。

---

## 2. 核心功能

### 2.1 群聊

用户选择左侧的 **所有人** 后发送消息：

```text
浏览器
   │
   │ SEND /app/chat.sendMessage
   │ to = all
   ▼
WebsocketController
   │
   ▼
ChatService
   │
   │ convertAndSend("/topic/public", message)
   ▼
/topic/public
   │
   ├── lisi
   ├── zhangsan
   └── 其他在线用户
```

所有订阅 `/topic/public` 的用户都会收到消息，**发送人自己也会收到该群聊消息**。

---

### 2.2 单对单私聊

例如：`lisi` 给 `zhangsan` 发送消息。

```text
lisi
  │
  │ SEND /app/chat.sendMessage
  │ to = zhangsan
  ▼
WebsocketController
  │
  ├── ChatService
  │      │
  │      │ convertAndSendToUser(
  │      │   "zhangsan",
  │      │   "/queue/private",
  │      │   message
  │      │ )
  │      ▼
  │   zhangsan
  │   /user/queue/private
  │
  └── @SendToUser("/queue/private", broadcast=false)
         │
         ▼
      lisi 当前发送 Session
      /user/queue/private
```

因此一条私聊消息会产生两条明确的投递链路：

1. **接收方投递**：通过 `convertAndSendToUser()` 投递给目标用户。
2. **发送方回显**：通过 `@SendToUser(..., broadcast=false)` 回到当前发送消息的 WebSocket Session。

这样可以保证：

- `zhangsan` 能看到 `lisi` 发来的消息。
- `lisi` 自己也能看到刚刚发出的私聊消息。
- 私聊不会广播到 `/topic/public`。

---

## 3. WebSocket / STOMP 路由

### 3.1 WebSocket 连接端点

```text
/ws
```

Spring 配置：

```java
registry.addEndpoint("/ws")
        .setAllowedOriginPatterns("*")
        .withSockJS();
```

浏览器连接：

```javascript
var socket = new SockJS('/ws');
stompClient = Stomp.over(socket);

stompClient.connect({ userid: username }, onConnected, onError);
```

`userid` 非常重要，它用于建立 Spring WebSocket `Principal`。

---

### 3.2 客户端发送地址

所有用户消息统一发送到：

```text
/app/chat.sendMessage
```

对应后端：

```java
@MessageMapping("/chat.sendMessage")
public ChatMessage sendMessage(...) {
    ...
}
```

因为配置了：

```java
registry.setApplicationDestinationPrefixes("/app");
```

所以：

```text
@MessageMapping("/chat.sendMessage")
```

实际客户端地址就是：

```text
/app/chat.sendMessage
```

---

### 3.3 客户端订阅地址

| 功能 | 订阅地址 | 说明 |
|---|---|---|
| 群聊 | `/topic/public` | 接收所有群聊消息 |
| 私聊 | `/user/queue/private` | 接收当前用户私聊消息 |
| 在线用户 | `/topic/online-users` | 接收完整在线用户列表 |

前端连接成功后：

```javascript
stompClient.subscribe('/topic/public', onMessageReceived);
stompClient.subscribe('/user/queue/private', onMessageReceived);
stompClient.subscribe('/topic/online-users', onOnlineUsersReceived);
```

---

## 4. `/user` 私聊机制

Spring WebSocket 的用户消息路由不是简单把用户名拼进 Topic，而是依赖：

```text
Principal.getName()
```

项目在 STOMP `CONNECT` 阶段读取：

```text
userid
```

并转换为：

```java
new UserPrincipal(username)
```

核心代码：

```java
String userId = accessor.getFirstNativeHeader("userid");

if (StringUtils.hasText(userId)) {
    String username = userId.trim();
    accessor.setUser(new UserPrincipal(username));
}
```

之后 Spring 才能把：

```java
messagingTemplate.convertAndSendToUser(
        "zhangsan",
        "/queue/private",
        message
);
```

正确解析到 `zhangsan` 当前已注册的 WebSocket Session。

客户端不需要订阅：

```text
/user/zhangsan/queue/private
```

也不要自行拼用户名。

客户端始终订阅：

```text
/user/queue/private
```

Spring 会自动完成用户目的地转换。

---

## 5. 已修复的私聊问题

之前存在以下问题：

```text
lisi -> zhangsan
```

发送私聊后：

- `zhangsan` 看不到消息。
- `lisi` 自己也看不到已发送消息。

### 根因

旧认证拦截器错误地假设 STOMP 原生 Header 必须是：

```java
LinkedList
```

但 Spring 实际暴露的是：

```java
List<String>
```

底层可能是 `ArrayList` 等其他实现。

因此 `userid` 没有稳定转换成 WebSocket `Principal`，最终导致：

```text
convertAndSendToUser("zhangsan", ...)
```

无法从 Spring 用户会话注册表中定位 `zhangsan`。

### 修复方式

现在统一使用 Spring 提供的 API：

```java
accessor.getFirstNativeHeader("userid")
```

不再依赖具体集合实现。

同时私聊发送方回显通过：

```java
@SendToUser(
    destinations = "/queue/private",
    broadcast = false
)
```

直接返回当前发送消息的 WebSocket Session，不需要再按发送方用户名做第二次用户查找。

---

## 6. 消息数据结构

实体：

```java
public class ChatMessage {
    private MessageType type;
    private String content;
    private String sender;
    private String to;
}
```

类型：

```java
public enum MessageType {
    CHAT,
    JOIN,
    LEAVE
}
```

---

### 6.1 群聊消息

```json
{
  "type": "CHAT",
  "sender": "lisi",
  "content": "大家好",
  "to": "all"
}
```

前端虽然会传 `sender`，但后端不会直接信任这个值。

服务端最终使用：

```java
principal.getName()
```

覆盖 `sender`：

```java
chatMessage.setSender(principal.getName());
```

避免用户直接篡改前端 JSON 冒充其他用户。

---

### 6.2 私聊消息

例如 `lisi -> zhangsan`：

```json
{
  "type": "CHAT",
  "sender": "lisi",
  "content": "你好 zhangsan",
  "to": "zhangsan"
}
```

关键字段：

```text
to = zhangsan
```

只要 `to` 不是 `all`，就按私聊处理。

---

## 7. 在线用户机制

在线用户存储在 Redis Set：

```text
websocket.onlineUsers
```

配置：

```yaml
redis:
  set:
    onlineUsers: websocket.onlineUsers
```

用户连接成功后：

```text
SessionConnectedEvent
```

会执行：

```java
redisTemplate.opsForSet().add(onlineUsersKey, username);
```

用户断开连接后：

```text
SessionDisconnectEvent
```

会从 Redis Set 移除用户。

在线列表发生变化时，服务端广播：

```text
/topic/online-users
```

客户端收到后重新渲染左侧在线用户列表。

---

## 8. 多浏览器 Tab 处理

项目维护：

```java
ConcurrentMap<String, String> sessionUsers
```

结构为：

```text
sessionId -> username
```

例如同一个 `lisi` 打开两个 Tab：

```text
session-A -> lisi
session-B -> lisi
```

关闭 `session-A` 时，因为 `session-B` 仍然存在，所以不会立即把 `lisi` 从在线用户集合删除。

只有当前应用实例中最后一个 `lisi` Session 断开后，才会执行离线处理。

> 注意：当前 `sessionUsers` 是单 JVM 内存结构。如果后续将应用部署成多个 Spring Boot 实例，需要进一步把“用户 Session 数量 / Session 注册信息”放到 Redis 等共享存储中，否则跨实例多 Session 的在线状态还需要增强。

---

## 9. 前端功能

页面入口：

```text
http://localhost:8181/
```

进入页面后先输入用户名，例如：

```text
lisi
```

连接时会发送：

```javascript
stompClient.connect({ userid: username }, onConnected, onError);
```

聊天室页面左侧包含：

```text
在线用户
├── 所有人
├── lisi（我）
├── zhangsan
└── wangwu
```

### 群聊

点击：

```text
所有人
```

页面显示：

```text
当前发送给：所有人（群聊）
```

发送消息时：

```json
{
  "to": "all"
}
```

### 私聊

点击：

```text
zhangsan
```

页面显示：

```text
当前发送给：zhangsan（私聊）
```

发送消息时：

```json
{
  "to": "zhangsan"
}
```

如果目标用户离线，在线用户列表刷新后会自动恢复到：

```text
所有人
```

---

## 10. HTTP 接口

除 WebSocket 接口外，项目保留了几个辅助 HTTP 接口。

### 10.1 获取在线用户

```http
GET /getOnlineUsers
```

示例响应：

```json
[
  "lisi",
  "zhangsan"
]
```

前端第一次连接成功后会调用该接口获取在线用户快照。

---

### 10.2 服务端发送单用户消息

```http
GET /sendToOne?uid=zhangsan&content=hello
```

服务端构造：

```text
sender = 系统消息
```

并通过 `/user` 机制投递给指定用户。

该接口主要用于开发和测试，不建议直接作为正式生产环境的公开接口使用。

---

### 10.3 服务端群发

```http
GET /sendToAll?content=hello
```

消息会发送到：

```text
/topic/public
```

同样建议仅用于开发测试。

---

## 11. 项目目录

```text
springboot-websocket-activemq
├── pom.xml
├── docker-compose.yml
├── README.md
└── src
    ├── main
    │   ├── java
    │   │   └── cc.ivera
    │   │       ├── Application.java
    │   │       ├── config
    │   │       │   ├── UserPrincipal.java
    │   │       │   ├── WebSocketAuthInterceptor.java
    │   │       │   └── WebSocketConfig.java
    │   │       ├── controller
    │   │       │   └── WebsocketController.java
    │   │       ├── entity
    │   │       │   └── ChatMessage.java
    │   │       ├── service
    │   │       │   ├── ChatService.java
    │   │       │   └── WebSocketPresenceListener.java
    │   │       └── util
    │   │           ├── JsonUtil.java
    │   │           └── RedisUtils.java
    │   └── resources
    │       ├── application.yml
    │       └── static
    │           ├── css
    │           │   └── main.css
    │           ├── index.html
    │           └── js
    │               ├── main.js
    │               ├── sockjs.min.js
    │               └── stomp.min.js
    └── test
        └── java
            └── cc.ivera
                ├── config
                │   └── WebSocketAuthInterceptorTest.java
                ├── controller
                │   └── WebsocketControllerTest.java
                └── service
                    └── ChatServiceTest.java
```

---

## 12. 环境要求

本地建议安装：

```text
JDK 8+
Maven 3.8+
Redis
Docker / Docker Compose（推荐用于启动 Artemis）
```

查看 Java：

```bash
java -version
```

查看 Maven：

```bash
mvn -version
```

---

## 13. 启动 ActiveMQ Artemis

项目根目录提供：

```text
docker-compose.yml
```

启动：

```bash
docker compose up -d
```

查看容器：

```bash
docker compose ps
```

查看日志：

```bash
docker compose logs -f artemis
```

当前 Compose 映射：

| 端口 | 用途 |
|---|---|
| `61616` | Artemis Broker |
| `8161` | Artemis Web 管理端 |

默认账号：

```text
username: artemis
password: artemis
```

停止：

```bash
docker compose down
```

删除数据卷：

```bash
docker compose down -v
```

---

## 14. Redis 配置

当前 `application.yml` 中 Redis 是外部地址配置，例如：

```yaml
spring:
  redis:
    database: 0
    host: 192.168.1.200
    port: 6379
    password: "***"
```

请根据自己的环境修改：

```yaml
spring:
  redis:
    database: 0
    host: 127.0.0.1
    port: 6379
    password:
```

如果本地 Redis 没有密码：

```yaml
password:
```

即可。

---

## 15. Artemis Broker 配置

当前项目通过：

```yaml
broker:
  host: 192.168.1.200
  port: 61616
  username: artemis
  password: artemis
```

配置 STOMP Broker Relay。

如果 Artemis 运行在本机 Docker：

```yaml
broker:
  host: 127.0.0.1
  port: 61616
  username: artemis
  password: artemis
```

核心配置：

```java
registry.enableStompBrokerRelay("/queue", "/topic")
        .setRelayHost(brokerHost)
        .setRelayPort(brokerPort)
        .setClientLogin(brokerUser)
        .setClientPasscode(brokerPass)
        .setSystemLogin(brokerUser)
        .setSystemPasscode(brokerPass);
```

---

## 16. 启动项目

### 16.1 修改配置

先确认：

```text
Redis 可访问
Artemis 可访问
```

然后修改：

```text
src/main/resources/application.yml
```

---

### 16.2 执行测试

```bash
mvn clean test
```

---

### 16.3 启动 Spring Boot

```bash
mvn spring-boot:run
```

或者：

```bash
mvn clean package
java -jar target/springboot-websocket-activemq-0.0.1-SNAPSHOT.jar
```

默认端口：

```text
8181
```

访问：

```text
http://localhost:8181/
```

---

## 17. 推荐测试方式

建议使用两个独立浏览器窗口，或者普通窗口 + 无痕窗口。

### 窗口 A

用户名：

```text
lisi
```

### 窗口 B

用户名：

```text
zhangsan
```

---

### 17.1 验证在线用户

两个用户进入后，左侧应该看到：

```text
2 人在线

所有人
lisi（我） / lisi
zhangsan（我） / zhangsan
```

具体“（我）”标识取决于当前窗口登录用户。

---

### 17.2 验证群聊

`lisi` 选择：

```text
所有人
```

发送：

```text
hello everyone
```

预期：

```text
lisi      收到
zhangsan  收到
```

消息标签：

```text
群聊
```

---

### 17.3 验证 lisi -> zhangsan 私聊

`lisi` 点击左侧：

```text
zhangsan
```

发送：

```text
hello zhangsan
```

预期：

```text
lisi      能看到自己刚发出的消息
zhangsan  能收到该消息
其他用户   不应该收到
```

---

### 17.4 验证 zhangsan -> lisi 私聊

`zhangsan` 选择：

```text
lisi
```

发送：

```text
hello lisi
```

预期：

```text
zhangsan  能看到自己刚发出的消息
lisi      能收到该消息
其他用户   不应该收到
```

---

### 17.5 验证离线用户

关闭 `zhangsan` 的浏览器窗口。

预期 `lisi` 页面：

- 在线人数减少。
- `zhangsan` 从在线列表消失。
- 如果 `lisi` 当前选择的是 `zhangsan`，发送目标自动切回“所有人”。

---

## 18. 单元测试

当前包含：

### `WebSocketAuthInterceptorTest`

验证：

- `userid=lisi` 能绑定 `Principal`。
- 用户名前后空格会被清理。
- 没有 `userid` 时不会生成错误 Principal。

### `WebsocketControllerTest`

验证：

- 私聊发送者被服务端 `Principal` 覆盖。
- 私聊会返回消息用于发送方回显。
- 群聊不会额外走私聊回显。

### `ChatServiceTest`

验证：

- `to=all` 路由到 `/topic/public`。
- `to=username` 使用 `convertAndSendToUser()`。
- 私聊不会错误广播到公共 Topic。

执行：

```bash
mvn test
```

---

## 19. 为什么群聊和私聊统一使用一个发送接口

项目不再维护：

```text
/app/chat.sendMessage
/app/chat.sendMessageTest
/app/chat.sendToOne
...
```

多套 WebSocket 消息入口。

现在统一为：

```text
/app/chat.sendMessage
```

路由由：

```text
to
```

决定。

### 群聊

```text
to = all
```

### 私聊

```text
to = username
```

这样可以避免：

- 前端维护多套发送逻辑。
- 测试接口被错误用于正式功能。
- 群聊 / 私聊消息结构不一致。
- 后续增加消息持久化、审计、敏感词、已读回执时重复开发。

---

## 20. 安全说明

当前项目是聊天室 Demo / 基础工程，`userid` 来自浏览器 STOMP Header：

```javascript
stompClient.connect({ userid: username }, ...);
```

这只能解决 WebSocket `/user` 路由问题，**不等于正式身份认证**。

生产环境不要直接信任用户自己填写的用户名。

推荐升级为：

```text
登录
  │
  ▼
JWT / Session
  │
  ▼
WebSocket CONNECT
  │
  ▼
服务端校验 Token
  │
  ▼
从认证信息生成 Principal
  │
  ▼
/user 单聊
```

也就是说，生产环境的：

```text
Principal.getName()
```

应该来源于服务端可信认证结果，而不是前端任意填写的 `userid`。

---

## 21. 常见问题

### 21.1 lisi 发给 zhangsan，zhangsan 收不到

首先确认浏览器订阅的是：

```text
/user/queue/private
```

而不是：

```text
/queue/private
```

然后确认 STOMP CONNECT Header 中存在：

```text
userid: zhangsan
```

服务端日志应该出现类似：

```text
WebSocket user authenticated: username=zhangsan
```

如果没有该日志，说明 `Principal` 没有正确建立。

---

### 21.2 私聊接收方能收到，但发送方自己看不到

当前版本发送方回显依赖：

```java
@SendToUser(destinations = "/queue/private", broadcast = false)
```

并且前端必须订阅：

```text
/user/queue/private
```

不要在前端手工 `append` 一条“假发送成功”消息，否则服务端发送失败时 UI 仍然会错误显示已发送。

当前实现使用服务端确认消息进行回显。

---

### 21.3 WebSocket 可以连接，但 Topic 没消息

检查 Artemis 是否正常：

```bash
docker compose ps
```

检查日志：

```bash
docker compose logs -f artemis
```

同时确认：

```yaml
broker:
  host: 127.0.0.1
  port: 61616
```

是否与实际环境一致。

---

### 21.4 在线用户列表为空

确认 Redis 是否可访问。

检查：

```yaml
spring:
  redis:
    host: ...
    port: 6379
```

Redis 中对应 Key：

```text
websocket.onlineUsers
```

可通过 Redis CLI 查看：

```bash
SMEMBERS websocket.onlineUsers
```

---

### 21.5 修改代码后运行结果还是旧逻辑

建议重新清理构建：

```bash
mvn clean
mvn test
mvn spring-boot:run
```

不要直接复用旧 `target/` 目录中的历史 class 文件。

---

## 22. 后续可升级方向

如果作为正式 IM / 企业聊天基础，可以继续增加：

- JWT WebSocket 鉴权
- Spring Security
- 私聊消息持久化
- MySQL / MongoDB 聊天记录
- 未读消息数
- 已读 / 未读回执
- 消息 ID
- 消息发送时间
- 消息撤回
- 离线消息
- 消息重试
- ACK / 送达状态
- 群组 / 群成员管理
- Redis 分布式在线 Session
- 多实例 WebSocket 网关
- 用户踢下线
- 心跳 / 超时检测
- 敏感词过滤
- 文件 / 图片消息
- WebSocket 限流
- 消息审计
- 消息幂等
- OpenTelemetry / Prometheus 可观测性

---

## 23. 当前关键约定

开发和后续修改时建议保持以下约定不变：

```text
WebSocket Endpoint
/ws

Application Prefix
/app

统一发送接口
/app/chat.sendMessage

群聊订阅
/topic/public

私聊订阅
/user/queue/private

在线用户订阅
/topic/online-users

在线用户 HTTP 快照
GET /getOnlineUsers

群聊标识
to = all

私聊标识
to = <username>

STOMP 用户 Header
userid = <username>
```

最重要的约束是：

> **群聊和私聊都只能从 `/app/chat.sendMessage` 进入；私聊必须通过 Spring `/user` 用户目的地完成投递。**

