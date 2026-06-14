#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
COMPOSE_FILE="${COMPOSE_FILE:-${PROJECT_DIR}/docker-compose.dm8.yml}"
CONTAINER_NAME="${DM_CONTAINER_NAME:-payment-demo-dm8}"
REMOVE_VOLUME="${REMOVE_VOLUME:-false}"

if [[ ! -f "${COMPOSE_FILE}" ]]; then
  echo "错误：未找到 ${COMPOSE_FILE}。" >&2
  echo "请确认脚本位于项目目录 scripts/dm8 下，或通过 COMPOSE_FILE 指定 compose 文件。" >&2
  exit 1
fi

if docker compose version >/dev/null 2>&1; then
  DOCKER_COMPOSE=(docker compose)
elif command -v docker-compose >/dev/null 2>&1; then
  DOCKER_COMPOSE=(docker-compose)
else
  echo "错误：未找到 docker compose 或 docker-compose。" >&2
  exit 1
fi

echo "准备停止 DM8。"
echo "项目目录：${PROJECT_DIR}"
echo "Compose 文件：${COMPOSE_FILE}"
echo "容器名称：${CONTAINER_NAME}"

cd "${PROJECT_DIR}"

if [[ "${REMOVE_VOLUME}" == "true" ]]; then
  echo "警告：REMOVE_VOLUME=true，将删除 dm8-data 数据卷，数据库数据会被清空。"
  "${DOCKER_COMPOSE[@]}" -f "${COMPOSE_FILE}" down -v
else
  "${DOCKER_COMPOSE[@]}" -f "${COMPOSE_FILE}" down
fi

echo "DM8 已停止。"
if [[ "${REMOVE_VOLUME}" != "true" ]]; then
  echo "数据卷已保留。若确实需要清库重建，请执行：REMOVE_VOLUME=true bash env/scripts/dm8/stop-dm8.sh"
fi
