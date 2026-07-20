#!/bin/bash
# 数据库初始化脚本：等待达梦就绪后执行 SQL
set -e

DM_HOST="${DM_HOST:-dm8}"
DM_PORT="${DM_PORT:-5236}"
DM_USER="${DM_USER:-SYSDBA}"
DM_PWD="${DM_PWD:-Cpc2026#@Dm}"
SQL_FILE="${SQL_FILE:-/sql/payment_demo.sql}"

echo "[init-db] 等待达梦数据库 ${DM_HOST}:${DM_PORT} 就绪..."
max_wait=120
waited=0
until /opt/dmdbms/bin/disql "${DM_USER}/\"${DM_PWD}\"@${DM_HOST}:${DM_PORT}" -e "SELECT 1;" > /dev/null 2>&1; do
  waited=$((waited + 2))
  if [ ${waited} -ge ${max_wait} ]; then
    echo "[init-db] 等待达梦数据库超时（${max_wait}s），退出"
    exit 1
  fi
  echo "[init-db] 达梦尚未就绪，已等待 ${waited}s..."
  sleep 2
done

echo "[init-db] 达梦数据库已就绪，开始执行 ${SQL_FILE}..."
/opt/dmdbms/bin/disql "${DM_USER}/\"${DM_PWD}\"@${DM_HOST}:${DM_PORT}" "\`start ${SQL_FILE}"
echo "[init-db] SQL 执行完成"

echo "[init-db] 验证表是否创建成功..."
/opt/dmdbms/bin/disql "${DM_USER}/\"${DM_PWD}\"@${DM_HOST}:${DM_PORT}" -e "SET SCHEMA PAYMENT_DEMO; SELECT COUNT(*) AS product_count FROM t_product; SELECT COUNT(*) AS channel_count FROM t_payment_channel; SELECT COUNT(*) AS app_count FROM t_payment_app;"

echo "[init-db] 初始化完成"
