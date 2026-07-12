#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
docker compose up -d --force-recreate
docker compose ps
docker compose logs --tail=120 powerjob-server
