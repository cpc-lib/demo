# rabbitmq-article-delay-demo

基于 **Spring Boot + RabbitMQ 延时队列（TTL + 死信队列）** 实现文章定时发布的示例项目。

## 1. 实现思路

- 使用 RabbitMQ 普通队列 + TTL + 死信交换机（DLX）实现延时队列：
  - `article.delay.queue`：消息进入时设置 TTL（延迟时间）
  - TTL 到期后，消息作为死信转发到：
    - 交换机：`article.publish.exchange`
    - 队列：`article.publish.queue`
- 监听 `article.publish.queue`，真正执行 MySQL 文章发布（status 从 0 -> 1）

## 2. 依赖环境

- JDK 8+
- Maven 3.6+
- MySQL 5.7/8.0
- RabbitMQ 3.x

可以使用项目自带的 `docker-compose.yml` 快速启动 RabbitMQ（含管理界面）：

```bash
docker-compose up -d
```

访问管理控制台：http://localhost:15672 （默认账号密码：guest/guest）

## 3. 初始化数据库

执行 `sql/article.sql`：

```sql
CREATE DATABASE IF NOT EXISTS article_db;
USE article_db;
CREATE TABLE article (...);
```

## 4. 配置说明

修改 `src/main/resources/application.yml` 中的数据源配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/article_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: 你的用户名
    password: 你的密码
```

RabbitMQ 默认连接 `localhost:5672`，如需调整，也在此文件中修改。

## 5. 启动项目

```bash
mvn spring-boot:run
```

## 6. 接口示例

### 6.1 创建定时发布文章

- 方法：`POST`
- URL：`http://localhost:8080/articles`
- Body(JSON) 示例：

```json
{
  "title": "RabbitMQ 延时发布测试",
  "content": "内容内容内容...",
  "publishTime": "2025-12-08 15:30:00"
}
```

服务会：

1. 保存文章为草稿（status = 0）
2. 计算 `publishTime` 与当前时间的毫秒差
3. 将文章 ID 作为消息投递到 `article.delay.queue`，并设置 TTL = 延时时间
4. TTL 到期后，消息作为死信进入 `article.publish.queue`
5. 消费者监听 `article.publish.queue`，调用 `ArticleService.publishArticle` 更新为已发布（status = 1）

### 6.2 查询文章列表

```bash
GET http://localhost:8080/articles
```

### 6.3 查询单个文章

```bash
GET http://localhost:8080/articles/{id}
```

## 7. 关键类说明

- `RabbitMqConfig`：声明交换机、队列及其绑定关系，配置死信参数
- `ArticleDelayService`：创建文章并将其投递到延时队列，设置 TTL
- `ArticlePublishConsumer`：监听最终发布队列，执行 MySQL 状态更新
- `ArticleService`：封装文章的查询与发布逻辑
- `ArticleController`：提供 REST 接口，方便调试和集成前端

你可以在此项目基础上扩展：
- 支持修改发布时间（重新发送延时消息）
- 支持取消定时任务（删除旧消息或增加标记判断）
- 增加失败重试 / 死信队列收集器
- 接入 Swagger/Knife4j 文档、统一返回体、鉴权等
