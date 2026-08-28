#!/usr/bin/env bash
set -euo pipefail

: "${ZK_QUORUM:=zookeeper}"
: "${ZK_PORT:=2181}"

mkdir -p /data/hbase

echo "[hbase] waiting zookeeper ${ZK_QUORUM}:${ZK_PORT} ..."
for i in {1..60}; do
  if echo ruok | nc -w 2 "${ZK_QUORUM}" "${ZK_PORT}" | grep -q imok; then
    echo "[hbase] zookeeper is ready."
    break
  fi
  sleep 1
done

echo "[hbase] starting..."
/opt/hbase/bin/start-hbase.sh

echo "[hbase] started, tail logs..."
tail -F /opt/hbase/logs/*.log
