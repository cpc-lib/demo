# Java 邮件发送示例工程（重构版）

这是一个 **纯 Java + Maven** 的 SMTP 邮件发送示例，已经按职责拆分完成重构，结构更清晰，支持：

- To 收件人
- Cc 抄送
- Bcc 密送
- 文本正文 / HTML 正文
- 本地文件附件
- 远程 URL 附件

## 1. 环境要求

- JDK 17+
- Maven 3.9+

依赖使用：

- Jakarta Mail API
- Eclipse Angus Mail Runtime
- Jakarta Activation API
- Eclipse Angus Activation Runtime

## 2. 项目结构

```text
mail-sender-demo
├── pom.xml
├── README.md
└── src
    └── main
        └── java
            └── cc
                └── ivera
                    └── mail
                        ├── config
                        │   └── MailProperties.java
                        ├── demo
                        │   └── MailDemo.java
                        ├── model
                        │   ├── AttachmentSource.java
                        │   ├── MailMessage.java
                        │   └── ResolvedAttachment.java
                        ├── service
                        │   ├── MailSender.java
                        │   └── SmtpMailSender.java
                        ├── support
                        │   ├── attachment
                        │   │   ├── AttachmentResolver.java
                        │   │   └── DefaultAttachmentResolver.java
                        │   ├── datasource
                        │   │   └── ByteArrayMailDataSource.java
                        │   └── session
                        │       └── SmtpSessionFactory.java
                        └── validation
                            └── MailMessageValidator.java
```

## 3. 分层说明

### config
存放 SMTP 基础配置，例如服务器地址、端口、认证方式、发件人、超时参数。

### model
存放领域对象：邮件消息、附件来源、已解析附件。

### service
只关心“发送邮件”这件事。

### support
存放底层支撑组件：

- 附件解析
- SMTP Session 创建
- DataSource 封装

### validation
集中处理发送前校验。

## 4. 使用示例

### 4.1 SMTP 配置

```java
MailProperties properties = new MailProperties()
        .setHost("smtp.example.com")
        .setPort(587)
        .setFromAddress("no-reply@example.com")
        .setFromName("订单中心")
        .setUsername("no-reply@example.com")
        .setPassword("your-smtp-password")
        .setAuth(true)
        .setStartTlsEnabled(true)
        .setSslEnabled(false)
        .setDebug(true);
```

### 4.2 组装邮件

```java
MailMessage message = new MailMessage()
        .subject("测试邮件")
        .content("<h3>您好，这是一封测试邮件</h3>")
        .html(true)
        .addTo("user1@test.com")
        .addCc("leader@test.com")
        .addBcc("audit@test.com")
        .addAttachment(AttachmentSource.fromLocalFile("D:/tmp/report.xlsx"))
        .addAttachment(AttachmentSource.fromRemoteUrl("https://example.com/files/manual.pdf"));
```

### 4.3 发送邮件

```java
MailSender mailSender = new SmtpMailSender(properties);
mailSender.send(message);
```

## 5. 运行方式

修改 `cc.ivera.mail.demo.MailDemo` 中的 SMTP 配置后执行：

```bash
mvn clean package
java -jar target/mail-sender-demo-1.0.1.jar
```

## 6. 这次重构做了什么

1. 将所有类从单包拆分为按职责分层的包结构。
2. 将 `MailAccount` 重命名为更直观的 `MailProperties`。
3. 将 `MailRequest` 重命名为更语义化的 `MailMessage`。
4. 将附件处理抽象为接口 `AttachmentResolver`，默认实现为 `DefaultAttachmentResolver`。
5. 新增 `SmtpSessionFactory`，把 Session 创建逻辑单独拆出。
6. 新增 `MailSender` 接口，方便后续扩展为异步发送器、队列发送器。
7. 删除示例中的真实账号信息，改成占位配置，避免泄露敏感信息。
8. 不再包含 `target` 编译产物，ZIP 内容更干净。

## 7. 生产建议

当前版本适合直接做公共组件基础版。如果你要上生产，建议继续补：

- 异步发送
- 重试机制
- 失败日志持久化
- 大附件流式发送
- 附件大小限制
- 统一异常封装
