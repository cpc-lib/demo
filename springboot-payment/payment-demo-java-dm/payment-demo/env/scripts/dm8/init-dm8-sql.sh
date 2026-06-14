#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"

CONTAINER_NAME="${DM_CONTAINER_NAME:-payment-demo-dm8}"
DM_USER="${DM_USERNAME:-SYSDBA}"
DM_PASSWORD="${DM_PASSWORD:-Cpc2026#@Dm}"
SQL_DIR="${DM_SQL_DIR:-/dm8-init}"
WAIT_TIMEOUT_SECONDS="${DM_WAIT_TIMEOUT_SECONDS:-120}"

if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
  echo "错误：容器未运行：${CONTAINER_NAME}" >&2
  echo "请先执行：bash env/scripts/dm8/start-dm8.sh" >&2
  exit 1
fi

echo "准备执行 DM8 初始化 SQL。"
echo "项目目录：${PROJECT_DIR}"
echo "容器：${CONTAINER_NAME}"
echo "用户：${DM_USER}"
echo "SQL 目录：${SQL_DIR}"

docker exec \
  -e DM_USER="${DM_USER}" \
  -e DM_PASSWORD="${DM_PASSWORD}" \
  -e SQL_DIR="${SQL_DIR}" \
  -e WAIT_TIMEOUT_SECONDS="${WAIT_TIMEOUT_SECONDS}" \
  -i "${CONTAINER_NAME}" bash -s <<'EOSH'
set -euo pipefail

resolve_disql() {
  local candidate=""

  if command -v disql >/dev/null 2>&1; then
    command -v disql
    return 0
  fi

  for candidate in \
    /home/dmdba/dmdbms/bin/disql \
    /opt/dmdbms/bin/disql \
    /dm8/bin/disql \
    /dmdbms/bin/disql \
    /opt/dm8/bin/disql
  do
    if [[ -f "${candidate}" ]]; then
      echo "${candidate}"
      return 0
    fi
  done

  find /home /opt /dm8 /dmdbms -maxdepth 6 -type f -name 'disql' 2>/dev/null | head -n 1 || true
}

DISQL_BIN="$(resolve_disql)"
if [[ -z "${DISQL_BIN}" ]]; then
  echo "错误：未找到 disql。" >&2
  echo "可进入容器后手动排查：find / -name 'disql*' 2>/dev/null" >&2
  exit 1
fi

DM_HOME="$(cd "$(dirname "${DISQL_BIN}")/.." && pwd)"
export DM_HOME
export PATH="${DM_HOME}/bin:${PATH}"
export LD_LIBRARY_PATH="${DM_HOME}/bin:${DM_HOME}/lib:/home/dmdba/dmdbms/bin:/home/dmdba/dmdbms/lib:/opt/dmdbms/bin:/opt/dmdbms/lib:${LD_LIBRARY_PATH:-}"

if [[ ! -x "${DISQL_BIN}" ]]; then
  chmod +x "${DISQL_BIN}" 2>/dev/null || true
fi

if [[ ! -x "${DISQL_BIN}" ]]; then
  echo "错误：disql 不可执行：${DISQL_BIN}" >&2
  exit 1
fi

echo "已找到 disql：${DISQL_BIN}"
echo "DM_HOME：${DM_HOME}"
echo "LD_LIBRARY_PATH：${LD_LIBRARY_PATH}"

if ! "${DISQL_BIN}" -h >/dev/null 2>&1; then
  echo "提示：disql -h 执行失败，开始检查动态库依赖。" >&2
  if command -v ldd >/dev/null 2>&1; then
    ldd "${DISQL_BIN}" || true
  fi
fi

if [[ ! -d "${SQL_DIR}" ]]; then
  echo "错误：SQL 目录不存在：${SQL_DIR}" >&2
  echo "请确认 docker-compose.dm8.yml 已挂载：./sql/dm8:/dm8-init:ro" >&2
  exit 1
fi

mapfile -t sql_files < <(find "${SQL_DIR}" -maxdepth 1 -type f -name '*.sql' | sort)
if [[ ${#sql_files[@]} -eq 0 ]]; then
  echo "未发现 SQL 文件：${SQL_DIR}/*.sql"
  exit 0
fi

echo "等待 DM8 可登录，超时时间：${WAIT_TIMEOUT_SECONDS}s"
start_ts="$(date +%s)"
while true; do
  if "${DISQL_BIN}" "${DM_USER}/${DM_PASSWORD}" <<'EOSQL' >/tmp/dm8-login-check.log 2>&1
select 1;
exit
EOSQL
  then
    break
  fi

  now_ts="$(date +%s)"
  if (( now_ts - start_ts >= WAIT_TIMEOUT_SECONDS )); then
    echo "错误：等待 DM8 登录成功超时。最近一次输出如下：" >&2
    cat /tmp/dm8-login-check.log >&2 || true
    exit 1
  fi

  sleep 2
done

echo "发现 SQL 文件数量：${#sql_files[@]}"

for sql_file in "${sql_files[@]}"; do
  echo "========================================"
  echo "执行 SQL：${sql_file}"
  echo "========================================"

  "${DISQL_BIN}" "${DM_USER}/${DM_PASSWORD}" <<EOSQL
start ${sql_file}
exit
EOSQL

  echo "执行完成：${sql_file}"
done

echo "验证 DM8 运行时业务表。"
verify_output="$("${DISQL_BIN}" "${DM_USER}/${DM_PASSWORD}" <<'EOSQL'
select 'PAYMENT_DEMO_REQUIRED_TABLE_COUNT=' || count(*) from user_tables
where upper(table_name) in (
  'T_PAYMENT_CHANNEL',
  'T_PAYMENT_APP',
  'T_ORDER_INFO',
  'T_PAYMENT_INFO',
  'T_PRODUCT',
  'T_REFUND_INFO'
);
exit
EOSQL
)"
echo "${verify_output}"

if ! grep -q "PAYMENT_DEMO_REQUIRED_TABLE_COUNT=6" <<<"${verify_output}"; then
  echo "错误：DM8 初始化后未发现全部必需业务表。请确认应用连接的 schema 与初始化用户一致。" >&2
  "${DISQL_BIN}" "${DM_USER}/${DM_PASSWORD}" <<'EOSQL' >&2 || true
select table_name from user_tables
where upper(table_name) in (
  'T_PAYMENT_CHANNEL',
  'T_PAYMENT_APP',
  'T_ORDER_INFO',
  'T_PAYMENT_INFO',
  'T_PRODUCT',
  'T_REFUND_INFO'
)
order by table_name;
exit
EOSQL
  exit 1
fi

echo "DM8 SQL 初始化完成。"
EOSH
