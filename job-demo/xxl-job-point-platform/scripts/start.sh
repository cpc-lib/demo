#!/usr/bin/env bash
set -euo pipefail
docker compose up -d --build
echo "Frontend: http://localhost:3000"
echo "Backend : http://localhost:8088/api/dashboard"
echo "XXL-JOB : http://localhost:8080 (admin / 123456)"
echo "In XXL-JOB create handler dailyPointRewardJob; route=SHARDING_BROADCAST; block=SERIAL_EXECUTION; cron=0 0 1 * * ?"
