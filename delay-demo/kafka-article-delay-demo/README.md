# kafka-article-delay-demo

基于 **Spring Boot + Kafka 延时队列** 实现的文章定时发布示例（Java + MyBatis-Plus）。

## 1. 功能说明

- 文章数据存 MySQL（表：`article`）
- 基于 **Kafka 多级延时 Topic** 实现定时发布：
  - `article-delay-5m`
  - `article-delay-1m`
  - `article-delay-30s`
  - `article-delay-5s`
  - `article-delay-final`（真正执行发布）
- 创建文章时，根据发布时间 `publishTime` 计算时间差，推入合适的延时 Topic
- 消费者不断判断是否到期：
  - 未到期 → 路由到更合适的延时 Topic
  - 已到期 → 推入 `article-delay-final` → 更新 MySQL 状态为已发布

## 2. 快速启动

### 2.1 启动 Kafka（本地 Docker）

```bash
docker-compose up -d
```

默认暴露端口：`localhost:9092`

### 2.2 初始化 MySQL 数据库

执行 `sql/article.sql`：

```sql
CREATE DATABASE IF NOT EXISTS article_db;
USE article_db;
CREATE TABLE article (...);
```

### 2.3 修改数据库配置

在 `src/main/resources/application.yml` 中调整：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/article_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: 你的用户名
    password: 你的密码
```

### 2.4 启动 Spring Boot

```bash
mvn spring-boot:run
```

## 3. 接口示例

### 3.1 创建定时发布文章

- 方法：`POST`
- URL：`http://localhost:8080/articles`
- Body(JSON) 示例：

```json
{
  "title": "Kafka 延时队列测试",
  "content": "内容内容内容...",
  "publishTime": "2025-12-08 15:30:00"
}
```

### 3.2 查询文章列表

```bash
GET http://localhost:8080/articles
```

### 3.3 查询单个文章

```bash
GET http://localhost:8080/articles/{id}
```

## 4. 说明

- 主要逻辑：
  - `ArticleDelayService`：负责创建文章并发送延时消息
  - `ArticleDelayConsumer`：负责在各个延时 Topic 之间“跳转”消息
  - `ArticlePublishConsumer`：监听 `article-delay-final`，真正执行发布（更新 MySQL）
- 该方案是利用 **多级 Topic + 不断重投** 的方式模拟延时队列，适合对延时精度要求在秒级的业务场景。

你可以基于本项目扩展：
- 取消定时发布 / 修改发布时间
- 增加失败重试 / 死信队列
- 接入 Swagger / Knife4j、认证鉴权等
