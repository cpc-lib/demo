# 企业级 Redis 全局唯一 ID（时间戳 + 号段混合 / tenant+biz / 降级 / 压测）
Spring Boot 2.7.8 + Redis 单机 + Java 17

## 你要的 4 个能力
1) ✅ 时间戳 + 号段混合 ID（趋势有序）  
2) ✅ Redis 故障降级（先用应急预留段，再硬兜底本地 nodeId 模式）  
3) ✅ 支持 tenantId + bizKey（序列隔离 + ID 内编码）  
4) ✅ 压测脚本：JMH（同进程）+ 多进程去重验证（MultiProcessVerify）

---

## 1. ID 格式（64-bit long）
按位拼接：

```
[ timeBits ][ tenantBits ][ bizBits ][ seqBits ]  = 64 bits
time unit: seconds  (趋势有序)
seq: per-second sequence allocated by Redis segments (INCRBY)
```

默认位数（可配置，但总和必须=64）：
- timeBits=30（约 34 年，从 epochSeconds 开始）
- tenantBits=12（0~4095）
- bizBits=8（0~255）
- seqBits=14（0~16383，表示每秒每个 tenant+biz 最大 16384 个 ID）

> 如果单个 tenant+biz 的峰值超过 16384/s：请增大 seqBits 或对 bizKey 做分片（例如 order_0..order_9）。

---

## 2. 启动 Redis（单机）
```bash
docker compose up -d
```

---

## 3. 启动应用
```bash
mvn -q -DskipTests package
mvn -q spring-boot:run
```

---

## 4. HTTP 获取 ID（含 tenantId + bizKey）
- 单个：
  - `GET http://localhost:8080/api/id/next?tenantId=100&bizKey=order`
- 批量：
  - `GET http://localhost:8080/api/id/batch?tenantId=100&bizKey=order&n=10`

---

## 5. Redis 故障策略（金融级：宁可失败也不重复）
按优先级：
1) 正常：Redis 号段（SegmentBuffer 双 buffer + 异步预取）
2) Redis 异常：使用 **EmergencyReserve 应急预留段**（平时 Redis 健康时额外预取并缓存）
3) 仍不可用（reserve 耗尽）：**FAIL FAST**（抛出 IdGenUnavailableException，HTTP 返回 503），宁可失败也不生成任何潜在重复 ID

配置：

> 金融级默认开启 fail-fast：Redis 不可用且 reserve 耗尽时直接失败（HTTP 503）。

```yaml
idgen:
  emergency-segments: 2   # 每个 key 预留段数量
  node-id: 1              # 硬兜底时用于去重的节点ID（务必唯一）
```

---

## 6. 压测 1：JMH（推荐）
> 需要 Redis 在 127.0.0.1:6379

构建 JMH fat-jar：
```bash
mvn -Pjmh -DskipTests package
```

运行：
```bash
java -jar target/redis-idgen-enterprise-jmh.jar IdGenJmhBenchmark -wi 3 -i 5 -t 16
```

---

## 7. 压测 2：多进程去重验证（模拟多实例）
构建：
```bash
mvn -DskipTests package
```

运行（示例：4 个进程，每个生成 200000 个）：
```bash
java -cp target/redis-idgen-enterprise-1.0.0.jar com.example.idgen.tools.MultiProcessVerify 4 200000 order 100
```

输出：
- `dup=0` 表示多进程验证不重号

---

## 8. 生产建议（你上线要注意）
1) **biz-map 建议配置**：避免 hash 映射冲突（bizBits=8 时最多 256 个 bizId）。
2) **seqBits 容量**：单个 tenant+biz 每秒最大 = 2^seqBits。超过就会抛 `Sequence overflow`。
3) **Redis 单机**：学习/简单生产可用；关键业务建议 Sentinel/Cluster。
4) **不要修改 epochSeconds**：上线后改 epoch 会造成 ID 语义变化。

---

如需我把它再升级到：Sentinel/Cluster、租户级分片、热 key 保护、Prometheus 指标完善、Grafana 面板，我也可以直接在这个工程上继续加。
