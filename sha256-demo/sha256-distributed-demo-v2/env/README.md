# Local infrastructure

This directory contains infrastructure only. There is no `build:` section for the Java applications.

```bash
docker compose -f env/docker-compose.yml up -d
```

Services:

- Redis: `6379`
- MySQL: `3306`
- MinIO S3 API: `9000`
- MinIO Console: `9001`
- RabbitMQ: `5672`
- RabbitMQ Management: `15672`
- Kafka: `9092`

## Multipart direct upload

v2 uploads file Parts directly from the browser to MinIO by using S3 Presigned URLs. The demo compose sets:

```yaml
MINIO_API_CORS_ALLOW_ORIGIN: "*"
```

so browser `PUT` requests can reach MinIO during local development. In production, replace `*` with the real frontend origin.

The API setting `sha256.storage.public-endpoint` must be reachable from the user's browser. Examples:

```text
Docker Desktop on the same computer: http://localhost:9000
Docker/MinIO in a VM:             http://192.168.x.x:9000
Kubernetes:                       https://s3-upload.example.com
```

`sha256.storage.endpoint` is used by API/Worker server-side access; `public-endpoint` is used only when generating browser-facing presigned URLs, so they may be different.


```text
sudo chown -R 1000:1000 ./kafka_data
sudo chmod -R u+rwX ./kafka_data
```