#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
printf '%s\n' 'WARNING: removing MySQL and PowerJob data volumes.'
docker compose down -v --remove-orphans
docker compose up -d --force-recreate
docker compose ps
