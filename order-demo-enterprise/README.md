# redis-leaf-double-buffer-demo

Leaf 风格“双 Buffer 号段”发号 Demo（Redis QPS 再降一个数量级）

## 1. 环境准备
- JDK 17
- Redis (默认 localhost:6379)
- MySQL (默认 localhost:3306/test)

建表：
```sql
CREATE TABLE t_order (
  id BIGINT PRIMARY KEY,
  order_no VARCHAR(64)
);
```

## 2. 配置
编辑 `src/main/resources/application.yml` 修改 MySQL/Redis 连接。

- `leaf.segment.step`: 号段大小（每次从 Redis 申请 step 个号）
- `leaf.segment.preload-ratio`: 使用到多少比例开始异步预取下一段

## 3. 运行
```bash
mvn spring-boot:run
```

启动后会并发插入一定量订单数据。Redis 只在“换号段/预取号段”时访问，QPS 很低。
