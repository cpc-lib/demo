#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
COMPOSE_FILE="${COMPOSE_FILE:-${PROJECT_DIR}/docker-compose.dm8.yml}"
CONTAINER_NAME="${DM_CONTAINER_NAME:-payment-demo-dm8}"
WAIT_TIMEOUT_SECONDS="${DM_WAIT_TIMEOUT_SECONDS:-120}"

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

echo "准备启动 DM8。"
echo "项目目录：${PROJECT_DIR}"
echo "Compose 文件：${COMPOSE_FILE}"
echo "容器名称：${CONTAINER_NAME}"

cd "${PROJECT_DIR}"
"${DOCKER_COMPOSE[@]}" -f "${COMPOSE_FILE}" up -d

echo "等待容器进入 running 状态，超时时间：${WAIT_TIMEOUT_SECONDS}s"
start_ts="$(date +%s)"
while true; do
  status="$(docker inspect -f '{{.State.Status}}' "${CONTAINER_NAME}" 2>/dev/null || true)"
  if [[ "${status}" == "running" ]]; then
    break
  fi

  now_ts="$(date +%s)"
  if (( now_ts - start_ts >= WAIT_TIMEOUT_SECONDS )); then
    echo "错误：等待容器 running 超时。当前状态：${status:-unknown}" >&2
    docker logs --tail=120 "${CONTAINER_NAME}" 2>/dev/null || true
    exit 1
  fi

  sleep 2
done

echo "等待 dmserver 进程启动。"
while true; do
  if docker exec "${CONTAINER_NAME}" bash -lc "ps -ef | grep -v grep | grep dmserver >/dev/null" >/dev/null 2>&1; then
    break
  fi

  now_ts="$(date +%s)"
  if (( now_ts - start_ts >= WAIT_TIMEOUT_SECONDS )); then
    echo "错误：等待 dmserver 进程启动超时。" >&2
    docker logs --tail=160 "${CONTAINER_NAME}" 2>/dev/null || true
    exit 1
  fi

  sleep 2
done

echo "DM8 已启动。"
echo "查看日志：docker logs -f ${CONTAINER_NAME}"
echo "初始化 SQL：bash env/scripts/dm8/init-dm8-sql.sh"
