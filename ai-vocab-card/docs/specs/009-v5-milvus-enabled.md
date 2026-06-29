# 009 - V5 Milvus Enabled Specification

## 1. 背景

V5 原版本保留了 Milvus 适配边界，但默认仍可使用本地语义搜索降级实现。本次改造要求在 Docker Compose 中正式启用 Milvus，并提供可视化管理面板，便于开发、调试和面试演示。

## 2. 目标

- Docker Compose 启动 Milvus Standalone。
- 追加 Milvus 依赖组件 etcd。
- 复用项目已有 MinIO 作为 Milvus 对象存储。
- 追加 Attu 作为 Milvus 管理面板。
- 追加 `milvus-adapter`，向 Spring Boot 暴露稳定 REST 接口。
- Spring Boot 默认启用 `milvus-adapter` 语义搜索 provider。

## 3. 服务清单

| 服务 | 端口 | 说明 |
|---|---:|---|
| etcd | 内网 | Milvus 元数据存储 |
| minio | 9000 / 9001 | Milvus 对象存储 + 项目导出文件存储 |
| milvus | 19530 / 9091 | Milvus Standalone |
| attu | 3000 | Milvus Web 管理面板 |
| milvus-adapter | 19531 | 项目向量检索 REST 适配器 |

## 4. 配置

```yaml
VECTOR_PROVIDER: milvus-adapter
MILVUS_ENABLED: true
MILVUS_ENDPOINT: http://milvus-adapter:19531
MILVUS_COLLECTION: word_card_vectors
MILVUS_DIMENSION: 1024
```

## 5. 验收标准

- [ ] `docker compose -f env/docker-compose.yml up --build` 可以启动 Milvus 相关服务。
- [ ] `http://localhost:3000` 可以打开 Attu 面板。
- [ ] `http://localhost:19531/health` 返回 UP。
- [ ] 保存词卡后调用 `/vectors/upsert` 写入 Milvus。
- [ ] 语义搜索调用 `/vectors/search` 返回相似词卡列表。

## 6. 当前边界

`milvus-adapter` 当前使用确定性本地向量生成器，目的是保证项目无需额外 embedding 服务也能运行。生产环境建议替换为真实 embedding 模型，并确保写入向量和查询向量由同一个模型生成。
