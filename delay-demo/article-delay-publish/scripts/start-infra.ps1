$ErrorActionPreference = "Stop"
docker compose up -d mysql redis kafka kafka-ui
docker compose ps
Write-Host "Kafka UI: http://localhost:8090"
