@echo off
setlocal
cd /d %~dp0\..
docker compose down
docker compose -f docker-compose.kafka.yml down
endlocal
