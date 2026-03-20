# Spring Boot 2.7 + Kafka 优雅关闭 Demo

## 1. 启动 Kafka（单节点）
```bash
docker run -d --name kafka   -p 9092:9092   -e KAFKA_BROKER_ID=1   -e KAFKA_ZOOKEEPER_CONNECT=host.docker.internal:2181   -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092   confluentinc/cp-kafka:7.5.0
```

## 2. 启动应用
```bash
mvn spring-boot:run
```

## 3. 发送消息
```bash
curl http://localhost:8080/send
```

## 4. 优雅关闭
```bash
kill -15 <pid>
```

观察日志：
- Kafka 停止拉取新消息
- 已消费消息处理完成后再退出
