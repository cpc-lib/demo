#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
docker compose -f env/docker-compose.yml up -d redis mysql minio kafka
