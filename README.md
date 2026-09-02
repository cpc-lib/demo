# demo

个人技术学习与实践仓库，包含 **100+ 个相互独立的技术演示项目**，涵盖 Java 基础与并发、Spring 生态、微服务与分布式、消息队列、缓存、大数据、数据库与搜索、AI 应用等方向。

## 特点

- 每个子目录是一个**独立的 Maven 工程**（部分为前后端分离项目），可单独导入 IDE 运行
- 各 Demo 聚焦单一技术点，方便按需查阅与验证
- 涉及中间件的模块大多附带 `docker-compose.yml` 或说明文档，便于一键搭建环境

## 目录导航

### Java 基础与并发

| 模块 | 说明 |
| --- | --- |
| [aba-demo](aba-demo) | CAS 的 ABA 问题演示及 `AtomicStampedReference` 解决方案 |
| [deadlock-demo](deadlock-demo) | 死锁复现与分析 |
| [leetcode](leetcode) | 算法练习（含图片压缩、SQL 脚本等） |
| [java-folder-demo](java-folder-demo) | 自定义线程池并发复制/移动目录 |
| [base-java-folder-demo](base-java-folder-demo) | 单线程目录复制/移动工具 |
| [go-folder-demo](go-folder-demo) | Go 语言版目录工具 |
| [python-folder-demo](python-folder-demo) | Python 版目录工具 |
| [shell-demo](shell-demo) | 常用 Shell 脚本集合（部署、分发、解压等） |

### 加解密与安全

| 模块 | 说明 |
| --- | --- |
| [aes-demo](aes-demo) | AES 加解密 |
| [sha256-demo](sha256-demo) | SHA256 摘要计算 |
| [springboot-data-security](springboot-data-security) | Spring Boot 数据安全实践 |
| [springboot-springsecurity](springboot-springsecurity) | Spring Security 整合 |
| [springboot-token-demo](springboot-token-demo) | Token 认证 |

### Spring / Spring MVC

| 模块 | 说明 |
| --- | --- |
| [spring-annotation](spring-annotation) | Spring 注解驱动 |
| [spring-aop](spring-aop) | AOP 切面编程 |
| [spring-exception](spring-exception) | 统一异常处理 |
| [spring-xml](spring-xml) | XML 配置方式 |
| [spring-mybatis](spring-mybatis) | Spring 整合 MyBatis |
| [spring-ssm](spring-ssm) | SSM 整合 |
| [springmvc-interceptor](springmvc-interceptor) | MVC 拦截器 |

### MyBatis 系列

| 模块 | 说明 |
| --- | --- |
| [mybatis-one](mybatis-one) ~ [mybatis-four](mybatis-four) | MyBatis 渐进式学习 |
| [springboot-mybatis-demo](springboot-mybatis-demo) | Spring Boot 整合 MyBatis |
| [springboot-mybatis-plus](springboot-mybatis-plus) | MyBatis-Plus |
| [springboot-mybatis-lock](springboot-mybatis-lock) | MyBatis 乐观锁/悲观锁 |
| [springboot-tk-mybatis](springboot-tk-mybatis) | 通用 Mapper |
| [mybatis-mate-encrypt](mybatis-mate-encrypt) | 字段加解密 |

### 消息队列

| 模块 | 说明 |
| --- | --- |
| [kafka-demo](kafka-demo) | Kafka 基础用法 |
| [springboot-kafka](springboot-kafka) | Spring Boot 整合 Kafka |
| [kafka-graceful-demo](kafka-graceful-demo) | Kafka 优雅停机 |
| [kafka-saga-demo](kafka-saga-demo) | 基于 Kafka 的 Saga 分布式事务 |
| [rocketmq-demo](rocketmq-demo) | RocketMQ 基础用法 |
| [springboot-rocketmq](springboot-rocketmq) | Spring Boot 整合 RocketMQ |
| [rabbitmq-demo](rabbitmq-demo) | RabbitMQ（含 confirm 确认机制） |
| [delay-demo](delay-demo) | 延迟队列 / 死信队列（DLQ） |
| [spring-cloud-stream](spring-cloud-stream) | Spring Cloud Stream |

