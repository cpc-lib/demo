@echo off
setlocal
cd /d %~dp0\..
docker compose -f docker-compose.kafka.yml up -d redis kafka
endlocal
