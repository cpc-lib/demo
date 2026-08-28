#!/usr/bin/env bash
set -euo pipefail

mkdir -p /data /datalog

echo "[zookeeper] starting..."
exec /opt/zookeeper/bin/zkServer.sh start-foreground