### 缓存

| 模块 | 说明 |
| --- | --- |
| [springboot-redis](springboot-redis) / [springboot-redis-demo](springboot-redis-demo) | Redis 基础整合 |
| [springboot-redis-geo](springboot-redis-geo) | Redis GEO 地理位置 |
| [springboot-redis-limiting](springboot-redis-limiting) | Redis 限流 |
| [springboot-redisson](springboot-redisson) | Redisson 分布式服务 |
| [springboot-jetcache](springboot-jetcache) / [springboot-j2cache](springboot-j2cache) | 多级缓存框架 |
| [multi-redis](multi-redis) | 多 Redis 数据源（含 Lettuce） |
| [cache-defense-demo](cache-defense-demo) | 缓存穿透/击穿/雪崩防护 |
| [global-cache-demo](global-cache-demo) | 全局缓存 |
| [springboot-cache](springboot-cache) | Spring Cache 注解缓存 |
| [queue-tokenbucket](queue-tokenbucket) / [tokenbucket](tokenbucket) | 令牌桶限流 |

### 微服务与分布式

| 模块 | 说明 |
| --- | --- |
| [spring-cloud-eureka](spring-cloud-eureka) | 服务注册发现 |
| [spring-cloud-gateway](spring-cloud-gateway) | 网关 |
| [spring-cloud-nacos-config](spring-cloud-nacos-config) | Nacos 配置中心 |
| [spring-cloud-restTemplate](spring-cloud-restTemplate) | 服务间调用 |
| [spring-cloud-seata-demo](spring-cloud-seata-demo) / [springboot-seata](springboot-seata) | Seata 分布式事务 |
| [dubbo-demo](dubbo-demo) / [spring-cloud-dubbo-demo](spring-cloud-dubbo-demo) | Dubbo RPC（api/provider/consumer） |
| [springboot-dubbo-parent](springboot-dubbo-parent) | Spring Boot 整合 Dubbo |
| [springboot-rpc](springboot-rpc) | 手写 RPC |
| [sentinel-demo](sentinel-demo) / [springboot-sentinel](springboot-sentinel) | Sentinel 流控 |
| [distributed-lock-demo](distributed-lock-demo) | 分布式锁 |
| [idgen-demo](idgen-demo) | 分布式 ID 生成（Redis） |
| [ip-spring-boot-starter](ip-spring-boot-starter) | 自定义 Spring Boot Starter |
| [order-state-machine](order-state-machine) | 订单状态机 |

### 大数据

| 模块 | 说明 |
| --- | --- |
| [hdfs-client](hdfs-client) | HDFS 客户端操作 |
| [mapreduce-demo](mapreduce-demo) | MapReduce 词频统计 |
| [yarn-demo](yarn-demo) | Yarn 提交与常用命令 |
| [flume-demo](flume-demo) | Flume 日志采集 |
| [hive-demo](hive-demo) | Hive 操作 |
| [hbase-demo](hbase-demo) | HBase（附 docker-compose） |
| [flink-demo](flink-demo) | Flink 批处理 |
| [flinkcdc-demo](flinkcdc-demo) | Flink CDC 数据同步 |
| [spark-demo](spark-demo) | Spark 计算（含测试数据） |
| [clickhouse-demo](clickhouse-demo) | ClickHouse |
| [canal-demo](canal-demo) | Canal 监听 binlog |
| [zookeeper-demo](zookeeper-demo) / [curator-zk](curator-zk) | ZooKeeper / Curator |

### 数据库与搜索

