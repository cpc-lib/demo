@echo off
setlocal
cd /d %~dp0\..
docker compose -f env\docker-compose.yml up -d redis mysql minio rabbitmq
endlocal
