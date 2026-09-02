# Local Infrastructure

此目录只提供基础设施，不包含任何 `build:`，API 与 Worker 直接在 IDEA 中运行。

## 启动全部环境

```bash
docker compose -f env/docker-compose.yml up -d
```

或者进入 `env` 目录：

```bash
docker compose up -d
```

## 端口与账号

| 服务 | 地址 | 账号 |
|---|---|---|
| Redis | `192.168.1.200:6379` | 无 |
| MySQL | `192.168.1.200:3306/sha256_demo` | `sha256 / sha256-demo` |
| MinIO S3 API | `http://192.168.1.200:9000` | `sha256minio / sha256-minio-secret` |
| MinIO Console | `http://192.168.1.200:9001` | `sha256minio / sha256-minio-secret` |
| RabbitMQ | `192.168.1.200:5672` | `sha256 / sha256-demo` |
| RabbitMQ UI | `http://192.168.1.200:15672` | `sha256 / sha256-demo` |
| Kafka | `192.168.1.200:9092` | - |

MinIO bucket 默认是 `sha256-files`。API/Worker 启动时会自动检查并创建。
MySQL 首次创建数据卷时会自动执行 `env/mysql/init/schema.sql`。

如果修改了初始化 SQL，但 MySQL 数据卷已经存在，需要显式删除数据卷后重新初始化；生产环境不要用这种方式升级数据库，应使用 Flyway/Liquibase。



## kafka文件夹
```
sudo chown -R 1000:1000 ./kafka_data
sudo chmod -R u+rwX ./kafka_data
```