| 模块 | 说明 |
| --- | --- |
| [jdbc-demo](jdbc-demo) | JDBC 基础 |
| [springboot-mysql-geo](springboot-mysql-geo) | MySQL 地理位置查询 |
| [springboot-transaction](springboot-transaction) | 事务管理 |
| [mongo-demo](mongo-demo) / [springboot-mongodb](springboot-mongodb) | MongoDB（附副本集环境） |
| [springboot-neo4j](springboot-neo4j) | Neo4j 图数据库 |
| [elasticsearch-demo](elasticsearch-demo) | Elasticsearch 基础 |
| [springboot-es](springboot-es) | ES 酒店搜索实战（hotel-demo 等） |
| [springboot-easy-es](springboot-easy-es) | Easy-ES ORM |

### 网络通信与存储

| 模块 | 说明 |
| --- | --- |
| [netty-demo](netty-demo) | Netty 网络编程 |
| [mqtt-demo](mqtt-demo) | MQTT 协议 |
| [iot-demo](iot-demo) | IoT 平台（前后端 + Docker 部署） |
| [springboot-grpc](springboot-grpc) | gRPC 通信 |
| [ftp-demo](ftp-demo) | FTP 上传下载 |
| [springboot-minio](springboot-minio) | MinIO 对象存储 |
| [springboot-fastdfs](springboot-fastdfs) | FastDFS 文件存储 |
| [springboot-proxy](springboot-proxy) | 代理模式实践 |

### AI 应用

| 模块 | 说明 |
| --- | --- |
| [ai-vocab-card](ai-vocab-card) | AI 单词卡片（前后端 + Docker 部署） |
| [rag-demo](rag-demo) | RAG 检索增强（langchain4j） |
| [springboot-face](springboot-face) | ArcFace 人脸识别 |

### 文档处理与办公

| 模块 | 说明 |
| --- | --- |
| [easyexcel-demo](easyexcel-demo) / [springboot-easyexcel](springboot-easyexcel) | EasyExcel 导入导出 |
| [poi-tl-demo](poi-tl-demo) | poi-tl Word 模板渲染 |
| [openpdf-renderer-demo](openpdf-renderer-demo) | PDF 渲染 |
| [mail-sender-demo](mail-sender-demo) / [springboot-email](springboot-email) | 邮件发送 |
| [wx-push](wx-push) | 微信推送 |
| [amp-demo](amp-demo) | 图片元数据读取等工具集 |

### 业务建模与其他

| 模块 | 说明 |
| --- | --- |
| [order-demo](order-demo) / [order-demo-enterprise](order-demo-enterprise) | 订单业务（基础版/企业级版） |
| [pi-contract-demo](pi-contract-demo) | 合同账期与金额计算 |
| [tree-demo](tree-demo) | 树形结构演进（v0 ~ v2） |
| [job-demo](job-demo) | Spring Boot 定时任务 |
| [activiti-mysql](activiti-mysql) / [activiti-spring](activiti-spring) / [springboot-activiti-web](springboot-activiti-web) | Activiti 工作流 |
| [springboot-designmode](springboot-designmode) | 设计模式实践 |
| [springboot-validation](springboot-validation) | 参数校验 |
| [springboot-zipkin-elk](springboot-zipkin-elk) | 链路追踪与日志（附 zipkin-server） |
| [user-demo](user-demo) | 用户模块（MySQL/HBase 双存储） |
| [hikvision-demo](hikvision-demo) | 海康威视 SDK（含 win/linux 依赖库） |

## 使用方式

每个子项目均为独立的 Maven 工程，进入对应模块目录后执行：

```bash
mvn clean package
```

- 普通工程：按各模块 README 运行 `main` 方法或 `java -jar`
- Web 工程：启动后按 README 中的接口说明访问
- 依赖中间件的模块：优先查看模块内 `env/docker-compose.yml` 或 README 搭建环境

## 参与贡献

1. Fork 本仓库
2. 新建 `Feat_xxx` 分支
3. 提交代码
4. 新建 Pull Request

## 许可证

[MIT License](LICENSE)
