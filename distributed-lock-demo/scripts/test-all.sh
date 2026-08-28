#!/usr/bin/env bash
set -euo pipefail
BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"

echo "== Unsafe baseline =="
curl -sS -X POST "$BASE_URL/api/demo/concurrent-unsafe?productId=1001&initialStock=20&requests=20"
echo

for provider in redisson zookeeper mysql; do
  echo "== $provider =="
  curl -sS -X POST "$BASE_URL/api/demo/concurrent/$provider?productId=1001&initialStock=20&requests=20"
  echo
done
