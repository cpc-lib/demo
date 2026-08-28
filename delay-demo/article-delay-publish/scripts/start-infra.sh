#!/usr/bin/env bash
set -euo pipefail
docker compose up -d mysql redis kafka kafka-ui
docker compose ps
echo "Kafka UI: http://localhost:8090"
