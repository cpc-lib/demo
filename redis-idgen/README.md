# Redis 号段全局唯一 ID（单工程版）

## 技术栈
- Spring Boot 2.7.8
- Redis 单机
- Java 17

## 启动 Redis
```bash
docker compose up -d
```

## 启动应用
```bash
mvn spring-boot:run
```

## 获取 ID
```text
GET http://localhost:8080/id/order
GET http://localhost:8080/id/user
```
