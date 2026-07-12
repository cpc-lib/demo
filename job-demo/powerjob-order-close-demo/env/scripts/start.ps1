$ErrorActionPreference = "Stop"
Set-Location (Join-Path $PSScriptRoot "..")
docker compose up -d --force-recreate
docker compose ps
docker compose logs --tail=120 powerjob-server
