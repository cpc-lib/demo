# Sentinel + Nacos + OpenFeign Demo（JDK17）

这是一个可运行的 Spring Cloud Alibaba Demo：
- provider-service：注册到 Nacos，提供接口
- consumer-service：注册到 Nacos；通过 **OpenFeign** 按服务名调用 provider；集成 **Sentinel**；并从 **Nacos 配置中心**加载 Sentinel 动态规则（flow / degrade）

## 0. 环境要求
- JDK 17
- Maven 3.8+
- Docker & Docker Compose

## 1. 启动 Nacos + Sentinel Dashboard
在项目根目录执行：

```bash
docker compose up -d
```

控制台地址：
- Nacos: http://localhost:8848/nacos  （默认账号密码：nacos/nacos）
- Sentinel: http://localhost:8858  （默认账号密码：sentinel/sentinel，若镜像不同可直接尝试无登录或查看容器日志）

> 注意：Sentinel 控制台需要看到“有流量”的资源才会显示在【簇点链路】里。

## 2. 启动 provider / consumer
打开两个终端，在项目根目录执行：

```bash
mvn -q -DskipTests spring-boot:run -pl provider-service
```

```bash
mvn -q -DskipTests spring-boot:run -pl consumer-service
```

端口：
- provider: 8081
- consumer: 8082

## 3. 验证 Nacos 注册
进入 Nacos 控制台 -> 服务管理 -> 服务列表，能看到：
- provider-service
- consumer-service

## 4. 访问接口制造流量（让 Sentinel 控制台出现资源）
访问：
- http://localhost:8082/api/consumer/callHello?name=he
- http://localhost:8082/api/consumer/callSlow?ms=800
- http://localhost:8082/api/consumer/callError

然后打开 Sentinel 控制台 http://localhost:8858 ，在【簇点链路】应看到资源：
- consumer_callHello
- consumer_callSlow
- consumer_callError

## 5. 在 Nacos 创建 Sentinel 动态规则（关键）
进入 Nacos 控制台 -> 配置管理 -> 配置列表 -> 新建配置

### 5.1 流控规则（flow）
- Data ID：consumer-service-flow-rules
- Group：DEFAULT_GROUP
- 格式：JSON
- 内容（示例：对 consumer_callHello 做 QPS=1 限流）

```json
[
  {
    "resource": "consumer_callHello",
    "limitApp": "default",
    "grade": 1,
    "count": 1,
    "strategy": 0,
    "controlBehavior": 0,
    "clusterMode": false
  }
]
```

保存后，快速多次刷新：
- http://localhost:8082/api/consumer/callHello?name=he
将会命中 blockHandler，返回 `blocked by sentinel ...`

### 5.2 降级规则（degrade）
- Data ID：consumer-service-degrade-rules
- Group：DEFAULT_GROUP
- 格式：JSON
- 内容（示例：异常比例熔断：30%，熔断 10 秒；统计窗口 10s；最小请求 5）

```json
[
  {
    "resource": "consumer_callError",
    "grade": 1,
    "count": 0.3,
    "timeWindow": 10,
    "minRequestAmount": 5,
    "statIntervalMs": 10000
  }
]
```

反复访问：
- http://localhost:8082/api/consumer/callError
当异常比例达到阈值后会触发熔断，后续快速失败（走 blockHandler / fallback）。

## 6. 你可以改规则验证“动态生效”
在 Nacos 修改 flow 规则里的 `count`（例如 1 -> 5），保存后无需重启 consumer，规则会自动刷新生效。

---
如需扩展：热点参数限流（param-flow）、网关层 Sentinel、规则持久化（Dashboard 推送到 Nacos），我也可以继续给你补齐。
