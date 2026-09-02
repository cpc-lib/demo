# springboot-websocket-simple

基于 Spring Boot + 原生 WebSocket（`javax.websocket`）的实时消息推送 Demo，实现了多客户端在线聊天广播。

## 技术栈

- Spring Boot 2.1.5.RELEASE
- Java 8
- spring-boot-starter-websocket（内嵌 Tomcat 9）

## 功能

- 客户端通过用户 ID 建立 WebSocket 连接
- 任意用户发送的消息会广播给所有在线用户（格式：`userId: 消息内容`）
- 实时在线人数统计，连接成功时返回欢迎消息
- 内置前端测试页面

## 项目结构

```
src/main/java/cc/ivera/
├── Application.java              # 启动类
├── config/
│   ├── WebSocketConfig.java      # 注册 ServerEndpointExporter
│   └── EndpointConfigure.java    # WebSocket端点配置（Spring单例桥接）
└── push/
    └── ProductWebSocket.java     # WebSocket端点（连接管理 + 消息广播）
src/main/resources/
├── application.yml               # 端口 8066
└── static/index.html             # 前端测试页面
```

## 快速开始

```bash
mvn spring-boot:run
```

启动后访问测试页面：http://localhost:8066/index.html

## 使用说明

1. 打开测试页面，输入用户 ID，点击「连接」
2. 连接成功后会收到欢迎消息，显示当前在线人数
3. 在消息输入框输入内容发送，所有在线用户（包括自己）都会收到广播
4. 可开多个浏览器标签、使用不同用户 ID 连接，体验群聊效果

## WebSocket 接口

| 项目 | 说明 |
| --- | --- |
| 地址 | `ws://localhost:8066/productWebSocket/{userId}` |
| 消息格式 | 文本（UTF-8） |
| 广播格式 | `userId: 消息内容` |
| 连接欢迎消息 | `userId连接成功--当前在线人数为：N` |

## 实现要点

- **端点实例为 Spring 单例**：`EndpointConfigure#getEndpointInstance` 返回 Spring 容器中的 Bean，因此所有连接共享同一个 `ProductWebSocket` 实例。连接状态（userId 等）必须绑定到各自的 `Session#getUserProperties`，而不能使用成员变量，否则会被连接间互相覆盖。
- **会话管理**：使用 `CopyOnWriteArraySet<Session>` 存放所有在线会话，线程安全。
- **消息广播**：`@OnMessage` 收到消息后调用静态方法 `sendInfo` 遍历所有会话群发。

## 常见问题

- **页面乱码**：页面已声明 `<meta charset="UTF-8">`，若仍乱码请强制刷新（Ctrl+F5）
- **连接失败**：确认连接地址为 `ws://localhost:8066/...`（后端固定端口 8066），与页面访问方式无关
